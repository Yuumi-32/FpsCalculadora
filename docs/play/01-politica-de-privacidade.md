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

## O que ainda precisa ser trocado

- [ ] `SEU-NOME-AQUI` → o nome que vai aparecer como responsável (o mesmo do
      "nome de desenvolvedor" na Play, para não confundir quem lê)
- [ ] `SEU-EMAIL@exemplo.com` → o e-mail de contato, nos **três** arquivos

```bash
grep -rn "SEU-EMAIL\|SEU-NOME" docs/
```

## O que o texto afirma (e que precisa continuar verdade)

A política diz, em pt-BR e em inglês, que o app:

- não coleta, não transmite e não compartilha nenhum dado pessoal;
- não declara **nenhuma** permissão no `AndroidManifest.xml`, nem internet;
- guarda builds, preferências e a marca de onboarding só no armazenamento
  privado do app;
- permite o backup automático do Android, que pode copiar esses dados para o
  Google Drive **do próprio usuário**;
- salva o PNG do resultado na galeria e o código de build na área de
  transferência, sempre por ação do usuário.

Isso é um retrato do app de hoje. **Qualquer versão futura que adicione rede,
anúncio, analytics ou uma permissão nova obriga a atualizar esta página antes
de publicar** — e a refazer o formulário de Segurança dos Dados, que hoje está
respondido como "não coleta" em
[02-declaracoes-play-console.md](02-declaracoes-play-console.md).
