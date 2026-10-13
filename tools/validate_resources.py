#!/usr/bin/env python3
"""Validate simulated/aeronautics/sable JSON resources: parse, model/texture/item-def/tag references.
Usage: validate_resources.py [vanilla_dir]   (vanilla_dir = extracted 1.21.11 jar, default /tmp/vanilla-1.21.11)
Namespaces other than the local ones and minecraft are not checked (create, c, ...)."""
import json, glob, os, sys, re
ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
VAN = sys.argv[1] if len(sys.argv) > 1 else '/tmp/vanilla-1.21.11'
MODS = ['simulated', 'aeronautics', 'sable']
errors, warns = [], []
assets, data = {}, {}   # ns -> set of relative paths
for m in MODS:
    for kind, store in (('assets', assets), ('data', data)):
        for f in glob.glob(f'{ROOT}/{m}/src/main/resources/{kind}/*/**', recursive=True):
            if os.path.isfile(f):
                ns, rel = os.path.relpath(f, f'{ROOT}/{m}/src/main/resources/{kind}').split('/', 1)
                store.setdefault(ns, set()).add(rel)
local = set(assets) | set(data)
OWN = {'simulated', 'aeronautics', 'sable'}
def vexists(kind, ns, rel):
    return os.path.exists(f'{VAN}/{kind}/{ns}/{rel}')
def has(kind, ns, rel):
    s = (assets if kind == 'assets' else data)
    if ns in s and rel in s[ns]: return True
    return vexists(kind, ns, rel)
def split(r):
    return r.split(':', 1) if ':' in r else ['minecraft', r]
def check_asset(ref, folder, ext, src, optional_ns=True):
    ns, p = split(ref)
    if ns not in OWN and ns != 'minecraft': return  # foreign namespace (create, ...)
    if not has('assets', ns, f'{folder}/{p}{ext}'):
        errors.append(f'{src}: missing {folder}/{p}{ext} in {ns}')
jsons = {}
for m in MODS:
    for f in glob.glob(f'{ROOT}/{m}/src/main/resources/**/*.json', recursive=True):
        rel = os.path.relpath(f, ROOT)
        try: jsons[rel] = json.load(open(f))
        except Exception as e: errors.append(f'{rel}: invalid JSON ({e})')
def walk_models(d):
    if isinstance(d, dict):
        for k, v in d.items():
            if k == 'model' and isinstance(v, str): yield v
            else: yield from walk_models(v)
    elif isinstance(d, list):
        for v in d: yield from walk_models(v)
for rel, d in jsons.items():
    parts = rel.split('/')
    if len(parts) < 6: continue
    kind, ns = parts[4], parts[5]
    sub = '/'.join(parts[6:])
    if kind == 'assets' and sub.startswith('models/'):
        if 'loader' in d: errors.append(f'{rel}: custom loader {d["loader"]}')
        if 'parent' in d and not d['parent'].startswith('builtin/'): check_asset(d['parent'], 'models', '.json', rel)
        for t, v in d.get('textures', {}).items():
            if isinstance(v, str) and not v.startswith('#'): check_asset(v, 'textures', '.png', rel)
    elif kind == 'assets' and sub.startswith('blockstates/'):
        for mdl in walk_models(d): check_asset(mdl, 'models', '.json', rel)
    elif kind == 'assets' and sub.startswith('items/'):
        for mdl in walk_models(d): check_asset(mdl, 'models', '.json', rel)
    elif kind == 'data' and '/recipe/' in rel:
        s = json.dumps(d)
        if '"item":' in s and not re.search(r'"(icon|result)', s) or '"tag":' in s: warns.append(f'{rel}: old-style ingredient object')
        for t in re.findall(r'"#([a-z_]+:[a-z_/]+)"', s):
            tns, tp = split(t)
            if tns in OWN and not any(has('data', tns, f'tags/{k}/{tp}.json') for k in ('item', 'fluid', 'block')):
                errors.append(f'{rel}: unknown local tag #{t}')
# every model in models/item and every blockstate/lang item has an item definition
for ns in local:
    for p in assets.get(ns, ()):
        if p.startswith('models/item/') and p.count('/') == 2:
            i = p[len('models/item/'):-5]
            if f'items/{i}.json' not in assets[ns]: errors.append(f'{ns}: models/item/{i}.json has no items/{i}.json')
for rel in sorted(set(errors)): print('ERROR', rel)
for rel in sorted(set(warns)): print('WARN ', rel)
print(f'{len(jsons)} json files, {len(set(errors))} errors, {len(set(warns))} warnings')
sys.exit(1 if errors else 0)
