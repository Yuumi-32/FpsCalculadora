# Preço do app — quanto cobrar para sobrar R$ 2,00

A meta é **R$ 2,00 líquidos por venda**, depois do que a Google Play retém.
Este documento tem a conta, o número que sai dela e as três consequências de
transformar o app em pago — uma delas cai em cima do teste fechado e precisa
ser resolvida antes de convidar os testadores.

> **Não confundir com o item 7 do [README](README.md).** Aquele é o preço das
> *peças* (`prices.byId` no catálogo, para a tela de upgrade mostrar
> custo-benefício), que continua vazio e depende de levantamento manual — ver
> [06-catalogo-remoto.md](06-catalogo-remoto.md). Este aqui é o preço do
> **aplicativo na loja**. São duas coisas sem relação.

---

## A resposta

```
R$ 2,49
```

Rende **R$ 2,12** por venda se a Play descontar só a taxa de serviço, e
continua acima de R$ 2,00 enquanto o tributo que ela separar do preço ficar
abaixo de ~5,8%. O número exato aparece no próprio Play Console na hora de
salvar o preço — a conta abaixo é para você chegar lá sabendo o que esperar.

---

## A conta

Três coisas podem entrar entre o que o comprador paga e o que cai na sua
conta. Só a primeira desconta com certeza; a terceira você evita de graça:

| Desconto | Quanto | Sai de onde |
|---|---|---|
| Taxa de serviço da Play | **15%** | 15% no primeiro US$ 1 milhão de ganhos por ano; 30% só acima disso, o que não é o seu caso |
| Tributo de transação | **depende** | O Brasil está na lista de países em que o preço exibido já inclui tributo. A taxa de serviço é cobrada **por fora** dele: o Google desconta os 15% sem abater imposto |
| Tributo retido (IRRF 25% + CIDE) | **não se aplica** | Só incide sobre quem recebe do Google em moeda diferente do real. **Configure o pagamento em BRL** e ele some |

A fórmula, com `t` sendo o tributo que a Play separar do preço:

```
preço = 2,00 × (1 + t) ÷ 0,85
```

E o que R$ 2,49 rende em cada cenário:

| Se a Play descontar… | Você recebe |
|---|---|
| só os 15% de taxa de serviço | **R$ 2,12** |
| 15% + 3% de tributo | R$ 2,05 |
| 15% + 5% de tributo | R$ 2,02 |
| 15% + 8% de tributo | R$ 1,96 ❌ |

Se quiser margem maior contra a reforma tributária (CBS/IBS entram em regime de
transição e a alíquota muda ano a ano), **R$ 2,69** aguenta até ~14% de tributo
por fora e ainda rende R$ 2,00. A diferença de preço é de vinte centavos; a
diferença de sossego é de oito anos de transição.

### Antes de salvar, confira no lugar certo

O editor de preços do Play Console mostra, para cada país, o preço com tributo
e **os seus ganhos estimados**. Esse número é o oficial — a conta acima existe
para você chegar na tela sabendo o que esperar, não para substituí-la. Se o
Console disser algo diferente de ~R$ 2,12, é o Console que está certo.

### O que esta conta **não** cobre

O imposto que **você** deve sobre o que recebeu. R$ 2,12 é o que a Play
deposita; quanto disso sobra depois do seu IR, MEI ou regime de empresa é outra
conta, que depende da sua situação e não dá para responder aqui. Se a meta for
R$ 2,00 **no seu bolso**, e não R$ 2,00 na sua conta, o preço precisa subir — e
o quanto é pergunta para contador, não para este arquivo.

---

## As três consequências de ser pago

### 1. É um caminho de mão única — e você está indo no sentido certo

A Play deixa transformar app pago em gratuito. **Não deixa o contrário**: app
que já foi publicado de graça nunca mais pode ter preço; para cobrar seria
preciso publicar outro app, com outro nome de pacote, perdendo instalações e
avaliações.

Ou seja: **o preço tem que estar definido antes da primeira publicação**,
inclusive antes do teste fechado. Se um dia você mudar de ideia e quiser abrir o
app, dá; o contrário, não.

### 2. Os 12 testadores vão precisar comprar o app

Esta é a que atrapalha o cronograma. Na regra da Play, **quem entra num teste
fechado de um app pago compra o app** (só o teste *interno* instala de graça — e
teste interno não conta para a exigência de 12 testadores por 14 dias).

Duas saídas, nessa ordem:

1. **Códigos promocionais.** O Play Console gera até 500 códigos por trimestre
   para app pago; cada código resgata o app de graça. Doze códigos cobrem os
   doze testadores e não custam nada. Gere-os depois de criar o app e mande
   junto do convite — o [kit de recrutamento](05-teste-fechado.md) precisa
   ganhar essa linha.
2. **Reembolsar.** Doze compras de R$ 2,49 são R$ 29,88. Funciona, mas pede
   combinar com cada pessoa e não deixa rastro de que foi combinado.

Confirme o passo 1 no Console assim que o app existir: é o tipo de detalhe que
muda de lugar entre versões da interface, e descobrir que não funciona no dia de
convidar custa uma semana do caminho crítico.

### 3. Nenhuma linha de código muda

App pago é uma configuração da ficha, não um recurso do aplicativo: a Play cobra
antes de deixar instalar. **Não** entra biblioteca de faturamento, **não** entra
permissão nova, **não** muda nada na Segurança dos Dados e **não** muda a
resposta "Compras no app: Não" — compra no app é outra coisa (item vendido
*dentro* do app, que este não tem).

O que muda é só o que está escrito nos documentos desta pasta.

---

## O que preencher no Play Console

| Onde | O quê |
|---|---|
| Monetizar → **Configuração do app** | Tipo: **Pago** |
| Monetizar → **Preços por país** | Brasil: **R$ 2,49** |
| Monetizar → **Configuração do app** | Categoria fiscal: **produtos digitais padrão** (o app não é livro, nem streaming, nem mídia) |
| Configurações → **Pagamentos** | Moeda de recebimento: **BRL** — é o que evita o IRRF de 25% |
| Monetizar → **Promoções** | 12 códigos promocionais para os testadores (ver acima) |

Países: a ficha está só em português, então **Brasil** basta no lançamento.
Abrir para outros países faz a Play converter o preço sozinha, e aí a conta de
cima vale só para as vendas em real.

---

## Por que app pago, e não as alternativas

Vale registrar o que foi descartado, para não refazer a discussão depois:

- **Anúncios.** Renderiam bem menos que R$ 2 por pessoa (um app utilitário
  aberto poucas vezes por mês não gera impressão suficiente), obrigariam a
  mudar a declaração de anúncios, a política de privacidade e o "sem anúncios"
  que hoje é argumento de venda na ficha — e entrariam SDK de terceiros num app
  que hoje não tem nenhum.
- **Gratuito + compra dentro do app.** Não entrega "R$ 2 por pessoa que baixa":
  entrega R$ 2 pela fração que decide pagar, que num utilitário é pequena. Em
  compensação, evita o problema do teste fechado e não é mão única. É a opção
  para quem prioriza alcance; não é o que foi pedido.
- **Doação.** A Play não permite doação via faturamento próprio para este tipo
  de app, e doação voluntária tem conversão ainda menor que compra no app.

---

## Fontes

- [Taxas de serviço — Play Console](https://support.google.com/googleplay/android-developer/answer/112622?hl=pt-BR)
- [Como funciona a taxa de serviço do Google Play](https://support.google.com/googleplay/android-developer/answer/11131145?hl=pt-BR)
- [Tributo retido (WHT)](https://support.google.com/googleplay/android-developer/answer/9384608?hl=pt-BR)
- [Real brasileiro e tributos obrigatórios](https://support.google.com/googleplay/android-developer/answer/6106161?hl=pt-BR)
- [Definir os preços do app](https://support.google.com/googleplay/android-developer/answer/6334373)
- [Configurar um teste aberto, fechado ou interno](https://support.google.com/googleplay/android-developer/answer/9845334)
- [Criar promoções (códigos promocionais)](https://support.google.com/googleplay/android-developer/answer/6321495)
