# p2 notes: aero-core

Scope: `aeronautics/.../{Aeronautics, AeronauticsClient, index/** (minus client/AeroRenderTypes), registry, data, config,
service, events, network, util, api, mixinterface, fabric/**, plugin, compat/jei}`, `fabric.mod.json`,
`aeronautics-fabric.mixins.json`, `META-INF/services`, `data/**`. Same patterns as Simulated (`registry.md`, `platform.md`).

## Registration (replaces Registrate)
- `AeroRegistrate extends SimulatedRegistrate` (Simulated's Registrate-free helper): everything registers immediately,
  items go to the Simulated tab in section `aeronautics:aeronautics` (`assets/aeronautics/simulated/sections`).
  `liftingGasType(name, supplier)` returns the object (`simple(...)` into `aeronautics:lifting_gas_type`).
- Index fields are the objects: `AeroBlocks.X` (block), `AeroItems.X` (all block items + items + bucket, tab order =
  upstream Registrate accept order), `AeroBlockEntityTypes.X`, `AeroEntityTypes.X`, `AeroParticleTypes.X`
  (`ParticleType<Data>`), `AeroDisplaySources.GAS_DISPLAY`, `AeroLiftingGasTypes.DEFAULT_GAS/STEAM`,
  `AeroLevititeBlendPropagationContexts.STANDARD_CONTEXT/SOUL_CONTEXT` (no `.get()`).
  `AeroRegistries.LIFTING_GAS_TYPE` / `LEVITITE_CRYSTAL_PROPAGATION_CONTEXT` are plain `Registry<T>` (was a veillite
  `RegistrationProvider`): `getValue(id)`, `getKey(v)`, iterate the registry instead of `getEntries()`.
- Dyed envelopes: `AeroItems.ENVELOPES` / `ENVELOPE_ENCASED_SHAFTS` are `Map<DyeColor, BlockItem>`
  (`WHITE_ENVELOPE` visible, other colors search-only, encased shafts invisible).
- Per-block Registrate hooks in `AeroBlocks.register()`: `AeroStress.setImpact(block, v)` (static now, like `SimStress`),
  `AeroDisplaySources.assign(source, blocks)`, `EncasingRegistry.addVariant(AllBlocks.SHAFT, shaft)`,
  `((FireBlock) Blocks.FIRE).setFlammable(envelope, 30, 60)`.
- Mounted potato cannon storage: `SimInventoryService.INSTANCE.registerInventory(type, (be, dir) -> be.getInventory())`
  in `AeroBlockEntityTypes.register()`.
- Entities: plain `EntityType.Builder` with upstream's properties; velocity updates keep the vanilla default.
- Armor (1.21.2+): `AeroArmorMaterials.AVIATORS_GOGGLES` is an `ArmorMaterial` record (no registry);
  `AeroArmorMaterials.helmet(material)` builds the item properties (attributes, enchantable, repairable,
  `EQUIPPABLE` with asset `aeronautics:aviators_goggles`). Added `assets/aeronautics/equipment/aviators_goggles.json` +
  `textures/entity/equipment/humanoid/aviators_goggles.png` (copy of `models/armor/..._layer_1.png`).
- `AeroTags`: generators removed (JSON shipped); `AllTags.commonItemTag` -> `SimTags.commonItemTag`; new
  `AeroTags.FluidTags.LEVITITE_BLEND` (+ `data/aeronautics/tags/fluid/levitite_blend.json`).
- `AeroPonderScenes`: key function `RegisteredObjectsHelper::getKeyOrThrow` over `ItemLike` (like `SimPonderScenes`).
- `AeroAdvancements` no longer extends the (removed) `DataProvider`; `AeroAdvancementTriggers.register()` in `init()`.
- Deleted: `data/AeroBlockStateGen` (datagen), `AeroLang.registrateLang`, `META-INF/accesstransformer.cfg` (particle
  entry already in the AW; the rest was datagen / Veil render type).

## Init order (`Aeronautics.init()` from `fabric.AeronauticsFabric`)
registries, data components, sounds, display sources, blocks (+stress), items (before any `asItem()`), fluids
(interactions/attributes), BE types (+storage), entities, particles, lifting gases, movement checks, packets,
propagation contexts, arm points, advancement triggers, Sable events. Then `FabricAeroConfigService.registerCommon()`
(Create Fly catnip JSON config `config/aeronautics/{server,client}.json`, stress providers, reload on SERVER_DATA reload)
and `AeroFabricCommonEvents` (SERVER_STOPPED, END_SERVER_TICK per level, `FuelRegistryEvents.BUILD`: `#aeronautics:envelope`
burns 100 ticks = upstream `neoforge:furnace_fuels` data map).
Client (`fabric.AeronauticsFabricClient`): client config, `AeronauticsClient.init()`, client ticks.

## Client registration (`AeronauticsClient.init`)
- `AeroBlockLayers`: CUTOUT for adjustable burner, steam vent, mounted potato cannon (their model JSON `render_type` is
  only honoured by Create Fly for partials).
- `AeroBlockEntityRenders` via `SimBlockEntityRenders.render/normal/visual` (`.visual(f)` -> `normal`,
  `.visual(f, false)` -> `visual`). `AeroEntityRenders`: propeller contraption -> Create Fly
  `ControlledContraptionEntityRenderer` + `ControlledContraptionVisual` (upstream used Create's generic pair); gust ->
  `NoopRenderer`. `AeroBlockEntityBehaviours`: kinetic audio + tooltip for every kinetic type.
- Tooltips: `SimItemTooltips.registerAll(Aeronautics.getRegistrate(), SimItemTooltips.DEFAULT_FACTORY)` (same factory as
  upstream's `setTooltips`).
- Levitite CT: `AllModels.register(block, CTModel.of(new SimpleCTBehaviour(AeroSpriteShift.X)))`.

## Levitite Blend (NeoForge fluid type -> Fabric)
- `index/AeroFluids`: Create Fly style `FluidEntry` with `fabric.content.fluids.levitite.LevititeBlendFluid.Source`
  (`FlowableFluid.Still`, crystallization tick = upstream `LevititeBlendNeoForge`) and `.Flowing`; ids
  `aeronautics:levitite_blend` / `flowing_levitite_blend`; block `AeroBlocks.LEVITITE_BLEND` (Create Fly `FluidBlock`,
  copy of water), bucket `AeroItems.LEVITITE_BLEND_BUCKET`. Create Fly's `FlowableFluid` defaults equal Registrate's
  standard fluid (drop-off 2, tick 25, slope 3, resistance 100). No fire extinguishing (NeoForge default).
- Lava interaction: Create Fly's `FluidInteractionRegistry` (same API as NeoForge's, keyed by `Fluid`).
- Bucket sounds: `FluidVariantAttributes.register(..., handler)` (fill/empty), `getPickupSound()` override.
- Entity physics (NeoForge patches, `fabric/mixin/levitite_blend`): `EntityMixin` (RETURN of
  `updateInWaterStateAndDoFluidPushing`: `updateFluidHeightAndDoFluidPushing(LEVITITE_BLEND tag, -7/3000)`, fall
  distance x0.5, return true when inside), `LivingEntityMixin` (HEAD of `travel`: upstream `LevititeBlendFluidType#move`;
  wrap of `isAffectedByFluids()` in `aiStep`: jump = `jumpInLiquid` like NeoForge `jumpInFluid`). Water keeps priority.
- Client (`LevititeBlendClient`): Fabric `SimpleFluidRenderHandler`, Create Fly `AllFluidConfigs.ALL` entry (tanks,
  pipes, items), and a `FogEnvironment` added first to `FogRenderer.FOG_ENVIRONMENTS` (AW `# aero-core`): fog color
  `AeroColors.LEVIBLEND_THE_FOG_IS_COMING`, -8..96*(1/32*chocolateTransparencyMultiplier) when the camera is below the
  blend surface. 1.21.11 fog is not cylindrical (upstream set `FogShape.CYLINDER`).
- `fabric/mixin/client/BlockRenderDispatcherMixin`: `@WrapMethod` of the 5-arg `renderBreakingTexture` (NeoForge had a
  `ModelData` overload).

## Data
- `data/aeronautics/advancement/root.json` background -> `aeronautics:gui/advancement`; recipes already Create Fly format.
- Added from the NeoForge module: `data/simulated/tags/entity_type/laser_entity_blacklist.json`.
- `converts_to_cloud_skipper.json` uses NeoForge's tag `"remove"` (ignored by vanilla/Fabric), so the Cloud Skipper
  disc itself is in the tag on Fabric; `AeroJEI` skips it.

## PORT-TODOs
- `FabricAeroConfigService`: server config not synced to clients (same as Simulated).
- Jumping in Levitite Blend resets `noJumpDelay` to 0 (vanilla's else branch); NeoForge left it untouched.
