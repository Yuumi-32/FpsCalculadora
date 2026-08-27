# Declarações do Play Console — respostas prontas

Tudo o que a seção **Conteúdo do app** (*App content*) e o formulário de
**Segurança dos dados** pedem, já respondido para o FPS Calculadora. É só copiar.

As respostas abaixo valem para o app como ele está hoje: sem internet, sem
permissões no manifesto, sem anúncios, sem login e sem SDK de terceiros
(confirmado em `app/src/main/AndroidManifest.xml` e `app/build.gradle`).
**Se qualquer uma dessas coisas mudar, o formulário precisa ser refeito antes
da atualização subir.**

---

## 1. Política de privacidade

| Campo | Resposta |
|---|---|
| URL da política de privacidade | `https://yuumi-32.github.io/FpsCalculadora/privacidade.html` |

Fonte da página: [`docs/privacidade.html`](../privacidade.html). Antes de colar a
URL na Play Console, troque `SEU-NOME-AQUI` e `SEU-EMAIL@exemplo.com` pelos
dados reais e confirme que a página abre no navegador.

---

## 2. Acesso ao app (*App access*)

| Pergunta | Resposta |
|---|---|
| Alguma parte do app exige credenciais de acesso? | **Todas as funcionalidades estão disponíveis sem restrição de acesso** |

Não há login, cadastro, assinatura, código promocional nem área bloqueada. Nada
a fornecer ao time de revisão.

---

## 3. Anúncios (*Ads*)

| Pergunta | Resposta |
|---|---|
| Seu app contém anúncios? | **Não** |

Consequência: o app **não** recebe o selo "Contém anúncios" na loja. Se um dia
entrar qualquer rede de anúncios (AdMob, Unity Ads, etc.), esta declaração
precisa ser trocada para "Sim" **antes** de publicar essa versão — divergência
aqui é motivo de suspensão.

---

## 4. Classificação indicativa (questionário IARC)

E-mail do responsável: `SEU-EMAIL@exemplo.com` (o IARC manda o certificado por e-mail).

**Categoria do app:** *Utilitário, produtividade, comunicação ou outro* —
**não** marque "Jogo". O app é uma ferramenta que fala sobre jogos, mas não é um
jogo; marcar "Jogo" abre um questionário de violência que não se aplica.

Todas as perguntas do questionário: **Não**. Em detalhe:

| Bloco | Pergunta | Resposta |
|---|---|---|
| Violência | Retrata violência, sangue, ferimentos, crueldade? | Não |
| Sexualidade | Contém nudez, conteúdo sexual ou insinuação? | Não |
| Linguagem | Contém palavrão ou linguagem grosseira? | Não |
| Substâncias | Referência a drogas, álcool ou tabaco? | Não |
| Jogos de azar | Simula ou permite apostas com dinheiro real? | Não |
| Medo | Contém conteúdo assustador ou perturbador? | Não |
| Diversos | Permite interação/comunicação entre usuários? | Não |
| Diversos | Compartilha a localização do usuário? | Não |
| Diversos | Permite compra de itens digitais? | Não |
| Diversos | Contém conteúdo gerado por usuários? | Não |
| Diversos | É navegador ou mecanismo de busca? | Não |
| Diversos | Coleta ou compartilha informações pessoais? | Não |

> Sobre "conteúdo gerado por usuários": o apelido que a pessoa dá a uma build
> fica só no aparelho dela e não é publicado nem visto por mais ninguém — isso
> não é UGC para efeito do questionário.

**Resultado esperado:** Livre (ClassInd/Brasil), Everyone (ESRB), PEGI 3, USK 0,
IARC 3+. Se sair diferente disso, alguma resposta foi marcada errada.

---

## 5. Público-alvo e conteúdo (*Target audience and content*)

| Pergunta | Resposta |
|---|---|
| Faixas etárias-alvo | **13–15, 16–17, 18 e mais** |
| O app tem apelo para crianças (menores de 13)? | **Não** |
| A ficha da loja atrai crianças (arte, personagens, linguagem)? | **Não** |

Como nenhuma faixa abaixo de 13 anos foi marcada, o app fica **fora** do
programa Projetado para Famílias e das exigências extras que vêm com ele.

Para o público 13+ a Play cobra que os anúncios sejam apropriados — como o app
não tem anúncio nenhum, o item não se aplica. Mantenha a arte da ficha
(ícone, feature graphic, screenshots) no tom técnico/adulto que ela já tem: o
apelo infantil é avaliado pelo material da loja, não só pelo app.

---

## 6. Segurança dos dados (*Data safety*)

| Pergunta | Resposta |
|---|---|
| Seu app coleta ou compartilha algum dos tipos de dados do usuário exigidos? | **Não** |

Marcando "Não", o formulário pula direto para revisão e envio — as perguntas de
criptografia em trânsito, exclusão de dados e tipos de dados só aparecem para
quem coleta.

**Por que "Não" está correto:** a Play define *coleta* como **transmitir dados
para fora do aparelho**. O FPS Calculadora guarda builds e preferências no
armazenamento privado do próprio app e nunca envia nada — o app não declara nem
a permissão de internet. Dado que só existe no aparelho não é coleta.

Pontos que costumam gerar dúvida, e por que nenhum deles vira "Sim":

- **Backup do Android (`allowBackup="true"`)** — quem copia os dados para o
  Drive é o sistema operacional, para a conta do próprio usuário. Isso não é
  coleta pelo app e não entra no formulário (está explicado no item 5 da
  política de privacidade, por transparência).
- **Código de build (copiar/importar)** — vai para a área de transferência do
  aparelho por ação do usuário; nenhum servidor recebe nada.
- **PNG do resultado salvo na galeria** — arquivo local, criado a pedido do
  usuário, via MediaStore. Não é acesso a fotos existentes: o app só grava a
  imagem que ele mesmo desenhou.

### ID de publicidade

| Pergunta | Resposta |
|---|---|
| Seu app usa o ID de publicidade (AAID)? | **Não** |

Coerente com o manifesto: o app não declara
`com.google.android.gms.permission.AD_ID` e não inclui Google Play Services.

---

## 7. Demais declarações da seção "Conteúdo do app"

| Declaração | Resposta |
|---|---|
| App de notícias | Não |
| Rastreamento de contato / status de COVID-19 | Não |
| App do governo | Não |
| Recursos financeiros (empréstimo, cripto, investimento, seguro) | **Nenhum destes** |
| Apps de saúde (pesquisa clínica, telemedicina, bem-estar) | **Nenhum destes** |
| Permissões de serviço em primeiro plano | Nenhuma — o app não usa `foregroundService` |
| Permissões sensíveis (SMS, chamadas, fotos/vídeos, arquivos) | Nenhuma — o manifesto não declara permissão alguma |
| Uso de APIs de acessibilidade | Não |
| Conteúdo violento ou de ódio gerado por usuários | Não se aplica |

---

## 8. Configurações da ficha (fora de "Conteúdo do app")

| Campo | Valor |
|---|---|
| Tipo de app | App (não é jogo) |
| Categoria | **Ferramentas** (alternativa: Estilo de vida — Ferramentas descreve melhor) |
| Tags | Calculadora, Utilitários, Hardware/PC |
| E-mail de contato (público) | `SEU-EMAIL@exemplo.com` |
| Site (opcional) | `https://yuumi-32.github.io/FpsCalculadora/` |
| Telefone (opcional) | deixar em branco |
| Preço | **Gratuito** — atenção: gratuito → pago é um caminho sem volta na Play |
| Países | Brasil (e o resto do mundo, se quiser; a ficha está em pt-BR) |
| Compras no app | Não |
| Idioma padrão da ficha | Português (Brasil) |

---

## Antes de enviar, confira

- [ ] `SEU-EMAIL@exemplo.com` e `SEU-NOME-AQUI` trocados em `docs/privacidade.html`, `docs/privacy.html` e `docs/index.html`
- [ ] A URL da política abre sem erro em uma aba anônima
- [ ] O AAB enviado é o de release assinado (`./gradlew bundleRelease`)
- [ ] A versão enviada não ganhou nenhuma permissão nova no manifesto
