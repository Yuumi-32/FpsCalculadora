/* Gera o esqueleto do catálogo remoto publicado no GitHub Pages.

   Uso: node tools/gen-catalogo.mjs

   Escreve docs/catalogo.json preservando os preços que já estiverem lá — a
   ideia é rodar de novo quando entrar peça nova na base, sem perder a tabela
   de preços que você já levantou.

   O que este script NÃO faz: inventar preço. Ele lista os ids que existem e
   deixa o valor para você preencher com amostragem de verdade. Preço chutado
   é pior que preço nenhum, porque a tela do app apresenta o número como média
   de mercado — e aí a promessa vira mentira.                                */
import { readFileSync, writeFileSync, existsSync } from 'node:fs';
import { resolve, dirname } from 'node:path';
import { fileURLToPath } from 'node:url';

const ROOT = resolve(dirname(fileURLToPath(import.meta.url)), '..');
const DATA = resolve(ROOT, 'core/src/main/resources/data');
const OUT = resolve(ROOT, 'docs/catalogo.json');

const read = f => JSON.parse(readFileSync(resolve(DATA, f), 'utf8'));

const cpus = read('cpus.json');
const gpus = read('gpus.json');
const constants = read('constants.json');

/* Preserva o que já foi preenchido à mão. */
let anterior = { prices: { byId: {} }, newCpus: [], newGpus: [] };
if (existsSync(OUT)) {
  try {
    anterior = JSON.parse(readFileSync(OUT, 'utf8'));
  } catch {
    console.warn('! docs/catalogo.json ilegível, começando do zero');
  }
}
const precosAntigos = anterior.prices?.byId ?? {};

/* Ids que a base embutida conhece, mais os que o próprio catálogo adiciona. */
const idsConhecidos = new Set([
  ...cpus.map(c => c.id),
  ...gpus.map(g => g.id),
  ...(anterior.newCpus ?? []).map(c => c.id),
  ...(anterior.newGpus ?? []).map(g => g.id),
]);

/* Preço órfão vira aviso em vez de sumir calado — quase sempre é typo no id. */
const orfaos = Object.keys(precosAntigos).filter(id => !idsConhecidos.has(id));
for (const id of orfaos) {
  console.warn(`! preço para id inexistente, mantido mas ignorado pelo app: ${id}`);
}

/* `--ids` imprime o esqueleto do byId com todas as peças agrupadas, pronto
   para colar e preencher. Sai antes de escrever qualquer arquivo.          */
if (process.argv.includes('--ids')) {
  const bloco = (lista, titulo) => {
    const linhas = [`  // ${titulo}`];
    let grupo = null;
    for (const p of lista) {
      if (p.group !== grupo) { grupo = p.group; linhas.push('', `  // ${grupo}`); }
      const atual = precosAntigos[p.id];
      linhas.push(`  "${p.id}": ${atual ?? ''},${atual ? '' : `   // ${p.name}`}`);
    }
    return linhas.join('\n');
  };
  console.log(bloco(gpus, 'GPUs') + '\n\n' + bloco(cpus, 'CPUs'));
  process.exit(0);
}

const catalogo = {
  schema: 1,
  version: anterior.version ?? constants.meta.version,
  updated: new Date().toISOString().slice(0, 10),
  prices: {
    currency: 'BRL',
    sampledOn: anterior.prices?.sampledOn ?? '',
    method: anterior.prices?.method ?? '',
    byId: precosAntigos,
  },
  newCpus: anterior.newCpus ?? [],
  newGpus: anterior.newGpus ?? [],
};

writeFileSync(OUT, JSON.stringify(catalogo, null, 2) + '\n', 'utf8');

const total = cpus.length + gpus.length + catalogo.newCpus.length + catalogo.newGpus.length;
const comPreco = Object.keys(precosAntigos).length;
console.log(`docs/catalogo.json escrito — ${comPreco}/${total} peças com preço`);
if (comPreco === 0) {
  console.log('  (nenhum preço ainda; veja docs/play/06-catalogo-remoto.md)');
}
