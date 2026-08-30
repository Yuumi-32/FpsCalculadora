package com.fps.calculadora;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.ContentValues;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Base64;
import android.util.Log;
import android.webkit.ConsoleMessage;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.view.View;
import android.view.Window;
import android.widget.Toast;

import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.webkit.WebViewAssetLoader;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.OutputStream;

public class MainActivity extends Activity {

    /** Domínio reservado do Google para servir assets locais como se fossem https:// de verdade. */
    private static final String APP_ORIGIN = "https://appassets.androidplatform.net";

    /** O host do APP_ORIGIN — o único que a WebView tem permissão de buscar. */
    private static final String APP_HOST = "appassets.androidplatform.net";

    private WebView webView;

    @SuppressLint("SetJavaScriptEnabled")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        SplashScreenBridge.install(this);
        super.onCreate(savedInstanceState);

        requestWindowFeature(Window.FEATURE_NO_TITLE);

        setContentView(R.layout.activity_main);
        webView = findViewById(R.id.webview);

        // A partir do targetSdk 35 o Android desenha sob as barras do sistema e
        // ignora FLAG_FULLSCREEN. Sem isto, a topbar do HTML fica atrás do
        // relógio e a navegação inferior atrás da barra de gestos.
        //
        // O padding vai no container, não na WebView: setPadding() numa WebView
        // não desloca o conteúdo da página de forma confiável (ainda mais com
        // useWideViewPort ligado). Aplicado na RelativeLayout, ele encolhe os
        // limites da WebView de verdade.
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.root), (view, windowInsets) -> {
            // O teclado entra junto: com o app desenhando sob as barras, o
            // adjustResize sozinho não reserva mais espaço para o IME e o
            // campo de busca dos bottom sheets ficaria coberto.
            Insets bars = windowInsets.getInsets(
                    WindowInsetsCompat.Type.systemBars()
                            | WindowInsetsCompat.Type.displayCutout()
                            | WindowInsetsCompat.Type.ime());
            view.setPadding(bars.left, bars.top, bars.right, bars.bottom);
            return WindowInsetsCompat.CONSUMED;
        });

        // Configurações do WebView
        WebSettings settings = webView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setCacheMode(WebSettings.LOAD_DEFAULT);
        settings.setLoadWithOverviewMode(true);
        settings.setUseWideViewPort(true);
        settings.setBuiltInZoomControls(false);
        settings.setDisplayZoomControls(false);
        settings.setSupportZoom(false);

        // O conteúdo local passou a vir pelo WebViewAssetLoader (https://appassets...),
        // não mais por file:// — não precisa mais de acesso a arquivo bruto.
        settings.setAllowFileAccess(false);
        settings.setAllowContentAccess(true);

        // Serve os assets locais por https://appassets.androidplatform.net em vez
        // de file:// — dá ao WebView uma origem estável e segura, o que faz o
        // localStorage parar de depender dos detalhes frágeis do file://.
        WebViewAssetLoader assetLoader = new WebViewAssetLoader.Builder()
                .addPathHandler("/assets/", new WebViewAssetLoader.AssetsPathHandler(this))
                .build();

        webView.setWebViewClient(new WebViewClient() {
            @Override
            public WebResourceResponse shouldInterceptRequest(WebView view, WebResourceRequest request) {
                Uri url = request.getUrl();
                WebResourceResponse local = assetLoader.shouldInterceptRequest(url);
                if (local != null) {
                    return local;
                }

                // O app voltou a declarar a permissão INTERNET (catálogo remoto),
                // mas ela é para o código Kotlin — não para cá. O assetLoader
                // devolve null para tudo que não é /assets/, e null significa
                // "carregue normalmente", ou seja: pela rede.
                //
                // Enquanto não havia permissão, esse pedido morria sozinho. Agora
                // ele sairia de verdade, num contexto com JavaScript e DOM storage
                // ligados — então a recusa precisa ser explícita. shouldOverrideUrl
                // Loading() abaixo cobre só a navegação de página; imagem, fetch e
                // XHR não passam por lá.
                String scheme = url.getScheme();
                boolean web = "https".equals(scheme) || "http".equals(scheme);
                if (web && !APP_HOST.equals(url.getHost())) {
                    Log.w("FpsWebView", "pedido de rede bloqueado: " + url);
                    return blocked();
                }

                // data:, blob: e afins seguem o caminho normal — é por aí que sai
                // o PNG do card de resultado.
                return null;
            }

            // Só o próprio app navega dentro da WebView. Qualquer URL de fora
            // abre no navegador do aparelho em vez de carregar aqui.
            //
            // O motivo original era o allowFileAccess, que foi desligado desde
            // então. O que sustenta a regra hoje é o JavaScript ligado somado à
            // permissão de internet que o app voltou a declarar: sem esta
            // trava, uma página externa carregada aqui rodaria script no mesmo
            // contexto do app.
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                if (url != null && url.startsWith(APP_ORIGIN)) {
                    return false;
                }
                try {
                    startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url)));
                } catch (ActivityNotFoundException e) {
                    Toast.makeText(MainActivity.this, "Não há app para abrir esse link", Toast.LENGTH_SHORT).show();
                }
                return true;
            }
        });

        webView.setWebChromeClient(new WebChromeClient() {
            @Override
            public boolean onConsoleMessage(ConsoleMessage msg) {
                Log.i("FpsWebConsole", msg.message() + " -- " + msg.sourceId() + ":" + msg.lineNumber());
                return true;
            }
        });

        // O card de resultado (botão "Baixar PNG") é um <a download> com uma
        // data URI. Sem DownloadListener, o WebView não tem pra onde mandar
        // esse toque — o clique simplesmente não faz nada.
        webView.setDownloadListener((url, userAgent, contentDisposition, mimeType, contentLength) ->
                saveDataUriToGallery(url, mimeType));

        // Carrega o HTML da pasta assets/www/, servido pelo WebViewAssetLoader.
        webView.loadUrl(APP_ORIGIN + "/assets/www/index.html");
    }

    /** Resposta vazia para o que a WebView não tem o direito de buscar. */
    private static WebResourceResponse blocked() {
        // Stream novo a cada chamada: um InputStream só pode ser lido uma vez.
        return new WebResourceResponse("text/plain", "utf-8", new ByteArrayInputStream(new byte[0]));
    }

    private void saveDataUriToGallery(String dataUri, String mimeType) {
        int comma = dataUri == null ? -1 : dataUri.indexOf(',');
        byte[] bytes = null;
        if (comma >= 0) {
            try {
                bytes = Base64.decode(dataUri.substring(comma + 1), Base64.DEFAULT);
            } catch (IllegalArgumentException e) {
                bytes = null;
            }
        }
        if (bytes == null) {
            Toast.makeText(this, "Não foi possível salvar a imagem", Toast.LENGTH_SHORT).show();
            return;
        }
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            // MediaStore.RELATIVE_PATH só existe a partir do Android 10; antes
            // disso salvar em galeria pública exigiria a permissão em tempo de
            // execução WRITE_EXTERNAL_STORAGE, que este app não pede.
            Toast.makeText(this, "Baixar imagem exige Android 10 ou mais recente", Toast.LENGTH_LONG).show();
            return;
        }

        ContentValues values = new ContentValues();
        values.put(MediaStore.Images.Media.DISPLAY_NAME, "fps-estimado-" + System.currentTimeMillis() + ".png");
        values.put(MediaStore.Images.Media.MIME_TYPE, mimeType != null ? mimeType : "image/png");
        values.put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/FPS Calculadora");

        Uri uri = getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);
        if (uri == null) {
            Toast.makeText(this, "Não foi possível salvar a imagem", Toast.LENGTH_SHORT).show();
            return;
        }
        try (OutputStream out = getContentResolver().openOutputStream(uri)) {
            if (out == null) throw new IOException("openOutputStream retornou null");
            out.write(bytes);
            Toast.makeText(this, "Imagem salva em Fotos → FPS Calculadora", Toast.LENGTH_SHORT).show();
        } catch (IOException e) {
            Toast.makeText(this, "Não foi possível salvar a imagem", Toast.LENGTH_SHORT).show();
        }
    }

    // Botão Voltar navega no histórico em vez de fechar o app
    @Override
    public void onBackPressed() {
        if (webView.canGoBack()) {
            webView.goBack();
        } else {
            super.onBackPressed();
        }
    }

    // Preserva o estado da WebView caso o processo seja recriado (ex.: o
    // sistema mata o app em segundo plano por memória) — não mais por
    // rotação de tela, que ficou travada em retrato.
    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        webView.saveState(outState);
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        webView.restoreState(savedInstanceState);
    }
}
