"""Writes the hand-made glyphs into the app as Compose ImageVectors, in the same lazily-cached
format scripts/generate-material-symbols.sh produces:

  shared/src/commonMain/kotlin/com/bernaferrari/bravedns/ui/icons/keepout/
    KeepoutIcons.kt  (facade)  + Keepout<Name>Icon.kt   — proposal D custom glyphs
    BrandIcons.kt    (facade)  + Brand<Name>Icon.kt     — About-screen brand marks, flattened from the
                                                          old app/src/main/res/drawable XML vectors

uv run --with skia-pathops --with fonttools python design/icons/tools/emit_repo.py
"""
import os
import re
import sys
import xml.etree.ElementTree as ET

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import icons as K  # noqa: E402
import cd  # noqa: E402
import symbols  # noqa: E402

ROOT = os.path.normpath(os.path.join(K.HERE, "../../.."))
OUT = os.path.join(ROOT, "shared/src/commonMain/kotlin/com/bernaferrari/bravedns/ui/icons/keepout")
PKG = "com.bernaferrari.bravedns.ui.icons.keepout"
ANDROID = "{http://schemas.android.com/apk/res/android}"


def blocklists_d():
    """A list whose last entry is blocked: list lines + Material do_not_disturb_on as a 10dp badge."""
    lines = K.union(K.S("M4 5.5H20", 2), K.S("M4 10.5H20", 2), K.S("M4 15.5H12", 2), K.S("M4 20.5H11", 2))
    badge = cd.sym("do_not_disturb_on").transform(0.42, 0, 0, 0.42, 13.4, 12.9)
    return K.Icon().add(lines).knock(badge, 1.0, to="body")


def firewall_off_d():
    """Firewall off for this app: the wall (centre brick = keep-out bar) with Material's off-slash."""
    ic = cd.wall_bar()
    slash = "M3.6 2.6L20.6 21.4"
    ic.body = K.diff(ic.body, K.S(slash, 2 + 2 * 1.5))
    ic.bar = K.diff(ic.bar, K.S(slash, 2 + 2 * 1.5))
    return ic.add(K.S(slash, 2))


def flat(icon):
    p = K.union(icon.ghost, icon.body, icon.bar)
    return p


def xml_vector(path):
    """Flatten an Android vector drawable the way Icon() renders it: visible fills ∪ strokes, scaled to 24."""
    root = ET.parse(path).getroot()
    vw = float(root.get(ANDROID + "viewportWidth"))
    out = K.pathops.Path()
    for el in root.iter("path"):
        d = el.get(ANDROID + "pathData")
        fill = el.get(ANDROID + "fillColor") or ""
        stroke = el.get(ANDROID + "strokeColor")
        width = float(el.get(ANDROID + "strokeWidth") or 0)
        if fill and fill not in ("?attr/svgFillColor", "#00000000"):
            p = K.P(d)
            if (el.get(ANDROID + "fillType") or "") == "evenOdd":
                p.fillType = K.pathops.FillType.EVEN_ODD
            out = K.union(out, K.simp(p))
        if stroke and width:
            p = K.P(d)
            cap = {"round": K.pathops.LineCap.ROUND_CAP, "square": K.pathops.LineCap.SQUARE_CAP}.get(el.get(ANDROID + "strokeLineCap"), K.pathops.LineCap.BUTT_CAP)
            join = {"round": K.pathops.LineJoin.ROUND_JOIN, "bevel": K.pathops.LineJoin.BEVEL_JOIN}.get(el.get(ANDROID + "strokeLineJoin"), K.pathops.LineJoin.MITER_JOIN)
            p.stroke(width, cap, join, 4)
            out = K.union(out, K.simp(p))
    s = 24.0 / vw
    alpha = float(root.get(ANDROID + "alpha") or 1)
    return out.transform(s, 0, 0, s, 0, 0), alpha


def kt_body(path):
    pen = K.KtPen()
    K.d_to_kt(K.dstr(path), pen)
    return "\n".join("            " + line for line in pen.lines)


def kt_file(prop, name, path, alpha, doc):
    return f"""package {PKG}

// {doc}
// 24dp viewport, fill-only paths (strokes flattened); tint at use, like MaterialSymbols.

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

@Suppress("CheckReturnValue")
internal val {prop}: ImageVector
  get() {{
    if (_{prop} != null) {{
      return _{prop}!!
    }}
    _{prop} =
      ImageVector.Builder(
          name = "{name}",
          defaultWidth = 24.dp,
          defaultHeight = 24.dp,
          viewportWidth = 24f,
          viewportHeight = 24f,
        )
        .apply {{
          path(
            fill = SolidColor(Color.Black),
            fillAlpha = {K.n(alpha)}f,
            stroke = null,
            strokeAlpha = 1f,
            strokeLineWidth = 1f,
            strokeLineCap = StrokeCap.Butt,
            strokeLineJoin = StrokeJoin.Bevel,
            strokeLineMiter = 1f,
            pathFillType = PathFillType.Companion.NonZero,
          ) {{
{kt_body(path)}
          }}
        }}
        .build()
    return _{prop}!!
  }}

internal var _{prop}: ImageVector? = null
"""


KEEPOUT = [
    ("Firewall", "firewall", lambda: flat(cd.wall_bar()), "Firewall: a brick wall whose centre brick is Keepout's keep-out bar."),
    ("FirewallOff", "firewall_off", lambda: flat(firewall_off_d()), "Firewall off for an app (bypass DNS & firewall): the wall with Material's off-slash."),
    ("Blocklists", "blocklists", lambda: flat(blocklists_d()), "On-device blocklists: list lines with the blocked sign (do_not_disturb_on) as a badge."),
]
BRAND = [("Telegram", "ic_telegram"), ("Github", "ic_github"), ("Twitter", "ic_twitter"), ("Reddit", "ic_reddit"),
         ("Element", "ic_element"), ("Mastodon", "ic_mastodon")]


def facade(obj, items, prefix, doc):
    vals = "\n".join(f"    val {p}: ImageVector get() = {prefix}{p}" for p in items)
    return f"""package {PKG}

import androidx.compose.ui.graphics.vector.ImageVector

/**
 * {doc}
 */
object {obj} {{
{vals}
}}
"""


def main():
    os.makedirs(OUT, exist_ok=True)
    for prop, name, build, doc in KEEPOUT:
        open(os.path.join(OUT, f"Keepout{prop}Icon.kt"), "w").write(kt_file("keepout" + prop, name, build(), 1.0, doc))
    open(os.path.join(OUT, "KeepoutIcons.kt"), "w").write(facade(
        "KeepoutIcons", [p for p, *_ in KEEPOUT], "keepout",
        "Keepout's own glyphs, for concepts Material Symbols has no convention for. Drawn on the Material\n * Symbols Rounded 24dp grid at FILL=1, wght 400 so they sit with [com.bernaferrari.bravedns.ui.icons.MaterialSymbols]."))
    res = os.path.join(ROOT, "app/src/main/res/drawable")
    for prop, drawable in BRAND:
        path, alpha = xml_vector(os.path.join(res, drawable + ".xml"))
        open(os.path.join(OUT, f"Brand{prop}Icon.kt"), "w").write(
            kt_file("brand" + prop, drawable.removeprefix("ic_"), path, alpha, f"{prop} brand mark, flattened from the former res/drawable/{drawable}.xml."))
    open(os.path.join(OUT, "BrandIcons.kt"), "w").write(facade(
        "BrandIcons", [p for p, _ in BRAND], "brand", "Third-party brand marks for the About screen's community links."))
    print("wrote", len(KEEPOUT) + len(BRAND), "vectors to", OUT)


if __name__ == "__main__":
    main()
