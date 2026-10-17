# p2 notes: aero-content

Scope (final): `aeronautics/.../content/{blocks/hot_air/** (minus balloon/effect), blocks/levitite/** (minus LevititeShaderManager), items, components, particle}`.
0 compile errors in these files (also with `-XDshould-stop.ifError=FLOW`), verified with `AeroParticleTypes` / `AeroLiftingGasTypes`
stubbed locally as requested below. Gas simulation, lift and balloon logic are unchanged; only API call sites moved.

## Client registration (orchestrator: call from the Aeronautics client entrypoint)
- `dev.eriksonn.aeronautics.content.particle.AeroParticleProviders#registerClient()`: particle providers (Fabric `ParticleFactoryRegistry`).
- `dev.eriksonn.aeronautics.content.blocks.hot_air.HotAirClient#registerClient()`: hot air amount value boxes and goggle tooltips of
  `AeroBlockEntityTypes.HOT_AIR_BURNER` / `STEAM_VENT` (`BlockEntityBehaviour.addClient`).
- Renderers (`(BlockEntityRendererProvider.Context)`, BER only, no visual, as upstream): `HotAirBurnerRenderer`, `SteamVentRenderer`.
- R5: the adjustable burner and steam vent models need a CUTOUT chunk layer (`BlockRenderLayerMap.putBlocks(ChunkSectionLayer.CUTOUT, ...)`).

## Registry shape this code expects (`index/*`, aero-core)
- `AeroParticleTypes`: plain fields like `SimParticleTypes`, e.g.
  `public static final ParticleType<PropellerAirParticleData> PROPELLER_AIR_FLOW = register("propeller_air_flow", PropellerAirParticleData.CODEC, PropellerAirParticleData.STREAM_CODEC);`
  same for `HOT_AIR_EMBER` ("hot_air_ember"), `LEVITITE_SPARKLE` ("levitite_sparkle"), `GUST` ("gust"), `AIR_POOF` ("air_poof"), with
  `Registry.register(BuiltInRegistries.PARTICLE_TYPE, Aeronautics.path(name), FabricParticleTypes.complex(codec, streamCodec))`.
  The enum / `ParticleEntry` / `registerClientParticles` go away. Every data class exposes `public static CODEC` + `STREAM_CODEC`.
- `AeroLiftingGasTypes.DEFAULT_GAS` / `STEAM`: the gas objects themselves (`DefaultLiftingGas` / `SteamLiftingGas`), no `.get()`.
- `AeroItems.AVIATORS_GOGGLES`: `AviatorsGogglesItem` is a plain `Item` now; the registration must give it the helmet armor data
  (upstream `ArmorItem`: defense 1, enchantability 15, leather equip sound, leather repair, **no durability**): e.g.
  `.attributes(material.createAttributes(ArmorType.HELMET)).enchantable(15).repairable(...)
  .component(DataComponents.EQUIPPABLE, Equippable.builder(EquipmentSlot.HEAD).setEquipSound(...).setAsset(<aeronautics:aviators_goggles key>).build())`.
  Resources: an equipment asset `assets/aeronautics/equipment/aviators_goggles.json` (`{"layers":{"humanoid":[{"texture":"aeronautics:aviators_goggles"}]}}`)
  and the layer texture at `textures/entity/equipment/humanoid/aviators_goggles.png` (today it is only at `textures/models/armor/*_layer_1.png`).
- `AeroBlocks.DYED_ENVELOPE_BLOCKS` must be initialized before `ENVELOPE_ENCASED_SHAFTS` (as upstream): the encased shaft passes its
  dyed envelope to Create Fly's `EncasedShaftBlock(Properties, Block casing)` constructor.

## API mappings (old -> new)
Particles
- Same as Simulated's (items-misc notes): `TextureSheetParticle` -> `SingleQuadParticle` (sprite passed to the constructor; `pickSprite` ->
  `spriteSet.get(random)` in the provider), `getRenderType()` -> `getLayer()` = `SingleQuadParticle.Layer.TRANSLUCENT`,
  `ParticleProvider#createParticle(..., RandomSource)`.
- Double-sided quads (`GustParticle`): the particle pipeline culls, so the back face is a second `QuadParticleRenderState#add` with the
  orientation turned 180 degrees around local y and u0/u1 swapped (same vertices as upstream's reversed second quad).
- `ExtraCodecs.QUATERNIONF` / `ByteBufCodecs.QUATERNIONF` are `Quaternionfc`: `.xmap/.map(Quaternionf::new, q -> q)`.

Block entities / value boxes
- Create's common `ScrollValueBehaviour` subclass (`HotAirBurnerValueBehaviour`) -> server `ServerScrollValueBehaviour` subclass (range
  suppliers, stepping, `getValueSettings/setValueSettings`, clipboard key) + client `HotAirBurnerScrollValueBehaviour` (label, slot, board,
  formatter; reads interval/min/max from the server half). `behaviour.value = x` while constructing -> `initValue(x)` (no side effects).
- `ValueBoxTransform.Sided#fromSide` returns `void`; `getLocalOffset(level, pos, state)` -> `getLocalOffset(state)`; `rotate(level, pos, state, ms)` -> `rotate(state, ms)`.
  The steam vent slot (uses `Minecraft.hitResult`) moved to client `SteamVentValueBoxTransform(BlockEntity)`.
- Goggle info: BEs drop `IHaveGoggleInformation`; client `LiftingGasGoggleInformation.LiftingGasTooltipBehaviour` (a `TooltipBehaviour`)
  reads `BlockEntityLiftingGasProvider#getClientBalloonInfo()/getTicksSinceSync()` (new interface methods);
  `addBalloonGoggleInformation` moved there. `LiftingGasType#getName()` -> `Component.translatable("aeronautics.lifting_gas.*")` (was AeroLang).
- `ClientBalloonInfo.writeToNBT/readFromNBT` take `ValueOutput/ValueInput` (same keys).

Blocks / entities
- `onRemove` (steam vent): block changed -> `affectNeighborsAfterRemoval`; upstream also ran it for state-only changes, now done at the
  top of `onPlace` when `oldState.is(this)`. `entityInside(..., InsideBlockEffectApplier, boolean)`; `neighborChanged(..., @Nullable Orientation, boolean)`;
  `getLightBlock(state)`; `getCloneItemStack(level, pos, state, includeData)`; `fallOn(..., double)`; `updateEntityAfterFallOn` ->
  `updateEntityMovementAfterFallOn`; `DirectionProperty` -> `EnumProperty<Direction>`; `BlockEntry#getDefaultState/asStack` -> `defaultBlockState()` / `new ItemStack(block)`.
- `GustEntity`: NeoForge `IEntityWithComplexSpawn` -> Create Fly `NbtSpawnPacket` from `getAddEntityPacket` (orientation under
  "GustOrientation"), read in `recreateFromPacket`; `hurtServer` returns false; `makeBoundingBox(Vec3)`.
  Upstream had load/save swapped (load wrote, save read the tag); now saved/loaded properly under the same key.
- `SavedData.Factory` -> one static `SavedDataType` (the data storage caches by record equality, so do not build a new type per call);
  loaded balloons are handed to the level's `BalloonMap` in `BalloonLevelSavedData.get(level)` (upstream did it in the loader).
- `Registry#get(id)` (Optional now) -> `getValue(id)` (`Converter`); `Camera#getPosition` -> `position()`.
- `BaseArmorItem`/`ArmorItem` -> plain `Item` + properties (see registry section); `ArmorItem.Type` -> `ArmorType`.

Rendering
- `SmartBlockEntityRenderer<T>` -> `SmartBlockEntityRenderer<T, S extends SmartRenderState>`; partials as `SuperByteBuffer` in the state,
  transforms applied in the `CustomGeometryRenderer` callback. `RenderType.cutoutMipped()/translucent()` -> `RenderTypes.cutoutMovingBlock()/translucentMovingBlock()`.
- Veil burner flame (`pinwheel/shaders/program/burner_flame`, drawn immediately with uniforms) -> `RenderPipeline` (`MATRICES_FOG_SNIPPET`,
  no cull, `POSITION_COLOR_TEX_LIGHTMAP`) + core shader `assets/aeronautics/shaders/core/burner_flame.{vsh,fsh}`, `RenderType` binding
  `textures/effects/fire_palette.png` as Sampler0 (`HotAirBurnerRenderer.FLAME_RENDER_TYPE`). The per-burner uniforms travel in the
  vertices so all flames share one batch: colour r = intensity, g = palette row, UV2.x = render time mod 10/3 s scaled to 0..32767
  (the shader is periodic with that period). The pinwheel files are now unused.

## Files edited before the scope change (left as they are)
After merging p2-port (aero-core) they compile with 0 errors (flow check included), but they were not reviewed further. If they are
kept, the propeller bearing contraption should be registered with `PropellerBearingContraptionEntityRenderer::new` /
`PropellerBearingContraptionVisual::new` (Create Fly's `ControlledContraption*` pair lacks the gyro tilt of upstream's `applyLocalTransforms`),
and the propeller client hooks are `PropellerBearingClient#registerClient()` plus `PropellerTooltipBehaviours.SmallPropellerTooltipBehaviour`
for the small propellers (not registered anywhere yet). WIP commit `9b3b41e` also touched `content/blocks/propeller/**` (render-state renderers incl. `PropellerBearingRenderer extends BearingRenderer`,
`PropellerActorBehaviour` without the goggle code + client `PropellerTooltipBehaviours`, `PropellerBearingClient` (thrust direction
`ScrollOptionBehaviour` + gyro slot), `ServerScrollOptionBehaviour` + `ValueOutput` in the bearing BEs, `PropellerBearingContraptionEntity`
without `applyLocalTransforms` + new `PropellerBearingContraptionEntityRenderer`/`Visual`, `Create.RANDOM` -> `level.random`,
`getNormal()` -> `getUnitVec3i()`) and `content/blocks/mounted_potato_cannon/{MountedPotatoCannonBlockEntity,MountedPotatoCannonRenderer}`
(`getNormal()` -> `getUnitVec3i()` only).

## PORT-TODOs
- None in the scope.
