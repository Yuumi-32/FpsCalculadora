# `:core` — o cérebro do app

Módulo Kotlin **puro** (sem Android): base de dados de hardware/jogos + o cálculo
de FPS. Não depende do WebView, não depende de nada do Android, e os testes rodam
na JVM em segundos.

Existe para que a lógica que define o produto inteiro — a `calc()` — pare de morar
dentro de um `index.html` de 3.550 linhas sem um único teste.

## O contrato

A migração **não pode mudar nenhum FPS**. Isso não é uma promessa, é um teste:
`GoldenParityTest` roda 3.975 combinações de hardware e 1.200 normalizações de
estado contra vetores gerados da implementação JavaScript original e compara
número por número — incluindo cada passo intermediário da cadeia de multiplicação,
que é o que pega divergência de ordem de operação que o resultado arredondado
esconderia.

Ninguém digitou esses vetores à mão: `tools/gen-golden.mjs` executa a `calc()` do
`index.html` num sandbox `node:vm` e grava o que ela produz.

## Fluxo enquanto a UI web existir

O `index.html` continua sendo a fonte da verdade. Mexeu nas tabelas (`G`, `C`,
`GP`, `MB`) ou no cálculo? Regenere:

```bash
node tools/extract-data.mjs && node tools/gen-golden.mjs && ./gradlew :core:test
```

- `extract-data.mjs` → reescreve os JSON em `src/main/resources/data/`
- `gen-golden.mjs` → reescreve os vetores em `src/test/resources/`
- o teste falha se o Kotlin não acompanhar

Quando a UI nativa assumir, os JSON viram a fonte, o extrator é aposentado e os
vetores golden congelam como teste de regressão histórico.

## O que está nos dados e o que está no código

| | Onde | Por quê |
|---|---|---|
| Jogos, CPUs, GPUs, placas-mãe, presets, upscalers, eficiência de RT por geração | `resources/data/*.json` | Catálogo: muda quando lança hardware novo, sem tocar em código |
| Penalidade de VRAM, fator de 1% low, folga da fonte, teto de CPU padrão | constantes nomeadas no Kotlin | Modelo: é a regra de estimativa, não dado |

## Ids estáveis

O app hoje referencia hardware por **índice de array** (`st.gpu = 45`). Inserir uma
GPU no meio da lista invalida silenciosamente todo build salvo e todo código
compartilhado entre amigos.

Cada item agora tem um `id` derivado do nome (`rtx-5070-ti`), e é o que a
`BuildState` usa. O campo `index` continua no JSON e é validado pelos testes, só
para conseguir ler os builds salvos no formato antigo.

## Rodando

```bash
./gradlew :core:test
```
