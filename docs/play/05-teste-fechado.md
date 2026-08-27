# Teste fechado — 12 testadores por 14 dias

O item mais longo da lista, e o único que não depende de você trabalhar mais
rápido: são **14 dias corridos de relógio** com **12 testadores inscritos ao
mesmo tempo**. Contas de desenvolvedor pessoais criadas de nov/2023 para cá só
conseguem pedir acesso à produção depois disso.

Por isso ele começa primeiro e corre em paralelo com o resto: enquanto a
verificação de identidade anda e a ficha é preenchida, os testadores já estão
sendo recrutados.

---

## As regras que costumam pegar as pessoas

- **Tem que ser a trilha de teste fechado.** Teste interno não conta para o
  prazo, por mais gente que tenha.
- **12 testadores inscritos, e continuando inscritos.** Não é "12 pessoas
  passaram por lá": é 12 ao mesmo tempo, todos os dias. Se um sair no meio e a
  conta cair para 11, você corre o risco de perder a contagem.
- **Cada testador precisa de uma Conta Google própria** e precisa **aceitar o
  convite pelo link de opt-in**. Instalar o APK por fora não conta — a Play só
  enxerga quem entrou pelo link.
- **Deixe folga.** Recrute 15 ou 16 para terminar com 12. Sempre tem quem troca
  de celular, desinstala ou some.
- **A Play pergunta como você coletou feedback** na hora de pedir produção.
  Guarde as respostas dos testadores; tem um roteiro de perguntas no fim deste
  arquivo.
- **Continue publicando versões durante o período.** Uma trilha parada por 14
  dias com zero atualização é sinal ruim na análise.

---

## Linha do tempo

O relógio só começa quando os 12 estiverem dentro. Contando a partir de hoje:

| Quando | O quê |
|---|---|
| 27/08/2026 | Criar a conta de desenvolvedor e começar a chamar testador |
| 27/08 – 02/09 | Recrutamento (meta: 15 confirmados) e envio dos convites |
| **Dia 0 — quando o 12º aceitar** | Começa a contagem dos 14 dias |
| Dia 0 + 14 | Fim do período mínimo — pedir acesso à produção |
| +1 a 7 dias | Análise da Play para liberar produção |

Na prática, do zero até poder publicar: **três a quatro semanas**.

---

## Passo a passo na Play Console

1. **Testes → Teste fechado → Criar trilha** (pode usar a trilha "Alpha" que já
   vem pronta).
2. **Envie o AAB assinado** — `./gradlew bundleRelease`, arquivo em
   `app/build/outputs/bundle/release/`.
3. **Testadores** — crie uma **lista de e-mails** na Play Console ou use um
   **Grupo do Google**. O grupo é mais fácil: você adiciona e remove gente sem
   mexer na trilha.
4. **Copie o link de opt-in** (algo como
   `https://play.google.com/apps/testing/com.fps.calculadora`) e mande junto com
   as instruções do fim deste arquivo.
5. **Acompanhe o número de inscritos** na própria trilha, todo dia, até fechar
   os 14. É aqui que se descobre cedo que alguém saiu.
6. Ao completar os 14 dias: **Painel → Solicitar acesso à produção**, contando o
   que os testadores relataram e o que você mudou por causa disso.

---

## Mensagem para chamar testador

Para WhatsApp, Discord, grupo de PC gamer, laboratório da faculdade — onde tiver
gente que monta PC:

```
Fiz um app Android que estima quantos FPS um PC roda em cada jogo (escolhe
processador e placa de vídeo e ele calcula). Para publicar na Play Store eu
preciso de 12 pessoas testando por 14 dias.

O que dá trabalho pra você: instalar, abrir de vez em quando e me dizer se
algo ficou estranho. E deixar instalado por 2 semanas.

O app é gratuito, funciona offline, não tem anúncio, não pede nenhuma
permissão e não coleta nada.

Topa? Me manda o e-mail da sua Conta Google (o do Android) que eu te mando o
link.
```

E, depois que a pessoa mandar o e-mail:

```
Valeu! Passo a passo:

1. Abra este link no celular, com a mesma conta Google que você me passou:
   https://play.google.com/apps/testing/com.fps.calculadora
2. Toque em "Tornar-se um testador"
3. Instale o app pela Play Store pelo link que aparece ali
4. Deixe instalado até DD/MM — se desinstalar antes, some da contagem

Qualquer coisa esquisita, me manda print. Um "abri e não achei nada errado"
também ajuda.
```

---

## Controle dos testadores

Preencha conforme for confirmando. Meta: 15 nomes, para sobreviver a três
desistências.

| # | Nome | E-mail da Conta Google | Convite enviado | Aceitou (opt-in) | Deu retorno |
|---|---|---|---|---|---|
| 1 |  |  |  |  |  |
| 2 |  |  |  |  |  |
| 3 |  |  |  |  |  |
| 4 |  |  |  |  |  |
| 5 |  |  |  |  |  |
| 6 |  |  |  |  |  |
| 7 |  |  |  |  |  |
| 8 |  |  |  |  |  |
| 9 |  |  |  |  |  |
| 10 |  |  |  |  |  |
| 11 |  |  |  |  |  |
| 12 |  |  |  |  |  |
| 13 |  |  |  |  |  |
| 14 |  |  |  |  |  |
| 15 |  |  |  |  |  |

> Esses e-mails são dados pessoais de outras pessoas. Este arquivo está num
> repositório **público** — preencha a tabela numa cópia local ou numa planilha
> privada, não commite a lista preenchida.

---

## O que perguntar aos testadores

Cinco perguntas, respondidas em dois minutos. São elas que viram o texto do
pedido de acesso à produção:

1. O app abriu normalmente no seu celular? Qual aparelho e qual versão do Android?
2. Você achou seu processador e sua placa de vídeo na lista? Faltou alguma peça?
3. O FPS estimado bateu com o que você vê na prática nos seus jogos?
4. Teve alguma tela travada, texto cortado ou botão que não respondeu?
5. Se fosse instalar de verdade, o que faltaria no app?

A pergunta 2 é a mais valiosa: a lista de peças hoje tem 69 placas de vídeo, 53
processadores e 31 jogos — o que os testadores sentirem falta é a fila de
trabalho da próxima versão.

---

## Antes de pedir produção, confira

- [ ] 12 testadores inscritos, sem cair desse número, por 14 dias corridos
- [ ] Pelo menos uma versão nova publicada na trilha durante o período
- [ ] Retorno dos testadores anotado, com o que virou correção
- [ ] Nenhum travamento pendente sem resposta
- [ ] Ficha da loja e "Conteúdo do app" 100% preenchidos
- [ ] Política de privacidade no ar e abrindo pela URL da ficha
