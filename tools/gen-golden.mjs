/* Gera os vetores golden a partir da implementação JS ATUAL do index.html.
   O port Kotlin tem que reproduzir estes números bit a bit — é o contrato de
   que a migração não muda nenhum FPS.
   Uso: node tools/gen-golden.mjs                                            */
import { writeFileSync, mkdirSync } from 'node:fs';
import { resolve } from 'node:path';
import { loadAppContext, ROOT } from './lib-extract.mjs';

const OUT = resolve(ROOT, 'core/src/test/resources');
mkdirSync(OUT, { recursive: true });

const app = loadAppContext();
const { G, C, GP, MB, PRESETS, PRESETS_BUILD, RAM_LABELS } = app;

const RES = ['1080p', '1440p', '4k'];
const RT = ['off', 'rt', 'rr'];
const FG = [1, 1.78, 3.15];
const DLSS = [0.60, 0.65, 0.92, 1.0, 1.12];
const RAM = Object.keys(RAM_LABELS);
const PRESET_KEYS = PRESETS.map(p => p.k);

/* PRNG determinístico — o golden precisa ser reprodutível byte a byte. */
function mulberry32(seed) {
  return function () {
    seed |= 0; seed = (seed + 0x6D2B79F5) | 0;
    let t = Math.imul(seed ^ (seed >>> 15), 1 | seed);
    t = (t + Math.imul(t ^ (t >>> 7), 61 | t)) ^ t;
    return ((t ^ (t >>> 14)) >>> 0) / 4294967296;
  };
}
const rnd = mulberry32(20260823);
const pick = arr => arr[Math.floor(rnd() * arr.length)];

const base = () => ({
  game: 2, cpu: 16, gpu: 45, mobo: 3, ram: 'ddr4_32', res: '1440p',
  preset: 'ultra', rt: 'off', fg: 1, dlss: 1.0, monHz: 144,
  hoursDay: 3, tariff: 0.95,
});
const mk = over => ({ ...base(), ...over });

/* ── Matriz de casos ─────────────────────────────────────────────────── */
const cases = [];
const add = (tag, state) => cases.push({ tag, state });

// Varredura por eixo: cada peça de hardware aparece pelo menos uma vez.
GP.forEach((_, gpu) => RES.forEach(res => RT.forEach(rt =>
  add('gpu-sweep', mk({ gpu, res, rt, game: 0 })))));          // jogo 0 = Path Tracing pesado
C.forEach((_, cpu) => [0, 20].forEach(game =>
  add('cpu-sweep', mk({ cpu, game }))));
G.forEach((_, game) => RES.forEach(res => RT.forEach(rt =>
  add('game-sweep', mk({ game, res, rt })))));
MB.forEach((_, mobo) => add('mobo-sweep', mk({ mobo })));
PRESET_KEYS.forEach(preset => RES.forEach(res => [45, 10].forEach(gpu =>
  add('preset-sweep', mk({ preset, res, gpu })))));
RAM.forEach(ram => [0, 20].forEach(game => add('ram-sweep', mk({ ram, game }))));
DLSS.forEach(dlss => [45, 60].forEach(gpu => add('dlss-sweep', mk({ dlss, gpu }))));
FG.forEach(fg => [45, 60, 10].forEach(gpu => add('fg-sweep', mk({ fg, gpu }))));

// Builds prontos do onboarding, contra a base inteira de jogos.
Object.entries(PRESETS_BUILD).forEach(([key, b]) =>
  G.forEach((_, game) => add(`build:${key}`, mk({ ...b, game }))));

// Combinações aleatórias — inclui estados "inválidos" de propósito (RT ligado
// em GPU sem RT cores, FG em Pascal, VRAM estourada), que exercitam os
// fallbacks defensivos dentro de calc().
for (let i = 0; i < 2500; i++) {
  add('random', {
    game: Math.floor(rnd() * G.length),
    cpu: Math.floor(rnd() * C.length),
    gpu: Math.floor(rnd() * GP.length),
    mobo: Math.floor(rnd() * MB.length),
    ram: pick(RAM), res: pick(RES), preset: pick(PRESET_KEYS),
    rt: pick(RT), fg: pick(FG), dlss: pick(DLSS),
    monHz: pick([60, 144, 210, 300]), hoursDay: 3, tariff: 0.95,
  });
}

/* ── Execução ────────────────────────────────────────────────────────── */
const round4 = n => (typeof n === 'number' && Number.isFinite(n) ? Math.round(n * 1e4) / 1e4 : n);

const calcCases = cases.map(({ tag, state }) => {
  const r = app.calc(state);
  const psu = app.calcPSU(state);
  return {
    tag,
    state,
    expected: {
      avg: r.avg, min: r.min, max: r.max,
      avgL: r.avgL, avgH: r.avgH, minL: r.minL, minH: r.minH, maxL: r.maxL, maxH: r.maxH,
      cBot: r.cBot, vBot: r.vBot, rBot: r.rBot,
      gpuWarn: r.gpuWarn, moboWarn: r.moboWarn,
      vNeed: r.vNeed, vAvail: r.vAvail,
      fgM: r.fgM, bFPS: r.bFPS, bMin: r.bMin,
      cpuCap: r.cpuCap, gpuFps: r.gpuFps,
      psuMin: psu.min, psuRecommended: psu.recommended, psuTotal: psu.total,
      // Os passos capturam a ORDEM das multiplicações — é o que pega
      // divergência de ponto flutuante que o resultado arredondado esconde.
      steps: r.steps.map(s => ({ t: s.t, m: round4(s.m ?? null), cap: s.cap === true, fps: round4(s.fps) })),
    },
  };
});

/* Paridade de normalizeS: estado cru -> estado corrigido. */
const normCases = [];
for (let i = 0; i < 1200; i++) {
  const raw = {
    game: Math.floor(rnd() * G.length),
    cpu: Math.floor(rnd() * C.length),
    gpu: Math.floor(rnd() * GP.length),
    mobo: Math.floor(rnd() * MB.length),
    ram: pick(RAM), res: pick(RES), preset: pick(PRESET_KEYS),
    rt: pick(RT), fg: pick(FG), dlss: pick(DLSS),
    monHz: pick([60, 144, 210, 300]), hoursDay: 3, tariff: 0.95,
  };
  const norm = { ...raw };
  app.normalizeS(norm);
  normCases.push({ raw, normalized: norm });
}

const meta = {
  generatedFrom: 'app/src/main/assets/www/index.html',
  dbVersion: app.DB_META.v,
  counts: { games: G.length, cpus: C.length, gpus: GP.length, mobos: MB.length },
};

writeFileSync(resolve(OUT, 'golden-calc.json'),
  JSON.stringify({ meta, cases: calcCases }) + '\n', 'utf8');
writeFileSync(resolve(OUT, 'golden-normalize.json'),
  JSON.stringify({ meta, cases: normCases }) + '\n', 'utf8');

const bots = calcCases.filter(c => c.expected.cBot).length;
const vbots = calcCases.filter(c => c.expected.vBot).length;
const warns = calcCases.filter(c => c.expected.gpuWarn).length;
console.log(`golden-calc.json      — ${calcCases.length} casos`);
console.log(`golden-normalize.json — ${normCases.length} casos`);
console.log(`cobertura: ${bots} com teto de CPU, ${vbots} com VRAM estourada, ${warns} com aviso de GPU`);
