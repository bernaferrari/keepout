# Keepout in-app icons

> **Round 2 is current: see `proposals-cd.md` and `cd-in-context.png`.** The owner rejected A and B: both changed conventional glyphs that people recognise (the Wi-Fi slash, nav, DNS, globe…). Round 2 is recognition first. C is Refined Material and D is Material + a restrained Keepout touch, both checked with a label-free test. Everything below documents round 1 (A/B) and is kept for reference. `inventory.md` and `review.png` still hold, except for their "New (style B)" column.

Scope: the icons *inside* the app. The launcher icon and notification icon are out of scope. Nothing here changes app code. The aim is one consistent family that fits the **current** UI: Material 3 Expressive, the coral/pink scheme, rounded 24dp cards, bare icons in list rows tinted primary/onSurfaceVariant, coloured wells on the Settings hub, and 40dp app icons. Keepout-specific meaning goes where it matters, and the generic Material glyphs that remain sit beside it naturally.

## What's here

| Path | What |
|---|---|
| `inventory.md` | Every ui/icons vector (103 names, 110 vectors) and every drawable Compose paints: meaning, verdict, replacement, reason |
| `review.png` | The same review drawn: today's glyph → new glyph, for all 103 |
| `style-a.png`, `style-b.png` | The two candidate sets, light + dark, 48dp and 24dp, mono and two-tone |
| `in-context.png`, `in-context-2.png` | The **real store captures** with only the icons swapped: same positions, sizes and tints, one solid colour per icon exactly as `Icon()` tints. Current / A / B per screen (Settings, Logs, Apps, App detail, DNS, Home, Settings dark, Apps dark) |
| `in-context-zoom.png` | Close-up crops of those composites. **Start here.** |
| `in-context-mock.png` | Mock rows covering states the captures don't show (bypassed/trusted badges, protection states, dark) |
| `composites/{current,a,b}/*.png` | Full-size composited screens |
| `svg/a/*.svg`, `svg/b/*.svg` | Sources: 24×24, flattened **fill-only** paths (no strokes, masks or clips), up to three paths per icon: `ghost` (fill-opacity .38, B only), `body`, `bar` |
| `kotlin/*.kt` | Style B generated as Compose `ImageVector`s in the repo's lazily-cached format. Preview only, not wired in |
| `tools/icons.py` | Icon definitions (primitives + boolean ops via skia-pathops) → SVG + Kotlin |
| `tools/composite.py` | Finds each old glyph on the captures (bbox, background, tint) → `composite.json` |
| `tools/review.py` | The review table → `inventory.md`, `review.json` |
| `tools/render.cjs` | All PNG sheets and composites (Playwright) |

Rebuild everything:

```sh
uv run --with skia-pathops --with fonttools python design/icons/tools/icons.py
uv run --with pillow python design/icons/tools/composite.py
python3 design/icons/tools/review.py
node design/icons/tools/render.cjs     # uses ~/.cache/weakspot-shots/node_modules/playwright
```

## The icon language (both styles)

The idea comes from the launcher badge, a scalloped seal with a bar across it: a keep-out sign. The **bar** (a fully rounded pill) is the unit of "kept out". The **seal** is Keepout itself.

**Grid.** 24×24 viewport, 20dp live area (2dp padding). Round shapes may reach 21dp optically. Corners: 3dp on containers ≥ 12dp, 2–2.75dp on smaller ones. All terminals are round.

**Weight.** Calibrated to Material Symbols Rounded, weight 400. Lines are 2dp, like Outlined, and solids match Filled. This is what lets the remaining Material glyphs (back, close, check, more, search…) stay unchanged.

**Semantics.**

| Concept | Rule |
|---|---|
| Blocked | **The bar laid horizontally across the glyph** with a 1.1–1.25dp knock-out gap. Never a diagonal slash. Wi-Fi blocked, mobile blocked, notifications off, blocked verdict: all the bar. |
| Verdicts | Live in a **circle**: allowed = check, blocked = bar, bypassed = an arrow hopping over the bar, partial = half. |
| Keepout's own stance | Lives in the **seal** (8 lobes at 24dp; the 12-lobe `Cookie12Sided` is kept for ≥ 40dp shapes like the hero): protected (bar), paused (two bars), off (hollow), recovering (three dots), trusted (check, "seal of approval"), critical (!), Rethink Plus (+), exclude (arrow leaving the seal). |
| DNS | A **signpost**: names → directions. It is used everywhere DNS means DNS (tile, logs tab, Home row), and nowhere else. |
| One meaning, one glyph | The globe means "the web" only. IP rule, domain rule, universal rule, network logs and smart DNS each get their own glyph. |
| Selected / unselected (nav) | M3 rule: unselected = 2dp outline with the solid bar inside; selected = solid silhouette with the bar knocked out. |
| Colour | Icons are single-colour, tinted at use (`Icon(tint = …)`). The `bar` path is separate, so a two-tone treatment (bar in coral) is possible with a custom painter, but nothing depends on it. B's ghost layer is alpha within one colour, which `Icon()`'s SrcIn tint preserves. |

## The two styles

**A · Line & Bar.** 2dp round line (Outlined weight). One filled pill per glyph: the bar. Selected states fill. It is light, precise and close to the line glyphs the Settings wells use today.

**B · Cutout (recommended).** Solid silhouettes at Filled weight. Detail is *knocked out* of the ink, never drawn on it, and the bar is cut out of solid ink or laid solid over a gap. A 38% ghost layer carries secondary context (the untouched part of the flask, the meter scale, the excluded seal). The nav follows the M3 outline→fill rule.

### Recommendation: B

1. **It's the weight the app already speaks.** 414 of the 423 MaterialSymbols references are Filled (397 `Filled` + 17 `AutoMirrored.Filled`); only 9 are `Outlined`. List-row icons, filter chips, verdict counters and DNS cards are all solid today, and B drops into those slots without changing their colour or contrast (see the App detail and DNS crops in `in-context-zoom.png`). A would need every remaining generic glyph moved to Outlined to stay consistent.
2. **It holds at small sizes and in coloured wells.** The tiles use 40dp wells with an on-container tint, the badges are 13dp and the chips 13–16dp. A's 2dp line goes thin and grey there, while B stays legible: compare the verdict badges and the "Blocked" chip.
3. **The verdict badge becomes the icon itself.** A solid disc or seal with the glyph knocked out replaces "red disc + ×". It reads as a badge, adds the missing bypassed and trusted states, and needs no second shape behind it.
4. **The brand shows up as behaviour, not decoration.** Wherever something is blocked, the same knocked-out bar appears, in the Wi-Fi/data pair, the badges, the counters and the hero.

Keep from A: the 2dp outline is exactly B's unselected-nav weight, and A's glyph shapes are the same metaphors. If the owner prefers a lighter feel later, A is a straight swap from the same sources.

Known trade-offs: B's Wi-Fi-blocked splits the wedge into two lens shapes (readable at 20dp, if a little odd at 48), and the ghost layer needs enough contrast. 38% of onSurfaceVariant on the pink cards passes visually; check it against the real dark theme before shipping.

## What stays Material

The generic glyphs, with **KEEP** in `inventory.md`: Add, ArrowBack/Forward, Backup/Restore, BrightnessAuto, BugReport, CameraAlt, Cancel, Check, Close, ContentCopy, DarkMode/LightMode, DeleteOutline, Download, Edit, EditLocationAlt, Email, Headset, HelpOutline, Info, KeyboardArrowDown/Right, MoreHoriz/Vert, NewReleases, Notifications(Off), Palette, PlayArrow/Stop, Refresh, Remove, Search, Send, Share, ShoppingCart, SystemUpdate, Warning. Rounded 400 already has B's weight, terminals and corners, so redrawing them would only add maintenance. They stay in `MaterialSymbols`, with these cleanups:

- Collapse duplicates: Clear→Close, Delete→DeleteOutline, WarningAmber→Warning, RestartAlt→Refresh, and the identical Filled/Outlined `MobiledataArrows`/`MobiledataOff` (replaced anyway).
- Delete the unused ones: ArrowOutward, RadioButtonChecked/Unchecked, SystemUpdateAlt.
- Fix `Outlined.StarBorder`, which renders filled.
- Move the 15 demo-app stand-ins (Movie, MusicNote, Map, LocalTaxi, Shop, Work, BusinessCenter, Cloud, Forum, Chat, Group, PhotoLibrary, CalendarMonth, AutoStories, WbSunny) to a `DemoAppGlyphs` object. They impersonate other apps and must not look like Keepout.

## Implementation plan (next round)

No drawables for in-app icons: every icon becomes a hard-coded Kotlin `ImageVector`.

1. **Land the generated files.** Copy `kotlin/*.kt` into `shared/src/commonMain/kotlin/com/bernaferrari/bravedns/ui/icons/`. Each file is generated by `tools/icons.py` from the same geometry as the SVGs. It is one `val KeepoutIcons.<Name>: ImageVector` with a lazily cached backing field, `ImageVector.Builder(24.dp, 24.dp, 24f, 24f)` and `path(fill = SolidColor(Color.Black), pathFillType = NonZero) { moveTo…; quadTo…; curveTo…; close() }`. Ghost paths add `fillAlpha = 0.38f`. Everything is fill-only, so tinting is clean. If the repo prefers the exact `MaterialSymbols` shape (`internal var _x` and the full `path(...)` argument list), it's a template change in `kotlin()` in `icons.py`. The files haven't been compiled yet; that's the first check.
2. **Make icons.py the converter.** Keep the SVGs as the design source and regenerate on change: SVG → pathops → Kotlin. Add a `--out shared/...` flag and a Gradle-free check that fails CI if `kotlin/` and the SVGs drift.
3. **Swap the callers, one meaning at a time.** Follow `inventory.md`:
   - Settings hub: replace the nine `R.drawable` tiles in `ConfigureScreen.kt` and `SettingsSearchIndex.kt` with `KeepoutIcons.*`. Change `iconRes: Int` to `icon: ImageVector`, and delete those drawables.
   - Bottom nav (`HomeNavigation.kt`): `Home/HomeSelected`, `Stats/StatsSelected`, `Settings/SettingsSelected`.
   - Verdict badge (`RethinkLogRow.VerdictBadge`): draw the solid verdict icon at 17dp on a 2dp ring in the card colour. `DnsQueryMark` keeps its shapes and swaps the glyphs.
   - Wi-Fi/mobile pair (`RethinkFirewallAppListScreen`, `RethinkAppInfoScreen`): `Wifi/WifiBlocked/Mobile/MobileBlocked`.
   - Split the overloaded glyphs: `Security`, `Public`/`Language`, `Dns`, `Apps`, `VpnKey`, `Tune`, `Settings` (System DNS → `Device`).
   - Home hero (`RethinkHomeScreen.HomeMark`): replace the inner Shield/WarningAmber with the state glyph (`Protected` bar / `Paused` / `Off` / `Recovering`) inside the existing cookie morph.
   - `FirewallRuleset.getRulesIcon`: return `ImageVector` (verdict family + the rule's subject) instead of 30 grey/alpha/white drawables.
   - Empty states: `EmptyLogs`, `EmptyRules`, `EmptyApps`.
4. **Keep as drawables (not in-app UI):** `ic_notification_icon` (status bar), `ic_firewall_bubble` (the system bubble API needs an `Icon` resource), `default_app_icon` (PackageManager fallback), `fingerprint_dialog_error`.
5. **Protocols and UDP:** replace the icons on DoH/DoT/ODoH/DNSCrypt/UDP rows with small text plates. Glyphs can't tell protocols apart.
6. **Verify on the real screens:** re-run the store capture, then `tools/composite.py` against the new captures. The composites should be pixel-near the B column.

## Style B set (`svg/b/`, `kotlin/`)

Nav: home, home_selected, stats, stats_selected, settings, settings_selected.
Hub: apps, dns, firewall, proxy, network, general, logs, anti_censorship, advanced.
Protection: protected, paused, off, recovering.
Verdicts: allowed, blocked, bypassed, trusted, partial (+ mark_* glyphs for badges).
Networks: wifi, wifi_blocked, mobile, mobile_blocked.
Logs: logs_network, logs_dns, filter, app_filter.
DNS: dns_system, dns_other, dns_rethink, dns_smart.
Rules: domain_rule, ip_rule, universal, exclude, lockdown, temp_allow.
Empty: empty_logs, empty_rules, empty_apps.
Review extras: critical, database, pulse, http_insecure, metered, key, bubble, events, terminal, plus, globe, device.
