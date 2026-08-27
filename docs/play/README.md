# Publicação na Google Play — o que falta

Checklist do lançamento do FPS Calculadora, com o estado de cada item e o
arquivo que resolve cada um.

| # | Item | Estado |
|---|---|---|
| 1 | Política de privacidade publicada numa URL | ✅ **no ar** — falta trocar nome e e-mail |
| 2 | Segurança dos Dados, classificação indicativa, público-alvo, anúncios | ✅ **respondido** — falta colar na Play Console |
| 3 | Ícone 512×512, feature graphic 1024×500, capturas, descrições | ✅ **pronto** — arquivos e textos gerados |
| 4 | Conta de desenvolvedor (US$ 25) | ⬜ **só você pode fazer** — passo a passo pronto |
| 5 | Teste fechado: 12 testadores por 14 dias | ⬜ **só você pode fazer** — kit de recrutamento pronto |

Os itens 4 e 5 exigem documento de identidade, cartão de crédito e pessoas de
verdade instalando o app: não dá para adiantar por aqui além do que já está
escrito.

## Os arquivos

| Arquivo | O que tem dentro |
|---|---|
| [01-politica-de-privacidade.md](01-politica-de-privacidade.md) | A URL publicada, onde mexer no texto e o que ele afirma |
| [02-declaracoes-play-console.md](02-declaracoes-play-console.md) | Todas as respostas de "Conteúdo do app", com a justificativa de cada uma |
| [03-ficha-da-loja.md](03-ficha-da-loja.md) | Título, descrição curta, descrição completa, notas da versão e o inventário dos gráficos |
| [04-conta-de-desenvolvedor.md](04-conta-de-desenvolvedor.md) | Cadastro de US$ 25, verificação de identidade e o que fazer logo depois |
| [05-teste-fechado.md](05-teste-fechado.md) | Regras dos 14 dias, linha do tempo, mensagens de recrutamento e controle |
| [graficos/](graficos/) · [screenshots/](screenshots/) | Ícone, feature graphic e as oito capturas 1080×1920 |

## Ordem sugerida

O caminho crítico é o teste fechado, então ele começa primeiro:

1. **Hoje** — criar a conta de desenvolvedor ([04](04-conta-de-desenvolvedor.md))
   e começar a chamar testadores ([05](05-teste-fechado.md)). São ~3 semanas de
   relógio até poder publicar em produção.
2. **Enquanto a verificação de identidade anda** — trocar nome e e-mail nas
   páginas ([01](01-politica-de-privacidade.md)) e conferir a URL no ar.
3. **Com a conta ativa** — criar o app, subir o AAB assinado
   (`./gradlew bundleRelease`) na trilha de teste fechado e mandar os convites.
4. **Com o app criado** — preencher "Conteúdo do app" com as respostas de
   [02](02-declaracoes-play-console.md) e a ficha com os textos e gráficos de
   [03](03-ficha-da-loja.md).
5. **Depois dos 14 dias** — solicitar acesso à produção, contando o retorno dos
   testadores.

## Regerar os gráficos

```bash
python tools/gen-store-graphics.py
python tools/gen-store-screenshots.py
```

Precisa de Python com Pillow e do Chrome (ou Edge) instalado. As capturas saem
do próprio `app/src/main/assets/www/index.html`, então elas acompanham qualquer
mudança na interface — vale rodar de novo antes de subir uma versão nova.
