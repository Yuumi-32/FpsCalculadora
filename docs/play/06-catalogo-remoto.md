# Catálogo remoto — como manter preços e peças novas

O app baixa `https://yuumi-32.github.io/FpsCalculadora/catalogo.json` para não
depender só da base congelada dentro do APK. Este documento é o manual de
operação desse arquivo.

Ele resolve as duas coisas que envelhecem sozinhas depois que a versão está
publicada:

| O que envelhece | Com que frequência | Campo |
|---|---|---|
| Preço das peças | toda semana, com dólar e promoção | `prices.byId` |
| Peça nova no mercado | a cada poucos meses, quando lança geração | `newCpus` / `newGpus` |

São campos separados justamente porque mudam em ritmos diferentes: dá para
publicar uma revisão de preço sem tocar na lista de peças.

---

## A regra que não pode ser quebrada

**O app apresenta esses números como média de mercado.** A tela escreve "≈ R$
4.200", com a data da amostragem ao lado, porque essa é a única coisa que ele
pode honestamente prometer sobre preço.

Isso significa:

- **não chute.** Preço inventado é pior que preço nenhum — sem preço a tela só
  não mostra o custo-benefício, com preço errado ela mostra um ranking errado
  com cara de certeza;
- **preencha `sampledOn` toda vez que mexer nos preços.** É a data que aparece
  na tela. Preço sem data é preço mentiroso;
- **preencha `method`** com uma linha dizendo como a média saiu. Aparece na
  tela junto do preço.

O código já força parte disso: valores fora da faixa plausível são descartados,
e o app arredonda tudo antes de exibir, para nenhum número parecer uma cotação
de loja. Mas nada disso salva um preço chutado que *parece* plausível.

---

## Levantando os preços

Um método que dá uma média defensável, sem virar um projeto:

1. Para cada peça, abra três varejistas grandes que entreguem no Brasil inteiro;
2. Anote o preço à vista de cada uma, ignorando frete e cupom;
3. Use a **mediana** das três, não a média — uma promoção agressiva ou um
   vendedor fora da curva distorce a média e não mexe na mediana;
4. Arredonde para a dezena. O app arredonda de novo na exibição, mas guardar
   já arredondado deixa claro no arquivo que é estimativa.

Anote em `method` o que você fez, por exemplo:
`"mediana de três varejistas nacionais, preço à vista, sem frete"`.

Peça fora de linha que só aparece usada: **deixe sem preço**. O app lida bem
com isso — a tela simplesmente não oferece custo-benefício para ela. Misturar
preço de usado com preço de novo faz a peça velha parecer um negócio
espetacular no ranking.

---

## Editando o arquivo

Gere o esqueleto (preserva o que já estiver preenchido):

```bash
node tools/gen-catalogo.mjs
```

Para obter a lista de todas as peças pronta para colar dentro de `byId`:

```bash
node tools/gen-catalogo.mjs --ids
```

Isso imprime as 122 peças agrupadas por família, com o nome legível em
comentário ao lado de cada id que ainda não tem preço.

### Preços

```json
"prices": {
  "currency": "BRL",
  "sampledOn": "2026-08-30",
  "method": "mediana de três varejistas nacionais, à vista, sem frete",
  "byId": {
    "rtx-5070": 4200,
    "ryzen-5-7600": 1300
  }
}
```

Id que não existe na base é ignorado pelo app e reportado — quase sempre é
erro de digitação. O `gen-catalogo.mjs` avisa sobre esses ao rodar.

### Peça nova

Precisa dos campos completos, porque a peça não existe na base do APK:

```json
"newGpus": [
  {
    "id": "rtx-5080-super",
    "group": "RTX 50 (Blackwell)",
    "name": "RTX 5080 Super",
    "mult": 1.62,
    "vram": 24.0,
    "gen": "rtx50",
    "watts": 400
  }
]
```

O `mult` é o desempenho relativo à RTX 5070 de referência (1.00) — é o número
que decide o FPS estimado, então vale o mesmo cuidado dos preços. `gen` precisa
ser uma das gerações que o app conhece (`gtx10`, `gtx16`, `rtx20`, `rtx30`,
`rtx40`, `rtx50`, `rdna2`, `rdna3`, `rdna4`); para CPU, `socket` é `AM4`,
`AM5`, `LGA1700` ou `LGA1851`.

Peças novas entram **no fim** da lista, nunca no meio. Isso é deliberado e tem
teste próprio: builds salvas e códigos compartilhados referenciam hardware por
posição de array, então inserir no meio converteria a máquina guardada de
alguém em outra, sem erro nenhum aparecer.

---

## O que o catálogo remoto **não** pode mudar

Multiplicadores de peças que já existem, jogos, placas-mãe e as constantes de
cálculo. Isso é o miolo do modelo, coberto pelos testes de paridade contra o JS
original — um arquivo remoto capaz de mexer ali mudaria o resultado do cálculo
sem passar por teste nenhum.

Peça nova entra; regra de cálculo não. Para mexer no cálculo, é versão nova do
app.

---

## Publicando

O arquivo é servido pelo GitHub Pages junto com o resto de `docs/`:

```bash
git add docs/catalogo.json && git commit -m "Atualiza preços do catálogo" && git push
```

O Pages leva um ou dois minutos para propagar. Para conferir:

```bash
curl -sI https://yuumi-32.github.io/FpsCalculadora/catalogo.json
```

O app faz GET condicional por ETag e só rebaixa quando o arquivo muda de
verdade, com no mínimo 12 horas entre tentativas — não espere ver o preço novo
no aparelho no mesmo minuto.

---

## Se algo der errado

O app nunca quebra por causa deste arquivo. As falhas são silenciosas por
projeto — ele continua com o último catálogo que deu certo, ou com a base do
APK:

| Situação | O que o app faz |
|---|---|
| Sem internet | usa o cache; se não houver, a base do APK |
| JSON inválido | ignora o download e mantém o que tinha |
| `schema` diferente de `1` | recusa o arquivo inteiro (é sinal de app desatualizado) |
| Preço absurdo (zero a mais) | descarta só aquele preço |
| Id desconhecido em `byId` | ignora aquela entrada |

Ou seja: um erro aqui não derruba ninguém, mas passa despercebido. Depois de
publicar, confira no aparelho se o preço apareceu.
