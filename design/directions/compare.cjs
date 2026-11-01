// Builds compare.png: the three directions side by side (Home, Logs, Settings), ~2400 px wide.
const fs = require('fs'), path = require('path');
const { chromium } = require(process.env.HOME + '/.cache/weakspot-shots/node_modules/playwright');
const D = [
  ['ledger', 'A · Ledger', 'A daily record of who your phone talked to. Paper, ink, and one red pencil for what was stopped.', '#F6F1E9', '#1F1A16'],
  ['seal', 'B · Seal', 'The keep-out sign, everywhere. Enamel plates, the scalloped seal, condensed caps. Only the stops are loud.', '#2A0F1C', '#FFF2EE'],
  ['trace', 'C · Trace', 'A precision instrument for your network. Everything is a readout; the live trace is the hero.', '#0A0B0D', '#E9EAEC'],
];
const html = `<html><head><link href="https://fonts.googleapis.com/css2?family=Geist:wght@400;600&display=block" rel="stylesheet"><style>
body{margin:0;width:2400px;background:#d9d6d1;font-family:Geist,sans-serif;display:flex;gap:24px;padding:28px;box-sizing:border-box}
.col{flex:1;border-radius:22px;padding:24px 20px 22px}
h2{margin:0;font-size:30px;letter-spacing:-.02em} p{margin:8px 0 18px;font-size:15px;line-height:1.35;opacity:.75;min-height:42px}
.ph{display:flex;gap:10px} img{width:calc((100% - 20px)/3);border-radius:14px;box-shadow:0 8px 24px #0003}
</style></head><body>${D.map(([s, n, c, bg, fg]) => `<div class="col" style="background:${bg};color:${fg}"><h2>${n}</h2><p>${c}</p><div class="ph">${['home', 'logs', 'settings'].map(f => `<img src="${path.join(__dirname, s, f + '.png')}">`).join('')}</div></div>`).join('')}</body></html>`;
fs.writeFileSync(path.join(__dirname, '_assets', 'compare.html'), html);
(async () => {
  const b = await chromium.launch(); const p = await b.newPage({ viewport: { width: 2400, height: 800 } });
  await p.goto('file://' + path.join(__dirname, '_assets', 'compare.html'), { waitUntil: 'networkidle' });
  await p.screenshot({ path: path.join(__dirname, 'compare.png'), fullPage: true }); await b.close();
})();
