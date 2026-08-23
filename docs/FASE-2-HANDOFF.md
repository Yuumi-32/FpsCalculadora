# FpsCalculadora — Handoff da Fase 2

> Documento de contexto completo para continuar a migração num chat novo.
> Escrito em 2026-08-23, ao fim da Fase 1.

---

## 1. O projeto

App Android que estima FPS de um PC gamer a partir da combinação CPU + GPU,
para uma base de 36 jogos, **100% offline**. Projeto pessoal, autor único
(`Yuumi-32`), repositório público.

Hoje o app é uma `Activity` que abre um WebView em tela cheia carregando um
único arquivo HTML de 3.550 linhas. A UI é boa — o problema nunca foi o visual,
foi a fundação.

## 2. Por que a migração

O dono do projeto pediu explicitamente "algo melhor que só HTML". Diagnóstico
que motivou o plano:

- **3.550 linhas num arquivo só**: ~860 de CSS, ~580 de dados, ~1.760 de lógica
  e UI misturadas, sem módulos, sem tipos, sem build, e **sem um único teste**
  na `calc()` — que é literalmente o produto inteiro.
- APIs web que **não funcionam** dentro de um WebView (share, download) e que a
  própria UI já contorna pedindo pro usuário "fazer um print".
- `targetSdk 34` impede publicar na Play Store.

### Plano em 3 fases

| Fase | O quê | Status |
|---|---|---|
| 1 | Extrair o cérebro: dados → JSON, `calc()` → Kotlin puro, com testes golden provando paridade | ✅ **Concluída** |
| 2 | UI nativa em Jetpack Compose por cima do `:core` | ⬅️ **Este documento** |
| 3 | O que WebView não dá: widget de tela inicial, dynamic color, base de jogos atualizável | Pendente |

---

## 3. Relatório da Fase 1 (o que já está pronto)

### 3.1 O módulo `:core`

Módulo Kotlin **puro**, sem dependência de Android. Testes rodam na JVM em ~15s.

```
core/
├── README.md                          # fluxo de trabalho do módulo
├── build.gradle                       # Kotlin JVM + kotlinx-serialization, bytecode 17
├── src/main/kotlin/com/fps/calculadora/core/
│   ├── Model.kt          (198 linhas) # tipos + enums do domínio
│   ├── Database.kt        (74 linhas) # carrega os JSON de resources
│   ├── FpsCalculator.kt  (246 linhas) # calc() + psu() portados 1:1
│   └── Options.kt        (118 linhas) # opções dinâmicas + normalize()
├── src/main/resources/data/           # 64 KB — a base de dados
│   ├── games.json        (36 jogos)
│   ├── cpus.json         (53 CPUs)
│   ├── gpus.json         (69 GPUs)
│   ├── mobos.json        (21 placas-mãe)
│   └── constants.json    (presets, upscalers, eficiência de RT, builds prontos)
└── src/test/
    ├── kotlin/.../GoldenParityTest.kt (263 linhas)
    └── resources/                     # 3,4 MB de vetores golden
        ├── golden-calc.json      (3.975 casos)
        └── golden-normalize.json (1.200 casos)
```

### 3.2 A garantia de paridade

**Nada foi copiado à mão.** `tools/lib-extract.mjs` executa os blocos `<script>`
do `index.html` num sandbox `node:vm` com stubs mínimos de DOM. Daí saem tanto
os JSON quanto os vetores de teste.

```bash
node tools/extract-data.mjs   # regenera core/src/main/resources/data/*.json
node tools/gen-golden.mjs     # regenera core/src/test/resources/golden-*.json
./gradlew :core:test          # falha se o Kotlin divergir do JS
```

Cobertura dos 3.975 casos: varredura completa de cada GPU × resolução × modo RT,
cada CPU, cada jogo, cada placa-mãe, cada preset/RAM/upscaler/frame-gen, os 9
builds prontos contra a base inteira, mais 2.500 combinações aleatórias
determinísticas (seed `20260823`) que incluem estados inválidos de propósito.
Dentro disso: 617 casos com teto de CPU, 789 com VRAM estourada, 1.253 com aviso
de GPU.

A comparação inclui **cada passo intermediário** da cadeia de multiplicação
(título, multiplicador e FPS acumulado), não só o resultado final — é o que pega
divergência de ordem de operação que o arredondamento esconderia.

**A suíte foi testada contra si mesma:** alterando duas constantes
(`MAX_MULT 1.24→1.25` e a penalidade de RAM `0.92→0.93`), **3.054 dos 3.975
casos falharam**. Ela pega regressão de verdade.

### 3.3 Decisões tomadas na Fase 1

**Ids estáveis.** O app referencia hardware por índice de array (`st.gpu = 45`).
Cada item agora tem um `id` derivado do nome (`rtx-5070-ti`), e é o que
`BuildState` usa. O campo `index` continua no JSON e é validado pelos testes,
só para conseguir migrar dados salvos no formato antigo (ver §5.4).

**Dados vs. modelo.** Separação deliberada:

| | Onde | Por quê |
|---|---|---|
| Jogos, CPUs, GPUs, placas, presets, upscalers, **eficiência de RT por geração** | `resources/data/*.json` | Catálogo: muda quando lança hardware, sem tocar em código |
| Penalidade de VRAM, fatores de 1% low, folga da fonte, teto de CPU padrão | constantes nomeadas em `FpsCalculator.kt` | Modelo: é a regra de estimativa |

A eficiência de RT por geração estava **enterrada dentro da `calc()`** no JS e
foi promovida a dado (`constants.rtEfficiencyByGen`).

**`jsRound`.** `Math.round` do JS arredonda `.5` para +infinito;
`kotlin.math.round` arredonda para longe do zero. O port usa
`floor(x + 0.5)` explicitamente para eliminar a classe inteira de divergência.

### 3.4 Bônus: o Gradle wrapper

`gradlew`, `gradlew.bat` e `gradle-wrapper.jar` **não estavam versionados** — só
o `.properties`, apontando para um Gradle 8.4 que nem roda em Java 21. O README
mandava rodar `./gradlew assembleDebug` e ninguém conseguia. Wrapper gerado e
fixado em **8.9**; agora funciona num clone limpo.

### 3.5 O que a Fase 1 **não** fez

- `:app` **não** depende de `:core` ainda. De propósito.
- O WebView continua rodando o JS dele, intacto. Nenhum arquivo em
  `app/src/main/` foi tocado.
- Nenhum dos bugs da §6 foi corrigido.

---

## 4. Estado atual do repositório

```
FpsCalculadora/
├── app/                                    # módulo Android (INTOCADO na fase 1)
│   ├── build.gradle                        # AGP 8.2.2, minSdk 24, targetSdk 34, zero deps
│   └── src/main/
│       ├── AndroidManifest.xml
│       ├── java/com/fps/calculadora/MainActivity.java   (85 linhas)
│       ├── assets/www/index.html           (3.550 linhas — a UI inteira)
│       └── res/                            # ícones, layout, tema
├── core/                                   # ✅ fase 1
├── tools/                                  # ✅ fase 1 — extratores Node
├── docs/
│   ├── screenshots/                        # 4 PNGs da UI atual
│   └── FASE-2-HANDOFF.md                   # este arquivo
├── gradlew, gradlew.bat, gradle/wrapper/   # ✅ fase 1
├── build.gradle                            # AGP 8.2.2 + Kotlin 2.2.10 declarados
└── settings.gradle                         # inclui :app e :core
```

### Ambiente de build nesta máquina

- `java` do PATH é 1.8, **velho demais**. Use a JBR do Android Studio:
  ```bash
  export JAVA_HOME="C:/Program Files/Android/Android Studio/jbr"   # Java 21
  ```
- `adb` não está no PATH: `C:/Users/saulo/AppData/Local/Android/Sdk/platform-tools/adb.exe`
- O celular (Samsung, serial `RXCY109SXDL`) tem a variante **debug** instalada:
  pacote `com.fps.calculadora.debug`.

```bash
./gradlew :core:test          # testes de paridade
./gradlew :app:assembleDebug  # APK em app/build/outputs/apk/debug/
```

---

## 5. Fase 2 — o trabalho

**Objetivo:** substituir o WebView por UI nativa em Jetpack Compose, consumindo
o `:core`, sem perder nenhuma funcionalidade nem mudar nenhum número.

### 5.1 Preparação do build

O `:app` hoje é Java puro sem dependência nenhuma. Precisa:

1. Aplicar `org.jetbrains.kotlin.android` (2.2.10, já declarado no
   `build.gradle` raiz) e o plugin do Compose Compiler.
2. `compileOptions` de `1.8` → **17** (o `:core` produz bytecode 17).
3. `implementation project(':core')`.
4. Subir AGP `8.2.2` → **8.6+** e `compileSdk`/`targetSdk` 34 → **35/36**.
   ⚠️ Isso **força edge-to-edge**: `FLAG_FULLSCREEN` vira no-op e o conteúdo
   passa a desenhar sob as barras do sistema. Tem que tratar `WindowInsets`.
5. Ativar `minifyEnabled` no release e criar signing config.

> ⚠️ O `:core` empacota JSON como resource de JAR. Confirme que
> `GameDatabase::class.java.getResourceAsStream("/data/…")` resolve dentro do
> APK — funciona, mas se o R8 remover os resources, a alternativa é mover os
> JSON para `app/src/main/assets/` e trocar o loader por `AssetManager`.

### 5.2 As telas a portar

Navegação por 5 abas (`index.html:1126-1152`), estado em `setTab()`
(`index.html:3432`).

| Aba | HTML | Render JS | O que tem |
|---|---|---|---|
| **Calcular** | `index.html:890-1069` | `renderAll()` :2218 | Gauge, 3 stats, barras de gargalo, comparação por resolução, custo de energia, builds prontos, 5 linhas de seleção de peça, chips de preset/RT/FG/upscaler, recomendação de fonte |
| **Jogos** | `index.html:1071-1078` | `renderGames()` :2824 | Ranking da build atual nos 36 jogos, ordenável por FPS ou nome |
| **Upgrade** | `index.html:1081-1088` | `renderUpg()` :2867 | "O que trocar primeiro" — ganho estimado por peça |
| **Comparar** | `index.html:1090-1110` | `renderComp()` :2652 | Build atual vs. uma do histórico, lado a lado |
| **Histórico** | `index.html:1113-1123` | `renderHist()` :2533 | Builds salvos, exportar/importar por código |

**Componentes compartilhados:**

- **Bottom sheet de seleção** com busca (`openSheet()` :2358, `buildSheetList()`
  :2373). Usado por CPU/GPU/placa/RAM/jogo. → `ModalBottomSheet` do M3.
- **Onboarding** (`buildOnboard()` :3093) — 9 builds prontos por resolução.
- **Perfis de PC** (`openProfilesSheet()` :3365) — múltiplos PCs salvos.
- **Toast** (`toast()` :1792) → `Snackbar`.
- **"Como é calculado"** (`openHowSheet()` :2800) — expõe os `steps` da `calc()`.
  Mapeia direto para `CalcResult.steps`, já pronto no `:core`.
- **Meta de FPS** (`openGoalSheet()` :3208) — busca reversa de upgrade.

### 5.3 Design system

Tokens em `index.html:19-42`. Tema **dark-only** com acento laranja:

| Token | Valor | Uso |
|---|---|---|
| `--bg0 / bg1 / bg2 / bg3` | `#0d0c0a` `#161511` `#1e1d18` `#27261f` | fundo → cards → controles |
| `--tx1 / tx2 / tx3` | `#f2f0e6` `#b3b0a2` `#7d7a70` | texto primário/secundário/terciário |
| `--acc / acc-deep` | `#e8825a` `#c26033` | acento (laranja) |
| `--ok / warn / bad / info` | `#a9d562` `#f0a836` `#f48a8a` `#6cb3ef` | badges de desempenho |
| `--r-lg / md / sm` | `22px` `14px` `10px` | raios de canto |
| `--nav-h` | `62px` | altura da barra inferior |

Fundo do body tem um `radial-gradient` laranja sutil no topo
(`index.html:53-56`). Números usam `Roboto Mono`.

**Widgets customizados** (nenhum tem equivalente pronto no M3):

- **Gauge semicircular** (`GAUGE` :2068, `gaugeArcPath()` :2076, `buildTicks()`
  :2081) — arco de −110° a +110°, raio 120, com marcadores de Hz do monitor
  (`constants.hzMarkers`) e número animado (`animateNumber()` :2112).
  → `Canvas` do Compose; fica **mais fácil** que o SVG atual.
- **Barras de gargalo CPU × GPU** (`buildBars()` :2167, `renderBneck()` :2762).
- **Card de compartilhamento** (`buildShareCanvas()` :2971) — desenha um PNG
  1080×1350 no `<canvas>`. → `Canvas` + `Bitmap`, e aí o share nativo
  finalmente funciona (§6).

Os 4 screenshots em `docs/screenshots/` mostram o alvo visual.

### 5.4 Migração de dados salvos

⚠️ **Ponto de atenção.** Chaves no `localStorage` do WebView:

| Chave | Conteúdo | Formato |
|---|---|---|
| `fps:state:v2` | estado atual | **índices** (`{game:2, cpu:16, gpu:45, mobo:3, …}`) |
| `fps:hist:v1` | até 30 builds salvos | `{id, ts, s:{...estado}}` — **índices** |
| `fps:profiles:v1` | perfis de PC | `{list:[{id,name,s:{...}}], active}` — **índices** |
| `fps:onboard:v1` | flag de onboarding visto | `'1'` |

O **código de compartilhamento** (`buildCode()` :3278) é o único que guarda
**nomes**, não índices — então códigos antigos continuam válidos e só o
`localStorage` precisa de migração.

> Correção de uma afirmação feita na Fase 1: eu disse que reordenar o catálogo
> invalidaria "builds salvos **e códigos compartilhados**". Só os salvos. O
> formato `FPS1.` resolve por nome (`G.findIndex(x => x.name === o.g)`).

**Plano de migração:** ler as 4 chaves do WebView uma última vez (via
`WebStorage`/`evaluateJavascript` numa Activity de migração, ou reescrevendo o
`index.html` para exportar tudo num `@JavascriptInterface`), converter índice →
id com o campo `index` dos JSON, gravar em DataStore e marcar como migrado.
Sem isso, todo mundo que já usa o app perde o histórico.

### 5.5 Ordem sugerida

1. Preparar o build (§5.1) e uma Activity Compose vazia atrás de uma flag.
2. Tela **Calcular** — é 70% do app. Começar pelo gauge.
3. Telas **Jogos** e **Upgrade** (listas simples, pouco risco).
4. **Comparar** e **Histórico** + migração do `localStorage` (§5.4).
5. Sheets, onboarding, perfis, share nativo.
6. Remover o WebView, o `index.html` e a permissão `INTERNET`; aposentar
   `tools/extract-data.mjs` e promover os JSON a fonte da verdade.

### 5.6 Regras invioláveis

1. **Nenhum número pode mudar.** `./gradlew :core:test` tem que ficar verde. Se
   a Fase 2 precisar de um cálculo novo, ele entra no `:core` com teste próprio
   — nunca dentro de um Composable.
2. **Enquanto o `index.html` existir, ele é a fonte da verdade dos dados.**
   Mexeu nas tabelas dele? Rode os dois extratores e o teste.
3. **Toda lógica nova vai pro `:core`**, não pra camada de UI.

---

## 6. Bugs conhecidos, ainda abertos

Independentes da Fase 2, todos verificados no código:

| # | Onde | O quê |
|---|---|---|
| 1 | `index.html:7` e `:3543` | Referenciam `manifest.json` e `sw.js`; **nenhum dos dois existe** em `assets/www/`. Dois 404 + erro no console toda abertura. Sobra de quando isso era PWA. |
| 2 | `index.html:3053` | Botão de compartilhar **nunca funciona**: `navigator.share` não existe em WebView Android. Cai sempre no fallback "Baixar PNG"… |
| 3 | `MainActivity.java` | …e o fallback também não funciona: sem `setDownloadListener`, clicar num `<a download>` no WebView não faz nada. Por isso a UI diz "faça um print da prévia". Resolve com `@JavascriptInterface` → `MediaStore` + `Intent.ACTION_SEND`. |
| 4 | `MainActivity.java:52` | `shouldOverrideUrlLoading` faz `loadUrl(url)` para **qualquer** URL, inclusive `http://` externo, com JS ligado. |
| 5 | `AndroidManifest.xml:6` | Permissão `INTERNET` declarada e **nunca usada**. |
| 6 | `MainActivity.java:44` | `setAllowFileAccess(true)` + origem `file://`. O certo é `WebViewAssetLoader` com origem `https://`. |
| 7 | `app/build.gradle` | `targetSdk 34` bloqueia a Play Store (35 exigido desde ago/2025, 36 a partir de ago/2026). |
| 8 | `MainActivity.java` | `Activity` cru, `FLAG_FULLSCREEN` e `onBackPressed()` — todos deprecados; falta `OnBackInvokedCallback` (predictive back). |
| 9 | `app/build.gradle` | Release com `minifyEnabled false`, sem signing config, `versionCode` fixo em 1. |

**Nota:** os itens 2, 3 e 6 **desaparecem sozinhos** quando o WebView sair na
Fase 2. Os itens 1, 4 e 5 são limpeza de 5 minutos. Os 7, 8 e 9 são obrigatórios
para publicar, e o 8 se resolve naturalmente ao adotar Compose.

---

## 7. Ideias de produto (fora do escopo da migração)

- **Busca na aba Jogos** — 36 jogos já pede filtro; as listas de CPU/GPU têm
  busca, a de jogos não.
- **Modo reverso** — "quero 144 FPS em 1440p no Cyberpunk, qual GPU?". O
  `openGoalSheet()` (:3208) já chega perto; dá pra virar feature principal.
- **R$ por FPS** — nicho brasileiro, seria o diferencial real. Precisa de base
  de preços.

---

## 8. Prompt sugerido para o chat da Fase 2

> Estou migrando o app FpsCalculadora (C:\Games\FpsCalculadora) de WebView para
> Jetpack Compose. A Fase 1 já terminou: existe um módulo `:core` em Kotlin puro
> com a base de dados em JSON e o cálculo de FPS portado, coberto por testes de
> paridade contra a implementação JS original.
>
> Leia `docs/FASE-2-HANDOFF.md` e `core/README.md` antes de qualquer coisa.
> Depois comece pela §5.1 (preparação do build) e pela tela Calcular.
>
> Regra número um: `./gradlew :core:test` tem que ficar verde. Nenhum número
> pode mudar.
