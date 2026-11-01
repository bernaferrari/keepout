"""Finds every icon we replace on the real store captures and writes composite.json.

For each target box it detects the background colour (box border), the old glyph's pixels,
its bounding box and its tint, so render.cjs can paint the old glyph out and draw the new
icon at the same place, size and colour — the real screen with only the icons changed.

uv run --with pillow python design/icons/tools/composite.py
"""
import json
import os
from collections import Counter
from PIL import Image

HERE = os.path.dirname(os.path.abspath(__file__))
RAW = os.path.normpath(os.path.join(HERE, "../../../store-listing/en-US/raw/phone"))
DP = 2.625  # captures are 1080 px wide at 420 dpi

NAV = [("home", (141, 2112, 204, 2175)), ("stats", (508, 2112, 571, 2175)), ("settings", (875, 2112, 938, 2175))]


def nav(selected):
    return [(k + ("_selected" if k == selected else ""), (b[0] - 4, b[1] - 4, b[2] + 4, b[3] + 4), 24, True) for k, b in NAV]


# (icon key, search box, size in dp, centre = box centre?)
SETTINGS = [
    ("apps", (84, 607, 169, 692), 24, True), ("dns", (584, 607, 669, 692), 24, True),
    ("firewall", (84, 801, 169, 886), 24, True), ("proxy", (584, 801, 669, 886), 24, True),
    ("network", (84, 1128, 169, 1213), 24, True), ("general", (584, 1128, 669, 1213), 24, True),
    ("logs", (84, 1322, 169, 1407), 24, True), ("anti_censorship", (584, 1326, 669, 1411), 24, True),
    ("advanced", (84, 1523, 169, 1608), 24, True),
] + nav("settings")

LOGS = [
    ("logs", (125, 284, 190, 349), 20, True), ("allowed", (360, 284, 425, 349), 20, True),
    ("blocked", (596, 284, 661, 349), 20, True), ("filter", (796, 284, 861, 349), 20, True),
    ("app_filter", (943, 284, 1008, 349), 20, True), ("logs_dns", (821, 126, 886, 191), 20, True),
    ("logs_network", (478, 127, 545, 192), 20, False),
] + nav("settings")
VERDICTS = ["blocked", "allowed", "blocked", "allowed", "allowed", "allowed", "blocked", "allowed", "allowed"]

APPS = [("filter", (837, 304, 902, 369), 20, True), ("domain_rule", (969, 121, 1044, 196), 24, True)] + nav("settings")
for i, y in enumerate([576, 728, 880, 1032, 1275, 1518, 1670, 1822]):
    suffix = "_blocked" if i in (1, 3) else ""  # Camera and Clock are blocked in the capture
    APPS += [("wifi" + suffix, (684, y - 7, 751, y + 60), 20, True), ("mobile" + suffix, (815, y - 7, 882, y + 60), 20, True)]

DETAIL = [
    ("wifi_blocked", (78, 755, 153, 830), 24, True), ("mobile_blocked", (578, 755, 653, 830), 24, True),
    ("trusted", (100, 958, 172, 1028), 24, False), ("bypassed", (100, 1127, 172, 1197), 24, False),
    ("universal", (100, 1295, 172, 1365), 24, False), ("exclude", (100, 1463, 172, 1533), 24, False),
    ("ip_rule", (100, 1762, 172, 1832), 24, False), ("domain_rule", (100, 1931, 172, 2001), 24, False),
    ("mark_blocked", (66, 618, 112, 662), 16, False),
] + nav("settings")

DNS = [
    ("dns_system", (80, 1104, 145, 1168), 24, False), ("dns_other", (589, 1104, 654, 1168), 24, False),
    ("dns_rethink", (80, 1442, 145, 1506), 24, False), ("dns_smart", (589, 1442, 654, 1506), 24, False),
    ("dns_rethink", (100, 1985, 170, 2055), 24, False),
] + nav("settings")

HOME = [
    ("allowed", (100, 1673, 150, 1723), 18, False), ("blocked", (555, 1673, 605, 1723), 18, False),
    ("dns", (98, 2008, 152, 2062), 20, False), ("dns", (695, 520, 742, 567), 16, False),
    ("hero", (200, 615, 282, 702), 54, False),
] + nav("home")

SCREENS = {
    "08-settings": SETTINGS, "13-settings-dark": SETTINGS, "02-logs": LOGS, "03-apps": APPS, "12-apps-dark": APPS,
    "07-app-detail": DETAIL, "09-dns": DNS, "01-home": HOME, "11-home-dark": HOME,
}


# ---- proposals C / D: same detection, concept keys from cd.py; unchanged glyphs are left untouched
SETTINGS_CD = SETTINGS
LOGS_CD = [
    ("logs", (125, 284, 190, 349), 20, True), ("allowed", (360, 284, 425, 349), 20, True),
    ("blocked", (596, 284, 661, 349), 20, True), ("logs_network", (478, 127, 545, 192), 20, False),
] + nav("settings")
APPS_CD = [("rules", (969, 121, 1044, 196), 24, True)] + nav("settings")
DETAIL_CD = [
    ("trusted", (100, 958, 172, 1028), 24, False), ("bypassed", (100, 1127, 172, 1197), 24, False),
    ("bypass_universal", (100, 1295, 172, 1365), 24, False), ("exclude", (100, 1463, 172, 1533), 24, False),
    ("ip_rule", (100, 1762, 172, 1832), 24, False), ("domain_rule", (100, 1931, 172, 2001), 24, False),
    ("blocked", (66, 618, 112, 662), 18, False),
] + nav("settings")
DNS_CD = [
    ("dns_system", (80, 1104, 145, 1168), 24, False), ("dns", (589, 1104, 654, 1168), 24, False),
    ("dns_rethink", (80, 1442, 145, 1506), 24, False), ("dns_smart", (589, 1442, 654, 1506), 24, False),
    ("blocklists", (100, 1985, 170, 2055), 24, False),
] + nav("settings")
HOME_CD = [
    ("allowed", (100, 1673, 150, 1723), 20, False), ("blocked", (555, 1673, 605, 1723), 20, False),
    ("hero", (200, 615, 282, 702), 34, False),
] + nav("home")
SCREENS_CD = {
    "08-settings": SETTINGS_CD, "13-settings-dark": SETTINGS_CD, "02-logs": LOGS_CD, "03-apps": APPS_CD, "12-apps-dark": APPS_CD,
    "07-app-detail": DETAIL_CD, "09-dns": DNS_CD, "01-home": HOME_CD, "11-home-dark": HOME_CD,
}


def dist(a, b):
    return sum((x - y) ** 2 for x, y in zip(a, b)) ** 0.5


def detect(im, box):
    x0, y0, x1, y1 = box
    px = im.load()
    border = [px[x, y0] for x in range(x0, x1)] + [px[x, y1 - 1] for x in range(x0, x1)] + \
             [px[x0, y] for y in range(y0, y1)] + [px[x1 - 1, y] for y in range(y0, y1)]
    bg = Counter(border).most_common(1)[0][0]
    pts = []
    for y in range(y0, y1):
        for x in range(x0, x1):
            d = dist(px[x, y], bg)
            if d > 48:
                pts.append((d, x, y, px[x, y]))
    if not pts:
        return None
    xs = [p[1] for p in pts]
    ys = [p[2] for p in pts]
    pts.sort(reverse=True)
    top = pts[: max(1, len(pts) // 4)]
    tint = tuple(sorted(c[i] for *_, c in top)[len(top) // 2] for i in range(3))
    return {"bg": "#%02x%02x%02x" % bg, "tint": "#%02x%02x%02x" % tint, "bbox": [min(xs), min(ys), max(xs) + 1, max(ys) + 1]}


def main():
    run(SCREENS, "composite.json")
    run(SCREENS_CD, "composite-cd.json")


def run(screens, outname):
    out = {}
    for screen, targets in screens.items():
        im = Image.open(os.path.join(RAW, screen + ".png")).convert("RGB")
        items = []
        for key, box, size, centre_on_box in targets:
            r = detect(im, box)
            if r is None:
                print("nothing found", screen, key)
                continue
            b = r["bbox"]
            cx, cy = ((box[0] + box[2]) / 2, (box[1] + box[3]) / 2) if centre_on_box else ((b[0] + b[2]) / 2, (b[1] + b[3]) / 2)
            items.append({"key": key, "cx": cx, "cy": cy, "size": size * DP, "patch": [b[0] - 3, b[1] - 3, b[2] + 3, b[3] + 3], **r})
        if screen == "02-logs":
            for i, v in enumerate(VERDICTS):
                cy = 525 + 185 * i
                r = detect(im, (154, cy - 24, 199, cy + 24))
                items.append({"key": "badge", "verdict": v, "cx": 176.5, "cy": cy, "size": 46, "patch": [150, cy - 26, 203, cy + 26], **r})
        out[screen] = items
    with open(os.path.join(HERE, outname), "w") as fh:
        json.dump(out, fh, indent=1)
    print({k: len(v) for k, v in out.items()})


if __name__ == "__main__":
    main()
