"""Proposals C ("Refined Material") and D ("Material + Keepout touch").

Recognition first: every concept gets the clearest conventional Material Symbols Rounded glyph
(opsz 24, wght 400, GRAD 0, FILL 1; FILL 0 for unselected nav). Custom geometry only where Material
has no convention (the firewall brick wall, a paused shield). D = C plus a few restrained Keepout
details that reinforce meaning without changing the recognisable base shape.

uv run --with skia-pathops --with fonttools python design/icons/tools/cd.py
"""
import json
import os
import sys

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import icons as K  # noqa: E402  (geometry helpers, SVG + Kotlin emitters)
import symbols  # noqa: E402

HERE = K.HERE


def sym(name, fill=1):
    paths = symbols.get(name, fill)
    if paths is None:
        raise SystemExit(f"missing symbol {name}")
    p = K.pathops.Path()
    for x in paths:
        q = K.P(x["d"])
        if x["eo"]:
            q.fillType = K.pathops.FillType.EVEN_ODD
        q = K.simp(q)
        p = K.union(p, q)
    return p


def S_(name, fill=1):
    f = lambda: K.Icon().add(sym(name, fill))  # noqa: E731
    f.sym = f"{name}{'' if fill else ' (outlined)'}"
    return f


def src(f):
    return getattr(f, "sym", None) or "custom: " + f.__name__


def wall():
    body = K.F(K.rr(2.5, 4, 19, 16, 2.25))
    cuts = [K.F(K.rr(1, 8.33, 22, 1.5, 0.2)), K.F(K.rr(1, 14.17, 22, 1.5, 0.2)),
            K.F(K.rr(11.25, 3, 1.5, 6, 0.2)), K.F(K.rr(6.75, 9, 1.5, 6, 0.2)), K.F(K.rr(15.75, 9, 1.5, 6, 0.2)),
            K.F(K.rr(11.25, 15, 1.5, 6, 0.2))]
    return K.Icon().add(body).cut(*cuts)


def wall_bar():
    """D: the same wall; its centre brick is the keep-out bar (pill ends, its own gap)."""
    ic = wall()
    brick = K.F(K.pill(8.75, 10.33, 6.5, 3.33))
    return ic.cut(K.F(K.rr(8.25, 9.83, 7.5, 4.33, 0.2))).a(brick)


def shield_paused():
    return K.Icon().add(sym("shield")).cut(K.F(K.pill(9.1, 8.25, 2.3, 7.5)), K.F(K.pill(12.6, 8.25, 2.3, 7.5)))


def step_over_bar():
    """D: Material step_over, its dot replaced by the keep-out bar — 'went over the block'."""
    d = symbols.get("step_over", 1)[0]["d"]
    arrow = d[d.index("M", 1):]  # second sub-path: the arrow (the first is the dot)
    return K.Icon().add(K.F(arrow)).a(K.F(K.pill(7, 15.25, 10, 3)))


def blocklist():
    """A list whose last entry is blocked: Material list lines + the Material block glyph as a badge."""
    lines = K.union(K.S("M4 5.5H20", 2), K.S("M4 10.5H20", 2), K.S("M4 15.5H12", 2), K.S("M4 20.5H11", 2))
    badge = sym("block")
    badge = badge.transform(0.42, 0, 0, 0.42, 13.4, 12.9)  # 24 -> 10dp, bottom-right
    return K.Icon().add(lines).knock(badge, 1.0, to="body")


def step_over_wall():
    """Bypass: Material step_over's arrow arcing over a small copy of the firewall wall."""
    d = symbols.get("step_over", 1)[0]["d"]
    arrow = K.F(d[d.index("M", 1):]).transform(1, 0, 0, 1, 0, -1.5)
    mini = wall().body.transform(0.55, 0, 0, 0.55, 5.4, 11.3)
    return K.Icon().add(arrow).knock(mini, 1.0, to="body")


def wall_off():
    """Bypass DNS & firewall = firewall off for this app: the wall with Material's off-slash
    (same construction as wifi_off / public_off: a cleared band + a 2dp round-capped slash)."""
    slash = K.S("M3.6 2.6L20.6 21.4", 2)
    ic = wall()
    ic.body = K.diff(ic.body, K.S("M3.6 2.6L20.6 21.4", 2 + 2 * 1.5))
    return ic.add(slash)


def seal_bar():
    """D hero option: the launcher's seal with the bar (used inside the existing hero cookie)."""
    return K.Icon().a(K.F(K.pill(4.5, 10.25, 15, 3.5)))


# key: (concept, C builder, D builder or None (= same as C), current glyph, why)
SET = [
    ("home", "Home tab (unselected)", S_("home", 0), None, "home (Outlined)", "Kept: the house is the convention."),
    ("home_selected", "Home tab (selected)", S_("home", 1), None, "home (Filled)", "Kept."),
    ("stats", "Stats tab (unselected)", S_("insert_chart", 0), None, "bar_chart (Filled)", "bar_chart has no outlined form, so the tab never changed state. insert_chart is the same bars in a frame and has both."),
    ("stats_selected", "Stats tab (selected)", S_("insert_chart", 1), None, "bar_chart (Filled)", "Filled frame = selected."),
    ("settings", "Settings tab (unselected)", S_("settings", 0), None, "settings (Outlined)", "Kept."),
    ("settings_selected", "Settings tab (selected)", S_("settings", 1), None, "settings (Filled)", "Kept."),

    ("apps", "Apps (tile, quick action, app filter, app-wise rules)", S_("apps"), None, "ic_app_info_accent (2x2) + apps", "Android's own 'apps' glyph; now used only for apps."),
    ("dns", "DNS (tile, logs tab, DNS rows, Other DNS)", S_("dns"), None, "dns_home_screen (globe) + dns", "Material's dns glyph *is* the server rack; the globe tile is dropped so the hub matches every other DNS row."),
    ("firewall", "Firewall", wall, wall_bar, "firewall_home_screen (a cloud!)", "The brick wall is the universal firewall sign; Material has none, so it is drawn at Filled weight with 1.5dp mortar cuts. D: the centre brick is the keep-out bar."),
    ("proxy", "Proxy", S_("conversion_path"), None, "ic_proxy (share-like node)", "A route through a waypoint: traffic goes via something else."),
    ("network", "Network / VPN tunnel", S_("vpn_lock"), None, "ic_network_tunnel (molecule)", "Material's VPN glyph: globe with a lock."),
    ("general", "General settings", S_("tune"), None, "ic_other_settings (two tiny cogs)", "Sliders = preferences; the cog stays reserved for the Settings tab."),
    ("logs", "Logs (tile, quick action, 'All' filter)", S_("format_list_bulleted"), None, "ic_logs_accent (heartbeat) + subject", "A list of entries. The heartbeat read 'health'."),
    ("anti_censorship", "Anti-censorship (DPI evasion)", S_("domino_mask"), None, "ic_anti_dpi (bird)", "Disguise: the traffic wears a mask so inspection can't recognise it."),
    ("advanced", "Advanced / experimental", S_("science"), None, "ic_advanced_settings (warning!) + build", "Flask = experimental; the warning triangle said 'danger'."),

    ("hero", "Home hero mark (inside the cookie)", S_("gpp_good"), None, "shield", "Same glyph as 'Protection on', so the plain shield is left to Rethink DNS. The bar-in-seal option failed the label-free test ('no rule / not set') and is withdrawn."),
    ("protected", "Protection on", S_("gpp_good"), None, "shield", "Shield + check = 'you're protected'."),
    ("paused", "Protection paused", shield_paused, None, "—", "Shield with the pause sign (no Material glyph; built from shield + pause bars)."),
    ("off", "Protection off", S_("remove_moderator"), None, "shield (same as on!)", "Slashed shield = protection off. Today 'off' and 'on' share one glyph."),
    ("recovering", "Protection recovering", S_("gpp_maybe"), None, "warning_amber", "Still a shield, with '!' = needs attention."),

    ("allowed", "Allowed verdict (badge, counter, chip)", S_("check_circle"), None, "check_circle / check", "Kept."),
    ("blocked", "Blocked verdict (badge, counter, chip, pill)", S_("block"), S_("do_not_disturb_on"),
     "block / close (badge)", "C: the slashed circle everywhere, including the badge (today's badge uses × = 'error'). D: the keep-out minus-in-circle everywhere blocked appears — the launcher's sign, and still the road 'no entry' sign."),
    ("bypassed", "Bypassed (DNS & firewall)", wall_off, None, "dns (server!)", "Firewall off for this app: the wall with Material's off-slash, the same pattern as public_off ('Bypass universal rules') right below it. Every arrow variant failed the blind test (step_over read as 'redirect', over a bar as 'retry', over the wall as 'refresh')."),
    ("trusted", "Trusted / allow only trusted", S_("verified"), None, "security (shield)", "The verified seal; happens to echo Keepout's seal."),
    ("partial", "Partly blocked app", S_("wifi_off"), None, "tune", "No glyph says 'partly'. contrast tested as 'dark mode' and indeterminate_check_box as 'disabled', so show the network that is actually blocked: wifi_off or mobiledata_off, which test high."),

    ("wifi", "Wi-Fi allowed", S_("wifi"), None, "wifi", "Kept."),
    ("wifi_off", "Wi-Fi blocked", S_("wifi_off"), None, "wifi_off", "Kept: the slash is the convention for network off."),
    ("mobile", "Mobile data allowed", S_("mobiledata_arrows"), None, "mobiledata_arrows (Outlined)", "Kept; use the Filled copy only (the two are identical)."),
    ("mobile_off", "Mobile data blocked", S_("mobiledata_off"), None, "mobiledata_off / mobile_off", "Kept; mobile_off (crossed phone) removed."),

    ("logs_network", "Network logs tab", S_("sync_alt"), None, "public (globe)", "Two-way arrows = connections; frees the globe for 'universal'."),
    ("filter", "Filter", S_("filter_list"), None, "filter_list", "Kept."),
    ("dns_system", "System DNS", S_("android"), None, "settings (gear)", "The Android system's resolver: the Android robot, not a gear."),
    ("dns_rethink", "Rethink DNS (blocklist-aware)", S_("shield"), None, "security", "Protective DNS = the shield (only meaning left for the plain shield)."),
    ("dns_smart", "Smart DNS", S_("auto_awesome"), None, "language (globe)", "Sparkles = automatic/smart choice."),
    ("blocklists", "On-device blocklists", blocklist, None, "security (shield)", "A list with the blocked sign on it. (playlist_remove tested as 'clear list'.)"),

    ("ip_rule", "IP rules", S_("pin"), None, "public (globe)", "Numbers in a box = numeric addresses."),
    ("domain_rule", "Domain rules", S_("link"), None, "dns / language", "A link = web address."),
    ("universal", "Universal rules / bypass universal", S_("public"), None, "security / public", "Globe = applies everywhere (only meaning left for the globe)."),
    ("bypass_universal", "Bypass universal rules", S_("public_off"), None, "public", "Slashed globe = universal rules off for this app."),
    ("rules", "Rules (apps toolbar, rules empty state)", S_("rule"), None, "gavel", "A checklist of ✓/✗ lines = rules. The gavel said 'legal'."),
    ("exclude", "Exclude from Keepout", S_("remove_moderator"), None, "apps (grid)", "The app's protection is off: same concept and glyph as 'Protection off'. (exit_to_app tested as 'import'.)"),
    ("lockdown", "Lockdown", S_("lock"), None, "lock / block", "Kept."),
    ("device_lock", "Block when device locked", S_("screen_lock_portrait"), None, "lock", "Phone with lock, distinct from lockdown."),
    ("temp_allow", "Temporarily allowed", S_("timer"), None, "timer", "Kept (an hourglass tested no better)."),
    ("critical", "Critical event", S_("report"), None, "gpp_bad (shield)", "The octagon '!' = critical; frees the shield."),
    ("database", "Database / DNS cache", S_("database"), None, "storage (server rack)", "Cylinder = database; frees the rack for DNS."),
    ("console", "Console logs", S_("terminal"), None, "code", "Terminal prompt."),
    ("events", "Event logs / history", S_("history"), None, "event_note (calendar)", "Clock with back arrow = history."),
    ("metered", "Block on metered", S_("cell_tower"), None, "cell_tower", "Kept."),
    ("http", "Block plain HTTP", S_("http"), None, "http", "Kept (text glyph, but unambiguous)."),
    ("wireguard", "WireGuard / keys", S_("vpn_key"), None, "vpn_key (5 meanings)", "Key only for WireGuard/keys now."),
    ("empty_logs", "Empty: no logs", S_("format_list_bulleted"), None, "info", "Empty states show the concept, not 'info'."),
    ("empty_apps", "Empty: no apps found", S_("search_off"), None, "apps / info", "Search with a slash = nothing found."),
]


def main():
    man = {"order": [k for k, *_ in SET], "rows": [], "styles": {"c": {}, "d": {}}}
    kdir = os.path.join(HERE, "..", "kotlin-cd")
    os.makedirs(kdir, exist_ok=True)
    for key, concept, c, d, current, why in SET:
        for style, b in (("c", c), ("d", d or c)):
            os.makedirs(os.path.join(HERE, "..", "svg", style), exist_ok=True)
            s = K.svg(b())
            open(os.path.join(HERE, "..", "svg", style, key + ".svg"), "w").write(s)
            man["styles"][style][key] = s
        custom = [n for n, f in (("c", c), ("d", d)) if f is not None and not hasattr(f, "sym")]
        for style in custom:
            f = c if style == "c" else d
            open(os.path.join(kdir, f"Keepout{K.camel(key)}{'' if style == 'c' else 'D'}Icon.kt"), "w").write(K.kotlin(f(), key + ('' if style == 'c' else '_d'), style))
        man["rows"].append({"key": key, "concept": concept, "current": current, "why": why, "d_differs": d is not None,
                            "c_src": src(c), "d_src": src(d) if d else "= C"})
    json.dump(man, open(os.path.join(HERE, "manifest-cd.json"), "w"))
    print(len(SET), "concepts;", sum(1 for r in man["rows"] if r["d_differs"]), "differ in D")


if __name__ == "__main__":
    main()
