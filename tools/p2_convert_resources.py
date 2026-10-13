#!/usr/bin/env python3
"""One-shot 1.21.1 -> 1.21.11 resource conversion for simulated/aeronautics (recipes, item defs, models)."""
import json, glob, os, sys
ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
MODS = ['simulated', 'aeronautics']
FLUID_UNIT = 81  # Create Fly: 81 droplets per mB

def ing(x):
    """1.21.1 ingredient -> 1.21.11 string / list of strings."""
    if isinstance(x, str): return x
    if isinstance(x, list): return [ing(i) for i in x]
    if 'item' in x: return x['item']
    if 'tag' in x: return '#' + x['tag']
    raise ValueError(x)

def is_fluid(x): return isinstance(x, dict) and x.get('type', '').startswith('neoforge:')

def fluid_ing(x):
    if x['type'] == 'neoforge:tag':
        return {'type': 'fluid_tag', 'amount': x['amount'] * FLUID_UNIT, 'fluid_tag': '#' + x['tag']}
    if x['type'] == 'neoforge:single':
        return {'type': 'fluid_stack', 'amount': x['amount'] * FLUID_UNIT, 'fluid': x['fluid']}
    raise ValueError(x)

def res(r):
    r = dict(r)
    return r

def conv_simple(d, step=False):
    """Convert one Create processing recipe (or sequence step). Returns new dict."""
    t = d['type']; out = {'type': t}
    ins = d.get('ingredients', [])
    items = [i for i in ins if not is_fluid(i)]
    fluids = [i for i in ins if is_fluid(i)]
    if t == 'create:deploying':
        tgt, held = items[0], items[1]
        out['target'] = '$ingredient' if step else ing(tgt)
        out['ingredient'] = ing(held)
    elif t == 'create:filling':
        out['ingredient'] = '$ingredient' if step else ing(items[0])
        out['fluid_ingredient'] = fluid_ing(fluids[0])
    elif t == 'create:mixing':
        out['ingredients'] = [ing(i) for i in items]
        if fluids: out['fluid_ingredients'] = [fluid_ing(f) for f in fluids]
    else:  # pressing cutting crushing splashing ...
        out['ingredient'] = '$ingredient' if step else ing(items[0])
    for k, v in d.items():
        if k in ('type', 'ingredients', 'results'): continue
        out[k] = v
    rs = d.get('results', [])
    if step:
        out['results'] = ['$result']
        if t == 'create:filling': del out['results']; out['result'] = '$result'
    elif t == 'create:filling':
        out['result'] = res(rs[0])
    elif t == 'create:mixing':
        fr = [r for r in rs if 'amount' in r]
        ir = [r for r in rs if 'amount' not in r]
        if ir: out['results'] = ir
        if fr: out['fluid_results'] = [{'amount': r['amount'] * FLUID_UNIT, 'id': r['id']} for r in fr]
    else:
        out['results'] = [res(r) for r in rs]
    return out

def conv_seq(d):
    rs = d['results']
    total = sum(r.get('chance', 1.0) for r in rs)
    main = dict(rs[0]); main['chance'] = round(main.get('chance', 1.0) / total, 4)
    out = {'type': d['type'], 'ingredient': ing(d['ingredient']),
           'transitional_item': d['transitional_item'], 'result': main}
    junks = [r for r in rs[1:]]
    if junks: out['junks'] = junks
    out['loops'] = d.get('loops', 1)
    out['sequence'] = [conv_simple(s, step=True) for s in d['sequence']]
    return out

def conv_recipe(d):
    t = d['type']
    if t == 'minecraft:crafting_shaped' or t == 'create:mechanical_crafting':
        d['key'] = {k: ing(v) for k, v in d['key'].items()}
    elif t == 'minecraft:crafting_shapeless':
        d['ingredients'] = [ing(i) for i in d['ingredients']]
    elif t == 'create:sequenced_assembly':
        return conv_seq(d)
    elif t.startswith('create:'):
        return conv_simple(d)
    return d

def dump(path, d, indent=2):
    with open(path, 'w') as f:
        json.dump(d, f, indent=indent); f.write('\n')

def recipes():
    n = 0
    for f in glob.glob(f'{ROOT}/*/src/main/resources/data/*/recipe/**/*.json', recursive=True):
        d = json.load(open(f)); new = conv_recipe(d)
        if new != json.load(open(f)) or True:
            dump(f, new); n += 1
    print('recipes', n)

def item_defs():
    n = 0
    for m in MODS:
        for ns in os.listdir(f'{ROOT}/{m}/src/main/resources/assets'):
            base = f'{ROOT}/{m}/src/main/resources/assets/{ns}'
            if not os.path.isdir(base + '/models/item') or ns in ('minecraft',): continue
            ids = {os.path.basename(f)[:-5] for f in glob.glob(base + '/models/item/*.json')}
            lang = json.load(open(base + '/lang/en_us.json'))
            skip = {'merging_glue', 'paired_docking_connector', 'swivel_bearing_link_block', 'levitite_blend'}
            for k in lang:
                p = k.split('.')
                if len(p) == 3 and p[0] in ('item', 'block') and p[1] == ns and p[2] not in skip \
                        and os.path.exists(f'{base}/models/block/{p[2]}.json'):
                    ids.add(p[2])
            os.makedirs(base + '/items', exist_ok=True)
            for i in sorted(ids):
                model = f'{ns}:item/{i}' if os.path.exists(f'{base}/models/item/{i}.json') else f'{ns}:block/{i}'
                dump(f'{base}/items/{i}.json', {'model': {'type': 'minecraft:model', 'model': model}})
                n += 1
    print('item defs', n)

def models():
    stripped = []
    for f in glob.glob(f'{ROOT}/*/src/main/resources/assets/*/models/**/*.json', recursive=True):
        d = json.load(open(f)); ch = False
        if d.pop('render_type', None): ch = True
        if 'loader' in d:
            d['_obj_model'] = d.pop('model', None); d.pop('loader'); d.pop('flip_v', None); ch = True
            stripped.append(os.path.relpath(f, ROOT))
        if ch: dump(f, d, indent=1)
    print('obj-loader models stripped:'); [print(' ', s) for s in stripped]

if __name__ == '__main__':
    for a in sys.argv[1:] or ['recipes', 'items', 'models']:
        {'recipes': recipes, 'items': item_defs, 'models': models}[a]()
