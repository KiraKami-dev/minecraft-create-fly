# sim-data notes (Simulated data/assets JSON on 1.21.11 + Create Fly)

Check with `python3 -I tools/validate_resources.py` (extracts vanilla data/assets from Loom's cache to
`/tmp/vanilla-1.21.11` on first use; uses `reference/` for Create Fly items/tags and Fabric convention tags).
Codecs: `reference/create-fly-1.21.11/src/main/java/com/zurrtum/create/content/**/*Recipe.java` (`Serializer.CODEC`),
examples in `reference/create-fly-1.21.11/src/main/resources/data/create/recipe/<type>/`.

## Create 6 (NeoForge, 1.21.1 datagen) -> Create Fly recipe JSON
Create 6 used one `ingredients` list + one `results` list (fluids mixed in); Create Fly has named fields per type.
Unknown keys are silently ignored by the codecs, so a leftover `ingredients` just vanishes: the validator flags them.

| type | required | optional | notes |
|---|---|---|---|
| cutting | `ingredient`, `results` (1-4), `processing_time` | | Create 6 default time was 0 (omitted in datagen): write `0` |
| crushing | `ingredient`, `results` (1-7), `processing_time` | | same |
| milling | `ingredient`, `results` (1-4), `processing_time` | | same |
| pressing | `ingredient`, `results` (1-2) | | no time field |
| splashing / haunting | `ingredient`, `results` (1-12) | | |
| deploying / item_application | `target` (was ingredients[0]), `ingredient` (held item, was ingredients[1]), `results` (1-4) | `keep_held_item` | |
| filling | `ingredient`, `fluid_ingredient`, `result` (ItemStack) | | |
| emptying | `ingredient`, `result` (ItemStack), `fluid_result` (FluidStack) | | |
| sandpaper_polishing | `ingredient`, `result` (ItemStack) | | |
| mixing | at least one of `ingredients`/`fluid_ingredients`, one of `results`/`fluid_results` | `heat_requirement`, `processing_time` (default 100 = Create 6's 0) | `ingredients` max 9, one entry per item (repeat for counts); fluids 1-2 |
| compacting | `results` (1-4) | `ingredients`, `fluid_ingredients`, `heat_requirement` | like mixing |
| mechanical_crafting | `key`, `pattern`, `result` (ItemStack) | `accept_mirrored` | key values are plain ingredients; every key symbol must be used |
| sequenced_assembly | `ingredient`, `transitional_item` ({"id"}), `result`, `sequence` | `junks`, `loops` (1) | see below |

- **Ingredient** (1.21.2+): `"ns:id"`, `"#ns:tag"` or `["ns:a", "ns:b"]` (ids only, no tags in lists).
  No `{"item"}`/`{"tag"}` objects.
- **Item results** (`results`): ProcessingOutput `{"id", "count"? 1-99, "chance"? (default 1), "components"?}`.
  **Single results** (`result`): ItemStack `{"id", "count"?, "components"?}`, no chance.
- **Fluids** are in droplets, **81 per mB** (1000 mB = 81000; Create Fly's own bottle recipes use 27000):
  - ingredient `{"type": "fluid_stack", "fluid": "ns:id", "amount": N}` or
    `{"type": "fluid_tag", "fluid_tag": "#ns:tag", "amount": N}` (type ids are bare, not namespaced; the tag
    needs the `#`). Was `{"type": "neoforge:single"|"neoforge:tag", "amount": mB, ...}`.
  - result `{"id": "ns:fluid", "amount": N}` in `fluid_results` (mixing) / `fluid_result` (emptying), never in
    `results`.
- **heat_requirement**: `heated` / `superheated` (lowercase), unchanged.
- **sequenced_assembly**: Create 6 had one weighted `results` list. Create Fly: `result` = first output with
  `chance` = its weight / total weight (probability), `junks` = the other outputs keeping their **raw weights**
  as `chance` (e.g. gyroscopic mechanism 200/222 -> 0.9009, junk 8.0, 8.0, 3.0, ...). Each `sequence` step is a
  full recipe of its own type with `"$ingredient"` in its input slot (`ingredient`; `target` for deploying) and
  `"results": ["$result"]` (`"result": "$result"` for filling); Create Fly expands them into real recipes, so each
  step must satisfy its type's codec (a cutting step needs `processing_time`). sequence x loops must be >= 2.
- `category`/`group`/`show_notification` on create types are ignored (harmless).

## Other 1.21.1 -> 1.21.11 data changes found
- Loot tables: an item entry named `minecraft:air` is rejected ("Item must not be minecraft:air"). Blocks without
  a block item (Registrate generated `dropSelf` -> air) get `"pools": []`
  (`data/simulated/loot_table/blocks/merging_glue.json`).
- Advancement `display.background` is an asset id: `"ns:gui/advancement"` (resolves to
  `assets/ns/textures/gui/advancement.png`), not `"ns:textures/gui/advancement.png"` (silent missing texture).
- Unchanged and fine: tag files, recipe-unlock advancements (`inventory_changed` item predicates
  `{"items": "id"}`), `placed_block` location predicates, world presets (`layers: []` is a valid void layer list),
  `simulated:end_sea`, Sable `physics_block_properties`.

## Server log notes
- `Registry 'simulated:property_tooltip' was empty after loading`: the registry is client-only content (filled in
  `BlockPropertiesTooltip.init()` from client init, as upstream). Fixed in `index/SimRegistries.java`: on a
  dedicated server it is a standalone `MappedRegistry` outside the root registry (client unchanged).
- `No key layers in MapLike[{}]` was vanilla parsing the empty `generator-settings` of the e2e `server.properties`
  (also in Sable-only runs), not our data; `tools/e2e.sh` now writes the default flat settings explicitly.
- Remaining WARN `TranslationConventionLogWarnings` (untranslated item tags) is pre-existing (Create Fly/Sable).

## For aeronautics (do not edit here; validator output)
- `data/aeronautics/advancement/root.json`: background -> `"aeronautics:gui/advancement"`.
- Its create recipes (crushing, deploying x17, mechanical_crafting, mixing, splashing) already match the rules
  above (crushing has `processing_time` 250; levitite blend mixing 500 mB = 40500 droplets).
