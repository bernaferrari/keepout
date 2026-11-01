# Round 2 — proposals C and D (recognition first)

**Rule:** an icon only changes if the replacement is at least as recognisable, without its label, as the glyph it replaces. Brand flavour is a bonus, never a reason. Each concept gets one glyph, and each glyph means one concept. Conventions are kept: Wi-Fi/mobile on/off (including the slash), house / bar chart / gear nav, globe, shield, lock, key, warning, bell, search, back, close, check, refresh.

- **C · Refined Material:** Material Symbols Rounded throughout (opsz 24, wght 400, GRAD 0; FILL 1, with FILL 0 for unselected nav), fixes the overloading, de-duplicates. Custom drawing only where Material has no convention: the firewall **brick wall**, the **paused shield** (shield + pause), **blocklists** (Material list lines + Material `block` as a badge) and **bypass** (the wall + Material's off-slash). All are drawn at Filled-400 weight from Material parts.
- **D · Material + Keepout touch:** C plus Keepout details only where they reinforce meaning. After the label-free test only two survive (see below):
  1. **Blocked** = the keep-out minus-in-circle (`do_not_disturb_on`) everywhere blocked appears: badge, chip, counter, "Blocked" pill. It is the launcher's sign and the road "no entry" sign.
  2. **Firewall** = the same wall, with its centre brick drawn as the keep-out bar.

## Files

| Path | What |
|---|---|
| `cd-in-context.png` | **Start here.** The real store captures, current \| C \| D, with only changed glyphs repainted (same place, size and tint). Kept glyphs are left as captured. |
| `cd-table.png` | Every concept: current glyph → C → D, with the reason |
| `svg/c/*.svg`, `svg/d/*.svg` | 53 concepts each (24×24, fill-only paths) |
| `kotlin-cd/*.kt` | The custom glyphs as Compose `ImageVector`s (`KeepoutIcons.Firewall`, `FirewallD`, `Paused`, `Blocklists`, `Bypassed`) |
| `composites/c`, `composites/d` | Full-size composited screens |
| `tools/cd.py` | Set definition (Material picks + custom geometry) → SVG, Kotlin, `manifest-cd.json` |
| `tools/symbols.py` | Fetches Material Symbols Rounded from the same Kotlin endpoint as `scripts/generate-material-symbols.sh` (cache: `tools/symbols/`) |
| `tools/render-cd.cjs` | Composites, table, blind-test sheets |
| `tools/blind-*.png` | The unlabelled sheets used for the label-free test |

Rebuild: `uv run --with skia-pathops --with fonttools python design/icons/tools/cd.py && uv run --with pillow python design/icons/tools/composite.py && node design/icons/tools/render-cd.cjs`

## Per-icon table

| Concept | Current | C | D | Why |
|---|---|---|---|---|
| Home tab (unselected) | home (Outlined) | `home (outlined)` | = C | Kept: the house is the convention. |
| Home tab (selected) | home (Filled) | `home` | = C | Kept. |
| Stats tab (unselected) | bar_chart (Filled) | `insert_chart (outlined)` | = C | bar_chart has no outlined form, so the tab never changed state. insert_chart is the same bars in a frame and has both. |
| Stats tab (selected) | bar_chart (Filled) | `insert_chart` | = C | Filled frame = selected. |
| Settings tab (unselected) | settings (Outlined) | `settings (outlined)` | = C | Kept. |
| Settings tab (selected) | settings (Filled) | `settings` | = C | Kept. |
| Apps (tile, quick action, app filter, app-wise rules) | ic_app_info_accent (2x2) + apps | `apps` | = C | Android's own 'apps' glyph; now used only for apps. |
| DNS (tile, logs tab, DNS rows, Other DNS) | dns_home_screen (globe) + dns | `dns` | = C | Material's dns glyph *is* the server rack; the globe tile is dropped so the hub matches every other DNS row. |
| Firewall | firewall_home_screen (a cloud!) | `custom: wall` | `custom: wall_bar` | The brick wall is the universal firewall sign; Material has none, so it is drawn at Filled weight with 1.5dp mortar cuts. D: the centre brick is the keep-out bar. |
| Proxy | ic_proxy (share-like node) | `conversion_path` | = C | A route through a waypoint: traffic goes via something else. |
| Network / VPN tunnel | ic_network_tunnel (molecule) | `vpn_lock` | = C | Material's VPN glyph: globe with a lock. |
| General settings | ic_other_settings (two tiny cogs) | `tune` | = C | Sliders = preferences; the cog stays reserved for the Settings tab. |
| Logs (tile, quick action, 'All' filter) | ic_logs_accent (heartbeat) + subject | `format_list_bulleted` | = C | A list of entries. The heartbeat read 'health'. |
| Anti-censorship (DPI evasion) | ic_anti_dpi (bird) | `domino_mask` | = C | Disguise: the traffic wears a mask so inspection can't recognise it. |
| Advanced / experimental | ic_advanced_settings (warning!) + build | `science` | = C | Flask = experimental; the warning triangle said 'danger'. |
| Home hero mark (inside the cookie) | shield | `gpp_good` | = C | Same glyph as 'Protection on', so the plain shield is left to Rethink DNS. The bar-in-seal option failed the label-free test ('no rule / not set') and is withdrawn. |
| Protection on | shield | `gpp_good` | = C | Shield + check = 'you're protected'. |
| Protection paused | — | `custom: shield_paused` | = C | Shield with the pause sign (no Material glyph; built from shield + pause bars). |
| Protection off | shield (same as on!) | `remove_moderator` | = C | Slashed shield = protection off. Today 'off' and 'on' share one glyph. |
| Protection recovering | warning_amber | `gpp_maybe` | = C | Still a shield, with '!' = needs attention. |
| Allowed verdict (badge, counter, chip) | check_circle / check | `check_circle` | = C | Kept. |
| Blocked verdict (badge, counter, chip, pill) | block / close (badge) | `block` | `do_not_disturb_on` | C: the slashed circle everywhere, including the badge (today's badge uses × = 'error'). D: the keep-out minus-in-circle everywhere blocked appears — the launcher's sign, and still the road 'no entry' sign. |
| Bypassed (DNS & firewall) | dns (server!) | `custom: wall_off` | = C | Firewall off for this app: the wall with Material's off-slash, the same pattern as public_off ('Bypass universal rules') right below it. Every arrow variant failed the blind test (step_over read as 'redirect', over a bar as 'retry', over the wall as 'refresh'). |
| Trusted / allow only trusted | security (shield) | `verified` | = C | The verified seal; happens to echo Keepout's seal. |
| Partly blocked app | tune | `wifi_off` | = C | No glyph says 'partly'. contrast tested as 'dark mode' and indeterminate_check_box as 'disabled', so show the network that is actually blocked: wifi_off or mobiledata_off, which test high. |
| Wi-Fi allowed | wifi | `wifi` | = C | Kept. |
| Wi-Fi blocked | wifi_off | `wifi_off` | = C | Kept: the slash is the convention for network off. |
| Mobile data allowed | mobiledata_arrows (Outlined) | `mobiledata_arrows` | = C | Kept; use the Filled copy only (the two are identical). |
| Mobile data blocked | mobiledata_off / mobile_off | `mobiledata_off` | = C | Kept; mobile_off (crossed phone) removed. |
| Network logs tab | public (globe) | `sync_alt` | = C | Two-way arrows = connections; frees the globe for 'universal'. |
| Filter | filter_list | `filter_list` | = C | Kept. |
| System DNS | settings (gear) | `android` | = C | The Android system's resolver: the Android robot, not a gear. |
| Rethink DNS (blocklist-aware) | security | `shield` | = C | Protective DNS = the shield (only meaning left for the plain shield). |
| Smart DNS | language (globe) | `auto_awesome` | = C | Sparkles = automatic/smart choice. |
| On-device blocklists | security (shield) | `custom: blocklist` | = C | A list with the blocked sign on it. (playlist_remove tested as 'clear list'.) |
| IP rules | public (globe) | `pin` | = C | Numbers in a box = numeric addresses. |
| Domain rules | dns / language | `link` | = C | A link = web address. |
| Universal rules / bypass universal | security / public | `public` | = C | Globe = applies everywhere (only meaning left for the globe). |
| Bypass universal rules | public | `public_off` | = C | Slashed globe = universal rules off for this app. |
| Rules (apps toolbar, rules empty state) | gavel | `rule` | = C | A checklist of ✓/✗ lines = rules. The gavel said 'legal'. |
| Exclude from Keepout | apps (grid) | `remove_moderator` | = C | The app's protection is off: same concept and glyph as 'Protection off'. (exit_to_app tested as 'import'.) |
| Lockdown | lock / block | `lock` | = C | Kept. |
| Block when device locked | lock | `screen_lock_portrait` | = C | Phone with lock, distinct from lockdown. |
| Temporarily allowed | timer | `timer` | = C | Kept (an hourglass tested no better). |
| Critical event | gpp_bad (shield) | `report` | = C | The octagon '!' = critical; frees the shield. |
| Database / DNS cache | storage (server rack) | `database` | = C | Cylinder = database; frees the rack for DNS. |
| Console logs | code | `terminal` | = C | Terminal prompt. |
| Event logs / history | event_note (calendar) | `history` | = C | Clock with back arrow = history. |
| Block on metered | cell_tower | `cell_tower` | = C | Kept. |
| Block plain HTTP | http | `http` | = C | Kept (text glyph, but unambiguous). |
| WireGuard / keys | vpn_key (5 meanings) | `vpn_key` | = C | Key only for WireGuard/keys now. |
| Empty: no logs | info | `format_list_bulleted` | = C | Empty states show the concept, not 'info'. |
| Empty: no apps found | apps / info | `search_off` | = C | Search with a slash = nothing found. |

## Label-free test

Method: a fresh agent with no context saw only numbered, unlabelled icons at 24dp and 48dp. It was told only that the app is a firewall / DNS / VPN tool, and was asked "what does this mean?" with a confidence score. The first run covered all 50 non-nav glyphs of C and D. Every failure was redrawn or reverted, then re-tested with fresh agents and distractors mixed in.

**Passed (high):** home, settings, stats (`insert_chart`), Wi-Fi on/off, firewall wall (both C and D), flask (Advanced), Android robot (System DNS), history (events), terminal, slashed shield (off / exclude), paused shield, shield-check (protected / hero), verified seal (trusted), timer, check (allowed), slashed circle (blocked, C), mask (anti-censorship: "private / incognito"), HTTP, report (critical), lock, sparkles (smart, med), server rack (DNS), blocklists (after the fix).

**Failed, then fixed:**

| Concept | Tried | Read as | Now |
|---|---|---|---|
| Exclude from Keepout | `exit_to_app` | "import rules" | `remove_moderator` (slashed shield): "protection off / app exempt", high |
| Partly blocked | `contrast` / `indeterminate_check_box` | "dark mode" / "disabled" | Show the network that is actually blocked (`wifi_off` / `mobiledata_off`); no glyph says "partly" |
| Blocklists | `playlist_remove`; then list + full-size badge | "clear list"; "banned" | List lines + a 10dp `block` badge: "blocklist", high |
| Bypass DNS & firewall | `step_over`; arrow over bar; arrow over wall | "redirect"; "retry"; "refresh" | Slashed brick wall: "firewall turned off / app allowed", med |
| Hero mark (D) | bar in the seal | "no rule / not set", the weakest of 50 | **Withdrawn.** The hero is the shield-check in both C and D |
| Hero vs Rethink DNS | both the plain shield | (same glyph, two concepts) | Hero = shield-check; plain shield = Rethink DNS only |

**Still weak (judge before shipping):**
- **Bypass universal rules** (`public_off`) read as "no internet". In a firewall app that's a real misread; the row label carries it.
- **Proxy** (`conversion_path`) read as "route / proxy chain" (med) in one run and "custom route" (low) in another. No proxy glyph is a convention, but it's still far better than today's share-node.
- **IP rules** (`pin`, "123") read as "numeric / IP entry" (low).
- **Domain rules** (`link`) read as "links" (med).
- **Network logs** (`sync_alt`) read as "data transfer" (med).
- **D's minus-in-circle** read as "blocked / remove" (med), against high for C's slashed circle. In one run the tester also noted it could be mixed up with a minus-in-square.

## Recommendation: C, optionally with D's blocked sign

C wins on the governing rule. It changes ~30 concepts, each to a glyph that tested at least as recognisable as today's (most much better: the cloud firewall, the bird, the warning-triangle Advanced, the heartbeat Logs, the gear System DNS, the server-rack Domain rules, the grid Exclude). It needs only five small custom glyphs, all built from Material parts at Material weight, so it is indistinguishable in weight and corners from the generic glyphs around it.

D's Keepout touch survived the test in just two places. The minus-in-circle "blocked" is the one worth taking: it ties every block in the app to the launcher's keep-out sign, and it's still the international "no entry" sign. Its cost is one notch of recognition (med vs high). Take it only if the owner values that link, and then use it everywhere blocked appears (badge, chip, counter, pill) so it stays one concept, one glyph. The bar-brick firewall is harmless but nobody will notice it.

## Implementation (next round, no app code yet)

1. Add the Material picks to `scripts/generate-material-symbols.sh` and regenerate. FILLED: android, auto_awesome, conversion_path, database, domino_mask, format_list_bulleted, gpp_good, gpp_maybe, history, insert_chart, link, pin, public_off, remove_moderator, report, rule, science, screen_lock_portrait, search_off, sync_alt, terminal, verified, vpn_lock (+ do_not_disturb_on for D). OUTLINED: insert_chart.
2. Copy `kotlin-cd/KeepoutFirewallIcon.kt`, `KeepoutPausedIcon.kt`, `KeepoutBlocklistsIcon.kt`, `KeepoutBypassedIcon.kt` (+ `KeepoutFirewallDIcon.kt` for D) into `shared/.../ui/icons/` alongside `object KeepoutIcons`. The vectors are fill-only, `SolidColor(Color.Black)`, tinted at use. Compile them; they haven't been compiled yet.
3. Replace the nine Settings-hub drawables (`ConfigureScreen.kt`, `SettingsSearchIndex.kt`: `iconRes: Int` → `ImageVector`) and `FirewallRuleset.getRulesIcon` with vectors. No drawables for in-app icons; keep only the notification, bubble, PackageManager-fallback and fingerprint drawables (system APIs).
4. Swap callers per the table: nav Stats (fixes the state bug), verdict badge (Close → `block` or `do_not_disturb_on`), split `Security` / `Public` / `Language` / `Dns` / `Apps` / `VpnKey` / `Tune` / `Settings`, plus empty states.
5. Clean up: collapse Clear/Delete/WarningAmber/RestartAlt and the duplicate Mobiledata Outlined copies, delete the four unused symbols, fix StarBorder, move the demo stand-ins to `DemoAppGlyphs` (see `inventory.md`).
6. Re-capture the store screens and re-run `tools/composite.py`; the result should match the C column.
