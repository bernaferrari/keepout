// Working sheet: Material Symbols Rounded candidates per concept (filled / outlined), used to pick C.
const fs = require('fs');
const path = require('path');
const { chromium } = require(process.env.HOME + '/.cache/weakspot-shots/node_modules/playwright');
const HERE = __dirname;
const C = JSON.parse(process.argv[2]);
const sym = (n, f) => {
  const p = path.join(HERE, 'symbols', `${n}_${f}.json`);
  if (!fs.existsSync(p)) return '<i>missing</i>';
  return `<svg viewBox="0 0 24 24">${JSON.parse(fs.readFileSync(p)).map((x) => `<path d="${x.d}" fill-rule="${x.eo ? 'evenodd' : 'nonzero'}"/>`).join('')}</svg>`;
};
const html = `<body style="margin:0;padding:16px;background:#FFF8F7;font:13px sans-serif;width:1800px;color:#231918">
${Object.entries(C).map(([concept, names]) => `<div style="display:flex;align-items:center;gap:10px;margin:6px 0"><b style="width:150px">${concept}</b>
${names.map((n) => `<div style="text-align:center;width:118px;background:#fff;border-radius:10px;padding:6px 2px"><div style="display:flex;justify-content:center;gap:6px;fill:#594042">${sym(n, 1).replace('<svg', '<svg width="40" height="40"')}${sym(n, 0).replace('<svg', '<svg width="40" height="40"')}</div>${n}</div>`).join('')}</div>`).join('')}</body>`;
(async () => {
  const b = await chromium.launch();
  const p = await b.newPage();
  await p.setContent(html);
  await p.screenshot({ path: process.argv[3], fullPage: true });
  await b.close();
})();
