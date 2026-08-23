/* Gera os JSON canônicos da base a partir do index.html.
   Reexecute sempre que mexer nas tabelas do HTML (enquanto a UI web existir).
   Uso: node tools/extract-data.mjs                                          */
import { writeFileSync, mkdirSync } from 'node:fs';
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
  // Espelhado de dentro de calc() no index.html: é catálogo de hardware
  // (eficiência de RT por geração), não regra de cálculo, então vive nos dados.
  // Qualquer divergência aqui quebra os testes golden imediatamente.
  rtEfficiencyByGen: { rtx20: 0.70, rtx30: 0.85, rdna2: 0.62, rdna3: 0.78, rdna4: 0.95 },
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
for (const [name, data] of Object.entries(files)) {
  const path = resolve(OUT, `${name}.json`);
  writeFileSync(path, JSON.stringify(data, null, 2) + '\n', 'utf8');
  const n = Array.isArray(data) ? `${data.length} itens` : `${Object.keys(data).length} chaves`;
  console.log(`  ${name}.json — ${n}`);
}
console.log(`\nBase v${constants.meta.version} (${constants.meta.updated}) escrita em core/src/main/resources/data/`);
