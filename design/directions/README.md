# Keepout: three visual directions

These are design proposals only. No app code was changed. Each folder holds `home.png`, `logs.png`, `settings.png` and `home-dark.png` at 412×915 dp, rendered @2x (824×1830 px), plus the HTML/CSS they were rendered from. `compare.png` puts all three side by side.

- Re-render: `node render.cjs [ledger|seal|trace]`, then `node compare.cjs`. You need the Playwright install at `~/.cache/weakspot-shots/node_modules/playwright`.
- Any page renders dark if you add `#dark` to its URL.
- All mock content comes from the store captures: 457 / 122 / 259, RDNS Default, 33 ms, the real log rows and the "most blocked" apps. App icons were cropped from those captures. Clock and Maps use plain letter placeholders.
- Some status lines were made up to show an idea: "6 with their own rules", "3 rules", "Kept for 7 days", "up 2h 29m" and similar. They show tiles that report their current state. They are not real values.

One rule holds in all three directions: **coral means stopped, and only stopped**. The brand colour is the colour of what Keepout prevents, so the brand shows up wherever the product does its job. Green means allowed and is always quieter than coral. Everything else is neutral.

---

## A · Ledger

**Concept:** A daily record of who your phone talked to. Paper, ink, hairline rules, and one red pencil for what was stopped.

**Why it fits Keepout.** Privacy tools tend to look either like antivirus software or like a hacker's terminal. Ledger looks like a well-kept record, which makes it trustworthy and calm. It treats the user as a reader, not an operator. The red pencil gives the brand coral a clear job: an editor striking something out.

**Type**
- Display and headlines: **Newsreader** (OFL, variable opsz 6–72). Uses 500 for headlines, 400 italic for kickers and emphasis, 600 for figures in running text.
- UI and body: **IBM Plex Sans** 400 / 500 / 600 (OFL).
- Domains, times and counts in logs: **IBM Plex Mono** 400 / 500 (OFL).

**Colour tokens**

| Role | Light | Dark |
|---|---|---|
| Background (paper) | `#F6F1E9` | `#15110E` |
| Surface | `#FFFCF6` | `#1D1814` |
| Sunk / container | `#ECE4D8` | `#29221C` |
| Ink (on-surface) | `#1F1A16` | `#F1E8DC` |
| Ink secondary | `#6B6158` | `#AA9D8E` |
| Rule / outline | `#DCD2C4` | `#382F27` |
| Blocked (red pencil) | `#CF3447` | `#FF7D84` |
| Blocked tint | `#F8DEDC` | `#3B1D1F` |
| Allowed | `#2D6A4E` | `#86C9A5` |
| Allowed tint | `#DCE9DF` | `#1C3026` |
| Neutral (ink tertiary) | `#A1968A` | `#76695C` |
| Brand seal (gradient) | `#FF8A7A → #F0485A` | same |

**Shape.** Mostly no containers. Structure comes from hairline rules, a double rule under the masthead, and a 2 dp ink rule over each section. Buttons and chips are full pills with a 1 dp ink outline. The selected state is solid ink. Settings is a ruled table with hairline column dividers, not a grid of cards. M3 shape tokens: `extraSmall`–`medium` set to 0, `full` kept for pills.

**Icons.** Material Symbols Outlined, weight 300, unfilled, in ink-secondary. The selected nav icon switches to FILL 1 at weight 400. Icons stay small and quiet, and they never sit in coloured wells.

**Signature moments**
1. **The day, written for you.** Home leads with a sentence, not a dashboard: "Your apps opened **579** connections. Keepout stopped **122**, most of them on the way to ad and analytics servers." It's generated from the data, and the stopped figure is set in red pencil.
2. **Struck through.** In Logs, a stopped domain is drawn with a coral strike-through: ~~googleads.g.doubleclick.net~~. The whole log reads at a glance without badges or icons.
3. **Dot leaders.** "Stopped most" is a ranked list in the style of a table of contents: *1* Chrome ........ 75.

**Motion.** Slow and confident. Standard M3 emphasized easing, 300–400 ms, no bounce. New log rows fade in with an 8 dp vertical drift. When a row is stopped, the strike-through draws left to right in 220 ms, like a pen stroke. The hourly chart columns rise in a 20 ms stagger on first load. Pause/resume cross-fades the headline ("Every app is on your *terms.*" ↔ "Paused. *Apps go unchecked.*").

**Voice.** Plain, declarative and a little literary. It never uses exclamation marks.
- "Keepout stopped 122, most of them on the way to ad and analytics servers."
- "Paused. Apps go unchecked until you resume."
- "Stopped by DNS · OISD Big"

**Risks and tradeoffs**
- A serif in a utility app can read as "lifestyle" if the editorial treatment is overdone. It needs strict discipline: serif only for headlines, numbers and kickers, never for controls.
- Low chroma means the protection state relies on copy and the small green kicker rather than a big coloured block. That makes the state harder to read at a glance.
- The Paused state needs its own hero treatment, for example a full-width sunk band, so it doesn't look like a typo.
- This is the least M3-Expressive-looking of the three because there are hardly any containers. Reviewers may find it austere.

**Compose cost: medium-low (~1.5–2 weeks).** Custom `Typography` with three bundled families (variable Newsreader, about 300 KB). Rules via `Modifier.drawBehind`. The strike-through is a `TextDecoration.LineThrough` plus a custom `drawWithContent` for coral colour and thickness. The written lede is string resources with plurals, which makes localisation the real cost. Dot leaders are a `Canvas` row.

---

## B · Seal

**Concept:** The keep-out sign, everywhere. Enamel plates, the scalloped seal, condensed caps. Only the stops are loud.

**Why it fits Keepout.** The name is a sign and the icon is already one: a coral seal with a bar across it. This direction makes that sign the whole system. It uses the language of wayfinding and regulatory signage, which people read instantly and trust, and it is the closest to the store listing's plum-and-coral aurora. It is also where M3 Expressive pays off most: the seal is literally `MaterialShapes.Cookie12Sided`.

**Type**
- Display (status words, numbers, section heads): **Big Shoulders Display** 800 / 900 (OFL), a condensed signage face. Always caps, and only at 20 sp or larger.
- UI and body: **Atkinson Hyperlegible Next** 400 / 500 / 600 / 700 / 800 (OFL). It was designed by the Braille Institute for maximum legibility, which is the right companion to a shouty display face.
- Domains and times: **Atkinson Hyperlegible Mono** 500 / 600 (OFL). It clearly tells apart l / 1 / I and 0 / O, which matters for spotting look-alike hosts.

**Colour tokens**

| Role | Light | Dark |
|---|---|---|
| Background (enamel) | `#F4EEE9` | `#12060C` |
| Surface (plate) | `#FFFFFF` | `#1F0E17` |
| Signboard (nav, directory, hero) | `#2A0F1C` | `#2E1422` |
| On-signboard | `#FFF2EE` / `#C9AEB8` | same |
| Ink | `#26101B` | `#FFF1EC` |
| Ink secondary | `#6E5A63` | `#C4AAB5` |
| Outline | `#E4D8D3` | `#34202A` |
| Blocked (sign coral) | `#EE4458` | `#FF5A6C` |
| Blocked tint (stopped rows) | `#FFE3DF` | `#3D1420` |
| Allowed | `#13784F` | `#52D49C` |
| Allowed tint | `#DCEFE4` | `#123326` |
| Neutral | `#A8979E` | `#7F6873` |
| Seal gradient | `#FF8A7A → #F0485A` | same |

**Shape.** Plates with 16–22 dp corners for cards and rows, 32 dp for the hero, and 8–12 dp "sign" corners for chips and segmented controls. Chips are deliberately squarer than M3 defaults, so they read as plates, not pills. The **seal** (12-sided cookie) is the only organic shape and has three roles:
- the brand
- the icon well for Protection tiles, filled when the feature is on and drawn as an outline when it is off (see Proxy)
- the "stopped" badge on an app icon, replacing the red ✕

The bar inside the seal is the unit of "blocked". It appears as the inner bar of the seal, as the ranked-app bars, and as the hourly tally.

**Icons.** Material Symbols Sharp, FILL 1, weight 600. These are bold pictograms, like signage. On the signboard they sit in a square enamel tile (on-signboard background, signboard-colour glyph). On Protection tiles they are white on a coral seal.

**Signature moments**
1. **The sign on Home.** A plum signboard with a big coral seal radiating faint rings, and PROTECTED in 82 sp condensed caps. **Hold to pause**: a conic progress ring fills around the pause glyph, then the bar slides out of the seal, the seal becomes an outline, and the board reads PAUSED. Holding is intentional friction for a privacy tool, and the moment is worth recording.
2. **"Just stopped" ticker on Logs.** A coral band above the list shows the latest stopped host live (white seal, "JUST STOPPED · CHROME", `googleads.g.doubleclick.net`). Tapping it jumps to that row. Stopped rows have a coral tint plate and a condensed STOPPED · DNS stamp. Allowed rows stay plain white.
3. **The directory board in Settings.** System settings are an airport-style plum directory with enamel pictograms, live states and arrows. Protection tiles are four big plates with seal icon wells.
4. Also: the 122 STOPPED plate, with the hourly tally drawn in white bars inside it.

**Motion.** Physical and springy, in the M3 Expressive spatial springs (`fast`/`default` spatial, damping about 0.6 for the seal, 0.8 elsewhere). The seal does a single 6° wobble and settles when protection starts. Every time something is stopped live, the ticker band slides down (spring) and the small seal badge "stamps" onto the app icon: scale 1.3 → 1 with a slight overshoot. Numbers roll like a split-flap display when they change. There is no ambient looping animation; signs stay still.

**Voice.** Short, imperative, signage-like for labels and warm for sentences. Caps only in the display face.
- "PROTECTED. Every app asks before it talks."
- "HOLD TO PAUSE" / "Paused. Nothing is being kept out."
- "JUST STOPPED · CHROME"

**Risks and tradeoffs**
- It is loud. It needs restraint: Big Shoulders only for status words, numbers and section heads, never for sentences or in lists. Condensed caps don't localise well (German, Russian, Vietnamese), so they need a fallback to Atkinson 800 sentence case when a string overflows.
- Coral tinted rows on a very blocky log could feel alarming. The tint must stay very light, and the STOPPED stamp is the only saturated element per row.
- The dark signboard nav on a light screen breaks from M3's tonal nav bar. That's intentional, but it needs care with edge-to-edge and the gesture handle.
- Three bundled families (Big Shoulders ~70 KB as two static weights, Atkinson Next variable ~150 KB, Atkinson Mono ~90 KB).

**Compose cost: medium (~2–3 weeks).** The seal is `MaterialShapes.Cookie12Sided.toShape()` (already used in the codebase), with the bar drawn on top. Plates are `Surface` with custom shapes. Hold-to-pause uses `pointerInput { detectTapGestures(onPress) }` plus `Animatable` for the ring, then a shape morph (`Morph(cookie, circle)`) for the paused state. The ticker is an `AnimatedContent` slide. Split-flap numbers are `AnimatedContent` per digit. The directory board is a plain `Column` on a dark `Surface`.

---

## C · Trace

**Concept:** A precision instrument for your network. Everything is a readout, and the live trace is the hero.

**Why it fits Keepout.** Rethink's users are curious and technical, and they want to see what's happening, not be reassured. Trace treats the phone's traffic like a signal on a scope. Passed traffic rises above the line in mint, stopped traffic falls below it in coral, and the protection state is a hardware switch with an indicator lamp. It earns trust through precision, honest scales and exact numbers. It is also the best fit for dense logs.

**Type**
- UI: **Geist** 400 / 500 / 600 (OFL). Headlines at 600 with tight tracking (-0.035 em).
- Data (every number, time, domain and label caption): **Geist Mono** 400 / 500 (OFL). Captions in caps with +0.08 em tracking.

**Colour tokens**

| Role | Light | Dark |
|---|---|---|
| Background | `#ECECE7` | `#0A0B0D` |
| Panel (surface) | `#F8F8F5` | `#121317` |
| Panel line (outline) | `#D2D2CB` | `#25272C` |
| Grid / divider | `#E3E3DD` | `#1A1C20` |
| Ink | `#111214` | `#E9EAEC` |
| Ink secondary | `#5C5E63` | `#8C8F97` |
| Neutral (ink tertiary) | `#9A9B9F` | `#565961` |
| Blocked (signal) | `#E33A4C` | `#FF5566` |
| Blocked tint (new-row sweep) | `#F9DCDF` | `#3A1419` |
| Allowed (trace) | `#0B8A70` | `#3AD6AE` |
| Lamp (status on) | `#16B88F` | `#3AD6AE` |

This direction is dark-first in spirit (the dark Home is its best screen), but light is a complete "lab paper" scheme, not an afterthought.

**Shape.** Small and engineered. Panels use 6 dp corners with a 1 dp outline. Segmented controls are 6/4 dp. Registration ticks sit on two corners of key panels and are the shape signature. There are no pills except the lamp. LED-segment meters (14 cells) replace progress bars. M3 shapes: `extraSmall` 4, `small` 6, `medium` 6, `large` 8.

**Icons.** Material Symbols Sharp, weight 300, unfilled, ink colour. The selected nav icon is FILL 1 inside a 6 dp inset well. State is never shown by an icon alone. It always comes with a lamp plus a mono caption (`● RDNS Default`, `○ off`).

**Signature moments**
1. **The scope on Home.** 24 h in 96 fifteen-minute buckets, passed above the baseline and stopped below it, on one honest shared scale. A "now" cursor pulses at the right edge. Readouts underneath: PASSED 457 · STOPPED 122 (21.1 % of all) · DNS REFUSED 259. It scrubs on drag, and a long-press freezes it.
2. **The last-hour strip on Logs.** Every connection is one tick on a mini-trace, with a bracket that shows the window the list below is displaying. Drag the bracket to scroll time. New rows enter with a coral sweep that fades in 1.2 s. The rate readout `● 4.1/min` sits in the top bar.
3. **Settings as a rack of modules.** Each tile reports live state: `33 MS · ● RDNS Default`, `3 rules armed`, `○ off`, `12 MB · kept 7 days`. Settings stops being a menu and becomes a status panel.
4. Also: the ON/OFF rocker and the FILTERING annunciator lamp.

**Motion.** Precise and fast, close to linear. 150–200 ms for state changes, using standard decelerate easing with no springs or bounce. The scope scrolls continuously, one bucket every 15 min, and new ticks draw in 120 ms. Lamps have a 2 s "breathing" glow only while filtering. When protection stops, every lamp goes off at once and the trace flattens to the baseline over 400 ms. Counters tick digit by digit, so they never jump.

**Voice.** Terse, exact and lower-case in captions, like an instrument label. Units are always included.
- "Filtering · RDNS Default · 33 ms"
- "stopped · DNS · OISD Big, AdGuard DNS filter +1"
- "Protection off. 0 connections checked since 09:41."

**Risks and tradeoffs**
- It can tip into a "Grafana on a phone" look and alienate non-technical users. The Home headline ("Protected") and plain-language readouts have to stay.
- Mono everywhere is dense. It needs strict hierarchy (only data in mono, never sentences) and is the most demanding on contrast in light mode.
- The scope needs real per-bucket data from the logs database (a cheap `GROUP BY` on timestamp/900). It must also look right with sparse data on day one.
- It is the least warm and makes the least of the brand. The seal appears only as a small mark.

**Compose cost: medium-high (~3 weeks).** The scope and strip are custom `Canvas` drawing plus `pointerInput` scrubbing, with a `Flow` of bucketed counts. Registration ticks are a `Modifier.drawWithContent`. LED meters and the rocker are small custom composables. Geist + Geist Mono are about 180 KB as static weights. Mono tabular figures are free.

---

## Recommendation: **B · Seal**, with two borrowings

I recommend **Seal** as the foundation, for four reasons:

1. **Only Seal could not belong to any other app.** Ledger could be a reading app and Trace could be any network tool, but Seal is Keepout's own sign. "Award-winning" needs a concept the whole app expresses, and here the concept is the name: the sign, the seal, the bar. It runs through the hero, the stopped badge, the tiles and the tally.
2. **It's continuous with what already exists.** The launcher icon, the plum/coral store aurora and the M3 Expressive cookie shapes are already in the code. That makes the redesign feel inevitable rather than arbitrary, and the store listing and README hero get stronger for free.
3. **It produces the screenshot moments.** The PROTECTED signboard, hold-to-pause, the "Just stopped" ticker and the directory board are the most screenshot-worthy screens of the three.
4. **It's honest about the one thing that matters.** Coral appears only when something was kept out, so the brand shows up exactly when the product is working.

Two things to take from the other directions:

- **From Ledger: the coral strike-through on stopped domains in Logs.** Keep the coral tint plate and the STOPPED stamp, but strike the host as well. It's the most legible "this didn't happen" signal of all, and it works even without colour (colour-blind users, monochrome screenshots).
- **From Trace: tiles and readouts that report live state, and honest scales.** Every Settings tile shows its current value (already in the Seal mock), and charts share one scale for allowed and stopped.

Guardrails for Seal:
- Use Big Shoulders only for status words, numbers and section heads.
- Fall back to Atkinson 800 sentence case for long localised strings.
- Keep the stopped-row tint very light, so a busy log doesn't look like an alarm.

Next step: prototype the Home signboard with hold-to-pause and the Logs row in Compose. Those two prove the system.
