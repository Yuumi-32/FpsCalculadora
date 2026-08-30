# FPS Calculadora

App Android que estima o desempenho (FPS) de um PC gamer a partir da combinação de CPU e GPU escolhidas, para uma base com dezenas de jogos. O cálculo roda inteiro no aparelho; a internet é usada apenas para atualizar o catálogo de peças, jogos e preços.

<p align="center">
  <img src="docs/screenshots/onboarding.png" width="200" alt="Tela de boas-vindas com presets de PC" />
  <img src="docs/screenshots/calculadora.png" width="200" alt="Calculadora de FPS" />
  <img src="docs/screenshots/jogos.png" width="200" alt="Lista de jogos com FPS estimado" />
  <img src="docs/screenshots/upgrade.png" width="200" alt="Sugestão de upgrade de peça" />
</p>

## Funcionalidades

- **Calculadora de FPS** — escolha CPU (Ryzen/Intel Core), GPU (GeForce/Radeon/Arc), resolução (1080p/1440p/4K), taxa de atualização do monitor, preset gráfico, Ray Tracing, Frame Generation e upscaling (DLSS/FSR/XeSS), e veja o FPS médio, 1% low e máximo estimados.
- **Seu PC em todos os jogos** — ranking do FPS estimado da sua build atual em toda a base de jogos cadastrada, ordenável por desempenho ou nome.
- **O que trocar primeiro?** — mostra se o gargalo é CPU ou GPU e quanto cada upgrade de peça ganharia em FPS.
- **Comparar builds** — coloca a build atual lado a lado com uma build salva no histórico.
- **Histórico local** — salva builds no aparelho e permite exportar/importar configurações por código de texto.
- **Recomendação de fonte** — potência mínima e recomendada com selo 80 Plus sugerido para a build.
- Onboarding com presets prontos (Econômico / Médio / Máximo) por resolução, para já sair vendo um resultado.

## Como funciona a estimativa

Os números partem de uma configuração de referência (RTX 5070 + Ryzen 7 5700X + B550 + DDR4 32GB, DLSS Balanceado = escala 1.00×) e aplicam multiplicadores por CPU, GPU, resolução, preset gráfico, Ray Tracing/Frame Generation e upscaling sobre a base de dados de jogos embutida no app. São **estimativas de referência**, não benchmarks — o desempenho real varia por jogo, drivers e configuração específica de cada PC.

## Tecnologia

A interface é nativa, em Jetpack Compose. O módulo `:core`, em Kotlin puro, guarda a base de dados em JSON e o cálculo de FPS, coberto por testes de paridade que comparam 3.975 combinações de hardware contra a implementação original. O cálculo, a base de jogos e o histórico rodam no aparelho.

A UI antiga — uma WebView carregando `app/src/debug/assets/www/index.html`, com dados e lógica em JavaScript — continua no repositório **apenas no build de debug**, como referência de comparação lado a lado. O arquivo mora em `src/debug/` justamente para não viajar no APK publicado. O `index.html` segue sendo a fonte de onde `tools/extract-data.mjs` gera os JSON do `:core` e `tools/gen-golden.mjs` gera os vetores de teste.

### Catálogo remoto

O app declara `INTERNET` e `ACCESS_NETWORK_STATE` para baixar
[`docs/catalogo.json`](docs/catalogo.json), servido pelo GitHub Pages do projeto, e não depender da base congelada no APK. Ele resolve as duas coisas que envelhecem sozinhas depois de publicar: **preço das peças** e **peça nova no mercado**.

A ordem de preferência é rede → cache → base embutida, e a queda de um nível para o outro é silenciosa: este é um app offline que fica melhor com internet, não um app online que tolera ficar sem. `CatalogUpdater` nunca lança, e uma falha de rede não tem efeito além de a tela continuar mostrando o que já mostrava.

Cuidados que valem conhecer antes de mexer:

- **peça nova é anexada ao fim da lista, nunca inserida no meio.** Builds salvas e códigos compartilhados referenciam hardware por índice de array, então inserir no meio converteria a máquina guardada de alguém em outra — sem erro nenhum aparecer. Tem teste próprio;
- **o catálogo remoto não mexe no miolo do cálculo.** Multiplicadores de peças existentes, jogos, placas-mãe e constantes ficam de fora: são o que os testes de paridade cobrem, e permitir troca remota mudaria o resultado do cálculo sem passar por teste;
- **preço é sempre média, nunca cotação.** `roundToAverage` joga a precisão fora antes de a UI ver o número, e `formatAveragePrice` sai sempre com `≈`. A regra é coberta por varredura em `PriceTest`, não por convenção;
- **a rede é do código Kotlin, nunca da WebView.** O `shouldInterceptRequest` da `MainActivity` recusa qualquer subrecurso fora de `appassets.androidplatform.net`, e o `network_security_config` proíbe texto claro.

Manutenção do arquivo publicado: [`docs/play/06-catalogo-remoto.md`](docs/play/06-catalogo-remoto.md).

As 5 telas (Calcular, Seu PC em todos os jogos, O que trocar primeiro, Comparar builds, Histórico) estão em Compose. Ver [`core/README.md`](core/README.md).

| | |
|---|---|
| Linguagem | Kotlin (core, UI Compose) · Java (Activity do WebView, só no debug) |
| UI | Jetpack Compose — WebView antiga preservada no build de debug |
| minSdk / targetSdk | 24 / 36 |
| Android Gradle Plugin | 8.13.0 · Gradle 8.14.3 |

## Estrutura do projeto

```
app/src/main/                                    # módulo Android
├── java/…/ComposeMainActivity.kt                # Activity publicada: UI Compose
├── java/…/MainActivity.java                     # WebView antiga (só no build de debug)
├── java/…/ui/                                    # telas, componentes, tema
├── java/…/data/CatalogRepository.kt             # cache + rede + base embutida
├── assets/www/index.html                        # UI antiga e fonte dos dados do :core
├── res/xml/network_security_config.xml          # proíbe texto claro
└── AndroidManifest.xml

core/                                            # módulo Kotlin puro, sem Android
├── src/main/resources/data/*.json               # jogos, CPUs, GPUs, placas-mãe
├── src/main/kotlin/…/FpsCalculator.kt           # o cálculo de FPS
├── src/main/kotlin/…/RemoteCatalog.kt           # fusão do catálogo remoto com a base
├── src/main/kotlin/…/Price.kt                   # média de mercado, nunca cotação
└── src/test/…                                   # paridade contra o JS + catálogo e preço

docs/catalogo.json                               # catálogo publicado no GitHub Pages
tools/*.mjs                                      # extraem dados e vetores do index.html; geram o catálogo
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
