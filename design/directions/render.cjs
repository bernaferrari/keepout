// Renders every direction's mocks at 412x915 dp @2x. Usage: node render.cjs [slug ...]
const path = require('path');
const { chromium } = require(process.env.HOME + '/.cache/weakspot-shots/node_modules/playwright');
const root = __dirname;
const slugs = process.argv.slice(2).length ? process.argv.slice(2) : ['ledger', 'seal', 'trace'];
(async () => {
  const b = await chromium.launch();
  const p = await b.newPage({ viewport: { width: 412, height: 915 }, deviceScaleFactor: 2 });
  for (const s of slugs) for (const [src, out] of [['home.html', 'home'], ['logs.html', 'logs'], ['settings.html', 'settings'], ['home.html#dark', 'home-dark']]) {
    await p.goto('file://' + path.join(root, s, src.split('#')[0]) + (src.includes('#') ? '#dark' : ''));
    await p.reload({ waitUntil: 'networkidle' });
    await p.evaluate(() => document.fonts.ready);
    await p.waitForTimeout(400); await p.evaluate(() => document.fonts.ready);
    await p.screenshot({ path: path.join(root, s, out + '.png') });
    console.log(s, out);
  }
  await b.close();
})();
