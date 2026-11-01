"""Fetch Material Symbols Rounded (opsz 24, wght 400, GRAD 0) from the same Kotlin endpoint
scripts/generate-material-symbols.sh uses, and convert to SVG path data. Cached in tools/symbols/.

python3 design/icons/tools/symbols.py name1 name2 ...      -> prints which were fetched
"""
import json
import os
import re
import subprocess
import sys

HERE = os.path.dirname(os.path.abspath(__file__))
CACHE = os.path.join(HERE, "symbols")
BASE = "https://fonts.gstatic.com/render/v1/Material+Symbols+Rounded/24dp/{name}.kt?var=opsz,wght,FILL,GRAD,ROND@24,400,{fill},0,50"
CMD = {'moveTo': 'M', 'moveToRelative': 'm', 'lineTo': 'L', 'lineToRelative': 'l', 'horizontalLineTo': 'H',
       'horizontalLineToRelative': 'h', 'verticalLineTo': 'V', 'verticalLineToRelative': 'v', 'quadTo': 'Q',
       'quadToRelative': 'q', 'reflectiveQuadTo': 'T', 'reflectiveQuadToRelative': 't', 'curveTo': 'C',
       'curveToRelative': 'c', 'reflectiveCurveTo': 'S', 'reflectiveCurveToRelative': 's',
       'arcTo': 'A', 'arcToRelative': 'a'}


def kt_to_paths(src):
    paths = []
    for pm in re.finditer(r'path\((.*?)\)\s*\{(.*?)\n\s*\}', src, re.S):
        args, body = pm.groups()
        d = []
        for c in re.finditer(r'(\w+)\(([^)]*)\)', body):
            k, a = c.groups()
            if k == 'close':
                d.append('Z')
                continue
            if k in CMD:
                vals = [x.strip().rstrip('f') for x in a.split(',') if x.strip()]
                vals = ['1' if v == 'true' else '0' if v == 'false' else v for v in vals]
                d.append(CMD[k] + ' ' + ' '.join(vals))
        paths.append({'d': ' '.join(d), 'eo': 'EvenOdd' in args})
    return paths


def get(name, fill):
    os.makedirs(CACHE, exist_ok=True)
    f = os.path.join(CACHE, f"{name}_{fill}.json")
    if os.path.exists(f):
        return json.load(open(f))
    r = subprocess.run(["curl", "-s", "--compressed", "--fail", BASE.format(name=name, fill=fill)], capture_output=True, text=True)
    if r.returncode != 0 or "ImageVector" not in r.stdout:
        return None
    paths = kt_to_paths(r.stdout)
    json.dump(paths, open(f, "w"))
    return paths


if __name__ == "__main__":
    for nm in sys.argv[1:]:
        a, b = get(nm, 1), get(nm, 0)
        print(nm, "ok" if a else "MISSING")
