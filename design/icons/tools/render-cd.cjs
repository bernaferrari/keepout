// Sheets for proposals C and D: composites on the real captures, the per-icon table, and the
// label-free test sheet.   node design/icons/tools/render-cd.cjs
const fs = require('fs');
const path = require('path');
const { chromium } = require(process.env.PLAYWRIGHT_MODULE || (process.env.HOME + '/.cache/weakspot-shots/node_modules/playwright'));

const HERE = __dirname;
const OUT = path.resolve(HERE, '..');
const RAW = path.resolve(HERE, '../../../store-listing/en-US/raw/phone');
const M = JSON.parse(fs.readFileSync(path.join(HERE, 'manifest-cd.json'), 'utf8'));
const COMP = JSON.parse(fs.readFileSync(path.join(HERE, 'composite-cd.json'), 'utf8'));
const COMP_DIR = path.join(OUT, 'composites');
const CSS = `*{box-sizing:border-box;margin:0;padding:0}body{font-family:'Google Sans','Roboto',system-ui,sans-serif;-webkit-font-smoothing:antialiased}svg{display:block}`;

const icon = (style, key, color, sz) => M.styles[style][key].replace('width="24" height="24"', `width="${sz}" height="${sz}" style="color:${color}"`);
const png = (f) => 'data:image/png;base64,' + fs.readFileSync(f).toString('base64');
const sym = (name) => {
  const f = path.join(HERE, 'symbols', `${name}_1.json`);
  if (!fs.existsSync(f)) return null;
  return `<svg viewBox="0 0 24 24" width="30" height="30" style="color:#594042">${JSON.parse(fs.readFileSync(f)).map((x) => `<path fill="currentColor" fill-rule="${x.eo ? 'evenodd' : 'nonzero'}" d="${x.d}"/>`).join('')}</svg>`;
};

function compositeHtml(screen, style) {
  const overlays = COMP[screen].map((it) => {
    const [x0, y0, x1, y1] = it.patch;
    const box = (sz, inner) => `<div style="position:absolute;left:${it.cx - sz / 2}px;top:${it.cy - sz / 2}px;width:${sz}px;height:${sz}px;display:grid;place-items:center">${inner}</div>`;
    if (it.key === 'badge') {
      return `<div style="position:absolute;left:${it.cx - 26}px;top:${it.cy - 26}px;width:52px;height:52px;border-radius:50%;background:${it.bg}"></div>`
        + box(44, icon(style, it.verdict, it.tint, 44));
    }
    return `<div style="position:absolute;left:${x0}px;top:${y0}px;width:${x1 - x0}px;height:${y1 - y0}px;background:${it.bg};border-radius:10px"></div>` + box(it.size, icon(style, it.key, it.tint, it.size));
  }).join('');
  return `<html><head><style>*{margin:0}body{width:1080px;height:2340px;position:relative;overflow:hidden}</style></head><body>
    <img src="${png(path.join(RAW, screen + '.png'))}" style="position:absolute;left:0;top:0;width:1080px">${overlays}</body></html>`;
}

const SHOT = (style, screen) => png(path.join(COMP_DIR, style, screen + '.png'));

function zoomSheet() {
  const crops = [
    ['Settings hub tiles', '08-settings', 30, 545, 1020, 1100],
    ['Settings hub tiles · dark', '13-settings-dark', 30, 545, 1020, 1100],
    ['Logs: tabs, filter chips, verdict badges', '02-logs', 0, 90, 1080, 900],
    ['App detail: blocked pill + toggle rows + rules (Wi-Fi/data cards unchanged)', '07-app-detail', 30, 600, 1020, 1430],
    ['DNS type cards + blocklists', '09-dns', 30, 1050, 1020, 1020],
    ['Home: hero mark (shield + check in both; bar-in-seal withdrawn after the label-free test) + counters', '01-home', 0, 440, 1080, 1180],
    ['Bottom navigation (Settings selected) · Apps toolbar', '03-apps', 0, 2070, 1080, 200],
    ['Bottom navigation (Home selected)', '01-home', 0, 2070, 1080, 200],
    ['Apps toolbar: Rules (Wi-Fi/data toggles unchanged)', '03-apps', 0, 90, 1080, 600],
  ];
  const s = 0.62;
  const cell = (style, [, screen, x, y, w, h]) => `<div class="crop" style="width:${w * s}px;height:${h * s}px"><img src="${SHOT(style, screen)}" style="margin-left:${-x * s}px;margin-top:${-y * s}px;width:${1080 * s}px"></div>`;
  return `<html><head><style>${CSS}
  body{background:#E9E1E1;padding:44px;width:2120px}
  h1{font-size:44px;color:#231918;letter-spacing:-1px}.sub{font-size:19px;color:#5d4a4a;margin:8px 0 22px;line-height:1.4}
  .hdr{display:grid;grid-template-columns:repeat(3,1fr);gap:16px;font-size:22px;font-weight:700;color:#231918;margin-bottom:8px}
  h2{font-size:18px;color:#B42541;margin:26px 0 10px}
  .trio{display:grid;grid-template-columns:repeat(3,1fr);gap:16px;align-items:start}
  .crop{overflow:hidden;border-radius:16px;background:#fff}.crop img{display:block}
  </style></head><body><h1>Proposals C and D on the real screens</h1>
  <div class="sub">Store captures with only the changed icons repainted (same position, size, tint). Glyphs the proposals keep — Wi-Fi/data pair, filter, back, more, search — are left as captured.</div>
  <div class="hdr"><div>Current</div><div>C · Refined Material</div><div>D · Material + Keepout touch</div></div>
  ${crops.map((c) => `<h2>${c[0]}</h2><div class="trio">${cell('current', c)}${cell('c', c)}${cell('d', c)}</div>`).join('')}</body></html>`;
}

function tableSheet() {
  const row = (r) => {
    const curName = r.current.split(' ')[0].replace(/[^a-z_]/g, '');
    const cur = sym(curName) || `<small>${r.current}</small>`;
    const same = !r.d_differs;
    return `<tr><td class="g">${cur}</td><td class="g">${icon('c', r.key, '#594042', 30)}</td><td class="g ${same ? 'same' : ''}">${icon('d', r.key, '#594042', 30)}</td>
      <td><b>${r.concept}</b><div class="src">C: ${r.c_src} · D: ${r.d_src}</div></td><td class="why">${r.why}</td></tr>`;
  };
  return `<html><head><style>${CSS}
  body{background:#FFF8F7;padding:40px;width:2000px;color:#231918}
  h1{font-size:40px;letter-spacing:-1px}.sub{font-size:18px;color:#5d4a4a;margin:6px 0 20px}
  table{border-collapse:separate;border-spacing:0 6px;width:100%}
  th{text-align:left;font-size:14px;color:#6E5B5B;font-weight:600;padding:0 12px}
  td{background:#FFF0F0;padding:10px 12px;vertical-align:middle;font-size:15px}
  td:first-child{border-radius:14px 0 0 14px}td:last-child{border-radius:0 14px 14px 0}
  td.g{width:70px}td.g svg{margin:auto}td.same{opacity:.35}
  .src{font-size:12px;color:#6E5B5B;margin-top:3px}.why{color:#4a3a3a;width:46%}
  td small{font-size:11px;color:#6E5B5B}
  </style></head><body><h1>Every concept: current → C → D</h1>
  <div class="sub">One glyph per concept. C = Material Symbols Rounded 400 (custom only where Material has no convention). D = C plus a Keepout detail where it reinforces meaning; faded D cells are identical to C.</div>
  <table><tr><th>Current</th><th>C</th><th>D</th><th>Concept</th><th>Why</th></tr>${M.rows.map(row).join('')}</table></body></html>`;
}

function blindSheet(style) {
  // No labels, numbered only, in a shuffled but fixed order — for the label-free test.
  const keys = M.order.filter((k) => !/_selected$/.test(k));
  const order = keys.map((k, i) => [k, (i * 37) % keys.length]).sort((a, b) => a[1] - b[1]).map((x) => x[0]);
  fs.writeFileSync(path.join(HERE, `blind-${style}.json`), JSON.stringify(order));
  return `<html><head><style>${CSS}body{background:#fff;padding:24px;width:1240px;font:16px sans-serif;color:#333}
  .g{display:grid;grid-template-columns:repeat(8,1fr);gap:14px}.c{display:flex;flex-direction:column;align-items:center;gap:6px;padding:10px;border:1px solid #eee;border-radius:12px}
  .r{display:flex;align-items:end;gap:10px}</style></head><body><div class="g">${order.map((k, i) => `<div class="c"><div class="r">${icon(style, k, '#1f1f1f', 24)}${icon(style, k, '#1f1f1f', 48)}</div><b>${i + 1}</b></div>`).join('')}</div></body></html>`;
}

(async () => {
  const browser = await chromium.launch();
  const cpage = await browser.newPage({ viewport: { width: 1080, height: 2340 } });
  for (const screen of Object.keys(COMP)) {
    for (const style of ['c', 'd']) {
      fs.mkdirSync(path.join(COMP_DIR, style), { recursive: true });
      await cpage.setContent(compositeHtml(screen, style));
      await cpage.screenshot({ path: path.join(COMP_DIR, style, screen + '.png') });
    }
  }
  const page = await browser.newPage();
  for (const [file, html] of [['cd-in-context.png', zoomSheet()], ['cd-table.png', tableSheet()], ['tools/blind-c.png', blindSheet('c')], ['tools/blind-d.png', blindSheet('d')]]) {
    await page.setContent(html);
    await page.screenshot({ path: path.join(OUT, file), fullPage: true });
    console.log('wrote', file);
  }
  await browser.close();
})();
