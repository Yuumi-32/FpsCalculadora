/* Utilitários compartilhados: isola os blocos <script> do index.html e
   avalia-os num sandbox com stubs mínimos de DOM, para que os dados e a
   lógica de cálculo possam ser lidos SEM copiar/colar (fidelidade 1:1). */
import { readFileSync } from 'node:fs';
import { fileURLToPath } from 'node:url';
import { dirname, resolve } from 'node:path';
import vm from 'node:vm';

export const ROOT = resolve(dirname(fileURLToPath(import.meta.url)), '..');
// A UI WebView ficou só no build de debug quando a Compose foi publicada,
// mas o HTML segue sendo a fonte da verdade dos dados e dos vetores golden.
export const INDEX_HTML = resolve(ROOT, 'app/src/debug/assets/www/index.html');

/** Devolve o conteúdo de cada bloco <script> sem atributos (os inline do app). */
export function scriptBlocks(html = readFileSync(INDEX_HTML, 'utf8')) {
  const out = [];
  const re = /<script>([\s\S]*?)<\/script>/g;
  let m;
  while ((m = re.exec(html)) !== null) out.push(m[1]);
  return out;
}

/** Remove o bootstrap que dispara init() ao final do bloco de lógica. */
function stripBootstrap(src) {
  const i = src.indexOf("if (document.readyState === 'loading')");
  return i === -1 ? src : src.slice(0, i);
}

/** Stubs de DOM suficientes para *declarar* as funções sem executá-las. */
function sandbox() {
  const noop = () => {};
  const el = new Proxy({}, {
    get: (t, k) => {
      if (k === 'classList') return { add: noop, remove: noop, toggle: noop, contains: () => false };
      if (k === 'style' || k === 'dataset') return {};
      if (k === 'getBoundingClientRect') return () => ({ top: 0, left: 0, width: 0, height: 0 });
      if (k === 'querySelectorAll') return () => [];
      if (k === 'addEventListener' || k === 'setAttribute' || k === 'appendChild') return noop;
      if (k === 'value' || k === 'textContent' || k === 'innerHTML') return '';
      return undefined;
    },
    set: () => true,
  });
  const store = new Map();
  const ctx = {
    console,
    document: {
      getElementById: () => el,
      createElement: () => el,
      querySelectorAll: () => [],
      addEventListener: noop,
      readyState: 'complete',
      body: el,
    },
    window: {
      addEventListener: noop,
      matchMedia: () => ({ matches: false }),
      scrollTo: noop,
      scrollY: 0,
    },
    navigator: {},
    localStorage: {
      getItem: k => (store.has(k) ? store.get(k) : null),
      setItem: (k, v) => store.set(k, String(v)),
      removeItem: k => store.delete(k),
    },
    history: { pushState: noop, back: noop },
    setTimeout, clearTimeout, IntersectionObserver: undefined,
  };
  ctx.globalThis = ctx;
  return vm.createContext(ctx);
}

/**
 * Avalia os blocos de DADOS + LÓGICA como UM único script — no navegador eles
 * compartilham o escopo léxico global, e `vm.runInContext` cria um escopo novo
 * por chamada, então concatenar é o que reproduz o comportamento real.
 * Declarações `const`/`let` de topo não viram propriedades do global, por isso
 * um trecho final exporta os símbolos que interessam.
 */
const EXPORTED = [
  'G', 'C', 'GP', 'MB', 'HZ_MARKERS', 'PRESETS', 'DLSS', 'FSR', 'XESS', 'DB_META',
  'VRAM_PRE', 'RAM_LABELS', 'PRESETS_BUILD', 'SYSTEM_W',
  'calc', 'calcPSU', 'vramRT', 'normalizeS', 'st', 'badge',
  'isRadeon', 'isArc', 'hasRTHW', 'canRRHW', 'hasFGHW',
  'ramOptionsFor', 'rtOptionsFor', 'fgOptionsFor', 'dlssOptionsFor', 'upscalerName',
];

export function loadAppContext({ withLogic = true } = {}) {
  const blocks = scriptBlocks();
  const dataBlock = blocks.find(b => b.includes('const G = ['));
  const logicBlock = blocks.find(b => b.includes('function calc('));
  if (!dataBlock) throw new Error('bloco de dados não encontrado no index.html');
  if (withLogic && !logicBlock) throw new Error('bloco de lógica não encontrado no index.html');

  const names = withLogic ? EXPORTED : EXPORTED.slice(0, EXPORTED.indexOf('calc'));
  const src = [
    dataBlock,
    withLogic ? stripBootstrap(logicBlock) : '',
    `globalThis.__app = { ${names.join(', ')} };`,
  ].join('\n;\n');

  const ctx = sandbox();
  vm.runInContext(src, ctx, { filename: 'index.html' });
  return ctx.__app;
}
