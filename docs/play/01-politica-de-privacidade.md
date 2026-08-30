# Política de privacidade — publicada

**URL para colar na Play Console:**

```
https://yuumi-32.github.io/FpsCalculadora/privacidade.html
```

A Play exige uma URL de política de privacidade **mesmo para app que não coleta
nada**, e ela precisa estar no ar, aberta a qualquer pessoa, fora de login.

## Onde ficam os arquivos

| Arquivo | No ar em |
|---|---|
| [`docs/privacidade.html`](../privacidade.html) | `/FpsCalculadora/privacidade.html` |
| [`docs/privacy.html`](../privacy.html) (versão em inglês) | `/FpsCalculadora/privacy.html` |
| [`docs/index.html`](../index.html) | `/FpsCalculadora/` — serve de site e de página de suporte |
| [`docs/assets/site.css`](../assets/site.css) | estilo das três |

O site é servido pelo **GitHub Pages**, a partir da branch `main`, pasta
`/docs` (Settings → Pages). O arquivo `docs/.nojekyll` desliga o Jekyll: o
conteúdo é HTML puro e o build falha se o Jekyll tentar processar os `.md`
desta pasta.

Publicar uma correção é só commitar em `main` — o Pages reconstrói sozinho em
menos de um minuto.

## Contato: por que não há e-mail nas páginas

As páginas publicadas não trazem endereço de e-mail. O responsável aparece como
**Yuumi-32** e o contato é feito pelas
[issues do repositório](https://github.com/Yuumi-32/FpsCalculadora/issues).

Foi decisão deliberada: e-mail pessoal numa página pública é indexado por
buscador e colhido por robô de spam, e para um app sem coleta de dados as
issues dão conta do que aparece — erro nos números, jogo ou peça faltando.

**Isso não dispensa o e-mail no Play Console.** A Play exige um endereço de
contato do desenvolvedor na ficha da loja, e ele fica público lá. É o único
lugar que ainda pede um — ver a tabela da ficha em
[03-ficha-da-loja.md](03-ficha-da-loja.md).

Se algum dia for preciso pôr e-mail nas páginas também, são quatro lugares:
seções 1 e 12 de `docs/privacidade.html` e as mesmas de `docs/privacy.html`.

## O que o texto afirma (e que precisa continuar verdade)

A política diz, em pt-BR e em inglês, que o app:

- não coleta, não transmite e não compartilha nenhum dado pessoal;
- declara só `INTERNET` e `ACCESS_NETWORK_STATE`, ambas para baixar o catálogo
  público de peças, jogos e preços — nenhum dado do usuário sai do aparelho;
- guarda builds, preferências e a marca de onboarding só no armazenamento
  privado do app;
- permite o backup automático do Android, que pode copiar esses dados para o
  Google Drive **do próprio usuário**;
- salva o PNG do resultado na galeria e o código de build na área de
  transferência, sempre por ação do usuário.

Isso é um retrato do app de hoje. **Qualquer versão futura que adicione
anúncio, analytics ou uma permissão nova obriga a atualizar esta página antes
de publicar** — e a revisar o formulário de Segurança dos Dados, que segue
respondido como "não coleta" em
[02-declaracoes-play-console.md](02-declaracoes-play-console.md).

> **Mudou em 30/08/2026:** o app voltou a declarar `INTERNET` (mais
> `ACCESS_NETWORK_STATE`) para o catálogo remoto. A resposta "não coleta"
> continua correta — a Play define coleta como *transmitir dados do usuário
> para fora do aparelho*, e o download é um GET de arquivo público estático,
> sem nada do usuário. Mas o texto da política mudou, então **a página
> revisada precisa estar no ar antes de subir um build com a permissão**.
