# p2 notes: registry

Scope: `simulated/.../{index/** (minus ponder), registrate/**, data/**, config/**, Simulated, SimulatedClient}`,
`fabric/SimulatedFabric`, `fabric/SimulatedFabricClient`, `fabric/service/FabricSimConfigService` (+ its
`META-INF/services` file). 0 compile errors of their own; the rest are calls into renderers/classes of areas not merged
yet (see "Waiting on").

## Registration model (replaces Registrate)
- `registrate/SimulatedRegistrate` is now a plain per-mod helper (no Registrate): every call registers immediately
  (`Registry.register`), Create Fly style. Kept for the creative tab bookkeeping and the API others call:
  `block(name, factory, props)` (does `props.setId(key)`), `item(name, factory[, props])` (adds to `TAB_ITEMS` +
  `ITEM_TO_SECTION` in the current section), `blockItem(block[, factory[, props]])` (block id, `useBlockDescriptionPrefix()`),
  `blockEntity(name, (type, pos, state) -> BE, blocks...)` (Registrate's 3-arg factory, built with
  `FabricBlockEntityTypeBuilder`; a 2-arg `(pos, state)` overload exists too), `entity(name, EntityType.Builder)`,
  `menu(name, Create Fly MenuType)`, `displaySource/displayTarget(name, supplier)` (return the instance),
  `simple(name, registryKey, supplier)` (any registry in `BuiltInRegistries.REGISTRY`), `navTarget`, `propertyTooltip`,
  `byNameCodecExpanded(key)`, `addExtraItem(id)`, `inSection(id)`, `getItems()`. Static: `MODS`, `TAB_ITEMS`,
  `ITEM_TO_SECTION`, `sectionOf(item)`, `onAddDefaultComponents(BiConsumer<ItemLike, Consumer<DataComponentMap.Builder>>)`,
  `forEachNavigationTargetItem`.
- Index fields are the objects. Block items live in `SimItems` (Create Fly style), declared in `SimBlocks` order first,
  then the plain items: that is the creative tab order (upstream: Registrate accept order). Dyed families:
  `registrate/DyedBlockList<T>` (`get(color)` -> block, `contains`, `toArray()` -> `Block[]`, `Iterable<T>`); their items are
  `Map<DyeColor, BlockItem>` in `SimItems`.
- `SharedProperties::wooden/stone/softMetal/netheriteMetal` -> `Properties.ofFullCopy(STRIPPED_SPRUCE_WOOD/ANDESITE/GOLD_BLOCK/NETHERITE_BLOCK)`;
  no `initialProperties` -> `Properties.of()`. `ofFullCopy` copies the drops/description *functions*, which use our id.
  Registrate applies all `.properties(..)` calls after `initialProperties` regardless of order (gimbal sensor).
- Per-entry Registrate hooks now in `SimBlocks.register()`: `SimStress.setImpact/setCapacity(block, v)` (must run before
  the server config is created), `BlockStressValues.setGeneratorSpeed(block, rpm)`, `SimDisplaySources.assign(source, blocks)`
  (= `DisplaySource.BY_BLOCK.add`), `DisplayTarget.BY_BLOCK.register(nameplate, NameplateBlockTarget.NAMEPLATE)`,
  `SimItemUseOverrides.addBlock` (handles, steering wheel), `SimMovementBehaviours.register()`
  (`MovementBehaviour.REGISTRY.register`; client renders go on `behaviour.attachRender`).
- Create's `ItemUseOverrides` (absent in Create Fly; it hardcodes crank/lever in a `ServerPlayerGameMode` mixin) ->
  `index/SimItemUseOverrides`: Fabric `UseBlockCallback` (fires at the same point as NeoForge `RightClickBlock`, both
  sides, before the spectator check) calling `state.useWithoutItem` on the block centre.
- Visibility: `CreativeTabItemTransforms.VisibilityType.X.apply(itemLike)` / `applyIf`, `transformItem(itemLike, fn)`
  (were Registrate transforms). Upstream: copper + all 16 dyed handles, non-red engines, non-white nameplates are search-only.
- Block entity storages: `SimBlockEntityTypes.register()` -> `SimInventoryService.registerInventory/Tank/Battery(type, getter)`.
- Entity types: `EntityType.Builder.of(X::create, MISC)` + `SimEntityService.loaderEntityTransform(builder, data)`
  (`alwaysUpdateVelocity` = NeoForge `setShouldReceiveVelocityUpdates`); `builder.build(ResourceKey)`.
- Menus: Create Fly's own menu system (`foundation.gui.menu.MenuType<H>` in `CreateRegistries.MENU_TYPE`, menus extend
  `MenuBase<H>`, opened by `MenuProvider.openHandledScreen`, screens in `client.AllMenuScreens.register(type, ScreenFactory)`).
  `SimMenuTypes.LINKED_TYPEWRITER: MenuType<LinkedTypewriterBlockEntity> = LinkedTypewriterMenuCommon::create`.
- Particles: `FabricParticleTypes.complex(X.CODEC, X.STREAM_CODEC)` (alwaysSpawn false, as Create's `createType`).
- Recipe: the NeoForge-module `PortableEngineDyeingRecipe` (`simulated:portable_engine_dyeing`, used by shipped JSON) is
  ported to `data/` + `index/SimRecipeSerializers` (`new CustomRecipe.Serializer<>(Recipe::new)`; `canCraftInDimensions`
  gone; NeoForge `DyeColor.getColor(stack)` = `DyeItem#getDyeColor` then the `c:dyes/<color>` tags;
  `isComponentsPatchEmpty()` -> `getComponentsPatch().isEmpty()`).
- Stats: `index/fabric/FabricSimStats` registers into `CUSTOM_STAT` and calls `Stats.CUSTOM.get(id, formatter)` right
  away (vanilla `makeCustomStat`), so no load-complete bootstrap.
- Item attributes: instances in `CreateRegistries.ITEM_ATTRIBUTE_TYPE` (Create Fly has no `CreateBuiltInRegistries`;
  arm interaction points: `CreateRegistries.ARM_INTERACTION_POINT_TYPE`).
- Keys: 1.21.9+ `KeyMapping(name, key, KeyMapping.Category)`; category `KeyMapping.Category.register(simulated:simulated)`
  (label `key.category.simulated.simulated`, added to `en_us.json`; upstream used the raw string "Create Simulated").
  catnip `ConflictSafeKeyMapping` is gone and no key used it. Registered by the platform's client events.
- Data components: `Registry#get(id)` is `Optional<Reference>` now -> `getValue(id)` in stream codecs.
- `AllTags.commonItemTag(path)` -> `SimTags.commonItemTag(path)` (`c:` item tag).
- Datagen removed: `data/SimBlockStateGen`, `SimLang.registrateLang`, `SimAdvancements` as `DataProvider`, `SimTags`
  generators, all `.recipe/.loot/.blockstate/.tag/.lang/.model` calls.

## Registration timing (task 2)
- Everything registers in `SimulatedFabric#onInitialize` -> `Simulated.init()`, then the creative tab
  (`simulated:main_tab`) and the server config. Create Fly registers its blocks from the `create_plugin` entrypoint during
  `Blocks.<clinit>` only so it also runs without Fabric API (Fabric API adds late block states to the id map and inits
  their shape cache, and delays the registry freeze until after the entrypoints). Nothing in Create Fly's init references
  Simulated content, so no early registration is needed. Accessing Create Fly registries (`CreateRegistries.X`) triggers
  their creation, so mod init order does not matter.
- `Block#asItem()` caches its result: nothing may call it on a Simulated block before `SimItems` registered the block items
  (`Simulated.init()` runs `SimBlocks.register()` then `SimItems.register()`; advancements after).

## Client (`SimulatedClient.init`, from `SimulatedFabricClient` before the first resource reload)
- Render layers: `index/SimBlockLayers` (`BlockRenderLayerMap.putBlocks(ChunkSectionLayer.CUTOUT, ...)`), every upstream
  `.addLayer(cutoutMipped)`. 1.21.11 has no mipped cutout layer (mipmaps are per atlas).
- R5: Create Fly reads NeoForge's `render_type` key in model JSON (`BlockModelDeserializerMixin`/`LayerUnbakedModel`) but
  only Flywheel/partial buffering (`BakedModelBufferer`) uses the per-part layer; chunk meshing still uses
  `ItemBlockRenderTypes.getChunkRenderType(state)`. So: the 4 Simulated models were partials -> `render_type` restored in
  JSON; the 9 Aeronautics block models (adjustable burner, mounted potato cannon, steam vent) also got it back, but those
  blocks additionally need a CUTOUT chunk layer in Aeronautics' client registration.
- BERs/visuals: `index/SimBlockEntityRenders` (Create Fly `AllBlockEntityRenders` helpers). Upstream Registrate
  `.visual(f)` defaulted to `renderNormally = true` -> `normal(...)` (BER kept, it checks Flywheel itself);
  `.visual(f, false)` -> `visual(...)`. Factories are `(VisualizationContext, BE, float)`; the auger one picks
  `AugerCogVisual` or `SingleAxisRotatingVisual.shaft`.
- Entities `index/SimEntityRenders` (`EntityRenderers.register`), screens `index/SimMenuScreens`, tooltips
  `index/SimItemTooltips` (every registrate item: `ItemDescription.Modifier` + `KineticStats`, epic palette; `useKey` for
  handles/nameplates; reusable as `registerAll(registrate, factory)`), client behaviours `index/SimBlockEntityBehaviours`
  (`KineticAudioBehaviour` + `KineticTooltipBehaviour` per kinetic type; swivel bearing audio only), then the content
  areas' `registerClient()` hooks (later registration of the same behaviour type wins).
- `RadialWrenchMenu.registerBlacklistedBlock` is client-only in Create Fly -> moved to `SimulatedClient`.
- `SimRenderTypes`: `RenderType.create(name, RenderSetup.builder(pipeline).withTexture("Sampler0", id).useLightmap()...)`;
  pipelines registered with `RenderPipelines.register(...)` (precompiled). Veil pinwheel programs -> core shaders in
  `assets/simulated/shaders/core/{laser,laser_pointer_lens,rope,spring}` using vanilla `fog/dynamictransforms/projection/light`
  includes (`ModelOffset`, `apply_fog`, Lighting UBO). Veil `block_brightness` uses fixed overworld face shades; Veil's
  `position_color_tex_lightmap` -> `core/rendertype_text`; `staff_overlay` had no shader upstream -> `core/position_color`.
  `spring` keeps the BLOCK layout (Position, Color = stress, UV0, UV2, Normal). Glowing item types bind the **items**
  atlas (`TextureAtlas.LOCATION_ITEMS`; the staff's `item/...` textures live there in 1.21.11), shader-pack fallback
  `Sheets.translucentItemSheet()`. Veil "fixed buffers" per stage have no equivalent (feature renderer draws by type).
- GUI: `SimGUITextures` implements `TextureSheetSegment` (`bind()` returns a `TextureSetup`); `blit(id, x, y, u, v, w, h,
  tw, th)` -> `blit(RenderPipelines.GUI_TEXTURED, id, ...)`; `UIRenderHelper.drawColoredTexture(g, TextureSetup, color, ...)`.
  `SimIcons#bind()` returns `RenderTypes.text(atlas)`.
- Creative tab (`registrate/simulated_tab/SimulatedCreativeTab`): `pose()` is a `Matrix3x2fStack`; no depth/shader state;
  `blitSprite(RenderPipelines.GUI_TEXTURED, ...)`; scissors via `graphics.enableScissor` in GUI coords (follow the pose);
  GUI sprites: `getAtlasManager().getAtlasOrThrow(AtlasIds.GUI)`. Expects `SimulatedSection.Title` colors as catnip
  `Color` (`getRGB()`). The tab (`SimulatedFabric.TAB`, `FabricItemGroup.builder()`) also has a fallback display generator.

## Config
- `config/**` extends Create Fly's catnip `ConfigBase`, which is backed by Create Fly's own JSON config files (Create Fly's
  `AllConfigs` is the same; neither uses Forge Config API Port). `FabricSimConfigService`: `Builder.create(SimServer::new,
  "simulated", "server")` (common) / `"client"` (client) -> `config/simulated/{server,client}.json`; stress providers
  registered like upstream; `SimServer#reload()` (Create Fly `CServer#reload`) runs from a SERVER_DATA reload listener.
  The `forgeconfigapiport` dependency in fabric.mod.json is not needed by Simulated's configs.

## R3 / R4
- R3: the 20 `simulated:<id>_builtin` triggers are created by `SimAdvancements` (class init) and registered into
  `TRIGGER_TYPES` by `SimAdvancementTriggers.register()` in `Simulated.init()`. `SimulatedAdvancement` keeps its API
  (builder, `awardTo`, `awardToNearby`); icon/criteria builder calls only mark "external trigger" (the JSON is shipped).
  `MethodsReturnNonnullByDefault` is gone in 1.21.11.
- R4: every `c:` tag used by Simulated/Aeronautics JSON exists on Fabric (convention tags v2: `stones`, `dusts/redstone`,
  `nuggets/iron|copper`, `strings`, `slime_balls`, `ingots/copper`, `gems/amethyst`, `dyes/*`, `enchantables`,
  `music_discs`, `leathers`, `armors`, block `storage_blocks/coal`, fluid `water`, `honey`) or in Create Fly's data
  (`plates/*`, `nuggets/brass`; Create Fly adds its honey to `c:honey`). Code: `Tags.Items.NUGGETS_IRON` ->
  `ConventionalItemTags.IRON_NUGGETS` (`HandleBlock`), `Tags.Items.DYES` -> `ConventionalItemTags.DYES`,
  `Tags.Items.DUSTS_REDSTONE` -> `ConventionalItemTags.REDSTONE_DUSTS`.

## For Aeronautics (same patterns)
- `AeroRegistrate extends SimulatedRegistrate` keeps working: `simple(...)` replaces Registrate's `simple`, returns the
  object; `inSection`, `item`, `blockItem`, `blockEntity`, `entity`, `displaySource` as above. Client:
  `SimItemTooltips.registerAll(Aeronautics.getRegistrate(), factory)`, `SimBlockEntityRenders.normal/visual/render`.
  `AeroAdvancements` must stop extending the removed `DataProvider` part of `SimAdvancements`.

## Waiting on other areas (the only errors left in these files)
- Renderers not yet on `BlockEntityRenderer<T, S>`: altitude/gimbal/velocity/optical sensors, laser pointer, nameplate,
  nav table, directional/modulating receivers, redstone accumulator, linked typewriter; entity renderers honey glue,
  launched plunger, diagram.
- `LinkedTypewriterScreen.create(Minecraft, MenuType<LinkedTypewriterBlockEntity>, int, Inventory, Component,
  RegistryFriendlyByteBuf)` (Create Fly `ScreenFactory`), `AltitudeSensorMovementBehaviour extends MovementBehaviour`.
- `NameplateBlockTarget.NAMEPLATE` must be the instance:
  `= Simulated.getRegistrate().displayTarget("nameplate", NameplateBlockTarget::new)`.
- `SimPonderScenes` imports the deleted `data.SimBlockStateGen` (unused import).

## PORT-TODOs
- `SimulatedClient`: Veil shader preprocessor `EndSeaFadeTransformer` (end sea fade in all shaders) not re-implemented.
- `FabricSimConfigService`: server config is not synced to clients (NeoForge did; Create Fly syncs its own with a packet).
- VoidAnchor has no block/BE registration upstream either; its renderer is not registered.
