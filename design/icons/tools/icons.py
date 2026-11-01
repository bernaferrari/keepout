"""Keepout icon sources.

Every icon is built from a few primitives (stroked lines, filled shapes) with boolean ops, then
flattened by skia-pathops into plain fill paths, so each SVG converts 1:1 into a Compose
ImageVector (no strokes, masks or clip paths left over).

Each SVG holds up to three paths, all fill="currentColor":
  id="ghost"  fill-opacity=0.38  secondary context (style B only)
  id="body"   the glyph
  id="bar"    the keep-out bar accent (can be tinted separately; same colour when mono)

Run:  uv run --with skia-pathops --with fonttools python design/icons/tools/icons.py
"""
import json
import math
import os
import pathops
from fontTools.svgLib.path import parse_path
from fontTools.pens.svgPathPen import SVGPathPen

HERE = os.path.dirname(os.path.abspath(__file__))
OUT = os.path.normpath(os.path.join(HERE, "..", "svg"))

SW = 2.0       # style A stroke (= Material Symbols Outlined, weight 400)
GAP = 1.25     # knock-out gap around the bar
GHOST = 0.38   # style B secondary opacity

# ---------------------------------------------------------------- geometry helpers

def P(d):
    p = pathops.Path()
    parse_path(d, p.getPen())
    return p


def simp(p):
    p.convertConicsToQuads()
    p.simplify()
    return p


def union(*ps):
    out = pathops.Path()
    for p in ps:
        if p is None:
            continue
        out = pathops.op(out, p, pathops.PathOp.UNION)
    return out


def diff(a, b):
    return pathops.op(a, b, pathops.PathOp.DIFFERENCE)


def inter(a, b):
    return pathops.op(a, b, pathops.PathOp.INTERSECTION)


def S(d, w=SW, cap="round"):
    """Stroke an SVG path into a filled outline."""
    p = P(d)
    p.stroke(w, pathops.LineCap.ROUND_CAP if cap == "round" else pathops.LineCap.BUTT_CAP,
             pathops.LineJoin.ROUND_JOIN, 4)
    return simp(p)


def F(d, grow=0.0):
    """Fill an SVG path; grow>0 inflates it with round corners (rounds a sharp polygon)."""
    p = simp(P(d))
    if grow:
        p = union(p, S(d, grow * 2))
    return p


def expand(p, by):
    q = pathops.Path()
    q.addPath(p)
    q.stroke(by * 2, pathops.LineCap.ROUND_CAP, pathops.LineJoin.ROUND_JOIN, 4)
    return union(p, simp(q))


def n(v):
    s = f"{v:.3f}".rstrip("0").rstrip(".")
    return "0" if s in ("-0", "") else s


def circ(cx, cy, r):
    return f"M{n(cx-r)} {n(cy)}A{n(r)} {n(r)} 0 1 1 {n(cx+r)} {n(cy)}A{n(r)} {n(r)} 0 1 1 {n(cx-r)} {n(cy)}Z"


def rr(x, y, w, h, r):
    r = min(r, w / 2, h / 2)
    return (f"M{n(x+r)} {n(y)}H{n(x+w-r)}A{n(r)} {n(r)} 0 0 1 {n(x+w)} {n(y+r)}V{n(y+h-r)}"
            f"A{n(r)} {n(r)} 0 0 1 {n(x+w-r)} {n(y+h)}H{n(x+r)}A{n(r)} {n(r)} 0 0 1 {n(x)} {n(y+h-r)}"
            f"V{n(y+r)}A{n(r)} {n(r)} 0 0 1 {n(x+r)} {n(y)}Z")


def pill(x, y, w, h):
    return rr(x, y, w, h, min(w, h) / 2)


def arc(cx, cy, r, a0, a1):
    """Open arc from angle a0 to a1 (degrees, 0 = +x, clockwise in screen space)."""
    x0, y0 = cx + r * math.cos(math.radians(a0)), cy + r * math.sin(math.radians(a0))
    x1, y1 = cx + r * math.cos(math.radians(a1)), cy + r * math.sin(math.radians(a1))
    large = 1 if (a1 - a0) % 360 > 180 else 0
    return f"M{n(x0)} {n(y0)}A{n(r)} {n(r)} 0 {large} 1 {n(x1)} {n(y1)}"


def seal(cx=12, cy=12, R=8.3, amp=0.55, lobes=8, phase=-90):
    """The Keepout seal: a softly scalloped circle (the launcher badge, simplified to 8 lobes)."""
    k = lobes * 6
    pts = []
    for i in range(k):
        t = math.radians(phase) + 2 * math.pi * i / k
        r = R + amp * math.cos(lobes * (t - math.radians(phase)))
        pts.append((cx + r * math.cos(t), cy + r * math.sin(t)))
    d = f"M{n(pts[0][0])} {n(pts[0][1])}"
    for i in range(k):  # Catmull-Rom -> cubic
        p0, p1, p2, p3 = pts[i - 1], pts[i], pts[(i + 1) % k], pts[(i + 2) % k]
        c1 = (p1[0] + (p2[0] - p0[0]) / 6, p1[1] + (p2[1] - p0[1]) / 6)
        c2 = (p2[0] - (p3[0] - p1[0]) / 6, p2[1] - (p3[1] - p1[1]) / 6)
        d += f"C{n(c1[0])} {n(c1[1])} {n(c2[0])} {n(c2[1])} {n(p2[0])} {n(p2[1])}"
    return d + "Z"


CHECK = "M8.1 12.3L10.75 14.9L15.9 9.5"
BAR = pill(7.5, 10.5, 9, 3)                 # the keep-out bar inside a 24dp seal/circle
SEAL_A = seal(R=8.2, amp=0.55)              # stroked: outer edge ~9.75
SEAL_FILL = seal(R=9.15, amp=0.6)           # solid: same outer edge
RING_A = circ(12, 12, 8)                    # stroked circle: outer edge 9.0
DISC = circ(12, 12, 9)

# ---------------------------------------------------------------- icon builder

class Icon:
    def __init__(self):
        self.ghost = pathops.Path()
        self.body = pathops.Path()
        self.bar = pathops.Path()

    def add(self, *ps):
        self.body = union(self.body, *ps)
        return self

    def g(self, *ps):
        self.ghost = union(self.ghost, *ps)
        return self

    def cut(self, *ps):
        for p in ps:
            self.body = diff(self.body, p)
            self.ghost = diff(self.ghost, p)
        return self

    def knock(self, p, gap=GAP, to="bar"):
        """Clear a gap around p in everything drawn so far, then draw p."""
        e = expand(p, gap)
        self.body = diff(self.body, e)
        self.ghost = diff(self.ghost, e)
        if to == "bar":
            self.bar = union(self.bar, p)
        else:
            self.body = union(self.body, p)
        return self

    def a(self, *ps):
        self.bar = union(self.bar, *ps)
        return self

    def clip_ghost(self):
        # ghost never sits under solid ink
        self.ghost = diff(self.ghost, union(self.body, self.bar))
        return self


def dstr(p):
    pen = SVGPathPen(None, ntos=n)
    p.draw(pen)
    return pen.getCommands()


def svg(icon):
    parts = []
    if icon.ghost.bounds != (0, 0, 0, 0) and list(icon.ghost.segments):
        parts.append(f'<path id="ghost" fill="currentColor" fill-opacity="{GHOST}" d="{dstr(icon.ghost)}"/>')
    if list(icon.body.segments):
        parts.append(f'<path id="body" fill="currentColor" d="{dstr(icon.body)}"/>')
    if list(icon.bar.segments):
        parts.append(f'<path id="bar" fill="currentColor" d="{dstr(icon.bar)}"/>')
    return ('<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" width="24" height="24">'
            + "".join(parts) + "</svg>\n")


# ================================================================ STYLE A — "Line & Bar"
# 1.75 line, round caps/joins. One filled pill — the keep-out bar — per glyph.
# Selected = solid silhouette with the bar knocked out.

A = {}


def a_wifi():
    return (S(arc(12, 19.25, 4.75, -135, -45)), S(arc(12, 19.25, 9, -135, -45)),
            S(arc(12, 19.25, 13.25, -135, -45)), F(circ(12, 19.25, 1.6)))


def a_mobile():
    return (S("M8.5 19.5V4.75"), S("M5 8.25L8.5 4.75L12 8.25"),
            S("M15.5 4.5V19.25"), S("M12 15.75L15.5 19.25L19 15.75"))


A["home"] = lambda: Icon().add(S(SEAL_A)).knock(F(BAR))
A["home_selected"] = lambda: Icon().add(F(SEAL_FILL)).cut(F(BAR))
A["stats"] = lambda: Icon().add(S("M6.25 15.75V11.5"), S("M12 15.75V4.25"), S("M17.75 15.75V8.25")).knock(F(pill(3, 17.5, 18, 3)))
A["stats_selected"] = lambda: Icon().add(F(pill(4.5, 9.5, 3.5, 7)), F(pill(10.25, 3, 3.5, 13.5)), F(pill(16, 6.25, 3.5, 10.25))).knock(F(pill(3, 17.5, 18, 3)), 1.25, to="body")
A["settings"] = lambda: Icon().add(S(circ(16.5, 7.75, 2.375)), S(circ(7.5, 16.25, 2.375)), S("M12 16.25H20.125")).knock(F(pill(3, 6.25, 9.5, 3)), 0)
A["settings_selected"] = lambda: Icon().add(F(circ(16.5, 7.75, 3.25)), F(circ(7.5, 16.25, 3.25)), S("M12 16.25H20.125", 2.5)).knock(F(pill(3, 6, 10, 3.5)), 0)

A["apps"] = lambda: (Icon().add(S(rr(3.875, 3.875, 6.5, 6.5, 2)), S(rr(13.625, 3.875, 6.5, 6.5, 2)),
                                S(rr(3.875, 13.625, 6.5, 6.5, 2))).knock(F(pill(12.75, 15.375, 8.25, 3)), 0))
A["dns"] = lambda: (Icon().add(S("M12 3.25V17.5"), S("M12 4.875H17.25L19.75 7.375L17.25 9.875H12"),
                               S("M12 11.875H6.75L4.25 14.375L6.75 16.875H12"))
                    .knock(F(pill(6.5, 18.75, 11, 3))))
A["firewall"] = lambda: (Icon().add(
    S(pill(3.875, 3.875, 7, 3.25)), S(pill(13.125, 3.875, 7, 3.25)),
    S(pill(3.875, 10.375, 2.75, 3.25)), S(pill(17.375, 10.375, 2.75, 3.25)),
    S(pill(3.875, 16.875, 7, 3.25)), S(pill(13.125, 16.875, 7, 3.25)))
    .a(F(pill(8.25, 9.5, 7.5, 5))))
A["proxy"] = lambda: (Icon().add(S(circ(5.25, 17, 2.125)), S(circ(18.75, 17, 2.125)),
                                 S("M6.6 15.2L9.4 11.25"), S("M17.4 15.2L14.6 11.25"))
                      .a(F(pill(7.75, 7.75, 8.5, 3.5))))
A["network"] = lambda: (Icon().add(S("M4.875 18.25V12.25A7.125 7.125 0 0 1 19.125 12.25V18.25"),
                                   S("M9.25 18.25V13A2.75 2.75 0 0 1 14.75 13V18.25"))
                        .knock(F(pill(2.5, 18.75, 19, 3))))
A["general"] = lambda: Icon().add(S(rr(2.875, 6.875, 18.25, 10.25, 5.125)), F(circ(16, 12, 3)))
A["logs"] = lambda: (Icon().add(F(circ(5, 5.75, 1.5)), S("M9 5.75H20.125"), F(circ(5, 12, 1.5)),
                                F(circ(5, 18.25, 1.5)), S("M9 18.25H15.5"))
                     .a(F(pill(8.25, 10.5, 12.75, 3))))
A["anti_censorship"] = lambda: (Icon().add(S("M3.875 12H19.5"), S("M16.25 8.5L19.75 12L16.25 15.5"))
                                .knock(F(pill(10.25, 2, 3.25, 6.75))).knock(F(pill(10.25, 15.25, 3.25, 6.75))))
A["advanced"] = lambda: (Icon().add(S("M8.75 3.875H15.25"), S("M10.25 3.875V9.25L5.2 18.1Q4.3 20.125 6.6 20.125H17.4Q19.7 20.125 18.8 18.1L13.75 9.25V3.875"))
                         .a(F(pill(8.25, 14, 7.5, 3))))

A["protected"] = A["home"]
A["paused"] = lambda: Icon().add(S(SEAL_A)).a(F(pill(8.5, 8, 2.75, 8)), F(pill(12.75, 8, 2.75, 8)))
A["off"] = lambda: Icon().add(S(SEAL_A), S(pill(8.375, 11, 7.25, 2), 1.5))
A["recovering"] = lambda: Icon().add(S(SEAL_A)).a(F(circ(8.25, 12, 1.5)), F(circ(12, 12, 1.5)), F(circ(15.75, 12, 1.5)))

A["allowed"] = lambda: Icon().add(S(RING_A), S(CHECK))
A["blocked"] = lambda: Icon().add(S(RING_A)).a(F(pill(7, 10.5, 10, 3)))
A["bypassed"] = lambda: (Icon().add(S(RING_A), S("M7.75 13.5V12.25Q7.75 8.25 12 8.25Q16.25 8.25 16.25 12.25V13.75"), S("M14.25 12L16.25 14L18.25 12"))
                         .knock(F(pill(5.25, 15.25, 7.5, 2.75)), 1.0))
A["trusted"] = lambda: Icon().add(S(SEAL_A), S(CHECK))
A["partial"] = lambda: Icon().add(S(RING_A)).a(F("M12 6.5A5.5 5.5 0 0 0 12 17.5Z"))

A["wifi"] = lambda: Icon().add(*a_wifi())
A["wifi_blocked"] = lambda: Icon().add(*a_wifi()).knock(F(pill(3, 13.25, 18, 3)))
A["mobile"] = lambda: Icon().add(*a_mobile())
A["mobile_blocked"] = lambda: Icon().add(*a_mobile()).knock(F(pill(3, 10.5, 18, 3)))

A["logs_network"] = lambda: Icon().add(S("M3.875 8H18.5"), S("M15 4.5L18.5 8L15 11.5"), S("M20.125 16H5.5"), S("M9 12.5L5.5 16L9 19.5"))
A["logs_dns"] = A["dns"]
A["filter"] = lambda: Icon().add(S("M5.75 9.75L10.25 14.25V20.125L13.75 18.375V14.25L18.25 9.75")).knock(F(pill(3, 4, 18, 3)), 0)
A["app_filter"] = lambda: (Icon().add(S(rr(3.875, 3.875, 10.25, 10.25, 3)))
                           .knock(F("M13.75 14.25H21L18.25 17.75V21.25L16.5 20.25V17.75Z", 0.6), 1.25))

A["dns_system"] = lambda: Icon().add(S(rr(6.375, 2.875, 11.25, 18.25, 3))).a(F(pill(9.75, 16.25, 4.5, 2)))
A["dns_other"] = lambda: (Icon().add(S(rr(3.875, 4.125, 16.25, 6.25, 2.5)), S(rr(3.875, 13.625, 16.25, 6.25, 2.5)),
                                     F(circ(7.625, 7.25, 1.25)), F(circ(7.625, 16.75, 1.25)))
                          .a(F(pill(10.5, 15.5, 6.25, 2.5))))
A["dns_rethink"] = lambda: (Icon().add(S(rr(4.875, 2.875, 14.25, 18.25, 3)), S("M8.75 7.5H15.25"), S("M8.75 16.5H12.5"))
                            .a(F(pill(8, 10.5, 8, 3))))
A["dns_smart"] = lambda: (Icon().add(S(arc(12, 12.5, 7.5, -62, 2)), S(arc(12, 12.5, 7.5, 58, 122)), S(arc(12, 12.5, 7.5, 178, 242)),
                                     S(circ(18.5, 16.25, 1.875)), S(circ(5.5, 16.25, 1.875)))
                          .a(F(circ(12, 5, 2.75))))

TAG = "M3.875 12L8.4 6.55Q9.1 5.875 10 5.875H17.5Q20.125 5.875 20.125 8.5V15.5Q20.125 18.125 17.5 18.125H10Q9.1 18.125 8.4 17.45Z"
A["domain_rule"] = lambda: Icon().add(S(TAG)).a(F(pill(11, 10.5, 6, 3)))
A["ip_rule"] = lambda: Icon().add(S(rr(2.875, 6.875, 18.25, 10.25, 3))).a(*[F(circ(x, 12, 1.25)) for x in (7, 10.33, 13.67, 17)])
A["universal"] = lambda: (Icon().add(S(RING_A), S("M12 3.875C8.6 6.6 8.6 17.4 12 20.125"), S("M12 3.875C15.4 6.6 15.4 17.4 12 20.125"))
                          .knock(F(pill(2, 10.5, 20, 3))))
A["exclude"] = lambda: (Icon().add(S(seal(10, 12, 6.4, 0.45))).knock(S("M10.5 12H20.25"), 1.0, to="body")
                        .add(S("M17 8.75L20.25 12L17 15.25")))
A["lockdown"] = lambda: (Icon().add(S("M8.25 10.875V8.25A3.75 3.75 0 0 1 15.75 8.25V10.875"), S(rr(4.875, 10.875, 14.25, 9.25, 2.75)))
                         .a(F(pill(8.5, 14, 7, 3))))
A["temp_allow"] = lambda: (Icon().add(S("M6 3.875H18"), S("M6 20.125H18"),
                                      S("M7.75 3.875V6.5Q7.75 9.25 12 12Q16.25 14.75 16.25 17.5V20.125"),
                                      S("M16.25 3.875V6.5Q16.25 9.25 12 12Q7.75 14.75 7.75 17.5V20.125"))
                           .a(F(pill(9.75, 16.5, 4.5, 2.25))))
A["empty_logs"] = lambda: (Icon().add(S("M3.875 13L6 5.6Q6.5 3.875 8.25 3.875H15.75Q17.5 3.875 18 5.6L20.125 13V17.5Q20.125 20.125 17.5 20.125H6.5Q3.875 20.125 3.875 17.5Z"),
                                      S("M3.875 13H8.25L9.75 15.75H14.25L15.75 13H20.125"))
                           .a(F(pill(8.5, 7.5, 7, 2.5))))
A["empty_rules"] = lambda: Icon().add(S(TAG), S("M14.25 9V15"), S("M11.25 12H17.25"))
A["empty_apps"] = lambda: (Icon().add(S(rr(3.875, 3.875, 9.5, 9.5, 2.75)), S(circ(15.5, 15.5, 3.75)), S("M18.25 18.25L20.5 20.5"))
                           .knock(F(pill(5.5, 7.375, 6.25, 2.5)), 0))


# ================================================================ STYLE B — "Cutout"
# Solid silhouettes; detail is knocked out, never drawn. Secondary context is a 38% ghost layer.
# The bar is always either cut out of solid ink or solid on top of ghost.
# Weight = Material Symbols Rounded Filled 400. Nav: unselected = 2dp outline + solid bar (M3 rule),
# selected = solid silhouette with the bar knocked out.

B = {}
SEAL_B = seal(R=9.2, amp=0.7)
DISC_B = circ(12, 12, 9.25)
BAR_B = pill(7, 10.25, 10, 3.5)


def b_wifi():
    wedge = F("M12 19.75L2.9 10.65A12.9 12.9 0 0 1 21.1 10.65Z", 1.25)
    return diff(wedge, union(S(arc(12, 19.75, 5.5, -150, -30), 1.5), S(arc(12, 19.75, 10, -150, -30), 1.5)))


def b_mobile():
    up = union(S("M8.25 20.25V8", 2.75), F("M8.25 3.25L12.5 8.75H4Z", 0.6))
    down = union(S("M15.75 3.75V16", 2.75), F("M15.75 20.75L20 15.25H11.5Z", 0.6))
    return up, down


B["home"] = lambda: Icon().add(S(seal(R=8.2, amp=0.6), 2.0)).knock(F(pill(7.5, 10.5, 9, 3)), 0)
B["home_selected"] = lambda: Icon().add(F(SEAL_B)).cut(F(BAR_B))
B["stats"] = lambda: Icon().add(S(arc(12, 12, 7.75, 58, 232), 2.0)).knock(S(arc(12, 12, 7.75, -90, 20), 3.5), 1.1)
B["stats_selected"] = lambda: Icon().add(S(arc(12, 12, 6.75, 52, 238), 4.5, "butt")).knock(S(arc(12, 12, 6.75, -90, 20), 4.5, "butt"), 1.1, to="body")
B["settings"] = lambda: (Icon().add(S(pill(3.5, 5.25, 17, 5), 2.0), S(pill(3.5, 13.75, 17, 5), 2.0))
                         .knock(F(circ(16, 7.75, 3)), 0.9).knock(F(circ(8, 16.25, 3)), 0.9))
B["settings_selected"] = lambda: (Icon().add(F(pill(2.5, 4.25, 19, 7)), F(pill(2.5, 12.75, 19, 7)))
                                  .knock(F(circ(16, 7.75, 1.75)), 1.1, to="body").knock(F(circ(8, 16.25, 1.75)), 1.1, to="body"))

B["apps"] = lambda: (Icon().add(F(rr(3, 3, 8, 8, 2.75)), F(rr(13, 3, 8, 8, 2.75)), F(rr(3, 13, 8, 8, 2.75)))
                     .g(F(rr(13, 13, 8, 8, 2.75))).knock(F(pill(12, 15.25, 10, 3.5)), 1.0).clip_ghost())
B["dns"] = lambda: (Icon().add(F(rr(10.9, 2.5, 2.2, 16.5, 1.1)))
                    .knock(F("M13.75 4.5H17.5L19.75 7L17.5 9.5H13.75Z", 0.9), 0.9, to="body")
                    .knock(F("M10.25 11.75H6.5L4.25 14.25L6.5 16.75H10.25Z", 0.9), 0.9, to="body")
                    .knock(F(pill(6, 19, 12, 3)), 0.9))
B["firewall"] = lambda: (Icon().add(F(pill(3, 3, 8.5, 5)), F(pill(12.5, 3, 8.5, 5)), F(pill(3, 9.5, 3.5, 5)), F(pill(17.5, 9.5, 3.5, 5)),
                                    F(pill(3, 16, 8.5, 5)), F(pill(12.5, 16, 8.5, 5)))
                         .a(F(pill(7.5, 9.5, 9, 5))))
B["proxy"] = lambda: (Icon().add(F(circ(5, 17.25, 2.75)), F(circ(19, 17.25, 2.75)), S("M6.4 14.9L9.75 10.25", 2.5), S("M17.6 14.9L14.25 10.25", 2.5))
                      .cut(F(circ(5, 17.25, 1.1)), F(circ(19, 17.25, 1.1)))
                      .knock(F(pill(7, 6.25, 10, 4.25)), 0.9))
B["network"] = lambda: (Icon().add(F("M3 18V12A9 9 0 0 1 21 12V18Z")).cut(F("M8.75 19V13.25A3.25 3.25 0 0 1 15.25 13.25V19Z"))
                        .knock(F(pill(2, 19.25, 20, 3)), 1.0))
B["general"] = lambda: Icon().add(F(pill(2, 6, 20, 12))).knock(F(circ(16, 12, 3)), 1.25, to="body")
B["logs"] = lambda: (Icon().add(F(rr(3, 2.5, 18, 19, 4.5)))
                     .cut(F(circ(7.5, 7.5, 1.4)), F(pill(10.25, 6.25, 7.25, 2.5)),
                          F(circ(7.5, 12, 1.4)), F(pill(10.25, 10.75, 7.25, 2.5)),
                          F(circ(7.5, 16.5, 1.4)), F(pill(10.25, 15.25, 4.5, 2.5))))
B["anti_censorship"] = lambda: (Icon().g(F(pill(9.75, 1.5, 4.5, 8.25)), F(pill(9.75, 14.25, 4.5, 8.25)))
                                .knock(union(S("M3.25 12H15.5", 2.75), F("M21 12L15.25 7.25V16.75Z", 0.6)), 1.1, to="body").clip_ghost())


def b_flask():
    return F("M9.75 2.75H14.25V9L19.6 18.3Q20.6 21 17.8 21H6.2Q3.4 21 4.4 18.3L9.75 9Z", 0.4)


B["advanced"] = lambda: (Icon().g(diff(b_flask(), F("M0 13.75H24V24H0Z")))
                         .add(inter(b_flask(), F("M0 15H24V24H0Z"))).clip_ghost())

B["protected"] = B["home_selected"]
B["paused"] = lambda: Icon().add(F(SEAL_B)).cut(F(pill(8.25, 7.5, 3, 9)), F(pill(12.75, 7.5, 3, 9)))
B["off"] = lambda: Icon().g(F(SEAL_B)).cut(F(BAR_B))
B["recovering"] = lambda: Icon().add(F(SEAL_B)).cut(F(circ(7.9, 12, 1.65)), F(circ(12, 12, 1.65)), F(circ(16.1, 12, 1.65)))

B["allowed"] = lambda: Icon().add(F(DISC_B)).cut(S(CHECK, 2.25))
B["blocked"] = lambda: Icon().add(F(DISC_B)).cut(F(BAR_B))
B["bypassed"] = lambda: (Icon().add(F(DISC_B)).cut(S("M8.25 13C8.25 8 15.75 8 15.75 13", 2), S("M13.5 11.4L15.75 13.6L18 11.4", 2), F(pill(7, 15.25, 10, 2.5))))
B["trusted"] = lambda: Icon().add(F(SEAL_B)).cut(S(CHECK, 2.25))
B["partial"] = lambda: Icon().g(F(DISC_B)).add(F("M12 2.75A9.25 9.25 0 0 0 12 21.25Z")).cut(F("M11.1 2H12.9V22H11.1Z"))

B["wifi"] = lambda: Icon().add(b_wifi())
B["wifi_blocked"] = lambda: Icon().add(b_wifi()).knock(F(pill(2.5, 13, 19, 3.5)), 1.1)
B["mobile"] = lambda: Icon().add(*b_mobile())
B["mobile_blocked"] = lambda: Icon().add(*b_mobile()).knock(F(pill(2.5, 10.25, 19, 3.5)), 1.1)

B["logs_network"] = lambda: (Icon().add(S("M3.5 7.75H15", 2.75), F("M20.75 7.75L15.25 3.5V12Z", 0.6))
                             .add(S("M20.5 16.25H9", 2.75), F("M3.25 16.25L8.75 12V20.5Z", 0.6)))
B["logs_dns"] = B["dns"]
B["filter"] = lambda: Icon().add(F("M5.5 9.5H18.5L14.25 14V19.5L9.75 21V14Z", 0.6)).knock(F(pill(3, 3, 18, 3.75)), 1.0)
B["app_filter"] = lambda: (Icon().add(F(rr(2.5, 2.5, 13, 13, 3.75)))
                           .knock(F("M12.75 12.75H22L18.6 16.75V21.25L16.15 20V16.75Z", 0.6), 1.1, to="body"))

B["dns_system"] = lambda: Icon().add(F(rr(5.75, 2, 12.5, 20, 3.5))).cut(F(pill(9.75, 16.75, 4.5, 2)))
B["dns_other"] = lambda: (Icon().add(F(rr(3, 3.5, 18, 7.5, 3)), F(rr(3, 13, 18, 7.5, 3)))
                          .cut(F(circ(7.25, 7.25, 1.4)), F(circ(7.25, 16.75, 1.4)), F(pill(10.5, 15.5, 7, 2.5))))
B["dns_rethink"] = lambda: (Icon().add(F(rr(4, 2, 16, 20, 3.75)))
                            .cut(F(pill(7.75, 6, 8.5, 2)), F(pill(7.25, 10.25, 9.5, 3.5)), F(pill(7.75, 15.75, 5, 2))))
B["dns_smart"] = lambda: (Icon().g(S(circ(12, 12.5, 7.25), 2.25))
                          .knock(F(circ(18.5, 16.25, 2.25)), 1.0, to="body").knock(F(circ(5.5, 16.25, 2.25)), 1.0, to="body")
                          .knock(F(circ(12, 4.75, 3.25)), 1.0).clip_ghost())

TAG_B = "M3 12L8.1 5.9Q8.9 5 10 5H18Q21 5 21 8V16Q21 19 18 19H10Q8.9 19 8.1 18.1Z"
B["domain_rule"] = lambda: Icon().add(F(TAG_B)).cut(F(circ(8.75, 12, 1.4)), F(pill(11.5, 10.5, 6.5, 3)))
B["ip_rule"] = lambda: Icon().add(F(rr(2, 6, 20, 12, 3.5))).cut(*[F(circ(x, 12, 1.4)) for x in (6.75, 10.25, 13.75, 17.25)])
B["universal"] = lambda: (Icon().add(F(DISC_B)).cut(S("M12 2.5C8 6 8 18 12 21.5", 1.5), S("M12 2.5C16 6 16 18 12 21.5", 1.5))
                          .knock(F(pill(1.5, 10.25, 21, 3.5)), 1.0))
B["exclude"] = lambda: (Icon().g(F(seal(10, 12, 7.3, 0.55)))
                        .knock(union(S("M10.5 12H18", 2.75), F("M22 12L16.5 7.25V16.75Z", 0.6)), 1.1, to="body").clip_ghost())
B["lockdown"] = lambda: (Icon().add(S("M8.25 11V8.25A3.75 3.75 0 0 1 15.75 8.25V11", 2.5), F(rr(4, 10, 16, 11.5, 3.25)))
                         .cut(F(pill(8, 14.25, 8, 3))))
B["temp_allow"] = lambda: (Icon().add(F(pill(5, 2.5, 14, 3)), F(pill(5, 18.5, 14, 3)))
                           .g(F("M7.5 5.5H16.5V7Q16.5 9.6 12.6 12Q16.5 14.4 16.5 17V18.5H7.5V17Q7.5 14.4 11.4 12Q7.5 9.6 7.5 7Z"))
                           .add(F("M8.75 18.5Q8.75 15.5 12 14.25Q15.25 15.5 15.25 18.5Z")).clip_ghost())
B["empty_logs"] = lambda: (Icon().g(F("M3 13L5.3 5.2Q5.9 3 8.2 3H15.8Q18.1 3 18.7 5.2L21 13V17.75Q21 21 17.75 21H6.25Q3 21 3 17.75Z"))
                           .cut(F("M3 12H8.25L9.75 14.75H14.25L15.75 12H21V24H3Z"))
                           .add(F("M3 13.25H7.6L9.1 16H14.9L16.4 13.25H21V17.75Q21 21 17.75 21H6.25Q3 21 3 17.75Z"))
                           .knock(F(pill(8.25, 6.75, 7.5, 3)), 0).clip_ghost())
B["empty_rules"] = lambda: Icon().g(F(TAG_B)).knock(union(S("M14.25 8.5V15.5", 2.75), S("M10.75 12H17.75", 2.75)), 0, to="body").clip_ghost()
B["empty_apps"] = lambda: (Icon().g(F(rr(3, 3, 11, 11, 3))).knock(S(circ(15.5, 15.5, 3.75), 2.5), 1.0, to="body")
                           .add(S("M18.5 18.5L20.75 20.75", 2.75)).clip_ghost())

# ---- B: Keepout-specific replacements found by the full ui/icons review
B["critical"] = lambda: Icon().add(F(SEAL_B)).cut(F(pill(10.75, 6.5, 2.5, 7.5)), F(circ(12, 16.6, 1.5)))
B["database"] = lambda: (Icon().add(F("M4 6.25Q4 3 12 3Q20 3 20 6.25V17.75Q20 21 12 21Q4 21 4 17.75Z"))
                         .cut(S("M3 10.25Q12 13.75 21 10.25", 1.5), S("M3 14.75Q12 18.25 21 14.75", 1.5)))
B["pulse"] = lambda: Icon().add(S("M2.75 12.5H6.5L9.25 6L13.75 18.5L16.25 12.5H18"), F(circ(19.75, 12.5, 2)))
B["http_insecure"] = lambda: (Icon().add(S("M8 10.5V7.5A4 4 0 0 1 15.6 5.75", 2.5), F(rr(4, 10, 16, 11.5, 3.25)))
                              .cut(F(circ(12, 14.75, 1.75)), F(pill(11.1, 14.75, 1.8, 3.75))))
B["metered"] = lambda: (Icon().g(S(arc(12, 15, 8, 180, 360), 3.5, "butt"))
                        .knock(S(arc(12, 15, 8, 180, 250), 3.5, "butt"), 0, to="body")
                        .knock(union(S("M12 15L16.25 9.5", 2.25), F(circ(12, 15, 2.5))), 1.0, to="body").clip_ghost())
B["key"] = lambda: (Icon().add(F(circ(7.25, 12, 5)), S("M11 12H20.75", 3), S("M17.75 12V16", 2.5), S("M20.75 12V15", 2.5))
                    .cut(F(circ(7.25, 12, 1.9))))
B["bubble"] = lambda: (Icon().add(F(circ(12.5, 11, 8.5)), F("M3.5 21.25L4.75 15.25L9.25 18.75Z", 0.6))
                       .cut(F(pill(8, 9.25, 9, 3.5))))
B["events"] = lambda: (Icon().g(F(rr(5.9, 3, 2.2, 18, 1.1)))
                       .knock(F(circ(7, 5.75, 1.9)), 0.7, to="body").knock(F(circ(7, 12, 1.9)), 0.7, to="body")
                       .knock(F(circ(7, 18.25, 1.9)), 0.7, to="body")
                       .add(F(pill(11.5, 4.5, 9.5, 2.5)), F(pill(11.5, 10.75, 6.5, 2.5)), F(pill(11.5, 17, 8, 2.5))).clip_ghost())
B["terminal"] = lambda: Icon().add(F(rr(2.5, 4, 19, 16, 3.5))).cut(S("M6.75 9L9.75 12L6.75 15", 2), F(pill(11.5, 14, 6, 2.25)))
B["plus"] = lambda: Icon().add(F(SEAL_B)).cut(F(pill(10.75, 7, 2.5, 10)), F(pill(7, 10.75, 10, 2.5)))
B["globe"] = lambda: (Icon().add(F(DISC_B)).cut(S("M12 2.5C8 6 8 18 12 21.5", 1.5), S("M12 2.5C16 6 16 18 12 21.5", 1.5),
                                               S("M2.5 12H21.5", 1.5)))
B["device"] = B["dns_system"]
B["rules"] = B["domain_rule"]

# Badge marks: the bare glyph for 12-16dp verdict badges (the badge disc/seal is the container).
A["mark_allowed"] = lambda: Icon().add(S("M5.75 12.5L9.75 16.5L18.25 7.75", 3))
A["mark_blocked"] = lambda: Icon().a(F(pill(4.5, 10.25, 15, 3.5)))
A["mark_bypassed"] = lambda: Icon().add(S("M5 17V12.5Q5 6.5 11.5 6.5Q18 6.5 18 12.5V16", 2.75), S("M14.5 13.5L18 17L21.5 13.5", 2.75))
A["mark_trusted"] = A["mark_allowed"]
B["mark_allowed"] = lambda: Icon().add(S("M5.5 12.5L9.75 16.75L18.5 7.5", 3.5))
B["mark_blocked"] = lambda: Icon().a(F(pill(4, 10, 16, 4)))
B["mark_bypassed"] = lambda: Icon().add(S("M5 17V12.5Q5 6.5 11.5 6.5Q18 6.5 18 12.5V16", 3.25), F("M13.75 14H22.25L18 18.75Z", 0.75))
B["mark_trusted"] = B["mark_allowed"]

STYLES = {"a": A, "b": B}

ORDER = [
    ("Navigation", ["home", "home_selected", "stats", "stats_selected", "settings", "settings_selected"]),
    ("Settings hub", ["apps", "dns", "firewall", "proxy", "network", "general", "logs", "anti_censorship", "advanced"]),
    ("Protection", ["protected", "paused", "off", "recovering"]),
    ("Verdicts", ["allowed", "blocked", "bypassed", "trusted", "partial"]),
    ("Networks", ["wifi", "wifi_blocked", "mobile", "mobile_blocked"]),
    ("Logs & filters", ["logs_network", "logs_dns", "filter", "app_filter"]),
    ("DNS types", ["dns_system", "dns_other", "dns_rethink", "dns_smart"]),
    ("Rules", ["domain_rule", "ip_rule", "universal", "exclude", "lockdown", "temp_allow"]),
    ("Empty states", ["empty_logs", "empty_rules", "empty_apps"]),
    ("Review extras (style B)", ["critical", "database", "pulse", "http_insecure", "metered", "key", "bubble", "events", "terminal", "plus", "globe", "device"]),
    ("Badge marks (12-16dp)", ["mark_allowed", "mark_blocked", "mark_bypassed", "mark_trusted"]),
]

# ---------------------------------------------------------------- Kotlin ImageVector emitter

class KtPen:
    """Records a flattened path as Compose PathBuilder calls."""
    def __init__(self):
        self.lines = []

    def f(self, v):
        return n(v) + "f"

    def moveTo(self, p):
        self.lines.append(f"moveTo({self.f(p[0])}, {self.f(p[1])})")

    def lineTo(self, p):
        self.lines.append(f"lineTo({self.f(p[0])}, {self.f(p[1])})")

    def qCurveTo(self, *pts):
        from fontTools.pens.basePen import decomposeQuadraticSegment
        for c, e in decomposeQuadraticSegment(pts):
            self.lines.append(f"quadTo({self.f(c[0])}, {self.f(c[1])}, {self.f(e[0])}, {self.f(e[1])})")

    def curveTo(self, c1, c2, e):
        self.lines.append(f"curveTo({self.f(c1[0])}, {self.f(c1[1])}, {self.f(c2[0])}, {self.f(c2[1])}, {self.f(e[0])}, {self.f(e[1])})")

    def closePath(self):
        self.lines.append("close()")

    endPath = closePath


def d_to_kt(d, pen):
    import re
    toks = re.findall(r"[MLHVQCZmlhvqcz]|-?[0-9]*\.?[0-9]+(?:e-?[0-9]+)?", d)
    i, cmd, cur, start = 0, None, (0.0, 0.0), (0.0, 0.0)
    def num():
        nonlocal i
        v = float(toks[i]); i += 1
        return v
    while i < len(toks):
        if toks[i].isalpha():
            cmd = toks[i]; i += 1
            if cmd in "Zz":
                pen.closePath(); cur = start
                continue
        if cmd == "M":
            cur = (num(), num()); start = cur; pen.moveTo(cur); cmd = "L"
        elif cmd == "L":
            cur = (num(), num()); pen.lineTo(cur)
        elif cmd == "H":
            cur = (num(), cur[1]); pen.lineTo(cur)
        elif cmd == "V":
            cur = (cur[0], num()); pen.lineTo(cur)
        elif cmd == "Q":
            c = (num(), num()); cur = (num(), num())
            pen.lines.append(f"quadTo({pen.f(c[0])}, {pen.f(c[1])}, {pen.f(cur[0])}, {pen.f(cur[1])})")
        elif cmd == "C":
            c1 = (num(), num()); c2 = (num(), num()); cur = (num(), num()); pen.curveTo(c1, c2, cur)
        else:
            raise ValueError(cmd)


def camel(name):
    return "".join(w.capitalize() for w in name.split("_"))


def kotlin(icon, name, style):
    prop = camel(name)
    backing = f"_{style}{prop}"
    blocks = []
    for layer, alpha in ((icon.ghost, GHOST), (icon.body, 1.0), (icon.bar, 1.0)):
        if not list(layer.segments):
            continue
        pen = KtPen()
        d_to_kt(dstr(layer), pen)
        body = "\n".join("        " + l for l in pen.lines)
        alpha_arg = f"fillAlpha = {n(alpha)}f, " if alpha != 1.0 else ""
        blocks.append(f"      path(fill = SolidColor(Color.Black), {alpha_arg}pathFillType = PathFillType.NonZero) {{\n{body}\n      }}")
    return f"""package com.bernaferrari.bravedns.ui.icons

// Generated from design/icons/svg/{style}/{name}.svg by design/icons/tools/icons.py. Do not edit by hand.

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

val KeepoutIcons.{prop}: ImageVector
  get() {{
    {backing}?.let {{ return it }}
    return ImageVector.Builder(
        name = "keepout.{name}",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f,
      )
      .apply {{
{chr(10).join(blocks)}
      }}
      .build()
      .also {{ {backing} = it }}
  }}

private var {backing}: ImageVector? = null
"""


if __name__ == "__main__":
    manifest = {"order": ORDER, "styles": {}}
    for key, table in STYLES.items():
        os.makedirs(os.path.join(OUT, key), exist_ok=True)
        manifest["styles"][key] = {}
        for _, names in ORDER:
            for name in names:
                if name not in table:
                    continue
                s = svg(table[name]())
                with open(os.path.join(OUT, key, name + ".svg"), "w") as fh:
                    fh.write(s)
                manifest["styles"][key][name] = s
    # Kotlin preview of the recommended style (B) — what would land in shared/.../ui/icons/
    kdir = os.path.normpath(os.path.join(HERE, "..", "kotlin"))
    os.makedirs(kdir, exist_ok=True)
    with open(os.path.join(kdir, "KeepoutIcons.kt"), "w") as fh:
        fh.write("package com.bernaferrari.bravedns.ui.icons\n\n/** Keepout's own glyphs (style B, Cutout). Generic UI glyphs stay in [MaterialSymbols]. */\nobject KeepoutIcons\n")
    for _, names in ORDER:
        for name in names:
            if name.startswith("mark_") and name.endswith("trusted"):
                continue
            with open(os.path.join(kdir, f"Keepout{camel(name)}Icon.kt"), "w") as fh:
                fh.write(kotlin(B[name](), name, "b"))
    with open(os.path.join(HERE, "manifest.json"), "w") as fh:
        json.dump(manifest, fh)
    print("wrote", sum(len(v) for v in manifest["styles"].values()), "icons")
