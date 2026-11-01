import re,sys,pathlib,json
D=pathlib.Path(sys.argv[1]); out=[]
cmd={'moveTo':'M','moveToRelative':'m','lineTo':'L','lineToRelative':'l','horizontalLineTo':'H','horizontalLineToRelative':'h','verticalLineTo':'V','verticalLineToRelative':'v','quadTo':'Q','quadToRelative':'q','reflectiveQuadTo':'T','reflectiveQuadToRelative':'t','curveTo':'C','curveToRelative':'c','reflectiveCurveTo':'S','reflectiveCurveToRelative':'s'}
for f in sorted(D.glob('*.kt')):
    s=f.read_text()
    # each ImageVector property
    for m in re.finditer(r'val\s+(\w+)\s*:\s*ImageVector',s):
        name=m.group(1); start=m.end(); nxt=re.search(r'val\s+\w+\s*:\s*ImageVector',s[start:]); end=start+nxt.start() if nxt else len(s)
        body=s[start:end]; paths=[]
        for pm in re.finditer(r'path\((.*?)\)\s*\{(.*?)\n\s*\}',body,re.S):
            args,pb=pm.groups(); d=[]
            for c in re.finditer(r'(\w+)\(([^)]*)\)',pb):
                k,a=c.groups()
                if k=='close': d.append('Z'); continue
                if k in cmd: d.append(cmd[k]+' '+' '.join(x.strip().rstrip('f') for x in a.split(',') if x.strip()))
            eo='EvenOdd' in args
            alpha=re.search(r'fillAlpha\s*=\s*([\d.]+)f?',args)
            paths.append({'d':' '.join(d),'eo':eo,'a':float(alpha.group(1)) if alpha else 1})
        vb=re.search(r'viewportWidth\s*=\s*([\d.]+)f?,\s*viewportHeight\s*=\s*([\d.]+)',body)
        out.append({'file':f.name,'name':name,'vb':[float(vb.group(1)),float(vb.group(2))] if vb else [24,24],'paths':paths})
json.dump(out,open('icons.json','w')); print(len(out))
