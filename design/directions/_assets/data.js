// Shared mock content for all three directions, taken from the store captures (store-listing/en-US/raw/phone).
// Apply ?dark / #dark to any page to render it in its dark scheme.
if (location.hash === '#dark' || location.search.includes('dark')) document.documentElement.classList.add('dark');

const A = '../_assets/';
const KP = {
  allowed: 457, blocked: 122, total: 579, dnsBlocked: 259, resolver: 'RDNS Default', latency: '33 ms',
  stopped: [
    { app: 'Chrome', icon: 'chrome', n: 75 },
    { app: 'YouTube', icon: 'youtube', n: 53 },
    { app: 'Camera', icon: 'camera', n: 46 },
    { app: 'YouTube Music', icon: 'ytmusic', n: 32 },
    { app: 'Google', icon: 'google', n: 30 },
  ],
  // [time, sub, root, app, icon, stopped?, how, detail]
  logs: [
    ['09:40:54', 'firebaselogging-pa.', 'googleapis.com', 'Clock', 'clock', true, 'App rule', 'HTTPS'],
    ['09:40:32', 'mail.', 'google.com', 'Gmail', 'gmail', false, 'HTTPS', '1.7 MB'],
    ['09:40:10', 'googleads.g.', 'doubleclick.net', 'Chrome', 'chrome', true, 'DNS', 'OISD Big, AdGuard DNS filter +1'],
    ['09:39:48', 'www.', 'google.com', 'Chrome', 'chrome', false, 'HTTPS', '1.6 MB'],
    ['09:39:25', 'youtubei.', 'googleapis.com', 'YouTube', 'youtube', false, 'HTTPS', '3.9 MB'],
    ['09:39:03', 'maps.', 'googleapis.com', 'Maps', 'maps', false, 'HTTPS', '671.4 KB'],
    ['09:38:41', '', 'app-measurement.com', 'Google', 'google', true, 'DNS', 'EasyPrivacy, Hagezi Multi Pro'],
    ['09:38:19', 'instantmessaging-pa.', 'googleapis.com', 'Messages', 'messages', false, 'HTTPS', '305.6 KB'],
    ['09:37:57', 'i.', 'ytimg.com', 'YouTube', 'youtube', false, 'HTTPS', '5.3 MB'],
    ['09:37:34', 'crashlyticsreports-pa.', 'googleapis.com', 'Camera', 'camera', true, 'DNS', 'Hagezi Multi Pro'],
    ['09:36:28', 'pagead2.', 'googlesyndication.com', 'YouTube', 'youtube', true, 'DNS', 'OISD Big'],
    ['09:33:09', 's.', 'youtube.com', 'YouTube Music', 'ytmusic', true, 'DNS', 'OISD Big'],
  ],
};

const HAVE = ['chrome', 'youtube', 'camera', 'ytmusic', 'google', 'messages', 'gmail', 'playstore'];
function appIcon(name, size, label) {
  if (HAVE.includes(name))
    return `<img class="appicon" src="${A}${name}.png" style="width:${size}px;height:${size}px;border-radius:50%;object-fit:cover;transform:scale(1.04)">`;
  const c = { clock: '#4f7fe0', maps: '#34a853' }[name] || '#888';
  return `<span class="appicon ph" style="width:${size}px;height:${size}px;border-radius:50%;background:${c};color:#fff;display:inline-grid;place-items:center;font:600 ${Math.round(size * .45)}px/1 sans-serif">${(label || name)[0].toUpperCase()}</span>`;
}

// Deterministic 24h series (oldest first, ending "now" 09:41) that sums to the real totals.
function rng(seed) { return () => (seed = (seed * 16807) % 2147483647) / 2147483647; }
function distribute(total, weights) {
  const s = weights.reduce((a, b) => a + b, 0);
  const raw = weights.map(w => w / s * total), out = raw.map(Math.floor);
  let left = total - out.reduce((a, b) => a + b, 0);
  raw.map((r, i) => [r - Math.floor(r), i]).sort((a, b) => b[0] - a[0]).slice(0, left).forEach(([, i]) => out[i]++);
  return out;
}
function series(buckets) {
  const r = rng(7), w = [], f = [];
  for (let i = 0; i < buckets; i++) {
    const hour = (10 + i * 24 / buckets) % 24;
    const day = hour < 6.5 ? 0.12 : hour < 8 ? 0.55 : (hour > 11.5 && hour < 13.5) || (hour > 19 && hour < 23) ? 1.25 : 0.8;
    w.push(day * (0.55 + r() * 0.9));
    f.push(hour < 6.5 ? 0.6 : 0.12 + r() * 0.3); // at night most traffic is background telemetry
  }
  return { allowed: distribute(KP.allowed, w.map((x, i) => x * (1 - f[i]))), blocked: distribute(KP.blocked, w.map((x, i) => x * f[i])) };
}

// The Keepout seal: a scalloped "cookie", the M3 Expressive Cookie shape the brand already uses.
function sealPath(cx, cy, R, bumps = 12, depth = 0.075) {
  let d = '';
  for (let i = 0; i <= 360; i++) {
    const t = i / 360 * Math.PI * 2, r = R * (1 - depth + depth * Math.cos(bumps * t));
    d += (i ? 'L' : 'M') + (cx + r * Math.sin(t)).toFixed(2) + ',' + (cy - r * Math.cos(t)).toFixed(2);
  }
  return d + 'Z';
}
const $ = (s) => document.querySelector(s);
// Status bar + gesture handle, filled in by every page at the end of <body>.
function frame() {
  document.querySelectorAll('.status').forEach(s => s.innerHTML = `<span>9:41</span><span class="ic">
    <svg viewBox="0 0 24 24"><path d="M12 21 0 8.5Q2.6 6 5.8 4.6T12 3.2t6.2 1.4T24 8.5Z"/></svg>
    <svg viewBox="0 0 24 24"><path d="M8 22q-.4 0-.7-.3T7 21V5q0-.4.3-.7T8 4h2V2h4v2h2q.4 0 .7.3t.3.7v16q0 .4-.3.7T16 22Z"/></svg></span>`);
  document.querySelectorAll('.phone').forEach(p => p.insertAdjacentHTML('beforeend', '<div class="gesture"></div>'));
}
// The seal as an inline SVG: coral gradient like the launcher icon, white bar across it.
let _sid = 0;
function sealSvg(size, o = {}) {
  const id = 'sg' + (_sid++), R = 50, bar = o.bar ?? '#fff', fill = o.fill || `url(#${id})`;
  const bw = (o.barW ?? .52) * 100, bh = (o.barH ?? .15) * 100;
  return `<svg class="seal" width="${size}" height="${size}" viewBox="0 0 100 100" style="${o.style || ''}">
    <defs><linearGradient id="${id}" x1="0" y1="0" x2="0" y2="1"><stop offset="0" stop-color="#FF8A7A"/><stop offset="1" stop-color="#F0485A"/></linearGradient></defs>
    <path d="${sealPath(50, 50, R, o.bumps || 12, o.depth || .075)}" fill="${o.outline ? 'none' : fill}" ${o.outline ? `stroke="${o.outline}" stroke-width="${o.sw || 3}" stroke-dasharray="${o.dash || ''}"` : ''}/>
    ${bar ? `<rect x="${50 - bw / 2}" y="${50 - bh / 2}" width="${bw}" height="${bh}" rx="${bh / 2}" fill="${bar}"/>` : ''}
    ${o.inner || ''}</svg>`;
}
// Long hosts lose characters from the left of the subdomain, never from the registrable domain.
function fitDomain(sub, root, max) {
  return sub.length + root.length <= max ? sub : '…' + sub.slice(-(max - root.length - 1));
}
