# Ficha da loja — textos e gráficos

Textos prontos para colar na Play Console (idioma padrão: **Português (Brasil)**)
e o inventário dos arquivos gráficos. Os limites de caractere são os da Play; os
números entre parênteses são o tamanho do texto abaixo.

---

## Título do app — máx. 30 caracteres

```
FPS Calculadora: PC Gamer
```
(25 caracteres)

Alternativas, se quiser testar outra: `FPS Calculadora` (15) · `Calculadora de
FPS para PC` (26).

> O nome dentro do app hoje é só **"Fps"** (`app/src/main/res/values/strings.xml`).
> O título da loja pode ser diferente do nome do launcher, mas se quiser os dois
> iguais é aí que se muda.

---

## Descrição curta — máx. 80 caracteres

```
Quantos FPS seu PC faz em cada jogo? Estimativa offline, sem anúncios.
```
(70 caracteres)

---

## Descrição completa — máx. 4000 caracteres

```
Vai montar um PC ou trocar uma peça e quer saber o que esperar de FPS antes de gastar? O FPS Calculadora estima o desempenho da sua máquina em dezenas de jogos, direto no celular, sem cadastro e sem anúncios.

Escolha o processador, a placa de vídeo, a placa-mãe e a memória; ajuste a resolução, a taxa do monitor, o preset gráfico, o Ray Tracing, o Frame Generation e o upscaling (DLSS/FSR). O app calcula na hora o FPS médio, o 1% low e o FPS máximo estimados.

O QUE VOCÊ ENCONTRA

• Calculadora de FPS — resultado com FPS médio, 1% low e máximo, mais a leitura de quanto do seu monitor a build aproveita.
• Equilíbrio CPU × GPU — mostra qual peça está segurando a outra e quanta folga sobra.
• Seu PC em todos os jogos — o ranking da sua build em toda a base de jogos, do mais leve ao mais pesado.
• O que trocar primeiro — quanto cada upgrade de processador ou de placa de vídeo somaria de FPS no jogo escolhido.
• Comparar builds — a configuração atual lado a lado com uma salva, jogo a jogo.
• Histórico no aparelho — salve builds, dê nome e exporte por código de texto para levar a outro celular.
• Fonte recomendada — potência mínima e recomendada, com o selo 80 Plus sugerido.
• Builds prontos — pontos de partida Econômico, Médio e Máximo para 1080p, 1440p e 4K.

O CATÁLOGO

Mais de 30 jogos, 69 placas de vídeo (GeForce GTX 10 até RTX 50 e Radeon RX 6000 até RX 9000), 53 processadores (Ryzen 1000 até 9000 e Intel Core 12ª geração até Core Ultra), 21 placas-mãe e as combinações usuais de memória DDR4 e DDR5.

FUNCIONA SEM INTERNET

Toda a base de dados e todo o cálculo estão dentro do aplicativo: dá para usar o app inteiro no modo avião. A internet serve para uma coisa só — baixar a versão mais nova do catálogo de peças, jogos e preços, para os números não envelhecerem junto com a versão instalada.

Nada é enviado para lugar nenhum: não há anúncios, não há rastreamento, não existe conta para criar e o app não pede acesso a localização, contatos, câmera ou aos seus arquivos. As builds que você salva ficam apenas no seu aparelho.

COMO OS NÚMEROS SÃO CALCULADOS

O app parte de uma configuração de referência medida em cada jogo e aplica multiplicadores de processador, placa de vídeo, resolução, preset gráfico, Ray Tracing, Frame Generation e upscaling. A tela "Como calculamos" explica o caminho do número que aparece.

São estimativas de referência para orientar uma compra ou um upgrade, não benchmarks. O desempenho real varia com o jogo, a versão dos drivers, a memória, a temperatura e a configuração específica de cada PC. Use como ponto de partida, não como promessa.

Sugestão de jogo ou de peça que faltou? Escreva para SEU-EMAIL@exemplo.com.
```
(2471 caracteres)

> **Marcas citadas** (Ryzen, Intel Core, GeForce, Radeon, DLSS, FSR e os nomes
> dos jogos) aparecem só para descrever compatibilidade — uso nominativo, que a
> Play aceita. O que não pode é dar a entender que o app é oficial, feito ou
> endossado pela AMD, pela NVIDIA ou por qualquer estúdio. Nada no texto, no
> ícone ou nas capturas sugere isso, e é assim que tem que continuar.

---

## Novidades desta versão — máx. 500 caracteres

```
Primeira versão pública.

• Calculadora de FPS com FPS médio, 1% low e máximo
• Seu PC em todos os jogos, com ranking por desempenho
• O que trocar primeiro: o ganho de cada upgrade de CPU ou GPU
• Comparação entre a build atual e uma salva
• Histórico no aparelho com exportação por código
• Recomendação de fonte com selo 80 Plus

Tudo offline, sem anúncios e sem coletar dados.
```
(380 caracteres)

---

## Gráficos

Todos gerados por script, para poder refazer depois de mudar cor ou texto:

| Arquivo | Tamanho | Onde entra |
|---|---|---|
| [`graficos/icone-512.png`](graficos/icone-512.png) | 512×512 | Ícone do app na loja |
| [`graficos/feature-graphic-1024x500.png`](graficos/feature-graphic-1024x500.png) | 1024×500 | Gráfico de destaque |
| [`screenshots/01-calculadora.png`](screenshots/01-calculadora.png) … `08-boas-vindas.png` | 1080×1920 | Capturas de celular |

```bash
python tools/gen-store-graphics.py && python tools/gen-store-screenshots.py
```

### Ícone

Quadrado inteiro e opaco, sem cantos arredondados no arquivo — a Play arredonda
sozinha, e um ícone que já vem arredondado ganha uma borda escura na loja. O
wordmark ocupa 70% da largura, o que sobra de margem cobre o corte dos cantos.

### Gráfico de destaque

Nada importante encosta na borda: dependendo da tela, a Play corta as laterais.
O medidor da direita e os cards da esquerda ficam dentro da área central segura.

### Capturas

Oito telas em 1080×1920 (9:16), na ordem em que contam a história do app:
calculadora → jogos → upgrade → comparar → histórico → peças → catálogo de GPUs
→ boas-vindas. A Play exige no mínimo 2 e aceita até 8; **a ordem importa**,
porque as duas primeiras são as que aparecem na busca.

As capturas saem do próprio `index.html` do app rodando no Chrome headless, não
de um emulador — mesmo HTML, mesmo CSS, mesmas fontes (Roboto e Roboto Mono).
São telas reais do app com dados de exemplo semeados, não montagens.

> A captura 06 esconde os cards do topo para mostrar a parte de baixo da
> calculadora: rolar a página deixa a imagem preta nesse modo do Chrome. O
> conteúdo é real, só está sem os cards que ficariam acima.

---

## Ainda falta você fazer

- [ ] Trocar `SEU-EMAIL@exemplo.com` no fim da descrição completa
- [ ] Decidir se o nome do launcher deixa de ser "Fps"
- [ ] Conferir as capturas em um aparelho de verdade antes de subir — as fontes do Android podem quebrar linha em lugar diferente
