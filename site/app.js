import {loadLiteRt, loadAndCompile, Tensor} from './vendor/litert.js';
const $ = id => document.getElementById(id);
const CLASSES = ["person","bicycle","car","motorcycle","airplane","bus","train","truck","boat","traffic light","fire hydrant","stop sign","parking meter","bench","bird","cat","dog","horse","sheep","cow","elephant","bear","zebra","giraffe","backpack","umbrella","handbag","tie","suitcase","frisbee","skis","snowboard","sports ball","kite","baseball bat","baseball glove","skateboard","surfboard","tennis racket","bottle","wine glass","cup","fork","knife","spoon","bowl","banana","apple","sandwich","orange","broccoli","carrot","hot dog","pizza","donut","cake","chair","couch","potted plant","bed","dining table","toilet","tv","laptop","mouse","remote","keyboard","cell phone","microwave","oven","toaster","sink","refrigerator","book","clock","vase","scissors","teddy bear","hair drier","toothbrush"];
const RELEVANT = new Set(["bottle","wine glass","cup","fork","knife","spoon","bowl","book","scissors","laptop","mouse","remote","keyboard","cell phone","backpack","handbag","suitcase","sports ball","teddy bear","vase","potted plant","clock","banana","apple","orange","sandwich","pizza","donut","cake","toothbrush","umbrella"]);
const photos = {B: null, A: null};
let model = null;

function setPhoto(which, file) {
  if (!file) return;
  const img = new Image();
  img.onload = () => { photos[which] = img; $('img' + which).src = img.src; update(); };
  img.src = URL.createObjectURL(file);
}
for (const w of ['B', 'A']) for (const s of ['', '2'])
  $('f' + w + s).addEventListener('change', e => setPhoto(w, e.target.files[0]));
function update() { $('go').disabled = !(photos.B && photos.A); if (photos.B && photos.A) $('status').textContent = 'Ready.'; }

// Same steps as the Android app: decode to max 512, center-crop AFTER to BEFORE's size.
function toCanvas(img, maxDim) {
  const s = Math.min(1, maxDim / Math.max(img.naturalWidth, img.naturalHeight));
  const c = document.createElement('canvas');
  c.width = Math.max(1, Math.round(img.naturalWidth * s)); c.height = Math.max(1, Math.round(img.naturalHeight * s));
  c.getContext('2d', {willReadFrequently: true}).drawImage(img, 0, 0, c.width, c.height);
  return c;
}
function cropTo(src, w, h) {
  const sa = src.width / src.height, da = w / h;
  let sx = 0, sy = 0, sw = src.width, sh = src.height;
  if (sa > da) { sw = Math.floor(src.height * da); sx = Math.floor((src.width - sw) / 2); }
  else { sh = Math.floor(src.width / da); sy = Math.floor((src.height - sh) / 2); }
  const c = document.createElement('canvas'); c.width = w; c.height = h;
  c.getContext('2d', {willReadFrequently: true}).drawImage(src, sx, sy, sw, sh, 0, 0, w, h);
  return c;
}
function changeMask(a, b, threshold = 48) {
  const lim = threshold * threshold, mask = new Uint8Array(a.length / 4);
  for (let i = 0; i < mask.length; i++) {
    const dr = a[i*4] - b[i*4], dg = a[i*4+1] - b[i*4+1], db = a[i*4+2] - b[i*4+2];
    mask[i] = dr*dr + dg*dg + db*db > lim ? 1 : 0;
  }
  return mask;
}
function overlay(base, mask) {
  const out = new Uint8ClampedArray(base.length);
  for (let i = 0; i < mask.length; i++) {
    const r = base[i*4], g = base[i*4+1], b = base[i*4+2];
    if (mask[i]) { out[i*4] = 255; out[i*4+1] = Math.floor(g/3); out[i*4+2] = Math.floor(b/3); }
    else { out[i*4] = r >> 1; out[i*4+1] = g >> 1; out[i*4+2] = b >> 1; }
    out[i*4+3] = 255;
  }
  return out;
}
function iou(a, b) {
  const ax1=a.cx-a.w/2, ay1=a.cy-a.h/2, ax2=a.cx+a.w/2, ay2=a.cy+a.h/2;
  const bx1=b.cx-b.w/2, by1=b.cy-b.h/2, bx2=b.cx+b.w/2, by2=b.cy+b.h/2;
  const iw = Math.max(0, Math.min(ax2,bx2)-Math.max(ax1,bx1)), ih = Math.max(0, Math.min(ay2,by2)-Math.max(ay1,by1));
  const inter = iw*ih; return inter / (a.w*a.h + b.w*b.h - inter + 1e-9);
}
function decode(out, candidates = 8400, channels = 84, thr = 0.45, iouThr = 0.5) {
  const dets = [];
  for (let i = 0; i < candidates; i++) {
    let best = -1, bs = 0;
    for (let c = 4; c < channels; c++) { const s = out[c*candidates + i]; if (s > bs) { bs = s; best = c - 4; } }
    if (bs >= thr) dets.push({cls: best, score: bs, cx: out[i], cy: out[candidates+i], w: out[2*candidates+i], h: out[3*candidates+i]});
  }
  dets.sort((a, b) => b.score - a.score);
  const kept = [];
  for (const d of dets) if (!kept.some(k => iou(k, d) >= iouThr)) kept.push(d);
  return kept;
}
async function ensureModel() {
  if (model) return;
  $('status').textContent = 'Loading model (4.6 MB, once)...';
  await loadLiteRt(new URL('./wasm/', location.href).href);
  model = await loadAndCompile(new URL('./model/yolo11n-int8.tflite', location.href).href, {accelerator: 'wasm'});
}
async function countRelevant(canvas) {
  const S = 640, c = document.createElement('canvas'); c.width = c.height = S;
  const ctx = c.getContext('2d', {willReadFrequently: true});
  ctx.drawImage(canvas, 0, 0, S, S);  // stretched, like the Android app
  const px = ctx.getImageData(0, 0, S, S).data, n = S * S, inp = new Int8Array(3 * n);
  for (let i = 0; i < n; i++) { inp[i] = px[i*4] - 128; inp[n+i] = px[i*4+1] - 128; inp[2*n+i] = px[i*4+2] - 128; }
  const t = new Tensor(inp, [1, 3, S, S]);
  const res = await model.run(t);
  t.delete();
  const o = await res[0].moveTo('wasm');
  const data = Float32Array.from(o.toTypedArray());
  const shape = [...o.shape];
  o.delete();
  if (data.length !== 84 * 8400) throw new Error('unexpected model output shape ' + shape.join('x'));
  const counts = {};
  for (const d of decode(data)) { const n = CLASSES[d.cls]; if (RELEVANT.has(n)) counts[n] = (counts[n] || 0) + 1; }
  return counts;
}
function sum(o) { return Object.values(o).reduce((a, b) => a + b, 0); }
function verdict(before, after) {
  const bt = sum(before), at = sum(after);
  if (bt === 0 && at === 0) return "Can't assess this one: no recognizable items in either photo, so there is nothing to count. The visible-change view below is the only evidence here, and changed pixels don't say whether things are clean.";
  if (at < bt) {
    const parts = []; for (const [k, b] of Object.entries(before)) { const a = after[k] || 0; if (a < b) parts.push(`${k} ${b}->${a}`); }
    return `Looks cleared: ${bt} recognized items before, ${at} after` + (parts.length ? ` (${parts.join(', ')}).` : '.') + " This only counts objects the detector knows. It can't see dirt, grease or whether you actually washed them.";
  }
  if (at > bt) return `More recognized items after (${bt} -> ${at}). Could be new items in frame, a different angle, or detector noise. Not a judgment.`;
  return `Same number of recognized items before and after (${bt}). That says nothing about whether they were washed or wiped.`;
}
$('go').addEventListener('click', async () => {
  $('go').disabled = true;
  try {
    const t0 = performance.now();
    const b = toCanvas(photos.B, 512), a = cropTo(toCanvas(photos.A, 512), b.width, b.height);
    const bd = b.getContext('2d').getImageData(0, 0, b.width, b.height).data;
    const ad = a.getContext('2d').getImageData(0, 0, a.width, a.height).data;
    const mask = changeMask(bd, ad);
    const pct = mask.reduce((x, y) => x + y, 0) * 100 / mask.length;
    const ov = $('ov'); ov.width = a.width; ov.height = a.height;
    ov.getContext('2d').putImageData(new ImageData(overlay(ad, mask), a.width, a.height), 0, 0);
    $('pct').textContent = pct.toFixed(1) + '%';
    await ensureModel();
    $('status').textContent = 'Counting objects...';
    const cb = await countRelevant(b), ca = await countRelevant(a);
    const names = [...new Set([...Object.keys(cb), ...Object.keys(ca)])].sort();
    $('tb').replaceChildren(...(names.length ? names : ['(none recognized)']).map(n => {
      const tr = document.createElement('tr');
      for (const v of names.length ? [n, cb[n] || 0, ca[n] || 0] : [n, '', '']) { const td = document.createElement('td'); td.textContent = v; tr.appendChild(td); }
      return tr; }));
    $('verdict').textContent = verdict(cb, ca);
    $('out').hidden = false;
    $('status').textContent = `Done in ${((performance.now() - t0) / 1000).toFixed(1)} s.`;
  } catch (e) {
    $('status').textContent = 'Error: ' + e.message + '. The pixel-change view still shows if it appeared; object counting needs a browser with WebAssembly.';
    $('out').hidden = $('pct').textContent === '';
  }
  $('go').disabled = false;
});
