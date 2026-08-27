# Conta de desenvolvedor Google Play — US$ 25

Este é o único item da lista que ninguém faz por você: envolve documento de
identidade, cartão e uma conta Google que vai ser a dona do app para sempre.
O passo a passo abaixo é para fazer de uma sentada, em ~30 minutos, mais a
espera da verificação.

> As telas e as exigências da Play mudam de tempos em tempos. Se algo aparecer
> diferente do descrito aqui, o que está na tela é que vale.

---

## Antes de começar, decida duas coisas que não dá para desfazer

**1. Qual conta Google vai ser a dona do app.**
A conta fica ligada ao app para sempre — transferir depois é um processo chato
com a Google no meio. Use uma conta que você não vá perder e que não seja
compartilhada. Ative a verificação em duas etapas nela **antes** de começar: a
Play exige, e é ela que protege a publicação do seu app.

**2. Conta pessoal ou de organização.**

| | Pessoal | Organização |
|---|---|---|
| Documentos | RG/CNH + comprovante de endereço | Número D-U-N-S da empresa |
| Prazo extra | — | O D-U-N-S é gratuito, mas pode levar ~30 dias |
| Teste fechado obrigatório | **Sim** (12 testadores por 14 dias) | Não |
| Nome que aparece na loja | Seu nome ou um nome público que você escolher | Razão social |

Para um projeto pessoal como este, **conta pessoal** é o caminho: mais rápido e
sem CNPJ. O preço é o teste fechado obrigatório antes de ir para produção — que
é justamente o item 5 desta lista, e por isso vale começar a recrutar testador
em paralelo.

---

## Passo a passo

1. **Cadastro** — entre em <https://play.google.com/console/signup> com a conta
   escolhida e selecione o tipo de conta (pessoal).
2. **Taxa de US$ 25** — pagamento único, vitalício, **não reembolsável**, no
   cartão de crédito. Cobrado em dólar: no Brasil vem com IOF e a conversão do
   banco, então na fatura sai por volta de R$ 150 (varia com o câmbio).
3. **Nome de desenvolvedor** — é o nome que aparece embaixo do app na loja.
   Pode ser diferente do seu nome civil, mas precisa combinar com a identidade
   verificada. Escolha pensando em como quer ser visto na ficha do app.
4. **Verificação de identidade** — documento com foto e comprovante de endereço,
   com os dados batendo exatamente com o que você digitou. Deve ser concluída
   em até **30 dias** depois do cadastro, ou a conta é encerrada. A análise em
   geral leva de 48 horas a alguns dias úteis.
5. **Dados de contato** — e-mail e telefone, ambos verificados. O e-mail de
   contato do app é público na ficha; o e-mail da conta não.
6. **Perfil de pagamentos** — só é necessário para app pago ou com compras no
   app. O FPS Calculadora é gratuito e sem compras, então dá para pular.

---

## Depois que a conta estiver ativa

1. **Criar o app** na Play Console: nome, idioma padrão (pt-BR), tipo "App",
   gratuito. Cuidado: **gratuito → pago é uma via de mão única** na Play.
2. **Nome do pacote** — o `applicationId` `com.fps.calculadora` fica gravado
   para sempre no primeiro envio. Não dá para renomear depois; só publicando
   um app novo do zero, sem os usuários do antigo.
3. **Assinatura pelo Google Play** (Play App Signing) — aceite. Você continua
   assinando com a chave de upload (a que o `keystore.properties` aponta) e o
   Google guarda a chave final. Se um dia você perder a chave de upload, dá para
   pedir uma nova; se não estivesse no Play App Signing, o app estaria perdido.
4. **Guardar a keystore com backup** — a de upload continua sendo sua. As
   instruções de geração estão no [README do projeto](../../README.md#build-de-release).
5. Preencher **Conteúdo do app** com as respostas de
   [02-declaracoes-play-console.md](02-declaracoes-play-console.md) e a ficha
   com os textos de [03-ficha-da-loja.md](03-ficha-da-loja.md).
6. Subir o AAB assinado (`./gradlew bundleRelease`) na trilha de **teste
   fechado** e seguir o [05-teste-fechado.md](05-teste-fechado.md).

---

## Erros que custam tempo

- **Nome de desenvolvedor diferente do documento** — reprova a verificação e
  reinicia a fila.
- **Cartão que recusa cobrança internacional** — o cadastro trava no pagamento;
  cartão virtual costuma resolver.
- **Deixar a verificação para depois** — os 30 dias correm a partir do cadastro,
  não a partir do primeiro envio.
- **Criar a conta e só então pensar no teste fechado** — os 14 dias corridos com
  12 testadores só começam a contar quando a trilha estiver publicada. Recrute
  os testadores enquanto a verificação anda.
