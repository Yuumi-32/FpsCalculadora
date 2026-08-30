/* Gera os JSON canônicos da base a partir do index.html.
   Reexecute sempre que mexer nas tabelas do HTML (enquanto a UI web existir).
   Uso: node tools/extract-data.mjs

   CUIDADO: nem tudo que está nos JSON veio do index.html. Houve dados
   acrescentados direto no Kotlin sem correspondência no HTML — resoluções
   ultrawide interpoladas em games.json, hzMarkers estendidos em
   constants.json. Regenerar por cima apaga esses campos sem erro nenhum.
   Por isso existe a trava de encolhimento no fim deste arquivo.           */
import { writeFileSync, mkdirSync, readFileSync, existsSync } from 'node:fs';
import { resolve } from 'node:path';
import { loadAppContext, ROOT } from './lib-extract.mjs';

const OUT = resolve(ROOT, 'core/src/main/resources/data');
mkdirSync(OUT, { recursive: true });

const app = loadAppContext({ withLogic: false });

/* ── Ids estáveis ────────────────────────────────────────────────────────
   O app hoje referencia hardware por índice de array (st.cpu = 16), o que
   quebra builds salvos e códigos compartilhados a cada item inserido no meio.
   A ordem dos arrays é preservada (compatibilidade), mas cada item ganha um
   id estável derivado do nome, que é o que o Kotlin deve usar daqui pra frente. */
const slug = s => s
  .normalize('NFD').replace(/[\u0300-\u036f]/g, '')
  .toLowerCase()
  .replace(/[^a-z0-9]+/g, '-')
  .replace(/^-|-$/g, '');

function withIds(list, label) {
  const seen = new Map();
  return list.map((item, index) => {
    let id = slug(item.n ?? item.name);
    if (seen.has(id)) throw new Error(`id duplicado em ${label}: "${id}" (índices ${seen.get(id)} e ${index})`);
    seen.set(id, index);
    return { id, index, ...item };
  });
}

/* Remove chaves undefined para o JSON não carregar campo vazio. */
const clean = o => Object.fromEntries(Object.entries(o).filter(([, v]) => v !== undefined));

const games = withIds(app.G, 'jogos').map(g => clean({
  id: g.id,
  index: g.index,
  group: g.grp,
  name: g.name,
  rtMode: g.rt,                    // pt | rt | lumen | none
  rrLabel: g.rrN,
  rtLabel: g.rtN,
  heavy: g.heavy === true,
  cpuCap: g.cpuCap,                // ausente => 155 (default aplicado no Kotlin)
  reference: g.r,                  // fps de referência (RTX 5070) por resolução
  vram: g.v,                       // GB necessários por resolução
}));

const cpus = withIds(app.C, 'cpus').map(c => clean({
  id: c.id, index: c.index, group: c.g, name: c.n,
  mult: c.m, socket: c.s, watts: c.w,
}));

const gpus = withIds(app.GP, 'gpus').map(g => clean({
  id: g.id, index: g.index, group: g.g, name: g.n,
  mult: g.m, vram: g.v, gen: g.gen, watts: g.w,
}));

const mobos = withIds(app.MB, 'placas-mãe').map(m => clean({
  id: m.id, index: m.index, group: m.g, name: m.n,
  socket: m.s, mult: m.m,
}));

const constants = {
  meta: { version: app.DB_META.v, updated: app.DB_META.updated },
  systemWatts: app.SYSTEM_W,
  presets: app.PRESETS.map(p => ({ key: p.k, name: p.n, mult: p.m })),
  upscalersNvidia: app.DLSS.map(o => ({ key: o.k, name: o.n, mult: o.m, needsRtx: o.rtx })),
  upscalersAmd: app.FSR.map(o => ({ key: o.k, name: o.n, mult: o.m })),
  upscalersIntel: app.XESS.map(o => ({ key: o.k, name: o.n, mult: o.m })),
  // Espelhado de dentro de calc() no index.html: é catálogo de hardware
  // (eficiência de RT por geração), não regra de cálculo, então vive nos dados.
  // Qualquer divergência aqui quebra os testes golden imediatamente.
  rtEfficiencyByGen: { rtx20: 0.70, rtx30: 0.85, rdna2: 0.62, rdna3: 0.78, rdna4: 0.95, arca: 0.72, arcb: 0.85 },
  vramByPreset: app.VRAM_PRE,
  ramLabels: app.RAM_LABELS,
  hzMarkers: app.HZ_MARKERS,
  buildPresets: Object.fromEntries(Object.entries(app.PRESETS_BUILD).map(([k, b]) => [k, {
    cpu: cpus[b.cpu].id, gpu: gpus[b.gpu].id, mobo: mobos[b.mobo].id,
    cpuIndex: b.cpu, gpuIndex: b.gpu, moboIndex: b.mobo,
    ram: b.ram, res: b.res, preset: b.preset, rt: b.rt, dlss: b.dlss, fg: b.fg,
  }])),
};

const files = { games, cpus, gpus, mobos, constants };

/* ── Trava contra apagamento silencioso ──────────────────────────────────
   Regenerar em cima de um JSON que tem dados fora do index.html apaga esses
   dados sem falhar: o script escreve um arquivo válido, só que menor, e a
   perda só aparece quando alguém repara que sumiu uma resolução.

   Encolher passa a exigir --force explícito.

   A contagem é recursiva de propósito. Contar só o topo não pega o caso
   real: games.json continua com os mesmos 36 jogos, e o que some são as
   resoluções *dentro* de cada um. Medir a profundidade inteira é o que
   torna a perda visível.                                                 */
const medir = valor => {
  if (Array.isArray(valor)) return valor.reduce((n, item) => n + medir(item), 0);
  if (valor && typeof valor === 'object') {
    return Object.keys(valor).reduce((n, k) => n + 1 + medir(valor[k]), 0);
  }
  return 0;
};

const encolhimentos = [];
for (const [name, data] of Object.entries(files)) {
  const path = resolve(OUT, `${name}.json`);
  if (!existsSync(path)) continue;
  try {
    const antes = medir(JSON.parse(readFileSync(path, 'utf8')));
    const depois = medir(data);
    if (depois < antes) encolhimentos.push({ name, antes, depois });
  } catch {
    console.warn(`! ${name}.json ilegível, seguindo sem comparar`);
  }
}

if (encolhimentos.length && !process.argv.includes('--force')) {
  console.error('\nABORTADO: regenerar encolheria arquivos que já existem.\n');
  for (const { name, antes, depois } of encolhimentos) {
    console.error(`  ${name}.json: ${antes} → ${depois} campos (perderia ${antes - depois})`);
  }
  console.error([
    '',
    'Quase sempre isso quer dizer que os JSON têm dados que o index.html não',
    'tem — e não que o index.html perdeu alguma coisa. Confira com git diff',
    'antes de insistir.',
    '',
    'Se a redução for mesmo intencional:  node tools/extract-data.mjs --force',
    '',
  ].join('\n'));
  process.exit(1);
}

for (const [name, data] of Object.entries(files)) {
  const path = resolve(OUT, `${name}.json`);
  writeFileSync(path, JSON.stringify(data, null, 2) + '\n', 'utf8');
  const n = Array.isArray(data) ? `${data.length} itens` : `${Object.keys(data).length} chaves`;
  console.log(`  ${name}.json — ${n}`);
}
console.log(`\nBase v${constants.meta.version} (${constants.meta.updated}) escrita em core/src/main/resources/data/`);
