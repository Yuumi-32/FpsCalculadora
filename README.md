# FPS Calculadora

App Android que estima o desempenho (FPS) de um PC gamer a partir da combinação de CPU e GPU escolhidas, para uma base com dezenas de jogos. O cálculo roda inteiro no aparelho; a internet é usada apenas para atualizar o catálogo de peças, jogos e preços.

<p align="center">
  <img src="docs/screenshots/onboarding.png" width="200" alt="Tela de boas-vindas com presets de PC" />
  <img src="docs/screenshots/calculadora.png" width="200" alt="Calculadora de FPS" />
  <img src="docs/screenshots/jogos.png" width="200" alt="Lista de jogos com FPS estimado" />
  <img src="docs/screenshots/upgrade.png" width="200" alt="Sugestão de upgrade de peça" />
</p>

## Funcionalidades

- **Calculadora de FPS** — escolha CPU (Ryzen/Intel Core), GPU (GeForce/Radeon), resolução (1080p/1440p/4K), taxa de atualização do monitor, preset gráfico, Ray Tracing, Frame Generation e upscaling (DLSS/FSR), e veja o FPS médio, 1% low e máximo estimados.
- **Seu PC em todos os jogos** — ranking do FPS estimado da sua build atual em toda a base de jogos cadastrada, ordenável por desempenho ou nome.
- **O que trocar primeiro?** — mostra se o gargalo é CPU ou GPU e quanto cada upgrade de peça ganharia em FPS.
- **Comparar builds** — coloca a build atual lado a lado com uma build salva no histórico.
- **Histórico local** — salva builds no aparelho e permite exportar/importar configurações por código de texto.
- **Recomendação de fonte** — potência mínima e recomendada com selo 80 Plus sugerido para a build.
- Onboarding com presets prontos (Econômico / Médio / Máximo) por resolução, para já sair vendo um resultado.

## Como funciona a estimativa

Os números partem de uma configuração de referência (RTX 5070 + Ryzen 7 5700X + B550 + DDR4 32GB, DLSS Balanceado = escala 1.00×) e aplicam multiplicadores por CPU, GPU, resolução, preset gráfico, Ray Tracing/Frame Generation e upscaling sobre a base de dados de jogos embutida no app. São **estimativas de referência**, não benchmarks — o desempenho real varia por jogo, drivers e configuração específica de cada PC.

## Tecnologia

A interface é uma WebView em tela cheia carregando uma única página HTML/CSS/JS (`app/src/main/assets/www/index.html`), totalmente embutida no APK. O cálculo, a base de jogos e o histórico rodam localmente no aparelho.

O app declara `INTERNET` e `ACCESS_NETWORK_STATE` para baixar o catálogo publicado em <https://yuumi-32.github.io/FpsCalculadora/> e não depender da base congelada no APK. Esse pedido sai do código Kotlin, nunca da WebView: o `shouldInterceptRequest` da `MainActivity` recusa qualquer subrecurso que não venha de `appassets.androidplatform.net`, e o `network_security_config` proíbe texto claro. **O download em si ainda não está implementado**: a permissão e o cerco de segurança estão prontos, o cliente HTTP e o catálogo publicado não.

O app está **migrando para UI nativa em Jetpack Compose**. O módulo `:core`, em Kotlin puro, guarda a base de dados em JSON e o cálculo de FPS, coberto por testes de paridade que comparam 3.975 combinações de hardware contra a implementação original. As 5 telas (Calcular, Seu PC em todos os jogos, O que trocar primeiro, Comparar builds, Histórico) já existem em Compose e convivem com o WebView — no build de debug as duas aparecem como ícones separados. Ver [`core/README.md`](core/README.md).

| | |
|---|---|
| Linguagem | Kotlin (core, UI Compose) · Java (Activity do WebView) |
| UI | WebView + HTML/CSS/JS embutidos — Compose em migração |
| minSdk / targetSdk | 24 / 36 |
| Android Gradle Plugin | 8.13.0 · Gradle 8.14.3 |

## Estrutura do projeto

```
app/src/main/                                    # módulo Android
├── java/com/fps/calculadora/MainActivity.java   # Activity: WebView em tela cheia
├── assets/www/index.html                        # UI completa (HTML/CSS/JS, dados e lógica)
├── res/                                          # ícones, layout, strings, tema
└── AndroidManifest.xml

core/                                            # módulo Kotlin puro, sem Android
├── src/main/resources/data/*.json               # jogos, CPUs, GPUs, placas-mãe
├── src/main/kotlin/…/FpsCalculator.kt           # o cálculo de FPS
└── src/test/…                                   # testes de paridade contra o JS

tools/*.mjs                                      # extraem os dados e os vetores de teste do index.html
```

## Como rodar

Pré-requisitos: [Android Studio](https://developer.android.com/studio) (recomendado) ou JDK 17+ com Android SDK (`compileSdk 34`).

```bash
git clone https://github.com/Yuumi-32/FpsCalculadora.git
```

Abra a pasta no Android Studio e deixe o Gradle sincronizar (usa o wrapper, sem configuração adicional), depois rode em um emulador ou dispositivo físico.

Ou via linha de comando (o wrapper já vem no repositório, não precisa instalar Gradle):

```bash
./gradlew assembleDebug
```

O APK debug é gerado em `app/build/outputs/apk/debug/` (`applicationId` com sufixo `.debug`).

Para rodar os testes do cálculo de FPS:

```bash
./gradlew :core:test
```

## Build de release

O pacote enviado à Play Store é um AAB assinado. A chave de assinatura não fica no repositório — gere a sua uma única vez:

```bash
keytool -genkeypair -v -keystore fps-calculadora-upload.jks -keyalg RSA -keysize 2048 -validity 10000 -alias upload
```

Copie `keystore.properties.example` para `keystore.properties`, aponte `storeFile` para o arquivo gerado e preencha as senhas. Os dois estão no `.gitignore`.

> Guarde a keystore e as senhas com backup. Quem tiver a chave de upload assina pacotes no seu nome, e perdê-la exige um processo de recuperação junto ao Google.

```bash
./gradlew bundleRelease
```

O AAB sai em `app/build/outputs/bundle/release/`. Sem `keystore.properties` completo o build ainda conclui, mas gera um pacote não assinado, que a Play recusa.

## Publicação na Play Store

O checklist do lançamento vive em [`docs/play/`](docs/play/): respostas prontas para o formulário de Segurança dos Dados, classificação indicativa, público-alvo e anúncios; título e descrições dentro dos limites de caractere; ícone, feature graphic e capturas; e os passos da conta de desenvolvedor e do teste fechado de 14 dias.

A [política de privacidade](https://yuumi-32.github.io/FpsCalculadora/privacidade.html) e a [página do app](https://yuumi-32.github.io/FpsCalculadora/) são servidas pelo GitHub Pages a partir da pasta `docs/`.

Os gráficos da loja são gerados por script, então acompanham qualquer mudança na interface:

```bash
python tools/gen-store-graphics.py
python tools/gen-store-screenshots.py
```

## Aviso

Os valores exibidos são estimativas para referência rápida na hora de montar ou planejar upgrade de um PC, não substituem benchmarks reais.

## Sobre este projeto

Projeto pessoal feito por hobby com apoio de IA, ainda em evolução — sugestões e PRs são bem-vindos.
