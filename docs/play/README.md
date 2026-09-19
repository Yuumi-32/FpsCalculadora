# Publicação na Google Play — o que falta

Checklist do lançamento do FPS Calculadora, com o estado de cada item e o
arquivo que resolve cada um.

| # | Item | Estado |
|---|---|---|
| 1 | Política de privacidade publicada numa URL | ✅ **no ar** — responsável e contato resolvidos em [01](01-politica-de-privacidade.md) |
| 2 | Segurança dos Dados, classificação indicativa, público-alvo, anúncios | ✅ **respondido** — falta colar na Play Console |
| 3 | Ícone 512×512, feature graphic 1024×500, capturas, descrições | ✅ **pronto** — arquivos e textos gerados |
| 4 | Conta de desenvolvedor (US$ 25) | ⬜ **só você pode fazer** — passo a passo pronto |
| 5 | Teste fechado: 12 testadores por 14 dias | ⬜ **só você pode fazer** — kit de recrutamento pronto |
| 6 | Keystore de release | ⬜ **só você pode fazer** — sem ele o AAB sai sem assinatura |
| 7 | Preços das peças no catálogo publicado | ⬜ **levantamento manual** — ver [06](06-catalogo-remoto.md) |
| 8 | Preço do app na loja | ✅ **decidido: R$ 2,49** — a conta e as consequências em [07](07-preco-do-app.md); falta preencher no Console |

Os itens 4, 5 e 6 exigem documento de identidade, cartão de crédito, pessoas de
verdade instalando o app e uma senha que só você pode guardar: não dá para
adiantar por aqui além do que já está escrito.

> **7 e 8 são coisas diferentes.** O 7 é o preço das *peças* (para a tela de
> upgrade mostrar custo-benefício); o 8 é quanto custa *baixar o app*.

### 6 — assinatura

`keystore.properties` não existe no repositório (e não deve existir). Sem ele o
`./gradlew bundleRelease` gera um pacote **não assinado**, que a Play recusa.
Copie `keystore.properties.example`, gere o `.jks` e guarde a senha em lugar de
onde ela não se perca — keystore perdido significa não conseguir mais atualizar
o app publicado.

### 7 — preço das peças

`docs/catalogo.json` está publicado com `prices.byId` vazio, então a tela de
upgrade não mostra custo-benefício. O app lida bem com isso (a linha some), e é
de propósito: preço chutado é pior que preço nenhum. Quando houver amostragem
de verdade, [06-catalogo-remoto.md](06-catalogo-remoto.md) tem o procedimento —
e não precisa de versão nova do app, só de um commit em `main`.

### 8 — preço do app

**R$ 2,49**, que rende ~R$ 2,12 líquidos depois dos 15% de taxa de serviço da
Play. A conta inteira, a sensibilidade a tributo e o que preencher em
"Monetizar" estão em [07-preco-do-app.md](07-preco-do-app.md).

Duas consequências mudam o que já estava planejado nos outros arquivos:

- **O preço precisa existir antes da primeira publicação**, teste fechado
  incluído: app publicado de graça não pode virar pago depois. O contrário é
  permitido.
- **Num app pago, quem entra no teste fechado compra o app.** Os 12 testadores
  do item 5 precisam de **códigos promocionais** (o Console gera até 500 por
  trimestre, de graça) — gere-os junto com o convite.

## Os arquivos

| Arquivo | O que tem dentro |
|---|---|
| [01-politica-de-privacidade.md](01-politica-de-privacidade.md) | A URL publicada, onde mexer no texto e o que ele afirma |
| [02-declaracoes-play-console.md](02-declaracoes-play-console.md) | Todas as respostas de "Conteúdo do app", com a justificativa de cada uma |
| [03-ficha-da-loja.md](03-ficha-da-loja.md) | Título, descrição curta, descrição completa, notas da versão e o inventário dos gráficos |
| [04-conta-de-desenvolvedor.md](04-conta-de-desenvolvedor.md) | Cadastro de US$ 25, verificação de identidade e o que fazer logo depois |
| [05-teste-fechado.md](05-teste-fechado.md) | Regras dos 14 dias, linha do tempo, mensagens de recrutamento e controle |
| [06-catalogo-remoto.md](06-catalogo-remoto.md) | Como levantar preço de peça e publicar peça nova sem versão nova do app |
| [07-preco-do-app.md](07-preco-do-app.md) | Quanto cobrar para sobrar R$ 2,00, e o que ser pago muda no resto |
| [graficos/](graficos/) · [screenshots/](screenshots/) | Ícone, feature graphic e as oito capturas 1080×1920 |

## Ordem sugerida

O caminho crítico é o teste fechado, então ele começa primeiro:

1. **Hoje** — criar a conta de desenvolvedor ([04](04-conta-de-desenvolvedor.md))
   e começar a chamar testadores ([05](05-teste-fechado.md)). São ~3 semanas de
   relógio até poder publicar em produção.
2. **Enquanto a verificação de identidade anda** — gerar o keystore (item 6) e
   conferir as três URLs no ar.
3. **Com a conta ativa** — criar o app, **definir o preço** ([07](07-preco-do-app.md))
   e gerar os códigos promocionais dos testadores, subir o AAB assinado
   (`./gradlew bundleRelease`) na trilha de teste fechado e mandar os convites
   junto dos códigos.
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
do próprio `app/src/debug/assets/www/index.html`, então elas acompanham qualquer
mudança na interface — vale rodar de novo antes de subir uma versão nova.
