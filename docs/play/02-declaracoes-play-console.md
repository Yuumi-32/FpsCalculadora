# Declarações do Play Console — respostas prontas

Tudo o que a seção **Conteúdo do app** (*App content*) e o formulário de
**Segurança dos dados** pedem, já respondido para o FPS Calculadora. É só copiar.

As respostas abaixo valem para o app como ele está hoje: sem anúncios, sem
login e sem SDK de terceiros, e com duas permissões no manifesto — `INTERNET`
e `ACCESS_NETWORK_STATE`, as duas a serviço do catálogo remoto (confirmado em
`app/src/main/AndroidManifest.xml` e `app/build.gradle`).
**Se qualquer uma dessas coisas mudar, o formulário precisa ser refeito antes
da atualização subir.**

> **Mudou em 30/08/2026:** até então o app não declarava permissão nenhuma, e
> este documento usava isso como argumento. A permissão de internet voltou
> para o catálogo remoto. **A resposta de Segurança dos dados não muda** — o
> porquê está reescrito na seção 6 —, mas a política de privacidade publicada
> foi revisada e precisa estar no ar antes do próximo envio.

---

## 1. Política de privacidade

| Campo | Resposta |
|---|---|
| URL da política de privacidade | `https://yuumi-32.github.io/FpsCalculadora/privacidade.html` |

Fonte da página: [`docs/privacidade.html`](../privacidade.html). A página já
está completa — responsável identificado e contato pelas issues do GitHub, sem
e-mail pessoal exposto (o porquê está em
[01-politica-de-privacidade.md](01-politica-de-privacidade.md)). Antes de colar
a URL, confirme que ela abre numa aba anônima.

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
do usuário para fora do aparelho**. O FPS Calculadora guarda builds e
preferências no armazenamento privado do próprio app e nunca envia nada. Dado
que só existe no aparelho não é coleta.

O app declara `INTERNET`, mas **declarar a permissão não é coletar dados** — o
formulário pergunta o que o app *envia*, não o que ele *pode* enviar. O único
tráfego é o download de um arquivo público e estático do catálogo, um GET sem
corpo, sem query e sem identificador: nada do usuário sobe junto. O endereço IP
que o GitHub Pages registra ao servir o arquivo é o mesmo de qualquer acesso
web, não é acessível ao desenvolvedor e não entra no formulário.

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
| E-mail de contato (público) | **preencher** — a Play exige, e fica visível na ficha. É o único e-mail do projeto: as páginas usam issues do GitHub |
| Site (opcional) | `https://yuumi-32.github.io/FpsCalculadora/` |
| Telefone (opcional) | deixar em branco |
| Preço | **Gratuito** — atenção: gratuito → pago é um caminho sem volta na Play |
| Países | Brasil (e o resto do mundo, se quiser; a ficha está em pt-BR) |
| Compras no app | Não |
| Idioma padrão da ficha | Português (Brasil) |

---

## Antes de enviar, confira

- [ ] E-mail de contato preenchido **no Play Console** (as páginas publicadas
      não usam e-mail — o contato delas são as issues do GitHub)
- [ ] A URL da política abre sem erro em uma aba anônima
- [ ] O AAB enviado é o de release assinado (`./gradlew bundleRelease`)
- [ ] A versão enviada não ganhou permissão nova além de `INTERNET` e
      `ACCESS_NETWORK_STATE` (as duas já cobertas pela política revisada)
- [ ] A política revisada (que menciona a permissão de internet) já está no ar
      **antes** de o AAB com a permissão ser enviado
