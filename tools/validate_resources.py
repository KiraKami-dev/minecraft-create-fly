#!/usr/bin/env python3
"""Validate simulated/aeronautics/sable JSON resources against Minecraft 1.21.11 and Create Fly.
Usage: validate_resources.py [vanilla_dir]
  vanilla_dir = extracted 1.21.11 client jar (data/ + assets/), default /tmp/vanilla-1.21.11; extracted from Loom's
  cache (~/.gradle/caches/fabric-loom/1.21.11/minecraft-client.jar) when missing.
Checks:
  - every JSON parses; model parents/textures, blockstate and item-definition models exist
  - recipes match the codecs of 1.21.11 (crafting_shaped/shapeless) and Create Fly (create:*), see
    docs/p2-notes/sim-data.md: per-type required/unknown keys, ingredient/result/fluid shapes, fluid amounts in
    droplets, sequenced-assembly steps and their $ingredient/$result placeholders
  - item/block/tag/recipe references in recipes, loot tables, tags and advancements exist: local resources, vanilla,
    Create Fly and Fabric convention tags (the latter two from reference/, skipped if it is missing)
  - 1.21.11 data formats: no minecraft:air loot entries, advancement backgrounds are asset ids
Exit code 1 if there are errors."""
import json, glob, os, sys, re, zipfile
ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
VAN = sys.argv[1] if len(sys.argv) > 1 else '/tmp/vanilla-1.21.11'
MODS = ['simulated', 'aeronautics', 'sable', 'offroad']
OWN = {'simulated', 'aeronautics', 'sable', 'offroad'}
REF = f'{ROOT}/reference'
CF_DATA = f'{REF}/create-fly-1.21.11/src/main/resources/data'
CF_ASSETS = f'{REF}/create-fly-1.21.11/src/client/resources/assets'
FABRIC_DATA = glob.glob(f'{REF}/fabric-api-1.21.11/*/src/*/resources/data')
errors, warns = [], []

if not os.path.isdir(f'{VAN}/data'):
    jar = os.path.expanduser('~/.gradle/caches/fabric-loom/1.21.11/minecraft-client.jar')
    if not os.path.exists(jar):
        sys.exit(f'no vanilla resources in {VAN} and no {jar} to extract them from (run a Gradle build first)')
    print(f'extracting vanilla data/ and assets/ from {jar} to {VAN}')
    with zipfile.ZipFile(jar) as z:
        z.extractall(VAN, [n for n in z.namelist() if n.startswith(('data/', 'assets/')) and not n.endswith('/')])

assets, data = {}, {}   # ns -> set of relative paths
for m in MODS:
    for kind, store in (('assets', assets), ('data', data)):
        for f in glob.glob(f'{ROOT}/{m}/src/main/resources/{kind}/*/**', recursive=True):
            if os.path.isfile(f):
                ns, rel = os.path.relpath(f, f'{ROOT}/{m}/src/main/resources/{kind}').split('/', 1)
                store.setdefault(ns, set()).add(rel)
local = set(assets) | set(data)
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

# ---- registries known offline: items/blocks via item definitions/blockstates, tags, recipes, advancements ----
def names(dirs, sub, ext='.json'):
    out = set()
    for d in dirs:
        for f in glob.glob(f'{d}/{sub}/**/*{ext}', recursive=True):
            out.add(os.path.relpath(f, f'{d}/{sub}')[:-len(ext)])
    return out
KNOWN = {'item': {}, 'block': {}}
for ns in OWN | {'minecraft'}:
    KNOWN['item'][ns] = {p[6:-5] for p in assets.get(ns, ()) if p.startswith('items/')}
    KNOWN['block'][ns] = {p[12:-5] for p in assets.get(ns, ()) if p.startswith('blockstates/')}
KNOWN['item']['minecraft'] |= names([f'{VAN}/assets/minecraft'], 'items')
KNOWN['block']['minecraft'] |= names([f'{VAN}/assets/minecraft'], 'blockstates')
HAVE_REF = os.path.isdir(CF_DATA) and os.path.isdir(CF_ASSETS)
if HAVE_REF:
    KNOWN['item']['create'] = names([f'{CF_ASSETS}/create'], 'items')
    KNOWN['block']['create'] = names([f'{CF_ASSETS}/create'], 'blockstates')
else:
    warns.append(f'{REF} not found: create:/c: references are not checked')
TAG_DIRS = [f'{ROOT}/{m}/src/main/resources/data' for m in MODS] + [f'{VAN}/data'] + ([CF_DATA] + FABRIC_DATA if HAVE_REF else [])
CHECKED_TAG_NS = OWN | {'minecraft'} | ({'create', 'c'} if HAVE_REF else set())
def tag_exists(registry, ref):
    ns, p = split(ref)
    return any(os.path.exists(f'{d}/{ns}/tags/{registry}/{p}.json') for d in TAG_DIRS)
RECIPE_DIRS = [f'{ROOT}/{m}/src/main/resources/data' for m in MODS] + [f'{VAN}/data'] + ([CF_DATA] if HAVE_REF else [])
def data_exists(folder, ref):
    ns, p = split(ref)
    return any(os.path.exists(f'{d}/{ns}/{folder}/{p}.json') for d in RECIPE_DIRS)

def check_id(registry, ref, src):
    ns, p = split(ref)
    known = KNOWN[registry].get(ns)
    if known is not None and p not in known:
        what = 'item definition (assets/<ns>/items)' if registry == 'item' else 'blockstate'
        errors.append(f'{src}: unknown {registry} {ref} (no {what})')
def check_tag(registry, ref, src):
    if ref.startswith('#'): ref = ref[1:]
    if split(ref)[0] in CHECKED_TAG_NS and not tag_exists(registry, ref):
        errors.append(f'{src}: unknown {registry} tag #{ref}')

# ---- recipe formats (1.21.11 crafting, Create Fly create:*) ----
def check_ing(v, src, registry='item'):
    """1.21.2+ ingredient: "ns:id", "#ns:tag" or a non-empty list of ids (no tags). Fabric custom ingredients pass."""
    if isinstance(v, str):
        if v.startswith('#'): check_tag(registry, v, src)
        elif v.startswith('$'): errors.append(f'{src}: placeholder {v} outside a sequenced-assembly step slot')
        else: check_id(registry, v, src)
    elif isinstance(v, list) and v:
        for i in v:
            if not isinstance(i, str) or i.startswith('#'):
                errors.append(f'{src}: ingredient lists may only hold item ids, got {i!r}')
            else: check_id(registry, i, src)
    elif isinstance(v, dict) and 'fabric:type' in v:
        pass
    else:
        errors.append(f'{src}: bad ingredient {json.dumps(v)[:80]} (1.21.11: "id", "#tag" or ["id", ...]; '
                      f'no {{"item"}}/{{"tag"}} objects)')
def check_keys(d, allowed, src):
    extra = set(d) - set(allowed)
    if extra: errors.append(f'{src}: unknown keys {sorted(extra)} (leftover 1.21.1 fields?)')
def check_count(o, src):
    c = o.get('count', 1)
    if not isinstance(c, int) or not 1 <= c <= 99: errors.append(f'{src}: count {c!r} not in 1..99')
def check_stack(o, src):
    """ItemStack: {"id", "count"?, "components"?}"""
    if not isinstance(o, dict) or 'id' not in o:
        errors.append(f'{src}: item stack needs {{"id": ...}}, got {json.dumps(o)[:80]}'); return
    check_keys(o, ('id', 'count', 'components'), src); check_count(o, src); check_id('item', o['id'], src)
def check_output(o, src, weight=False):
    """Create Fly ProcessingOutput: {"id", "count"? (1-99), "chance"? (default 1), "components"?}"""
    if not isinstance(o, dict) or 'id' not in o:
        errors.append(f'{src}: processing output needs {{"id": ...}}, got {json.dumps(o)[:80]}'); return
    if 'amount' in o: errors.append(f'{src}: fluid result in an item result list (use fluid_results)'); return
    check_keys(o, ('id', 'count', 'chance', 'components'), src); check_count(o, src); check_id('item', o['id'], src)
    ch = o.get('chance', 1)
    if not isinstance(ch, (int, float)) or ch <= 0 or (ch > 1 and not weight):
        errors.append(f'{src}: chance {ch!r} not in (0, 1]')
def check_amount(a, src):
    if not isinstance(a, int) or a <= 0: errors.append(f'{src}: fluid amount {a!r} must be a positive int')
    elif a <= 1000: errors.append(f'{src}: fluid amount {a} looks like millibuckets (Create Fly: 81 droplets per mB)')
    elif a % 81: warns.append(f'{src}: fluid amount {a} is not a whole number of mB (81 droplets per mB)')
def check_fluid_ing(o, src):
    """{"type": "fluid_stack", "fluid": id, "amount"} or {"type": "fluid_tag", "fluid_tag": "#tag", "amount"}"""
    t = o.get('type') if isinstance(o, dict) else None
    if t == 'fluid_stack':
        check_keys(o, ('type', 'fluid', 'amount'), src)
        if not isinstance(o.get('fluid'), str) or o['fluid'].startswith('#'): errors.append(f'{src}: fluid_stack needs "fluid": id')
    elif t == 'fluid_tag':
        check_keys(o, ('type', 'fluid_tag', 'amount'), src)
        if not str(o.get('fluid_tag', '')).startswith('#'): errors.append(f'{src}: fluid_tag needs "fluid_tag": "#ns:tag"')
        else: check_tag('fluid', o['fluid_tag'], src)
    else:
        errors.append(f'{src}: bad fluid ingredient {json.dumps(o)[:80]} (type fluid_stack/fluid_tag)'); return
    check_amount(o.get('amount', 81000), src)
def check_fluid_stack(o, src):
    if not isinstance(o, dict) or not isinstance(o.get('id'), str):
        errors.append(f'{src}: fluid stack needs {{"id", "amount"}}, got {json.dumps(o)[:80]}'); return
    check_keys(o, ('id', 'amount', 'components'), src); check_amount(o.get('amount', 1), src)
def check_list(d, k, lo, hi, src, fn):
    v = d.get(k)
    if v is None: return
    if not isinstance(v, list) or not lo <= len(v) <= hi:
        errors.append(f'{src}: "{k}" must be a list of {lo}..{hi}'); return
    for i, x in enumerate(v): fn(x, f'{src} {k}[{i}]')

PT = 'processing_time'
# type: (required keys, optional keys, results size) -- from the Serializer codecs under com.zurrtum.create.content
CREATE = {
    'create:cutting': ({'ingredient', 'results', PT}, set(), (1, 4)),
    'create:pressing': ({'ingredient', 'results'}, set(), (1, 2)),
    'create:crushing': ({'ingredient', 'results', PT}, set(), (1, 7)),
    'create:milling': ({'ingredient', 'results', PT}, set(), (1, 4)),
    'create:splashing': ({'ingredient', 'results'}, set(), (1, 12)),
    'create:haunting': ({'ingredient', 'results'}, set(), (1, 12)),
    'create:deploying': ({'target', 'ingredient', 'results'}, {'keep_held_item'}, (1, 4)),
    'create:item_application': ({'target', 'ingredient', 'results'}, {'keep_held_item'}, (1, 4)),
    'create:filling': ({'ingredient', 'fluid_ingredient', 'result'}, set(), None),
    'create:emptying': ({'ingredient', 'result', 'fluid_result'}, set(), None),
    'create:sandpaper_polishing': ({'ingredient', 'result'}, set(), None),
    'create:mixing': (set(), {'ingredients', 'fluid_ingredients', 'results', 'fluid_results', 'heat_requirement', PT}, (1, 4)),
    'create:compacting': ({'results'}, {'ingredients', 'fluid_ingredients', 'heat_requirement'}, (1, 4)),
    'create:mechanical_crafting': ({'key', 'pattern', 'result'}, {'accept_mirrored', 'category', 'group', 'show_notification'}, None),
    'create:sequenced_assembly': ({'ingredient', 'transitional_item', 'result', 'sequence'}, {'junks', 'loops'}, None),
}
STEP_INPUT = {'create:deploying': 'target', 'create:item_application': 'target'}  # slot of "$ingredient" (else ingredient)
def check_pattern(d, src, max_size=None):
    pat, key = d.get('pattern'), d.get('key')
    if not isinstance(pat, list) or not pat or not all(isinstance(r, str) for r in pat) or not isinstance(key, dict):
        errors.append(f'{src}: needs "pattern" (list of strings) and "key"'); return
    if len({len(r) for r in pat}) != 1: errors.append(f'{src}: pattern rows differ in width')
    if max_size and (len(pat) > max_size or len(pat[0]) > max_size): errors.append(f'{src}: pattern larger than {max_size}x{max_size}')
    used = set(''.join(pat)) - {' '}
    if used - set(key): errors.append(f'{src}: pattern symbols {sorted(used - set(key))} not in key')
    if set(key) - used: errors.append(f'{src}: key symbols {sorted(set(key) - used)} not used in pattern')
    for k, v in key.items(): check_ing(v, f'{src} key.{k}')

def check_create(d, src, step=False):
    t = d['type']
    req, opt, rng = CREATE[t]
    check_keys(d, req | opt | {'type'}, src)
    for k in sorted(req - set(d)): errors.append(f'{src}: missing required "{k}" ({t})')
    if PT in d and (not isinstance(d[PT], int) or d[PT] < 0): errors.append(f'{src}: {PT} must be an int >= 0')
    if 'heat_requirement' in d and d['heat_requirement'] not in ('none', 'heated', 'superheated'):
        errors.append(f'{src}: heat_requirement {d["heat_requirement"]!r}')
    inp = STEP_INPUT.get(t, 'ingredient')
    for k in ('ingredient', 'target'):
        if k in d and not (step and k == inp and d[k] == '$ingredient'): check_ing(d[k], f'{src} {k}')
    if step:
        if d.get(inp) != '$ingredient': errors.append(f'{src}: step needs "{inp}": "$ingredient"')
        res = d.get('result') if t == 'create:filling' else d.get('results')
        want = '$result' if t == 'create:filling' else ['$result']
        if res != want: errors.append(f'{src}: step needs "{"result" if t == "create:filling" else "results"}": {json.dumps(want)}')
    elif rng and 'results' in d:
        check_list(d, 'results', rng[0], rng[1], src, check_output)
    if 'result' in d and not step and t != 'create:sequenced_assembly': check_stack(d['result'], f'{src} result')
    if 'fluid_ingredient' in d: check_fluid_ing(d['fluid_ingredient'], f'{src} fluid_ingredient')
    if 'fluid_result' in d: check_fluid_stack(d['fluid_result'], f'{src} fluid_result')
    check_list(d, 'fluid_ingredients', 1, 2, src, check_fluid_ing)
    check_list(d, 'fluid_results', 1, 2, src, check_fluid_stack)
    if t in ('create:mixing', 'create:compacting'):
        check_list(d, 'ingredients', 1, 9, src, check_ing)  # one entry per item: repeat an ingredient for counts
        if not d.get('ingredients') and not d.get('fluid_ingredients'): errors.append(f'{src}: needs ingredients or fluid_ingredients')
        if not d.get('results') and not d.get('fluid_results'): errors.append(f'{src}: needs results or fluid_results')
    if t == 'create:mechanical_crafting': check_pattern(d, src)
    if t == 'create:sequenced_assembly':
        check_stack(d.get('transitional_item'), f'{src} transitional_item')
        check_output(d.get('result'), f'{src} result')  # chance = main output weight / total weight
        check_list(d, 'junks', 1, 99, src, lambda o, s: check_output(o, s, weight=True))  # chance = raw weight
        loops, seq = d.get('loops', 1), d.get('sequence')
        if not isinstance(loops, int) or loops < 1: errors.append(f'{src}: loops must be an int >= 1')
        if not isinstance(seq, list) or len(seq) * (loops if isinstance(loops, int) else 1) < 2:
            errors.append(f'{src}: sequence needs at least two steps (sequence x loops)'); return
        for i, s in enumerate(seq):
            ss = f'{src} sequence[{i}]'
            if not isinstance(s, dict) or s.get('type') not in CREATE or s['type'] in (
                    'create:sequenced_assembly', 'create:mechanical_crafting', 'create:mixing', 'create:compacting'):
                errors.append(f'{ss}: not a single-input create recipe'); continue
            check_create(s, ss, step=True)

def check_recipe(d, src):
    t = d.get('type')
    if not isinstance(t, str): errors.append(f'{src}: no recipe type'); return
    if 'neoforge:' in json.dumps(d): errors.append(f'{src}: NeoForge-only content (neoforge:...)')
    if t in CREATE:
        check_create(d, src)
    elif t == 'minecraft:crafting_shaped':
        check_keys(d, ('type', 'category', 'group', 'key', 'pattern', 'result', 'show_notification'), src)
        check_pattern(d, src, 3); check_stack(d.get('result'), f'{src} result')
    elif t == 'minecraft:crafting_shapeless':
        check_keys(d, ('type', 'category', 'group', 'ingredients', 'result'), src)
        if not isinstance(d.get('ingredients'), list) or not 1 <= len(d['ingredients']) <= 9:
            errors.append(f'{src}: ingredients must be a list of 1..9')
        else:
            for i, v in enumerate(d['ingredients']): check_ing(v, f'{src} ingredients[{i}]')
        check_stack(d.get('result'), f'{src} result')
    elif t.startswith('create:'):
        errors.append(f'{src}: create recipe type {t} not covered by this validator (add its Create Fly codec)')

# ---- loot tables, tags, advancements ----
def walk(d, fn):
    if isinstance(d, dict):
        fn(d)
        for v in d.values(): walk(v, fn)
    elif isinstance(d, list):
        for v in d: walk(v, fn)
def check_loot(d, src):
    def fn(o):
        if o.get('type') == 'minecraft:item' and 'name' in o:
            if o['name'] == 'minecraft:air': errors.append(f'{src}: loot entry minecraft:air (1.21.11 rejects it; use "pools": [])')
            else: check_id('item', o['name'], src)
        elif o.get('type') == 'minecraft:tag' and 'name' in o: check_tag('item', o['name'], src)
        if o.get('condition') == 'minecraft:block_state_property' and 'block' in o: check_id('block', o['block'], src)
    walk(d, fn)
def check_tag_file(d, registry, src):
    for v in d.get('values', []):
        ref, required = (v, True) if isinstance(v, str) else (v.get('id'), v.get('required', True))
        if not required or not isinstance(ref, str): continue
        if ref.startswith('#'): check_tag(registry, ref, src)
        elif registry in ('item', 'block'): check_id(registry, ref, src)
        elif registry == 'worldgen/world_preset' and not data_exists('worldgen/world_preset', ref):
            errors.append(f'{src}: unknown world preset {ref}')
def check_advancement(d, src):
    disp = d.get('display', {})
    if 'icon' in disp: check_stack(disp['icon'], f'{src} icon')
    bg = disp.get('background')
    if isinstance(bg, str) and ('textures/' in bg or bg.endswith('.png')):
        errors.append(f'{src}: background {bg} must be an asset id (1.21.11: "ns:gui/x" = assets/ns/textures/gui/x.png)')
    if 'parent' in d and not data_exists('advancement', d['parent']): errors.append(f'{src}: unknown parent {d["parent"]}')
    for r in d.get('rewards', {}).get('recipes', []):
        if not data_exists('recipe', r): errors.append(f'{src}: unknown reward recipe {r}')
    for name, c in d.get('criteria', {}).items():
        cond = c.get('conditions')
        if not isinstance(cond, dict): continue
        if c.get('trigger') == 'minecraft:recipe_unlocked' and not data_exists('recipe', cond.get('recipe', '')):
            errors.append(f'{src}: criterion {name} unlocks unknown recipe {cond.get("recipe")}')
        for p in cond.get('items', []):
            if isinstance(p, dict) and 'items' in p:
                it = p['items']
                for i in ([it] if isinstance(it, str) else it):
                    (check_tag if i.startswith('#') else check_id)('item', i, f'{src} criterion {name}')
        walk(cond, lambda o: check_id('block', o['block'], f'{src} criterion {name}')
             if o.get('condition') == 'minecraft:block_state_property' and 'block' in o else None)

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
        # neoforge:obj is supported on Fabric by Create Fly's OBJ loader (client BlockModelMixin)
        if 'loader' in d and d['loader'] != 'neoforge:obj': errors.append(f'{rel}: custom loader {d["loader"]}')
        if 'parent' in d and not d['parent'].startswith('builtin/'): check_asset(d['parent'], 'models', '.json', rel)
        for t, v in d.get('textures', {}).items():
            if isinstance(v, str) and not v.startswith('#'): check_asset(v, 'textures', '.png', rel)
    elif kind == 'assets' and sub.startswith('blockstates/'):
        for mdl in walk_models(d): check_asset(mdl, 'models', '.json', rel)
    elif kind == 'assets' and sub.startswith('items/'):
        for mdl in walk_models(d): check_asset(mdl, 'models', '.json', rel)
    elif kind == 'data' and sub.startswith('recipe/'):
        check_recipe(d, rel)
    elif kind == 'data' and sub.startswith('loot_table/'):
        check_loot(d, rel)
    elif kind == 'data' and sub.startswith('advancement/'):
        check_advancement(d, rel)
    elif kind == 'data' and sub.startswith('tags/'):
        reg = os.path.dirname(sub[len('tags/'):])
        for r in ('item', 'block', 'fluid', 'entity_type', 'worldgen/world_preset', 'map_decoration_type'):
            if reg == r or reg.startswith(r + '/'):
                check_tag_file(d, r, rel); break
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
