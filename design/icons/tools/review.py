"""The full icon review: every MaterialSymbols vector in shared/.../ui/icons and every drawable the
Compose UI paints, with meaning, verdict and replacement. Writes ../inventory.md and review.json
(the latter feeds the current -> new sheet in render.cjs).

python3 design/icons/tools/review.py
"""
import json
import os
import re
import subprocess

HERE = os.path.dirname(os.path.abspath(__file__))
ROOT = os.path.normpath(os.path.join(HERE, "../../.."))

# verdicts
R = "REPLACE"      # Keepout glyph replaces it
SPLIT = "SPLIT"    # one Material glyph carries several meanings -> several Keepout glyphs
KEEP = "KEEP"      # generic UI glyph; Material Symbols Rounded 400 matches style B's weight, keep
DUP = "COLLAPSE"   # duplicate of another file; delete and point callers at the survivor
DEMO = "DEMO"      # stands in for a third-party app's icon in the web demo; keep Material, move out
DEL = "DELETE"     # unused
FIX = "FIX"        # bug

# name: (meaning(s) in Keepout, verdict, replacement keys (style B), why)
SYMBOLS = {
    "Add": ("add rule / endpoint / WireGuard peer, pause +1 min", KEEP, [], "Generic action; Rounded 400 already matches."),
    "Apps": ("app list, quick tile, Exclude from Keepout, app filter, app-wise rules, empty states", SPLIT, ["apps", "app_filter", "exclude"],
             "A 2x2 grid means 'apps' in the launcher, but here it also means 'exclude' and 'filter by app' — three meanings, one glyph."),
    "ArrowBack": ("top-bar back", KEEP, [], "Generic; AutoMirrored."),
    "ArrowForward": ("log row: forwarded/redirected", KEEP, [], "Generic."),
    "ArrowOutward": ("—", DEL, [], "Unused."),
    "Article": ("docs, licence (About); 'Logs' entry in the web demo", SPLIT, ["logs"], "Docs/licence keep Material; the demo 'Logs' entry must use the Logs glyph."),
    "AutoStories": ("demo app icon", DEMO, [], "Third-party stand-in."),
    "Backup": ("backup & restore", KEEP, [], "Generic; recognisable."),
    "BarChart": ("bottom nav Stats, About stats", R, ["stats", "stats_selected"], "Nav needs a selected/unselected pair; current Filled==Outlined so the tab never changes state."),
    "Block": ("blocked verdict, counters, bulk lockdown, new-app rule, filter", R, ["blocked", "lockdown"],
              "The diagonal slash is generic 'prohibited'. Keepout's sign is the horizontal bar; lockdown gets its own glyph."),
    "BrightnessAuto": ("theme: follow system", KEEP, [], "Generic appearance glyph."),
    "BugReport": ("crash reports, debug log level, support diagnostics", KEEP, [], "Generic."),
    "Build": ("Advanced: experimental features", R, ["advanced"], "Same idea as the Advanced tile; use the flask."),
    "BusinessCenter": ("demo app icon", DEMO, [], ""),
    "CalendarMonth": ("demo app icon", DEMO, [], ""),
    "CameraAlt": ("WireGuard: scan QR; demo app icon", KEEP, [], "Generic camera/QR action."),
    "Cancel": ("console error level, ping test failed", KEEP, [], "Generic error."),
    "CellTower": ("universal rule: block on metered", R, ["metered"], "A tower reads as 'cellular', not 'metered'."),
    "Chat": ("demo app icon", DEMO, [], ""),
    "ChatBubble": ("Settings: firewall bubble", R, ["bubble"], "The feature is a floating head showing blocks; draw that."),
    "Check": ("checkmarks in chips, pickers, selected theme", KEEP, [], "Generic."),
    "CheckCircle": ("allowed verdict, counters, success, selected rule", R, ["allowed"], "Pairs with the new blocked/bypassed/trusted family."),
    "Clear": ("clear search", DUP, [], "Same path as Close."),
    "Close": ("close/dismiss", KEEP, [], "Generic."),
    "Cloud": ("demo app icon", DEMO, [], ""),
    "Code": ("Console logs; GitHub link; demo", SPLIT, ["terminal"], "Console logs get a terminal glyph; GitHub link keeps Code."),
    "ContentCopy": ("copy", KEEP, [], "Generic."),
    "DarkMode": ("theme: dark", KEEP, [], "Generic."),
    "Delete": ("delete", DUP, [], "Same path as DeleteOutline; keep one."),
    "DeleteOutline": ("delete rule/log/peer", KEEP, [], "Generic."),
    "Dns": ("DNS (tiles, rows, tabs, quick action), domain rules, Bypass DNS, server/endpoint, DNS-ALG, proxy DNS", SPLIT,
            ["dns", "logs_dns", "dns_other", "domain_rule", "bypassed"],
            "A server rack is used for 'DNS', 'domain', 'bypass' and 'server' — the most overloaded glyph in the app after Security."),
    "Download": ("download blocklists / config", KEEP, [], "Generic."),
    "Edit": ("edit", KEEP, [], "Generic."),
    "EditLocationAlt": ("Rethink Plus: server location", KEEP, [], "Generic."),
    "Email": ("contact / support", KEEP, [], "Generic."),
    "EventNote": ("Event logs, history, no-history empty state", R, ["events"], "A calendar page isn't an event log; a timeline is."),
    "FilterList": ("filter (logs, apps, blocklists, events, console)", R, ["filter"], "Three lines = sort; the funnel with the bar reads 'filter' and is on-brand."),
    "Forum": ("demo app icon", DEMO, [], ""),
    "Gavel": ("Apps toolbar: Rules; About: Terms", SPLIT, ["domain_rule"], "Rules are IP/domain rules — use the rule tag. Terms keeps a document glyph."),
    "GppBad": ("event severity critical; firewall settings", R, ["critical"], "One of five shields; critical = seal with '!'."),
    "Group": ("demo app icon", DEMO, [], ""),
    "Headset": ("support (Rethink Plus); demo", KEEP, [], "Generic support."),
    "HelpOutline": ("help (About)", KEEP, [], "Generic."),
    "Home": ("bottom nav Home (Filled/Outlined pair)", R, ["home", "home_selected"], "Home is the protection screen; its tab is the Keepout seal."),
    "Http": ("universal rule: block plain HTTP; proxy HTTP", R, ["http_insecure"], "Text glyphs ('HTTP') are not icons; an open padlock says 'unencrypted'."),
    "Info": ("info rows, severities low/medium, empty states", KEEP, [], "Generic; but empty states move to the new empty_* glyphs."),
    "KeyboardArrowDown": ("expand", KEEP, [], "Generic."),
    "KeyboardArrowRight": ("row chevron", KEEP, [], "Generic."),
    "Language": ("domain rules, smart DNS, split DNS, favicons, allowed DNS answer", SPLIT, ["domain_rule", "dns_smart", "globe"],
                 "Globe stands for four things; each gets its own glyph, the plain globe survives only for 'the web'."),
    "LightMode": ("theme: light", KEEP, [], "Generic. (WbSunny is a near-duplicate used only by the demo.)"),
    "LocalTaxi": ("demo app icon", DEMO, [], ""),
    "Lock": ("lockdown filter, device-locked rule, universal lockdown; demo Signal", R, ["lockdown"], "Lockdown = padlock with the bar."),
    "Map": ("demo app icon", DEMO, [], ""),
    "MobileOff": ("bulk block mobile, blocked-on-mobile filter", R, ["mobile_blocked"], "A crossed-out phone is not 'mobile data'."),
    "MobiledataArrows": ("mobile data allowed (Filled + Outlined copies)", R, ["mobile"], "Thin 2dp arrows beside a heavy filled Wi-Fi wedge; Filled == Outlined (duplicate)."),
    "MobiledataOff": ("mobile data blocked (Filled + Outlined copies)", R, ["mobile_blocked"], "Diagonal slash; duplicate pair."),
    "MoreHoriz": ("endpoint row menu", KEEP, [], "Generic."),
    "MoreVert": ("overflow menu", KEEP, [], "Generic."),
    "Movie": ("demo app icon", DEMO, [], ""),
    "MusicNote": ("demo app icon", DEMO, [], ""),
    "NetworkCheck": ("flight recorder, tunnel network check", R, ["pulse"], "A speedometer for 'recording'; the pulse line says 'diagnostics'."),
    "NetworkPing": ("demo fixture", R, ["pulse"], ""),
    "NewReleases": ("what's new", KEEP, [], "Generic."),
    "Notifications": ("notifications", KEEP, [], "Generic."),
    "NotificationsOff": ("alerts off", KEEP, [], "Generic."),
    "Palette": ("theme colour", KEEP, [], "Generic."),
    "PhoneAndroid": ("system app info, proxy: this device", R, ["device"], "Same glyph as System DNS: 'the device'."),
    "PhotoLibrary": ("demo app icon", DEMO, [], ""),
    "PlayArrow": ("start protection, resume", KEEP, [], "Generic media-style action."),
    "Policy": ("privacy policy; demo anti-censorship", SPLIT, ["anti_censorship"], "Shield #4. Privacy policy keeps a document glyph; anti-censorship gets its own."),
    "Public": ("IP rules, bypass universal, universal rules, network logs tab, block-free DNS, countries…", SPLIT,
               ["ip_rule", "universal", "logs_network", "globe"], "Globe #2 — 20 uses, five meanings."),
    "RadioButtonChecked": ("—", DEL, [], "Unused."),
    "RadioButtonUnchecked": ("—", DEL, [], "Unused."),
    "Refresh": ("refresh", KEEP, [], "Generic."),
    "Remove": ("pause −1 min", KEEP, [], "Generic (and it already is the bar)."),
    "RestartAlt": ("reset server/countries", DUP, [], "Near-duplicate of Refresh; use Refresh."),
    "Restore": ("restore backup", KEEP, [], "Generic."),
    "Search": ("search", KEEP, [], "Generic."),
    "Security": ("firewall, Rethink DNS, isolate, exclude, universal, fallback DNS, prevent leaks… (26 uses)", SPLIT,
                 ["firewall", "dns_rethink", "trusted", "exclude", "universal", "protected"],
                 "The most overloaded glyph: one shield for six unrelated meanings."),
    "Send": ("send feedback", KEEP, [], "Generic."),
    "Settings": ("bottom nav Settings (pair), System DNS, misc settings rows", SPLIT, ["settings", "settings_selected", "dns_system"],
                 "System DNS should say 'device', not 'settings'."),
    "Share": ("share", KEEP, [], "Generic."),
    "Shield": ("Home hero, VPN-off empty state, DoT protocol, background rule", R, ["protected", "off"], "Shield #5. The hero already lives in a seal; the glyph inside is the bar."),
    "ShieldMoon": ("ODoH protocol", R, [], "Replace protocol icons with text plates (DoH / DoT / ODoH / DNSCrypt) — icons can't tell protocols apart."),
    "Shop": ("demo app icon", DEMO, [], ""),
    "ShoppingCart": ("no orders empty state; demo", KEEP, [], "Generic commerce."),
    "SignalCellularAlt": ("reachability probe: cellular", R, ["mobile"], "Mobile data has one glyph everywhere."),
    "Star": ("favourite, Rethink Plus", SPLIT, ["plus"], "Favourite keeps Star; Rethink Plus gets the seal with +."),
    "StarBorder": ("not favourite", FIX, [], "Renders filled (bug): the Outlined variant points at the filled path. Regenerate."),
    "Stop": ("stop protection", KEEP, [], "Generic."),
    "Storage": ("database inspector, DNS cache, db dump, custom downloader", R, ["database"], "A server rack (again) for 'local data'; draw a database."),
    "Subject": ("logs (quick action, misc settings, 'All' filter)", R, ["logs"], "Text-align glyph; Logs gets a log-row glyph."),
    "SystemUpdate": ("app updates", KEEP, [], "Generic."),
    "SystemUpdateAlt": ("—", DEL, [], "Unused."),
    "Timer": ("temporary allow", R, ["temp_allow"], "Hourglass with the bar of sand: 'allowed for now'."),
    "Tune": ("DNS settings, tunnel, auto-dial, manual config; partial status", SPLIT, ["settings", "partial"], "'Partial' (some networks blocked) is a status, not a setting."),
    "VpnKey": ("proxy, WireGuard, bypass, DNSCrypt, secure routing (18 uses)", SPLIT, ["proxy", "key", "bypassed"], "A key for five meanings."),
    "Warning": ("panic mode, severity high, console warning", KEEP, [], "Generic."),
    "WarningAmber": ("Home hero 'recovering', empty-state error", SPLIT, ["recovering"], "Duplicate of Warning; the hero gets the recovering seal."),
    "WbSunny": ("demo app icon", DEMO, [], "Near-duplicate of LightMode."),
    "Wifi": ("Wi-Fi allowed, SSID, UDP rule (!)", R, ["wifi"], "Matched pair with mobile; UDP rule needs its own (text plate)."),
    "WifiOff": ("Wi-Fi blocked", R, ["wifi_blocked"], "Diagonal slash -> the bar."),
    "Work": ("demo app icon", DEMO, [], ""),
}

# Drawables painted by Compose (to become Kotlin ImageVectors — no drawables for in-app icons)
DRAWABLES = [
    ("ic_app_info_accent", "Settings tile Apps, search index", "apps", "256-unit viewport, 2x2 grid; meaningless next to 'Exclude' using the same grid."),
    ("dns_home_screen", "Settings tile DNS, search index", "dns", "Globe — shared with four other meanings."),
    ("firewall_home_screen", "Settings tile Firewall, search index", "firewall", "It is a CLOUD. Wrong glyph."),
    ("ic_proxy", "Settings tile Proxy, search index", "proxy", "Share-like node; reads as 'share'."),
    ("ic_network_tunnel", "Settings tile Network, search index", "network", "32-unit viewport molecule; reads as 'chemistry'."),
    ("ic_other_settings", "Settings tile General, search index", "general", "Two tiny cogs; illegible at 24dp."),
    ("ic_logs_accent", "Settings tile Logs, search index", "logs", "512-unit viewport heartbeat; 'health', not logs."),
    ("ic_anti_dpi", "Settings tile Anti-censorship, search index", "anti_censorship", "192-unit viewport stroked bird; nobody reads 'bird' as anti-DPI."),
    ("ic_advanced_settings", "Settings tile Advanced, search index", "advanced", "A warning triangle — tells users Advanced is dangerous."),
    ("ic_settings", "search index (21 entries)", "settings", "Generic cog for every search hit."),
    ("ic_appearance / ic_auto_start / ic_backup / ic_blocklist_update_check / ic_dns_cache / ic_fav_icon / ic_filter / ic_ip_address / ic_ip_info / ic_local_blocklist / ic_network / ic_split_dns / ic_socks5 / ic_tombstone / ic_rethink_plus / ic_right_arrow_small / ic_allow_dns_records / ic_adv_dns_filter / ic_dns_rules_as_firewall / ic_prevent_dns_leaks / ic_orbot / ic_http",
     "settings search index rows", "per row: Keepout glyph or kept Material equivalent", "Search results should show the same glyph as the row they open."),
    ("FirewallRuleset.getRulesIcon (30 rules): ic_update, ic_app_info, ic_auto_start, ic_filter_error, ic_firewall_wifi_on_grey, ic_firewall_data_on_grey_alpha, ic_univ_metered, ic_firewall_lockdown_off, ic_bypass_dns_firewall_off, ic_firewall_block_grey, ic_bypass, bs_dns_home_screen (x8), ic_device_lock, ic_foreground, ic_unknown_app, ic_udp, ic_prevent_dns_leaks, bs_firewall_home_screen, ic_orbot, ic_http, ic_global_lockdown, ic_proxy_white (x4)",
     "Log detail: why a connection was blocked/allowed (NetworkLogsScreen painterResource)", "blocked / allowed / bypassed / trusted / wifi_blocked / mobile_blocked / metered / lockdown / http_insecure / dns / proxy / exclude",
     "Grey/alpha/white variants of the same idea; the reason icon should be the verdict family plus the rule's subject."),
    ("default_app_icon", "fallback app icon (Utilities)", "keep as drawable", "Used where a Drawable is required (PackageManager fallback)."),
    ("ic_firewall_bubble", "BubbleHelper (system bubble)", "keep as drawable", "Android bubble/notification APIs need an Icon resource; not an in-app icon."),
    ("fingerprint_dialog_error", "TransactionErrors", "keep as drawable", "System dialog."),
]

# Keepout-specific spots that are not files: hero, nav, badges
SPOTS = [
    ("Home hero mark", "Shield (or WarningAmber) inside a Cookie12Sided morph", "the bar (protected) / two bars (paused) / hollow bar (off) / three dots (recovering)",
     "The hero already IS the seal; a shield inside a seal is two brand marks fighting."),
    ("Log verdict badge", "red disc + Close (x) / green disc + Check", "blocked / allowed / bypassed / trusted (solid icon, ring in card colour)",
     "'x' reads as 'error/remove'. The bar is Keepout's 'kept out'. Adds the missing bypassed and trusted states."),
    ("DNS query mark", "Cookie6Sided + Block / circle + Language", "blocked / globe", "Already shape-coded; only the glyphs change."),
    ("Bottom nav", "Home Filled/Outlined, BarChart Filled/Filled (no state!), Settings Filled/Outlined", "home(_selected), stats(_selected), settings(_selected)",
     "Stats never changes on selection today."),
    ("Wi-Fi / mobile data pair", "bold filled Wi-Fi wedge next to thin 2dp data arrows; slash for blocked", "wifi / wifi_blocked / mobile / mobile_blocked",
     "Designed as one pair, same weight, blocked = bar across."),
]


def usages():
    """Count MaterialSymbols.*.<Name> references outside ui/icons."""
    out = subprocess.run(["grep", "-rhoE", r"MaterialSymbols\.(Filled|Outlined|AutoMirrored\.Filled)\.[A-Za-z]+", "--include=*.kt",
                          os.path.join(ROOT, "shared"), os.path.join(ROOT, "app")], capture_output=True, text=True).stdout
    counts = {}
    for line in out.split():
        name = line.split(".")[-1]
        counts[name] = counts.get(name, 0) + 1
    return counts


def main():
    counts = usages()
    rows = []
    for name, (meaning, verdict, new, why) in sorted(SYMBOLS.items()):
        rows.append({"name": name, "uses": counts.get(name, 0), "meaning": meaning, "verdict": verdict, "new": new, "why": why})
    with open(os.path.join(HERE, "review.json"), "w") as fh:
        json.dump(rows, fh, indent=1)

    tally = {}
    for r in rows:
        tally[r["verdict"]] = tally.get(r["verdict"], 0) + 1

    md = ["# Keepout icon inventory", "",
          "Every icon the Compose UI paints, what it means here, and what happens to it. Verdicts:",
          "",
          "- **REPLACE** — a Keepout glyph takes over (style B names in `svg/b/`).",
          "- **SPLIT** — one Material glyph carries several unrelated meanings; each meaning gets its own glyph.",
          "- **KEEP** — truly generic UI glyph (back, close, copy, share…). Material Symbols Rounded, weight 400, is the weight style B is calibrated to, so these sit in the family unchanged.",
          "- **COLLAPSE** — duplicate file; delete it and point callers at the survivor.",
          "- **DEMO** — stands in for a third-party app's icon in the web demo. Keep Material (it should *not* look like Keepout), move to a `DemoAppGlyphs` object so nobody reuses it as UI.",
          "- **DELETE** — unused. **FIX** — bug.",
          "",
          "Tally (MaterialSymbols: %d icon names, 110 vectors incl. Outlined variants, 106 files): " % len(rows) + ", ".join(f"{k} {v}" for k, v in sorted(tally.items())) + ".",
          "Rendered side by side: `review.png` (current glyph → new glyph).",
          "",
          "## The problems in one paragraph",
          "",
          "Keepout speaks in five shields (Shield, Security, GppBad, Policy, ShieldMoon) whose differences mean nothing; "
          "`Security` alone stands for firewall, Rethink DNS, isolate, exclude, universal rules and leak prevention (26 uses). "
          "Two globes (`Language`, `Public`) cover IP rules, domain rules, smart DNS, network logs and 'universal' (30 uses). "
          "A server rack (`Dns`) means DNS, domain, bypass and server. A 2x2 grid means apps, exclude-from-Keepout and filter-by-app. "
          "The Settings hub is nine drawables from five sources (viewports 24/32/192/256/512, mixed stroke and fill) — Firewall is a "
          "cloud, Logs is a heartbeat, Anti-censorship is a bird, Advanced is a warning triangle. Blocked is a diagonal slash "
          "everywhere, although Keepout's own sign is a horizontal bar. Stats in the nav uses the same glyph for selected and "
          "unselected, StarBorder renders filled, and seven files are duplicates.",
          "",
          "## Keepout-specific spots",
          "",
          "| Where | Today | New | Why |", "|---|---|---|---|"]
    for w, t, nw, why in SPOTS:
        md.append(f"| {w} | {t} | {nw} | {why} |")
    md += ["", "## Drawables painted in Compose → all become Kotlin ImageVectors", "",
           "No drawable resources for in-app icons. Everything below that is an in-app icon moves to `KeepoutIcons.*` (or a kept `MaterialSymbols.*`).", "",
           "| Drawable | Where | New | Why |", "|---|---|---|---|"]
    for d, w, nw, why in DRAWABLES:
        md.append(f"| `{d}` | {w} | {nw} | {why} |")
    md += ["", "## MaterialSymbols (shared/src/commonMain/kotlin/com/bernaferrari/bravedns/ui/icons/)", "",
           "| Icon | Uses | Means in Keepout | Verdict | New (style B) | Why |", "|---|---:|---|---|---|---|"]
    for r in rows:
        md.append(f"| `{r['name']}` | {r['uses']} | {r['meaning']} | **{r['verdict']}** | {', '.join('`' + n + '`' for n in r['new']) or '—'} | {r['why']} |")
    md += ["", "Notes:", "",
           "- Filled/Outlined pairs (Home, Settings, Info, Star, Mobiledata*) exist for selected/unselected states; the new nav keeps that rule (outlined + solid bar → solid with the bar cut out).",
           "- `MobiledataArrows`/`MobiledataOff` Filled and Outlined are identical paths; `Clear` = `Close`, `Delete` = `DeleteOutline`, `Warning` = `WarningAmber`, `LightMode` ≈ `WbSunny`, `Refresh` ≈ `RestartAlt`.",
           "- Protocol rows (DoH, DoT, ODoH, DNSCrypt) should use text plates, not glyphs: VpnKey/Shield/ShieldMoon/Security can't tell protocols apart.",
           "- The UDP universal rule currently shows the Wi-Fi glyph — wrong; use a text plate `UDP` like the protocols.",
           ""]
    with open(os.path.join(HERE, "..", "inventory.md"), "w") as fh:
        fh.write("\n".join(md))
    print(tally, sum(tally.values()))


if __name__ == "__main__":
    main()
