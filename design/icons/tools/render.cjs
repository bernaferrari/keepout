// Renders the icon presentation sheets from manifest.json.
// node design/icons/tools/render.cjs [preview]
const fs = require('fs');
const path = require('path');
const { chromium } = require(process.env.PLAYWRIGHT_MODULE || (process.env.HOME + '/.cache/weakspot-shots/node_modules/playwright'));

const HERE = __dirname;
const OUT = path.resolve(HERE, '..');
const RAW = path.resolve(HERE, '../../../store-listing/en-US/raw/phone');
const manifest = JSON.parse(fs.readFileSync(path.join(HERE, 'manifest.json'), 'utf8'));

const LABEL = (n) => n.replace(/_/g, ' ');
const icon = (style, name) => manifest.styles[style][name].replace('width="24" height="24"', '');
const img = (f) => 'data:image/png;base64,' + fs.readFileSync(path.join(RAW, f)).toString('base64');

// Keepout palette (sampled from the current app captures and ConfigureAccents.kt)
const PAL = {
  light: { bg: '#FFF8F7', card: '#FFF0F0', nav: '#FFE9E9', pill: '#FB979F', ink: '#231918', sub: '#6E5B5B', primary: '#B42541', bar: '#E8455A' },
  dark: { bg: '#120809', card: '#2B1D1E', nav: '#2F2021', pill: '#7B3039', ink: '#F7DEDE', sub: '#C9A9AA', primary: '#FFB3B8', bar: '#FF8A7A' },
};
const ACCENTS = { apps: '#74C5FF', dns: '#C5ACFF', firewall: '#FF907F', proxy: '#46EBC8', network: '#A3BCFF', general: '#FFD878', logs: '#7EED92', anti_censorship: '#FFA7E0', advanced: '#FFE182' };

const BASE_CSS = `
*{box-sizing:border-box;margin:0;padding:0}
body{font-family:'Google Sans','Product Sans','Roboto',system-ui,sans-serif;-webkit-font-smoothing:antialiased}
svg{display:block}
.duo #bar{fill:var(--bar)}
`;

function sheet(style) {
  const title = style === 'a' ? 'Style A — Line & Bar' : 'Style B — Cutout';
  const blurb = style === 'a'
    ? '2dp round line (Material Symbols Outlined weight) on a 24dp grid, 20dp live area. One filled pill per glyph: the keep-out bar. "Blocked" is always the bar laid across with a 1.25dp gap — never a diagonal slash. Selected = solid silhouette, bar knocked out.'
    : 'Solid silhouettes at Material Symbols Rounded Filled weight; detail is knocked out of the ink, never drawn on it. A 38% ghost layer carries secondary context. The keep-out bar is cut out of solid ink or laid solid over a gap. Nav: outlined + solid bar when unselected, solid with the bar cut out when selected.';
  const panel = (mode) => {
    const p = PAL[mode];
    const groups = manifest.order.filter(([g, names]) => names.some((n) => manifest.styles[style][n])).map(([g, names]) => `
      <div class="group"><h3>${g}</h3><div class="grid">
      ${names.filter((n) => manifest.styles[style][n]).map((n) => `<div class="cell">
          <div class="big duo">${icon(style, n)}</div>
          <div class="row"><div class="s1">${icon(style, n)}</div><div class="s1 duo">${icon(style, n)}</div></div>
          <div class="lbl">${LABEL(n)}</div></div>`).join('')}
      </div></div>`).join('');
    return `<section class="panel ${mode}" style="--bg:${p.bg};--ink:${p.ink};--sub:${p.sub};--bar:${p.bar};--card:${p.card}">
      <div class="mode">${mode === 'light' ? 'Light' : 'Dark'} · 48dp (2×) in two-tone · 24dp (1×) mono and two-tone</div>${groups}</section>`;
  };
  return `<html><head><style>${BASE_CSS}
  body{background:#EDE6E6;width:2400px;padding:56px}
  header{margin-bottom:36px} header h1{font-size:52px;letter-spacing:-1px;color:#231918} header p{font-size:22px;color:#5d4a4a;max-width:1500px;margin-top:10px;line-height:1.4}
  .wrap{display:grid;grid-template-columns:1fr 1fr;gap:32px}
  .panel{background:var(--bg);color:var(--ink);border-radius:32px;padding:32px 36px}
  .mode{font-size:16px;color:var(--sub);margin-bottom:12px;letter-spacing:.3px}
  .group h3{font-size:15px;text-transform:uppercase;letter-spacing:1.4px;color:var(--bar);margin:22px 0 12px}
  .grid{display:grid;grid-template-columns:repeat(6,1fr);gap:10px}
  .cell{background:var(--card);border-radius:18px;padding:14px 8px 10px;display:flex;flex-direction:column;align-items:center;gap:8px;color:var(--ink)}
  .big svg{width:48px;height:48px}
  .row{display:flex;gap:10px}.s1 svg{width:24px;height:24px}
  .lbl{font-size:13px;color:var(--sub);text-align:center}
  </style></head><body><header><h1>Keepout icons · ${title}</h1><p>${blurb}</p></header>
  <div class="wrap">${panel('light')}${panel('dark')}</div></body></html>`;
}

// ------------------------------------------------------------ in-context comparison
function navBar(style, mode, sel) {
  const p = PAL[mode];
  const items = [['home', 'Home'], ['stats', 'Stats'], ['settings', 'Settings']];
  return `<div class="nav" style="background:${p.nav}">${items.map(([k, l]) => {
    const on = k === sel;
    return `<div class="ni"><div class="np" style="background:${on ? p.pill : 'transparent'};color:${on ? (mode === 'light' ? '#5C0A1A' : '#FFDADC') : p.ink}">${icon(style, on ? k + '_selected' : k)}</div><div class="nl" style="color:${on ? p.primary : p.ink};font-weight:${on ? 700 : 500}">${l}</div></div>`;
  }).join('')}</div>`;
}

function tiles(style, mode) {
  const p = PAL[mode];
  const rows = [['apps', 'Apps'], ['dns', 'DNS'], ['firewall', 'Firewall'], ['proxy', 'Proxy'], ['network', 'Network'], ['general', 'General'], ['logs', 'Logs'], ['anti_censorship', 'Anti-censorship'], ['advanced', 'Advanced']];
  return `<div class="tiles">${rows.map(([k, l]) => {
    const a = ACCENTS[k];
    const well = mode === 'light' ? a : mix(a, '#000000', 0.62);
    const ink = mode === 'light' ? mix(a, '#000000', 0.78) : mix(a, '#FFFFFF', 0.35);
    return `<div class="tile" style="background:${p.card}"><div class="well" style="background:${well};color:${ink}">${icon(style, k)}</div><span style="color:${p.ink}">${l}</span></div>`;
  }).join('')}</div>`;
}

function mix(a, b, t) {
  const h = (s) => [1, 3, 5].map((i) => parseInt(s.slice(i, i + 2), 16));
  const [x, y] = [h(a), h(b)];
  return '#' + x.map((v, i) => Math.round(v * (1 - t) + y[i] * t).toString(16).padStart(2, '0')).join('');
}

function logRows(style, mode) {
  const p = PAL[mode];
  const rows = [
    ['firebaselogging.googleapis.com', 'App blocked', 'blocked', '#4F7BF7'],
    ['mail.google.com', '↓ 1.7 MB', 'allowed', '#EA4335'],
    ['googleads.g.doubleclick.net', 'DNS blocked', 'blocked', '#34A853'],
    ['update.signal.org', 'Bypassed', 'bypassed', '#3A76F0'],
    ['dns.quad9.net', 'Trusted app', 'trusted', '#7B5CF0'],
  ];
  const color = { blocked: '#C2273B', allowed: '#1E7A43', bypassed: '#9A5B00', trusted: '#1E6E8C' };
  return `<div class="logs">${rows.map(([d, t, v, c]) => `<div class="lr" style="background:${p.card}">
    <div class="app" style="background:${c}">${badge(style, v, color[v], p.card)}</div>
    <div class="lt"><div style="color:${p.ink}">${d}</div><div class="chip" style="color:${color[v]};background:${color[v]}1f">${v === 'allowed' ? '' : `<i>${icon(style, v)}</i>`}${t}</div></div></div>`).join('')}</div>`;
}

// A: verdict-coloured disc (seal for trusted) with a white mark. B: the solid verdict icon itself.
function badge(style, v, col, ring) {
  if (style === 'b') return `<div class="badge solid" style="background:${ring};color:${col}">${icon('b', v)}</div>`;
  const shape = v === 'trusted' ? icon('a', 'home_selected').replace(/<path id="body"/, '<path id="body" style="fill:' + col + '"').replace(/<path id="bar"[^>]*>/, '') : '';
  return `<div class="badge ${v === 'trusted' ? 'seal' : ''}" style="background:${v === 'trusted' ? ring : col};color:#fff">${shape}<span>${icon('a', 'mark_' + v)}</span></div>`;
}

function statusRow(style, mode) {
  const p = PAL[mode];
  const rows = [['protected', 'Protected', '#C2273B'], ['paused', 'Paused', '#9A5B00'], ['off', 'Off', p.sub], ['recovering', 'Recovering', '#1E6E8C']];
  return `<div class="status">${rows.map(([k, l, c]) => `<div class="st" style="background:${p.card};color:${c}">${icon(style, k)}<span>${l}</span></div>`).join('')}</div>`;
}

function dnsCards(style, mode) {
  const p = PAL[mode];
  const rows = [['dns_system', 'System DNS'], ['dns_other', 'Other DNS'], ['dns_rethink', 'Rethink DNS'], ['dns_smart', 'Smart DNS']];
  return `<div class="dnsc">${rows.map(([k, l], i) => `<div class="dc" style="background:${i === 2 ? p.pill : p.card};color:${i === 2 ? (mode === 'light' ? '#5C0A1A' : '#FFDADC') : p.primary}">${icon(style, k)}<span style="color:${i === 2 ? 'inherit' : p.ink}">${l}</span></div>`).join('')}</div>`;
}

function appRows(style, mode) {
  const p = PAL[mode];
  const rows = [['Calendar', false, '#4285F4'], ['Camera', true, '#5F6368'], ['Chrome', false, '#34A853']];
  return `<div class="logs">${rows.map(([n, b, c]) => `<div class="lr" style="background:${p.card}">
    <div class="app" style="background:${c}"></div><div class="lt" style="flex:1"><div style="color:${p.ink}">${n}</div></div>
    <div class="seg"><div style="background:${b ? '#FFD9DB' : p.nav};color:${b ? '#B3261E' : p.ink}">${icon(style, b ? 'wifi_blocked' : 'wifi')}</div><div style="background:${b ? '#FFD9DB' : p.nav};color:${b ? '#B3261E' : p.ink}">${icon(style, b ? 'mobile_blocked' : 'mobile')}</div></div></div>`).join('')}</div>`;
}

function context(style, mode) {
  const p = PAL[mode];
  return `<div class="ctx" style="background:${p.bg};--bar:${p.bar}">
    <div class="phone-h" style="color:${p.primary}">Settings hub</div>${tiles(style, mode)}
    <div class="phone-h" style="color:${p.primary}">Logs — verdict badges</div>${logRows(style, mode)}
    <div class="phone-h" style="color:${p.primary}">Apps — Wi-Fi / mobile data</div>${appRows(style, mode)}
    <div class="phone-h" style="color:${p.primary}">DNS type</div>${dnsCards(style, mode)}
    <div class="phone-h" style="color:${p.primary}">Protection state</div>${statusRow(style, mode)}
    <div class="phone-h" style="color:${p.primary}">Bottom navigation</div>${navBar(style, mode, 'settings')}${navBar(style, mode, 'home')}</div>`;
}

function crop(file, x, y, w, h, scale) {
  return `<div class="crop" style="width:${w * scale}px;height:${h * scale}px"><img src="${img(file)}" style="margin-left:${-x * scale}px;margin-top:${-y * scale}px;width:${1080 * scale}px"></div>`;
}

function inContext() {
  const s = 0.5;
  const current = `<div class="ctx cur" style="background:#FFF8F7">
    <div class="phone-h" style="color:#B42541">Settings hub</div>${crop('08-settings.png', 30, 545, 1020, 1120, s)}
    <div class="phone-h" style="color:#B42541">Logs — verdict badges</div>${crop('02-logs.png', 30, 385, 1020, 560, s)}
    <div class="phone-h" style="color:#B42541">Apps — Wi-Fi / mobile data</div>${crop('03-apps.png', 30, 470, 1020, 410, s)}
    <div class="phone-h" style="color:#B42541">DNS type</div>${crop('09-dns.png', 30, 1050, 1020, 720, s)}
    <div class="phone-h" style="color:#B42541">Bottom navigation</div>${crop('08-settings.png', 0, 2065, 1080, 210, s)}
    <div class="phone-h" style="color:#B42541">App detail rows (one globe, two meanings)</div>${crop('07-app-detail.png', 30, 820, 1020, 600, s)}</div>`;
  return `<html><head><style>${BASE_CSS}
  body{background:#E9E1E1;width:3000px;padding:48px}
  h1{font-size:46px;color:#231918;letter-spacing:-1px} .sub{font-size:20px;color:#5d4a4a;margin:8px 0 28px}
  .cols{display:grid;grid-template-columns:repeat(5,1fr);gap:22px;align-items:start}
  .col h2{font-size:22px;color:#231918;margin:0 0 10px 6px} .col h2 small{font-weight:400;color:#6E5B5B;font-size:16px}
  .ctx{border-radius:28px;padding:18px 16px 20px}.ctx:not(.cur){zoom:1.2}
  .phone-h{font-size:14px;font-weight:600;margin:14px 6px 8px;letter-spacing:.2px}
  .crop{overflow:hidden;border-radius:16px}.crop img{display:block}
  .tiles{display:grid;grid-template-columns:1fr 1fr;gap:4px}
  .tile{border-radius:18px;padding:12px;display:flex;align-items:center;gap:12px;font-size:17px;font-weight:500;min-height:66px}
  .tile:last-child{grid-column:span 2}
  .well{width:42px;height:42px;border-radius:13px;display:grid;place-items:center;flex:none}.well svg{width:24px;height:24px}
  .logs{display:flex;flex-direction:column;gap:4px}
  .lr{border-radius:16px;padding:10px 12px;display:flex;align-items:center;gap:12px;font-size:15px;font-weight:500}
  .app{width:34px;height:34px;border-radius:50%;position:relative;flex:none}
  .badge{position:absolute;right:-4px;bottom:-4px;width:17px;height:17px;border-radius:50%;display:grid;place-items:center;color:#fff}.badge svg{width:11px;height:11px;position:relative}
  .badge.seal{background:transparent!important}.badge.seal>svg{position:absolute;inset:-2px;width:21px;height:21px}.badge span{position:relative;display:grid}
  .badge #bar{fill:currentColor}.badge.solid svg{width:17px;height:17px}
  .status{display:grid;grid-template-columns:1fr 1fr;gap:4px}.st{display:flex;align-items:center;gap:10px;padding:10px 12px;border-radius:16px;font-size:15px;font-weight:600}.st svg{width:28px;height:28px}
  .dnsc{display:grid;grid-template-columns:1fr 1fr;gap:4px}.dc{display:flex;flex-direction:column;gap:8px;padding:12px;border-radius:18px;font-size:15px;font-weight:600}.dc svg{width:24px;height:24px}
  .lt{display:flex;flex-direction:column;gap:4px;min-width:0}.lt>div:first-child{white-space:nowrap;overflow:hidden;text-overflow:ellipsis;max-width:330px}
  .chip{display:inline-flex;align-self:flex-start;align-items:center;gap:4px;font-size:12px;font-weight:600;padding:2px 8px 2px 5px;border-radius:8px}.chip i svg{width:13px;height:13px}
  .seg{display:flex;gap:3px}.seg>div{width:44px;height:36px;display:grid;place-items:center;border-radius:18px 6px 6px 18px}.seg>div:last-child{border-radius:6px 18px 18px 6px}.seg svg{width:22px;height:22px}
  .nav{display:flex;justify-content:space-around;padding:10px 0 12px;border-radius:18px;margin-bottom:6px}
  .ni{display:flex;flex-direction:column;align-items:center;gap:4px}.np{width:60px;height:32px;border-radius:16px;display:grid;place-items:center}.np svg{width:24px;height:24px}.nl{font-size:13px}
  </style></head><body>
  <h1>Keepout icons in context</h1><div class="sub">Same screens, same palette — only the icons change. Left: the shipping app (captures from store-listing/en-US/raw/phone).</div>
  <div class="cols">
    <div class="col"><h2>Current <small>Material Symbols + mixed drawables</small></h2>${current}</div>
    <div class="col"><h2>Style A <small>Line & Bar · light</small></h2>${context('a', 'light')}</div>
    <div class="col"><h2>Style A <small>dark</small></h2>${context('a', 'dark')}</div>
    <div class="col"><h2>Style B <small>Cutout · light</small></h2>${context('b', 'light')}</div>
    <div class="col"><h2>Style B <small>dark</small></h2>${context('b', 'dark')}</div>
  </div></body></html>`;
}

// ------------------------------------------------------------ composites on the real captures
const COMP = JSON.parse(fs.readFileSync(path.join(HERE, 'composite.json'), 'utf8'));
const COMP_DIR = path.join(OUT, 'composites');

function tinted(svg, color) {
  return svg.replace('<svg ', `<svg style="color:${color};width:100%;height:100%" `);
}

function compositeHtml(screen, style) {
  const overlays = COMP[screen].map((it) => {
    const [x0, y0, x1, y1] = it.patch;
    let out = `<div style="position:absolute;left:${x0}px;top:${y0}px;width:${x1 - x0}px;height:${y1 - y0}px;background:${it.bg};border-radius:10px"></div>`;
    const box = (sz, inner) => `<div style="position:absolute;left:${it.cx - sz / 2}px;top:${it.cy - sz / 2}px;width:${sz}px;height:${sz}px;display:grid;place-items:center">${inner}</div>`;
    if (it.key === 'badge') {
      out = `<div style="position:absolute;left:${it.cx - 26}px;top:${it.cy - 26}px;width:52px;height:52px;border-radius:50%;background:${it.bg}"></div>`;
      if (style === 'b') out += box(44, tinted(icon('b', it.verdict), it.tint));
      else out += box(36, `<div style="width:36px;height:36px;border-radius:50%;background:${it.tint};display:grid;place-items:center"><div style="width:24px;height:24px">${tinted(icon('a', 'mark_' + it.verdict), '#fff')}</div></div>`);
      return out;
    }
    const key = it.key === 'hero' ? 'mark_blocked' : it.key;
    const st = manifest.styles[style][key] ? style : 'b';
    return out + box(it.size, tinted(icon(st, key), it.tint));
  }).join('');
  return `<html><head><style>*{margin:0}body{width:1080px;height:2340px;position:relative;overflow:hidden}</style></head><body>
    <img src="${img(screen + '.png')}" style="position:absolute;left:0;top:0;width:1080px">${overlays}</body></html>`;
}

const SHOT = (style, screen) => 'data:image/png;base64,' + fs.readFileSync(path.join(COMP_DIR, style, screen + '.png')).toString('base64');

function phonesSheet(title, screens) {
  const col = (label, style, screen) => `<figure><img src="${SHOT(style, screen)}"><figcaption>${label}</figcaption></figure>`;
  return `<html><head><style>${BASE_CSS}
  body{background:#E9E1E1;padding:44px;width:${screens.length > 2 ? 2600 : 2600}px}
  h1{font-size:44px;color:#231918;letter-spacing:-1px}.sub{font-size:19px;color:#5d4a4a;margin:8px 0 26px}
  .row{display:grid;grid-template-columns:repeat(2,1fr);gap:40px}
  .grp{background:#F6EFEF;border-radius:28px;padding:18px}.grp h2{font-size:22px;color:#231918;margin:2px 4px 12px}
  .trio{display:grid;grid-template-columns:repeat(3,1fr);gap:12px}
  figure img{width:100%;display:block;border-radius:18px;box-shadow:0 1px 0 #0001}
  figcaption{font-size:17px;color:#5d4a4a;margin:8px 4px 0;font-weight:600}
  </style></head><body><h1>${title}</h1><div class="sub">The real store captures (store-listing/en-US/raw/phone) with only the icons replaced: same positions, sizes and tints, one solid colour per icon exactly as Compose's Icon() tints it.</div>
  <div class="row">${screens.map(([name, screen]) => `<div class="grp"><h2>${name}</h2><div class="trio">${col('Current', 'current', screen)}${col('A · Line & Bar', 'a', screen)}${col('B · Cutout', 'b', screen)}</div></div>`).join('')}</div></body></html>`;
}

function zoomSheet() {
  const crops = [
    ['Settings hub tiles', '08-settings', 30, 545, 1020, 1100],
    ['Logs: filters, tabs and verdict badges', '02-logs', 0, 90, 1080, 900],
    ['Apps: Wi-Fi / mobile data pair', '03-apps', 30, 520, 1020, 480],
    ['App detail: network pair + rule rows', '07-app-detail', 30, 600, 1020, 1430],
    ['DNS type cards', '09-dns', 30, 1050, 1020, 1020],
    ['Bottom navigation (Settings selected)', '08-settings', 0, 2070, 1080, 200],
    ['Home: hero mark + today counters', '01-home', 0, 440, 1080, 1640],
  ];
  const s = 0.62;
  const cell = (style, [, screen, x, y, w, h]) => `<div class="crop" style="width:${w * s}px;height:${h * s}px"><img src="${SHOT(style, screen)}" style="margin-left:${-x * s}px;margin-top:${-y * s}px;width:${1080 * s}px"></div>`;
  return `<html><head><style>${BASE_CSS}
  body{background:#E9E1E1;padding:44px;width:2120px}
  h1{font-size:44px;color:#231918;letter-spacing:-1px}.sub{font-size:19px;color:#5d4a4a;margin:8px 0 22px}
  .hdr{display:grid;grid-template-columns:repeat(3,1fr);gap:16px;font-size:22px;font-weight:700;color:#231918;margin-bottom:8px}
  h2{font-size:18px;color:#B42541;margin:26px 0 10px}
  .trio{display:grid;grid-template-columns:repeat(3,1fr);gap:16px;align-items:start}
  .crop{overflow:hidden;border-radius:16px;background:#fff}.crop img{display:block}
  </style></head><body><h1>Icons in context — close-up</h1><div class="sub">Crops of the composites at 0.62×. Left: shipping app. Middle: style A. Right: style B (recommended).</div>
  <div class="hdr"><div>Current</div><div>A · Line & Bar</div><div>B · Cutout</div></div>
  ${crops.map((c) => `<h2>${c[0]}</h2><div class="trio">${cell('current', c)}${cell('a', c)}${cell('b', c)}</div>`).join('')}</body></html>`;
}

// ------------------------------------------------------------ full review: current -> new
function reviewSheet() {
  const rows = JSON.parse(fs.readFileSync(path.join(HERE, 'review.json'), 'utf8'));
  const cur = JSON.parse(fs.readFileSync(path.join(HERE, 'current-icons.json'), 'utf8')).filter((x) => /^(filled|outlined)/.test(x.name));
  const curSvg = (name) => cur.filter((x) => x.name.replace(/^(filled|outlined)/, '') === name).map((x) =>
    `<svg viewBox="0 0 ${x.vb[0]} ${x.vb[1]}" title="${x.name}">${x.paths.map((p) => `<path d="${p.d}" fill="currentColor" fill-opacity="${p.a}" fill-rule="${p.eo ? 'evenodd' : 'nonzero'}"/>`).join('')}</svg>`).join('');
  const VC = { REPLACE: '#B42541', SPLIT: '#8A3FD1', KEEP: '#4C6B5A', COLLAPSE: '#9A5B00', DEMO: '#6E5B5B', DELETE: '#6E5B5B', FIX: '#9A5B00' };
  const cell = (r) => `<div class="r"><div class="cur">${curSvg(r.name)}</div><div class="mid"><b>${r.name}</b><span class="v" style="background:${VC[r.verdict]}">${r.verdict}</span><small>${r.uses} uses</small></div>
    <div class="arrow">→</div><div class="new">${r.new.length ? r.new.map((n) => `<i title="${n}">${icon('b', n)}</i>`).join('') : `<em>${r.verdict === 'KEEP' ? 'Material' : r.verdict === 'DEMO' ? 'Material (demo)' : '—'}</em>`}</div></div>`;
  return `<html><head><style>${BASE_CSS}
  body{background:#FFF8F7;padding:44px;width:2600px;color:#231918}
  h1{font-size:44px;letter-spacing:-1px}.sub{font-size:19px;color:#5d4a4a;margin:8px 0 26px;max-width:1900px;line-height:1.4}
  .g{display:grid;grid-template-columns:repeat(4,1fr);gap:8px}
  .r{display:flex;align-items:center;gap:12px;background:#FFF0F0;border-radius:16px;padding:10px 14px;min-height:64px}
  .cur{display:flex;gap:4px;color:#594042}.cur svg{width:30px;height:30px}
  .mid{display:flex;flex-direction:column;gap:3px;min-width:170px}.mid b{font-size:15px}.mid small{font-size:12px;color:#6E5B5B}
  .v{align-self:flex-start;color:#fff;font-size:11px;font-weight:700;letter-spacing:.6px;padding:2px 7px;border-radius:6px}
  .arrow{color:#B42541;font-size:20px}
  .new{display:flex;gap:8px;flex-wrap:wrap;color:#594042}.new i svg{width:30px;height:30px}.new em{font-size:13px;color:#6E5B5B}
  </style></head><body><h1>Every ui/icons vector, reviewed</h1>
  <div class="sub">Left: today's MaterialSymbols vector(s) (Filled, then Outlined when both exist). Right: what replaces it in style B. KEEP = generic glyph that already matches B's weight (Material Symbols Rounded 400). Details and reasons per icon in inventory.md.</div>
  <div class="g">${rows.map(cell).join('')}</div></body></html>`;
}

(async () => {
  const browser = await chromium.launch();
  const page = await browser.newPage({ deviceScaleFactor: 1 });
  for (const style of ['current', 'a', 'b']) fs.mkdirSync(path.join(COMP_DIR, style), { recursive: true });
  const cpage = await browser.newPage({ viewport: { width: 1080, height: 2340 }, deviceScaleFactor: 1 });
  for (const screen of Object.keys(COMP)) {
    fs.copyFileSync(path.join(RAW, screen + '.png'), path.join(COMP_DIR, 'current', screen + '.png'));
    for (const style of ['a', 'b']) {
      await cpage.setContent(compositeHtml(screen, style));
      await cpage.screenshot({ path: path.join(COMP_DIR, style, screen + '.png') });
    }
  }
  const jobs = [['style-a.png', sheet('a')], ['style-b.png', sheet('b')], ['in-context-mock.png', inContext()],
    ['in-context.png', phonesSheet('Keepout icons on the real screens', [['Settings', '08-settings'], ['Logs', '02-logs'], ['Apps', '03-apps'], ['App detail', '07-app-detail']])],
    ['in-context-2.png', phonesSheet('Keepout icons on the real screens (2)', [['DNS', '09-dns'], ['Home', '01-home'], ['Settings · dark', '13-settings-dark'], ['Apps · dark', '12-apps-dark']])],
    ['in-context-zoom.png', zoomSheet()], ['review.png', reviewSheet()]];
  for (const [file, html] of jobs) {
    await page.setContent(html);
    await page.screenshot({ path: path.join(OUT, file), fullPage: true });
    console.log('wrote', file);
  }
  await browser.close();
})();
