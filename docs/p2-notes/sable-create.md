# sable-create: Sable <-> Create (Fly) integration (ROADMAP C10)

Upstream Sable kept all of its Create compat in the NeoForge module
(`reference/sable-1.21.1/neoforge/src/main/java/dev/ryanhcode/sable/neoforge/{mixin,mixinhelper,mixinterface}/compatibility`,
`.../compatibility/flywheel`, `.../physics/callback`). This branch ports it to the Fabric Sable against Create Fly.

## Setup
- `sable/build.gradle`: `modCompileOnly "maven.modrinth:create-fly:${create_fly_version}"`; Sable still runs without Create.
- Mixins live in their own config `sable/src/main/resources/sable-create.mixins.json`
  (package `dev.ryanhcode.sable.fabric.mixin.compatibility.create`, listed in `fabric.mod.json`). `SableMixinPlugin`
  (`AbstractSableMixinPlugin.shouldApplyMixin`) already gates `dev.ryanhcode.sable.fabric.mixin.compatibility.<modid>.*`
  on `<modid>` being loaded, so every mixin here needs mod `create`.
- Packages: upstream `dev.ryanhcode.sable.neoforge.X` -> `dev.ryanhcode.sable.fabric.X` (same sub-path). Flywheel is
  bundled in Create Fly (`com.zurrtum.create.client.flywheel`, no separate `flywheel` mod id), so upstream
  `neoforge.mixin.compatibility.flywheel.*` -> `fabric.mixin.compatibility.create.flywheel_lib.*` and
  `neoforge.{compatibility,mixinhelper,mixinterface}.compatibility.flywheel` / `compatibility.flywheel` ->
  `fabric.{compatibility,mixinhelper,mixinterface}.compatibility.create.flywheel`.
- Helpers that reference Create classes are only reached from Create-gated mixins, or behind a `create`-loaded check
  in always-loaded code (`SablePlatformImpl`, `SableSubLevelRenderPlatformImpl`).
- `remap` (only matters for intermediary production jars; dev runs Mojang-named): `remap = false` on name-only selectors
  and `@Shadow`/`@Accessor`/`@Invoker` of Create-owned members; any `@At` target or method selector whose descriptor
  contains Minecraft types (`net/minecraft/`, `com/mojang/`) keeps `remap = true` even when the owner is a Create class
  (the remapper still rewrites the descriptor); MC-inherited members keep the default. Checked with a lint over all 139
  mixins.
- Class mapping: `com.simibubi.create` -> `com.zurrtum.create` (client classes `com.zurrtum.create.client`),
  `net.createmod.catnip` -> `com.zurrtum.create.(client.)catnip`, `dev.engine_room.flywheel` ->
  `com.zurrtum.create.client.flywheel`.

## Platform
- `SablePlatformImpl.isWrappedLevel`: `level instanceof com.zurrtum.create.catnip.levelWrappers.WrappedServerLevel` when
  `create` is loaded (via `fabric.compatibility.create.CreateLevelWrappers`), as upstream NeoForge did; was `false`.
- `SableSubLevelRenderPlatformImpl.tryAddFlywheelVisual` -> `fabric.compatibility.create.flywheel.FlywheelCompat.tryAddVisual`
  when `create` is loaded (was a no-op; single-block sub-levels get their Flywheel BE visuals again).
- `SableFabricClient` logs upstream's "Sable is loaded with Flywheel" note when Create is present.
- Flywheel shader overrides: upstream shipped them at `assets/flywheel/...` and relied on NeoForge's mod resource
  order; on Fabric they live at `assets/sable/flywheel_overrides/...` and `flywheel_lib.ShaderSourcesMixin` makes
  Create Fly's `ShaderSources$SourceFinder#readResource` prefer them.
- Redstone contacts: Sable registers a `create:redstone_contact` block entity type (as upstream did through
  Registrate) from a `<clinit>` TAIL inject on Create Fly's `AllBlockEntityTypes`, so a client also needs Sable when
  the server has Sable + Create (same as upstream).
- `sable/build.gradle`: `maven-artifact` is now `include(api(...))` (was `implementation`). `SableMixinPlugin`'s
  `MixinConstraints` (version constraints) links against it as soon as any compat mod is loaded; with Create present,
  every from-source run (e2e, simulated, aeronautics) crashed at startup with `NoClassDefFoundError: ArtifactVersion`.

## Testing
- `:sable:compileJava` 0 errors, `:sable:validateAccessWidener` passes; `tools/e2e.sh server` (no Create): mixin audit
  passed, 0 mixin errors, 0 exceptions (the Create mixins are skipped by the plugin).
- Server e2e **with Create Fly** on the e2e runtime classpath: mixin audit passed (all 82 common Create mixins applied to
  Create Fly), 0 mixin errors/exceptions, sub-levels spawn and the server runs 600 ticks. Done without editing
  `e2e/build.gradle`, via an init script passed to Gradle (`-I e2e-create.gradle :e2e:runServer`, same setup as
  `tools/e2e.sh server`):
  ```groovy
  allprojects {
      plugins.withId('net.fabricmc.fabric-loom-remap') {
          if (path == ':e2e') dependencies.add('modRuntimeOnly', "maven.modrinth:create-fly:${property('create_fly_version')}")
      }
  }
  ```
- Not run: a client e2e with Create (it opens a game window on the desktop). The 57 client mixins are verified against
  Create Fly bytecode (javap) and compile, but have not been runtime-audited; the same init script with `:e2e:runClient`
  does that. Nothing was play-tested in game.

<!-- generated below -->

## Upstream file -> port status

### Priority 1: contraptions, behaviours, block breakers, raycasts/interaction, sticker, super glue, impacts

| upstream (under `neoforge/`) | status | notes |
|---|---|---|
| mixin/.../contraptions/AbstractContraptionEntityMixin | ported | KinematicContraption impl unchanged; `@Overwrite saveWithoutId` now `(ValueOutput)V`; tick inject before `Map.entrySet()` verified (1 call); `Direction#getNormal` -> `getUnitVec3i` |
| mixin/.../contraptions/ContraptionColliderMixin | ported + split | same 8 redirects in `ContraptionCollider.collideEntities` (server half); Create Fly moved the client half (local player collision) to `client.content.contraptions.ContraptionColliderClient.collideEntities` -> new client mixin `ContraptionColliderClientMixin` with the same redirects; shared logic in `fabric.mixinhelper.compatibility.create.contraptions.ContraptionColliderHelper` |
| mixin/.../contraptions/Matrix3dAccessor | ported | `com.zurrtum.create.foundation.collision.Matrix3d` fields still package-private |
| mixin/.../contraptions/ContraptionHandlerClientMixin | ported | target `client.content.contraptions.ContraptionHandlerClient`; `getRayInputs` now `(Minecraft, LocalPlayer)`, call sites identical |
| mixin/.../contraptions/ContraptionControlsRendererMixin | retargeted | -> `ContraptionControlsMovementRenderMixin` on `client...ContraptionControlsMovementRender#getRenderState` (render-state port of `renderInContraption`): project `MovementContext.position` out of the sub-level for the camera-distance check (single GETFIELD now, null-safe). The `getViewProjection -> getModelViewProjection` swap is dropped: Create Fly submits the text with the actor's own model pose, no ContraptionMatrices call left |
| mixin/.../behaviour_compatibility/BlockEntityBehaviourMixin | ported | target `com.zurrtum.create.api.behaviour.BlockEntityBehaviour` (generic); `get(BlockGetter,BlockPos,BehaviourType)` HEAD |
| mixin/.../behaviour_compatibility/block_breaking_behaviour/BlockBreakingMovementBehaviourMixin | ported | `MovementBehaviour` is an abstract class in Create Fly (mixin extends it); BreakingPos read via `BlockPos.CODEC` (Create Fly stores it that way); ProjectedPos via catnip `VecHelper.writeNBT/readNBT` |
| mixin/.../behaviour_compatibility/block_breaking_behaviour/SawMovementBehaviourMixin | ported | `dropItemFromCutTree(MovementContext,BlockPos,ItemStack)`: 1x `Vec3.distanceTo`, 1x GETFIELD `relativeMotion`, `@Local(ordinal=0) Vec3` = dropPos verified |
| mixin/.../behaviour_compatibility/harvester_behaviour/HarvesterMovementBehaviourMixin | ported | `@WrapMethod visitNewPosition` |
| mixin/.../behaviour_compatibility/harvester_block_entity/HarvesterBlockEntityMixin | ported | extends `CachedRenderBBBlockEntity`; catnip `LerpedFloat` is common in Create Fly |
| mixin/.../behaviour_compatibility/harvester_block_entity/HarvesterBlockEntityUsageMixin | ported | `lambda$visitNewPosition$0` + `collectOrDropItem` verified in bytecode |
| mixin/.../behaviour_compatibility/harvester_block_entity/HarvesterBlockMixin | ported | client-only ticker; `level.isClientSide()` |
| mixin/.../behaviour_compatibility/harvester_block_entity/HarvesterRendererMixin | retargeted | Create Fly `HarvesterRenderer` is a render-state renderer: TAIL of `extractRenderState(HarvesterBlockEntity,HarvesterRenderState,F,Vec3,CrumblingOverlay)` replaces `state.angle` with the lerped sub-level speed when `getAnimatedSpeed()==0` (same pivot/rotation as upstream's custom transform) |
| mixinhelper/.../behavior_compatibility/harvester_block_entity/DummyMovementContext | ported | |
| mixinhelper/.../harvester/HarvesterLerpedSpeed, HarvesterTicker | ported | |
| mixin/.../block_breakers/BlockBreakingKineticBlockEntityDamageMixin | ported | `SawBlock`/`DrillBlock#entityInside` now 6-arg (InsideBlockEffectApplier, boolean); 1x `AABB.intersects` each |
| mixin/.../block_breakers/BlockBreakingKineticBlockEntityMixin | ported | redirect `getBreakingPos()` in `tick` (1 call) |
| mixinhelper/.../block_breakers/SubLevelBlockBreakingUtility | ported | |
| mixin/.../raycast/RaycastHelperMixin | ported (client) | Create Fly `RaycastHelper` is client-only (`client.foundation.utility`), so mixin + helper moved to client |
| mixinhelper/.../raycasts/SableRaycastHelper | ported (client-only) | null-guard on `rayTraceUntil` result (it returns null for NaN input) |
| mixin/.../big_outlines_interaction/BigOutlinesMixin | ported (client) | `pick(Minecraft)` now takes mc as arg (`@Local(argsOnly=true)`); `lambda$pick$0` name + 3x distanceToSqr verified |
| mixin/.../sticker/StickerBlockEntityMixin | ported | NBT -> ValueOutput/ValueInput (same keys); Create Fly's sticker has no `write` override, so the mixin adds `write(ValueOutput,boolean)` calling super instead of a TAIL inject; `SuperGlueItem.spawnParticles` + `CatnipServices...executeOnClientOnly` -> `AllClientHandle.INSTANCE.spawnSuperGlueParticles` + direct `playSound` (client branch only) |
| mixin/.../sticker/StickerBlockMixin | ported | |
| mixin/.../sticker/ContraptionMixin | ported | `Contraption#getBlockEntityNBT` still calls `BlockEntity#saveWithFullMetadata(Provider)` |
| mixinterface/.../StickerBlockEntityExtension | ported | |
| mixin/.../super_glue/LevelAccessor | skipped (redundant) | sable.accesswidener already makes `Level#getEntities` accessible; SuperGlueEntityMixin calls it directly |
| mixin/.../super_glue/SuperGlueEntityMixin | ported | |
| mixin/.../super_glue/SuperGlueRemovalPacketMixin | retargeted | packet handling moved to `com.zurrtum.create.AllHandle#onSuperGlueRemoval(ServerGamePacketListenerImpl, SuperGlueRemovalPacket)` (static); same `ServerPlayer#distanceToSqr(Vec3)` redirect |
| mixin/.../super_glue/SuperGlueSelectionHandlerMixin | ported (client) | `client.content.contraptions.glue.SuperGlueSelectionHandler#tick(Minecraft)`, ordinal 0 = the clip box |
| mixin/.../impact/AbstractBellBlockAccessor | ported | `ring(Level,BlockPos,Direction,Player)` is Create's (protected) |
| mixin/.../impact/AbstractBellBlockMixin | ported | |
| physics/callback/AbstractBellBlockCallback | ported | -> `dev.ryanhcode.sable.fabric.physics.callback`; `Direction.getNearest` -> `getApproximateNearest` |
| mixin/.../turntable/TurntableBlockMixin | ported | `lambda$entityInside$0` (static, TurntableBlockEntity last arg) verified |
| mixin/.../crushing_wheel/CrushingWheelBlockMixin | ported | `@Overwrite entityInside` with the 1.21.11 6-arg signature; body identical to Create Fly's apart from the sub-level transform |
| mixin/.../saw/SawBlockEntityMixin | ported | |
| mixin/.../blueprint/BlueprintEntityMixin | ported | kept `@MixinModVersionConstraint("(,6.0.11)")` (Create Fly reports 6.0.9-5 and still has the old box check); `canInteractWithEntity` -> `Player#isWithinEntityInteractionRange(Entity,double)` (what Sable's PlayerMixin patches); `canPlayerUse` is Create's `IInteractionChecker` -> remap=false |
| mixin/.../elevator_controls/ElevatorControlsHandlerMixin | ported (client) | `client.content.contraptions.elevator.ElevatorControlsHandler#onScroll(Minecraft,double)` |

### Priority 2: belts, depots, deployers, arms, funnels, ejectors, fans/airflow, sails, fall-on blocks

| upstream (under `neoforge/`) | status | notes |
|---|---|---|
| mixin/compatibility/create/belt/BeltBlockEntityMixin | ported | `onSpeedChanged` override (BeltBlockEntity doesn't override it in Create Fly) wakes physics at the belt |
| mixin/compatibility/create/belt/BeltBlockMixin | ported | adds `BlockWithSubLevelCollisionCallback` -> BeltBlockCallback |
| mixin/compatibility/create/belt/BeltMovementHandlerMixin | ported | `transportEntity` bytecode re-checked: getY ordinals 0-2, only double local = diffCenter, Vec3 ordinal 0 = movement at `maxUpStep` |
| mixin/compatibility/create/belt/BeltRendererMixin | retargeted | renderer is render-state now: camera pos is `BeltRenderState.camera` (extracted from `extractRenderState(..., Vec3 cameraPos, ...)`); `@ModifyExpressionValue` on both GETFIELD reads in `renderItem` (was `Camera#getPosition()`), sub-level looked up from `state.blockPos` |
| physics/callback/BeltBlockCallback | ported | -> `dev.ryanhcode.sable.fabric.physics.callback.BeltBlockCallback`; `getNormal()` -> `getUnitVec3i()` |
| mixin/compatibility/create/depot/DepotRendererMixin | retargeted | `DepotRenderer.renderItem` now receives the camera as its 2nd Vec3 arg (was `Camera#getPosition()` inside): `@ModifyVariable(HEAD, argsOnly, ordinal = 1)`, sub-level from 1st Vec3 arg (itemPosition); also covers ejector/station/table-cloth item rendering which call it |
| mixin/compatibility/create/deployer/DeployerBlockEntityMixin | ported | `DeployerHandler.activate` first param is `DeployerPlayer` (was DeployerFakePlayer); `start()` locals re-checked (Vec3 ordinal 1 = rayOrigin) |
| mixin/compatibility/create/funnels/FunnelBlockMixin | ported | `entityInside` now has (…, InsideBlockEffectApplier, boolean); VecHelper -> `com.zurrtum.create.catnip.math.VecHelper` |
| mixin/compatibility/create/sails_providing_lift/SailBlockMixin | ported | unchanged |
| mixin/compatibility/create/entity_falls_on_block/BasinBlockMixin | retargeted | `updateEntityAfterFallOn` -> 1.21.11 `updateEntityMovementAfterFallOn` (2 blockPosition calls, both redirected as upstream) |
| mixin/compatibility/create/entity_falls_on_block/BeltMillstoneBlocksMixin | retargeted | same rename; `BlockEntry.has(state)` -> `state.is(AllBlocks.BELT / MILLSTONE)` |
| mixin/compatibility/create/entity_falls_on_block/SawBlockMixin | retargeted | same rename |
| mixin/compatibility/create/entity_falls_on_block/SeatBlockMixin | retargeted | same rename |
| mixin/compatibility/create/mechnical_arm/MechanicalArmBlockEntity | ported | `isAreaActuallyLoaded(BlockPos,int)` unchanged |
| mixin/compatibility/create/mechnical_arm/MechanicalArmSublevelFailure | ported (client) | `ArmInteractionPointHandler` is client-only in Create Fly and `flushSettings` is now `(LocalPlayer, BlockPos)`; LangBuilder -> `com.zurrtum.create.client.catnip.lang.LangBuilder`; LVT names `point`/`removed` verified |
| mixinhelper/compatibility/create/mechanical_arm/MechanicalArmDuck | ported | unused upstream too (dead code kept) |
| mixinhelper/compatibility/create/mechanical_arm/PointHolder | ported | unused upstream; `NbtUtils.write/readBlockPos` -> `tag.store/read("pos", BlockPos.CODEC)` (same int-array format) |
| mixinterface/compatibility/create/ArmTargetUpdater | ported | unused upstream too |
| mixin/compatibility/create/ejector/EjectorBlockEntityMixin | ported | shadows updated: `launcher` and `getFacing()` are public in Create Fly; `getNormal()` -> `getUnitVec3i()`, `isClientSide` -> `isClientSide()` |
| mixin/compatibility/create/ejector/EjectorBlockMixin | retargeted | method renamed to `updateEntityMovementAfterFallOn`; single distanceTo / add(Vec3) calls verified |
| mixinhelper/compatibility/create/ejector/SubLevelScanResult | ported | |
| mixin/compatibility/create/airflow/AirCurrentMixin | ported | `tickAffectedEntities(Level)` LVT re-checked (flow, acceleration=float#2, previousMotion, maxAcceleration=float#3) |
| mixin/compatibility/create/airflow/FanProcessingTypeMixin | ported | static interface `getAt(Level, BlockPos)` unchanged |
| mixin/compatibility/create/fans_provide_force/EncasedFanBlockEntityMixin | ported | no member clashes with Create Fly's EncasedFanBlockEntity |
| mixin/compatibility/create/particles/AirFlowParticleMixin | ported (client) | class moved to `com.zurrtum.create.client.infrastructure.particle`; AABB.contains #0, NEW Vec3(DDD) #0, getAirCurrent #1 (after `motion` store) verified |
| mixin/compatibility/create/particles/AirParticleMixin | ported (client) | class moved to client package; ctor gained trailing `RandomSource` (TAIL handler updated); AirParticleData -> `com.zurrtum.create.infrastructure.particle.AirParticleData` |

### Priority 2: basins, chutes/inventories, fluids, hose pulleys, nozzles, boilers, redstone links/contacts

| upstream (under `neoforge/`) | status | notes |
|---|---|---|
| mixin/compatibility/create/basin_interactions/BasinBlockEntityMixin | ported | same hooks: `getHeatLevel` @ INVOKE `getHeatLevelOf` (AFTER), `*` @ `Level.getBlockEntity` (5 sites in Create Fly) |
| mixin/compatibility/create/basin_interactions/BasinOperatingBlockEntityMixin | ported | `tick` HEAD, `getBasin` @ `Level.getBlockEntity` |
| mixin/compatibility/create/blaze_burner/BlazeBurnerBlockEntityMixin | retargeted -> `blaze_burner.BlazeBurnerRendererMixin` (client) | Create Fly moved `BlazeBurnerBlockEntity#tickAnimation` to static client `BlazeBurnerRenderer.tickAnimation(BlazeBurnerBlockEntity)`; same INVOKE_ASSIGN `LocalPlayer.getZ()` + named locals `x`/`z`/`player` (LVT present, verified with javap -l) |
| mixin/compatibility/create/chain_conveyor/ChainConveyorBlockEntityMixin | ported | `drop` is private in Create Fly -> private `@Shadow`; `next` local verified |
| mixin/compatibility/create/chain_conveyor/ChainConveyorBlockMixin | ported | `BlockSubLevelAssemblyListener` impl, unchanged |
| mixin/compatibility/create/crushing_wheel_entity_processing/CrushingWheelControllerBlockEntityMixin | ported | `tick` Entity.getBoundingBox/getX/getY/getZ (all on `processingEntity`) |
| mixin/compatibility/create/inventory_manipulation/CapManipulationBehaviourBaseMixin | retargeted | NeoForge `Level#getCapability` is gone: Create Fly calls abstract `getCapability(Level, BlockPos, BlockEntity, Direction)`; WrapOperation swaps in the sub-level pos. BE search now only accepts a non-null BE passing the filter and otherwise keeps the original pos (upstream could leave a stale pos for block-only inventories, e.g. composters) |
| mixin/compatibility/create/inventory_manipulation/ChuteBlockEntityMixin | retargeted | `grabCapability` returns `Container`; sub-level lookup uses `ItemHelper.getInventory(level, pos, dir)` instead of `Capabilities.ItemHandler.BLOCK` |
| mixin/compatibility/create/hose_pulley/HosePulleyBlockEntityMixin | ported | `lazyTick`/`tick`/`onSpeedChanged` @ `Level.getBlockState` (1 each) |
| mixin/compatibility/create/hose_pulley/HosePulleyFluidHandlerMixin | retargeted | Create Fly's handler is slot based (`SidedFluidInventory`): `drainInternal` + `getFluidInTank` -> `getStack(int)` (slot 1 pulls/drains). HEAD inject (slot 1 only) updates the last valid pos; wraps `getDrainableFluid` + both `pullNext` calls in `getStack` |
| mixin/compatibility/create/fluid_handling/OpenEndedPipeMixin | ported | `outputPos` is `final` in Create Fly -> `@Shadow @Final @Mutable`; `<init>` selector pinned to `(BlockFace)V` (Create Fly adds a private codec constructor that delegates to it); `provideFluidToSpace` setBlock ordinal 1 verified; unused `getPos` shadow dropped |
| mixin/compatibility/create/fluid_handling/PipeConnectionMixin | retargeted -> `fluid_handling.PipeParticleDistanceMixin` (client) | `PipeConnection#isRenderEntityWithinDistance` moved to client `AllHandle#isRenderEntityWithoutDistance(Minecraft, BlockPos)` (private static, inverted result, same `Vec3.distanceTo`) |
| mixin/compatibility/create/fluid_tank_heating/BoilerDataMixin | ported | FIELD `ticksUntilNextSample` ordinal 0 (the `--` getfield after the heat update) verified; `-1` -> `BoilerHeater.NO_HEAT` |
| mixin/compatibility/create/flywheel/FlywheelBlockEntityMixin | ported (write retargeted) | Create Fly's `FlywheelBlockEntity` doesn't override `write`, so the mixin adds `write(ValueOutput, boolean)` (calls super, stores `SmoothedSpeed`); `read` TAIL inject uses `getFloatOr` |
| mixin/compatibility/create/vertical_gearbox/VerticalGearboxItemMixin | ported | `updateCustomBlockEntityTag` (vanilla BlockItem override) @ `Player.getDirection` |
| mixin/compatibility/create/redstone_links/RedstoneLinkNetworkHandlerMixin | ported | both `withinRange` calls in `updateNetworkOf` |
| mixin/compatibility/create/nozzle/NozzleBlockEntityAccessor | ported | `@Accessor(value = "range", remap = false)` |
| mixin/compatibility/create/nozzle/block_entity/NozzleBEFixesMixin | ported | `VecHelper.getCenterOf` in tick/lazyTick/canSee, `Mth.clamp(III)`, `addParticle` with Vec3 locals ordinal 0 (`center`) / 1 (`start`) verified |
| mixin/compatibility/create/nozzle/block_entity/NozzleHoveringMixin | ported | Create Fly constructor is `(BlockPos, BlockState)` (no type param) -> handler args changed |
| mixin/compatibility/create/nozzle/block_entity/ValidNozzledirectionMixin | ported | |
| mixin/compatibility/create/redstone_contacts/AllBlockEntityTypesMixin | retargeted | Registrate `BlockEntityEntry` field -> `@Inject <clinit> TAIL` registering `create:redstone_contact` (`Registry.register` + `FabricBlockEntityTypeBuilder`); result kept in `RedstoneContactBlockEntity.TYPE` |
| mixin/compatibility/create/redstone_contacts/RedstoneContactBlockMixin | ported | adds `IBE<RedstoneContactBlockEntity>`; `getBlockEntityType()` returns `RedstoneContactBlockEntity.TYPE` (no `new AllBlockEntityTypes()` duck hack); server-only ticker |
| mixinhelper/compatibility/create/nozzles/NozzleHoveringHelper | ported | `AllBlockEntityTypes.ENCASED_FAN.get()` -> field; `Direction#getNormal` -> `getUnitVec3i` |
| mixinterface/compatibility/create/NozzleBlockEntityExtension | ported | |
| mixinhelper/compatibility/create/redstone_contact/RedstoneContactBlockEntity | ported | extra `(BlockPos, BlockState)` factory ctor using `TYPE`; `AllBlocks.X.has(s)` -> `s.is(AllBlocks.X)`; `addBehaviours(List<BlockEntityBehaviour<?>>)` |
| mixinhelper/compatibility/create/redstone_contact/RedstoneContactBlockEntityTypeGetter | dropped | Registrate duck interface; replaced by the static `RedstoneContactBlockEntity.TYPE` |

### Priority 3: render fixes

| upstream (under `neoforge/`) | status | notes |
|---|---|---|
| mixin/compatibility/create/render_fixes/AABBOutlineMixin | ported | -> `com.zurrtum.create.client.catnip.outliner.AABBOutline`; `render` gained a leading `Minecraft` param; `@ModifyArg` on `AABBOutline.renderBox(PoseStack,SuperRenderTypeBuffer,Vec3,AABB,Vector4f,IZ)V` verified in bytecode; dropped the unused `@Shadow render` |
| mixin/compatibility/create/render_fixes/BeltRendererMixin | skipped (superseded) | Create Fly's `BeltRenderer.renderItem` compares `state.camera` (the `cameraPos` given to `extractRenderState`) with the item's plot position; Sable's `BlockEntityRenderDispatcherMixin` already swaps that camera to plot space for sub-level BEs, so the distance is correct; re-projecting would break when the plot-space camera falls outside the plot |
| mixin/compatibility/create/render_fixes/BlockClusterOutlineMixin | ported | `render` gained `Minecraft` param; `<init>(Iterable)` TAIL + `@ModifyVariable` on the single `Vec3` arg unchanged |
| mixin/compatibility/create/render_fixes/ChainConveyorRendererMixin | skipped (superseded) | `renderChains` is gone; the mip distance check (`cameraPos.closerThan`) moved to `ChainConveyorRenderer.getChainsRenderState(.., cameraPos)` called from `extractRenderState`, whose camera Sable already puts in plot space |
| mixin/compatibility/create/render_fixes/ChasingAABBOutlineMixin | ported | `render` gained `Minecraft` param; `@ModifyArg` target `ChasingAABBOutline.renderBox(...)` (owner is ChasingAABBOutline in bytecode) |
| mixin/compatibility/create/render_fixes/ChasingAABBOutlinerMixin | ported | `private static interpolateBBs(AABB,AABB,F)AABB` unchanged |
| mixin/compatibility/create/render_fixes/FilteringRendererMixin | skipped (superseded) | `renderOnBlockEntity` is gone; `FilteringRenderer.getFilterRenderState(.., double distance)` gets the distance from callers' `extractRenderState` (`cameraPos.distanceToSqr(center)` in SmartBlockEntityRenderer/DeployerRenderer/SawRenderer), already plot-space for sub-level BEs |
| mixin/compatibility/create/render_fixes/GhostBlockValueBoxMixin | ported | target `com.zurrtum.create.client.catnip.ghostblock.GhostBlockRenderer$TransparentGhostBlockRenderer.render(Minecraft,PoseStack,SuperRenderTypeBuffer,Vec3,GhostBlockParams)`; first `PoseStack.translate(DDD)V` (ordinal 0) is still the `pos - camera` one; `Camera.getPosition()` -> `position()` |
| mixin/compatibility/create/render_fixes/LineOutlineMixin | ported | adds the `bufferCuboidLine(PoseStack,VertexConsumer,Vec3,Vector3d,Vector3d,F,Vector4f,IZ)V` override to `LineOutline` (signature identical in Create Fly's `Outline`); now transforms copies of the end points instead of mutating the outline's own `start`/`end` vectors (upstream left them in world space after the first frame, so lines stopped following a moving sub-level until the next `set`) |
| mixin/compatibility/create/render_fixes/LinkRendererMixin | skipped (superseded) | `renderOnBlockEntity` is gone; `LinkRenderer.getLinkRenderState(.., distance)` gets the distance from `SmartBlockEntityRenderer.extractRenderState` (plot-space camera for sub-level BEs) |
| mixin/compatibility/create/render_fixes/OutlineMixin | skipped | empty mixin upstream (no members, no behaviour) |
| mixin/compatibility/create/render_fixes/PlacementClientMixin | ported | `drawDirectionIndicator(GuiGraphics,FFFF)V` (private static); redirect `com/zurrtum/create/catnip/math/VecHelper.getCenterOf(Vec3i)Vec3` (single call) |
| mixin/compatibility/create/render_fixes/SafeBlockEntityRendererMixin | skipped (target removed) | Create Fly has no `SafeBlockEntityRenderer`/`shouldCullItem` (BE renderers moved to render states; no item frustum culling left to fix) |
| mixin/compatibility/create/render_fixes/ValueBoxMixin | ported | `com.zurrtum.create.client.foundation.blockEntity.behaviour.ValueBox.render(Minecraft,PoseStack,SuperRenderTypeBuffer,Vec3,F)`; first `PoseStack.translate(DDD)V` is still the `pos - camera` one; no subclass overrides `render` |
| mixinhelper/compatibility/create/renderers/AABBOutlineRenderingOptions | ported (by coordinator) | `dev.ryanhcode.sable.fabric.mixinhelper.compatibility.create.renderers`; implemented by AABBOutlineMixin, used by D1's DeployToolMixin |

### Priority 3: Flywheel embedding (Create Fly bundled Flywheel)

| upstream (under `neoforge/`) | status | notes |
|---|---|---|
| mixin/compatibility/flywheel/AbstractBlockEntityVisualMixin | ported (extended) | -> create/flywheel_lib. Redirect of `LevelRenderer.getLightColor(BlockAndTintGetter,BlockPos)` now also covers the `relight(BlockPos, Iterator)`/`relight(BlockPos, Iterable)` overloads and `computePackedLight()` (same call; e.g. belt visuals relight via Iterable). Upstream only patched `relight(BlockPos, FlatLit...)` |
| mixin/compatibility/flywheel/BlockEntityStorageMixin | ported | `createRaw` WrapOperation of `BlockEntityVisualizer.createVisual` unchanged. Upstream *overwrote* `remove`/`recreateAll` (mixin methods without @Overwrite replace the target's, dropping Flywheel's `posLookup` bookkeeping) -> now `@Inject(HEAD)` so `posLookup` stays correct (Create Fly uses it for crumbling + Sable's sky-light relight) |
| mixin/compatibility/flywheel/EmbeddedEnvironmentMixin | ported | `setupDraw(GlProgram)` / `flush(long)` TAIL, `poseComposed` identical in Create Fly |
| mixin/compatibility/flywheel/EngineImplMixin | ported | ctor `(LevelAccessor, DrawManager, int)` TAIL; replaces `lightStorage` with SableFlywheelLightStorage |
| mixin/compatibility/flywheel/EnvironmentStorageMixin | ported | ModifyArg `CpuArena.<init>(JI)V` in `<init>` (field initializer) -> INFO_SIZE_BYTES (192) |
| mixin/compatibility/flywheel/LightStorageAccessor | ported | `collector`, `needsLutRebuild` |
| mixin/compatibility/flywheel/MatrixBufferMixin | ported | ModifyArg `ResizableStorageArray.<init>(J)V` (was 112); dropped `@Debug(export)` |
| mixin/compatibility/flywheel/RenderDispatcherImplMixin | ported | `VisualizationManagerImpl$RenderDispatcherImpl.onStartLevelRender` HEAD, shadows synthetic `this$0` |
| mixin/compatibility/flywheel/ShaderSourcesMixin | retargeted | upstream was empty (relied on NeoForge mod resource order to override `assets/flywheel/...`). Now targets `ShaderSources$SourceFinder#readResource(Identifier, Resource)` (HEAD ModifyVariable): every flywheel shader read checks `sable:flywheel_overrides/<path>` first |
| mixin/compatibility/flywheel/VisualManagerImplMixin | ported | `framePlan(VisualizationContext)` HEAD, `storage` |
| compatibility/flywheel/FlywheelCompatNeoForge | ported | -> `fabric.compatibility.create.flywheel.FlywheelCompat`; `FLYWHEEL_LOADED` = `create` loaded |
| compatibility/flywheel/SableFlywheelEmbeddingUniforms | ported | |
| compatibility/flywheel/SableFlywheelLightStorage | ported | imports only (Create Fly LightStorage == Flywheel 1.0 LightStorage) |
| compatibility/flywheel/SableFlywheelMatrixBuffer | ported | |
| compatibility/flywheel/SableLightLut | ported | verbatim |
| mixinhelper/compatibility/flywheel/SubLevelEmbedding | ported | -> mixinhelper.compatibility.create.flywheel |
| mixinterface/compatibility/flywheel/BlockEntityStorageExtension | ported | -> mixinterface.compatibility.create.flywheel |
| mixinterface/compatibility/flywheel/EmbeddedEnvironmentExtension | ported | -> mixinterface.compatibility.create.flywheel |
| mixin/compatibility/create/contraptions/ContraptionVisualMixin | ported | `ContraptionVisual` is `com.zurrtum.create.client.content.contraptions.render`; HEAD-cancel of private `setEmbeddingMatrices(F)V`. `entity.applyLocalTransforms(ps, pt)` -> `this.transform(ps, pt)` (Create Fly moved it into the visual; overridden by Oriented/ControlledContraptionVisual) |
| mixin/compatibility/create/contraptions/VisualizationEventHandlerMixin | ported | `onEntityJoinLevel(Level, Entity)` TAIL (called by Create Fly's ClientLevelMixin on `addEntity`) |
| mixin/compatibility/create/trains/CarriageContraptionVisualMixin | ported | Create Fly: `CarriageContraptionVisual extends OrientedContraptionVisual` (mixin superclass changed accordingly); Redirect `PoseTransformStack.translate(Vector3fc)` in private `animate(F)V` (1 occurrence); dropped the unused `@Local Vector3f` |
| resources/assets/flywheel/** (10 shaders + explanation.md) | ported | -> `sable/src/main/resources/assets/sable/flywheel_overrides/{internal,light}/...` + explanation.md. Base = Create Fly's copy (identical to upstream Flywheel except fog: `flw_distance`/`fogDistance` -> `flw_sphericalDistance`/`flw_cylindricalDistance`), Sable's scene-ID changes applied. api_impl.glsl, light_lut.glsl, indirect/{main.vert,matrices.glsl}, instancing/main.vert, light/*.glsl == Sable's files verbatim; common.vert + api_impl.frag merged (Create Fly fog + Sable lines) |

### Priority 4: schematics, display links, lectern controller, toolboxes, stock ticker, symmetry wand, zapper

| upstream (under `neoforge/`) | status | notes |
|---|---|---|
| mixin/compatibility/create/schematics/DeployToolMixin | ported | client. `renderTool` gained a `Minecraft` param (selector by name); single `PoseStack.translate(DDD)V`; unused `@Local` int/Vec3 captures dropped |
| mixin/compatibility/create/schematics/SchematicAndQuillHandlerMixin | ported | client (`client.content.schematics.client`). `tick()` -> `tick(Minecraft)`; `outlineSlot` shadow now `@Final` (final in Create Fly) |
| mixin/compatibility/create/schematics/SchematicExportMixin | ported | `fillFromWorld(.., Block)` -> `(.., List<Block>)`; `putUUID` -> `store("uuid", UUIDUtil.CODEC, ..)` (same int-array NBT) |
| mixin/compatibility/create/schematics/SchematicHandlerMixin | ported | client. `setupRenderer(Minecraft)`; wraps all 3 `placeInWorld`; `@Local Level` = `clientWorld` (LVT) |
| mixin/compatibility/create/schematics/SchematicLevelMixin | ported | `com.zurrtum.create.catnip.levelWrappers.SchematicLevel` (common) |
| mixin/compatibility/create/schematics/SchematicPlacePacketMixin | retargeted | `SchematicPlacePacket.handle(ServerPlayer)` is now `AllHandle.onSchematicPlace(ServerGamePacketListenerImpl, SchematicPlacePacket)` (static); same 3 points (after `SchematicPrinter.loadSchematic`, at `AllConfigs.server()`, TAIL); `this.stack` -> `packet.stack()`, `Level` local -> `ServerLevel world`; added a null guard for an unloaded printer |
| mixin/compatibility/create/schematics/SchematicPrinterMixin | ported | `loadSchematic` unchanged; TAIL + `@Local StructureTransform` verified (LVT) |
| mixin/compatibility/create/schematics/SchematicRendererMixin | ported | client. `drawLayer(RenderType)` -> `drawLayer(Minecraft, ChunkSectionLayer)`; block loop rewritten like Create Fly's: `ItemBlockRenderTypes.getChunkRenderType(state) == layer`, `BlockStateModel.collectParts` / `WrapperBlockStateModel.addPartsWithInfo`, `tesselateBlock(level, parts, state, pos, ps, sbb, true, NO_OVERLAY)` (no ModelData) |
| mixin/compatibility/create/schematics/SchematicToolBaseMixin | ported | client. `updateSelection()` -> `updateSelection(Minecraft)`; `RaycastHelper` is client-side in Create Fly |
| mixin/compatibility/create/schematics/SchematicTransformMixin | ported | client. ordinal 0 `PoseTransformStack.translate(Vec3)` (Create Fly's bundled flywheel) |
| mixin/compatibility/create/schematics/StructureTemplateMixin | ported | vanilla target (gated on create like upstream). `fillEntityList` gained a `ProblemReporter` param; `getList(k, TAG_COMPOUND)` -> `getListOrEmpty`, `getUUID` -> `read("uuid", UUIDUtil.CODEC)`, `getCompound` -> `getCompoundOrEmpty` |
| mixinterface/compatibility/create/schematics/SchematicLevelExtension | ported | |
| mixinterface/compatibility/create/schematics/SchematicPrinterExtension | ported | |
| mixinterface/compatibility/create/schematics/StructureTemplateExtension | ported | |
| mixin/compatibility/create/display_link/ClickToLinkBlockItemMixin | ported | |
| mixin/compatibility/create/display_link/DisplayLinkBlockEntityMixin | ported | |
| mixin/compatibility/create/display_link/DisplayLinkBlockMixin | ported | `BlockSubLevelAssemblyListener` |
| mixin/compatibility/create/toolbox/ToolBoxClientHandlerMixin | ported | client (`client.content.equipment.toolbox.ToolboxHandlerClient`); `method = "*"` hits `onPickItem` + `renderOverlay` |
| mixin/compatibility/create/toolbox/ToolboxHandlerMixin | ported | fixed upstream's missing `return` after the different-level check (matches Create's original) |
| mixin/compatibility/create/stock_ticker/EntityHatsMixin | retargeted | `EntityHats.getLogisticsHatFor` doesn't exist: Create Fly inlines it in its client `LivingEntityRendererMixin.addHat` (TAIL of `LivingEntityRenderer.extractRenderState`). Now a priority-1100 TAIL inject on the same method that re-does the logistics-hat check around the root vehicle when it is in a sub-level, via `HatState.create$getHat/setHat/updateHatInfo` |
| mixin/compatibility/create/stock_ticker/StockTickerInteractionHandlerMixin | ported | |
| mixin/compatibility/create/factory_panel/FactoryPanelConnectionHandlerMixin | ported | client. `clientTick()` -> `clientTick(Minecraft mc)` (mc captured as arg) |
| mixin/compatibility/create/wand_of_symmetry/SymmetryHandlerMixin | retargeted | `SymmetryHandler.onRenderWorld` -> client `SymmetryHandlerClient.onRenderWorld(Minecraft, PoseStack, MultiBufferSource, Vec3)`; ordinal 0 `translate(DDD)` unchanged; `pos` captured by type |
| mixin/compatibility/create/zapper/ZapperItemMixin | ported | `use` returns `InteractionResult` now; both `applyCooldown` calls, `LocalRef<BlockHitResult>` |
| mixin/compatibility/create/lectern_controller/LecternControllerBlockEntityMixin | ported | `user` is public in Create Fly; `((ServerLevel) level).getEntity` -> `level.getEntity(UUID)` |
| mixin/compatibility/create/lectern_controller/LecternControllerBlockMixin | ported | |
| mixinterface/compatibility/create/LecternControllerBlockEntityExtension | ported | |

### Priority 4: tracks, frogports, chain conveyors

| upstream (under `neoforge/`) | status | notes |
|---|---|---|
| mixin/compatibility/create/tracks/CurvedTrackDestroyPacketMixin | retargeted | packets are records in Create Fly, handled in `AllHandle`; same redirect of `TrackBlockEntity.getBlockPos()` (1 call) now in `AllHandle.lambda$onCurvedTrackDestroy$5` (was `CurvedTrackDestroyPacket.applySettings`) |
| mixin/compatibility/create/tracks/TrackBlockItemMixin | ported | `TrackBlockItem.useOn` + `TrackTargetingBlockItem.useOn`, 1 `Player.getLookAngle()` each |
| mixin/compatibility/create/tracks/TrackBlockMixin | ported | `TrackBlock.getStateForPlacement`, 1 `Player.getLookAngle()` |
| mixin/compatibility/create/tracks/TrackBlockOutlineMixin | ported | client class `client.content.trains.track.TrackBlockOutline`; methods now take `Minecraft` (`pickCurves(Minecraft)`, `drawCurveSelection(Minecraft, PoseStack, MultiBufferSource, Vec3)`, `drawCustomBlockSelection(Minecraft, BlockPos, MultiBufferSource, Vec3, PoseStack)`); ordinals re-verified (subtract 1/2 = localOrigin/localDirection, translate(DDD) ordinal 0); `@Local(name)` -> type/argsOnly locals. Small fix: the curve selection keeps the original 1/8 block lift (upstream dropped it on sub-levels) |
| mixin/compatibility/create/tracks/TrackGraphVisualizerMixin | ported (moved to client list) | target is client-only in Create Fly (`client.content.trains.graph.TrackGraphVisualizer.debugViewGraph(Minecraft, TrackGraph, boolean)`); 1 GETFIELD `TrackGraphBounds.box`, 1 `Vec3.distanceTo` |
| mixin/compatibility/create/tracks/TrackPlacementMixin | ported | `TrackPlacement.tryConnect(Level, Player, BlockPos, BlockState, ItemStack, boolean, boolean)`, 1 `getLookAngle` |
| mixin/compatibility/create/tracks/TrackTargetingClientMixin | retargeted | Create Fly no longer translates the pose stack; it bakes `pos - camera` into the `offset` of `TrackBlockRenderer.getRenderState(...)`. Now: wrap `getRenderState` (offset -> `Vec3.ZERO` on sub-levels) + wrap `TrackBlockRenderState.render(PoseStack, MultiBufferSource)` with push/translate(renderPos - camera)/rotate(sub-level)/pop, shared via `@Share`. Same final transform as upstream |
| mixin/compatibility/create/frogports/ChainConveyorBlockEntityMixin | ported | `exportToPort` (1 `FrogportBlockEntity.isBackedUp`), `tick` (2 `notifyPortToAnticipate`); the `portEntry` local is no longer captured, the wrapped call's `BlockPos` arg is the same key |
| mixin/compatibility/create/frogports/ChainConveyorInteractionHandlerMixin | retargeted | client class; `clientTick(Minecraft)` redirects unchanged (subtract ordinals 0/1, both distanceToSqr); the `@Overwrite` of `drawCustomBlockSelection` became a `@Redirect` of its only `PoseStack.translate(DDD)` (same result, keeps Create Fly's own `drawOutline` call) |
| mixin/compatibility/create/frogports/ChainConveyorRidingHandlerMixin | ported | client class; `clientTick(Minecraft)` subtract ordinal 1 = `targetPosition.subtract(playerPosition)`; `updateTargetPosition(Minecraft, ChainConveyorBlockEntity)`: `clbe` is now an argument |
| mixin/compatibility/create/frogports/ChainConveyorShapeAccessor | skipped | not needed: only used by the upstream overwrite, which became a redirect (Create Fly's method still calls the protected `drawOutline` itself) |
| mixin/compatibility/create/frogports/ChainPackageInteractionHandlerMixin | retargeted (moved to client list) | client class; Create Fly inlined `lambda$onUse$0` into `onUse(Minecraft)`; `ChainConveyorPackagePhysicsData` is now a top-level client class, captured as a local |
| mixin/compatibility/create/frogports/ChainPackageInteractionPacketMixin | retargeted | handler moved to `AllHandle.lambda$onChainPackageInteraction$16`; injects before `addLoopingPackage`/`addTravellingPackage`, packet values via the record accessors |
| mixin/compatibility/create/frogports/FrogportBlockEntityMixin | ported | `getYaw`, 1 `getExactTargetLocation` |
| mixin/compatibility/create/frogports/FrogportBlockMixin | ported | `lambda$setPlacedBy$0(BlockPos, LivingEntity, FrogportBlockEntity)` (same name), 1 `LivingEntity.position()`; BE captured by type |
| mixin/compatibility/create/frogports/FrogportRendererMixin | retargeted | render-state renderer: `renderSafe` -> `extractRenderState(FrogportBlockEntity, FrogportRenderState, float, Vec3, CrumblingOverlay)` (full descriptor: a bridge with the same name exists) |
| mixin/compatibility/create/frogports/FrogportVisualMixin | ported | client `FrogportVisual.animate(float)` |
| mixin/compatibility/create/frogports/PackagePortPlacementPacketMixin | retargeted | handler is `AllHandle.onPackagePortPlacement(ServerGamePacketListenerImpl, PackagePortPlacementPacket)`; player = `listener.player` |
| mixin/compatibility/create/frogports/PackagePortTargetMixin | ported | `PackagePortTarget$ChainConveyorFrogportTarget.export`, before `Set.contains` (only reached when `connection != null`, as upstream) |
| mixin/compatibility/create/frogports/PackagePortTargetSelectionHandlerMixin | ported (moved to client list) | client class; `@Overwrite(remap = false) validateDiff(Vec3, BlockPos)` same signature |
| mixin/compatibility/create/frogports/SmartBlockEntityRendererMixin | retargeted (+ new `frogports.NameplateRenderStateMixin`, `mixinterface/.../frogports/NameplateRenderStateExtension`) | `renderNameplateOnHover` is gone; Create Fly extracts `SmartBlockEntityRenderer.NameplateRenderState` (record) and vanilla orients the tag with `CameraRenderState.orientation` inside `submitNameTag`. Now: tag the returned record with the BE's sub-level (`@Inject` RETURN of `getNameplateRenderState`, `@Unique` field added to the record) and, in `NameplateRenderState.render`, wrap `submitNameTag` with orientation = conj(subLevel) * camera, restored after (upstream mutated the dispatcher's quaternion in place). No distance fix needed: Sable already passes a plot-space camera to sub-level BE extraction |
| mixinhelper/compatibility/create/frogports/FrogportMixinHelper | ported | unchanged logic |

## API mappings found while porting

- `ContraptionCollider.collideEntities` (upstream, both sides) -> Create Fly server `ContraptionCollider.collideEntities` + client `com.zurrtum.create.client.content.contraptions.ContraptionColliderClient.collideEntities` (local/remote player collision lives there).
- `ContraptionControlsRenderer.renderInContraption` -> `client...ContraptionControlsMovementRender#getRenderState(Vec3 camera, Font, MovementContext, VirtualRenderWorld, Matrix4f)`; movement renders are `MovementRenderBehaviour`/`MovementRenderState`.
- `com.simibubi.create.foundation.utility.RaycastHelper` -> client-only `com.zurrtum.create.client.foundation.utility.RaycastHelper`; `BigOutlines` -> `com.zurrtum.create.client.foundation.block.BigOutlines` (`pick(Minecraft)`).
- `MovementBehaviour` is an abstract class (`com.zurrtum.create.api.behaviour.movement.MovementBehaviour`), not an interface.
- `BlockEntityBehaviour` -> `com.zurrtum.create.api.behaviour.BlockEntityBehaviour<T extends SmartBlockEntity>`; `BehaviourType` stays in `foundation.blockEntity.behaviour`.
- `SmartBlockEntity#write/read(ValueOutput|ValueInput, boolean)` are protected; Create Fly BEs often don't override `write` (add an override in the mixin instead of injecting).
- Packet `handle` bodies moved into `com.zurrtum.create.AllHandle.on<Packet>(ServerGamePacketListenerImpl, <Packet>)` (static).
- `SuperGlueItem.spawnParticles(...)` client call -> `AllClientHandle.INSTANCE.spawnSuperGlueParticles(level, pos, dir, fullBlock)`; `CatnipServices.PLATFORM.executeOnClientOnly` not needed inside `level.isClientSide()` branches.
- `HarvesterRenderer.renderSafe/transform` -> `extractRenderState` fills `HarvesterRenderState.angle` (radians).
- `Player#canInteractWithEntity(Entity,double)` -> `Player#isWithinEntityInteractionRange(Entity,double)`; `Direction#getNearest` -> `getApproximateNearest`; `Direction#getNormal` -> `getUnitVec3i`; `NbtUtils.readBlockPos(tag,key)` -> `tag.read(key, BlockPos.CODEC)`; `getInt`/`getList(key, type)` -> `getIntOr`/`getListOrEmpty`.
- `Block#entityInside(BlockState, Level, BlockPos, Entity, InsideBlockEffectApplier, boolean)` in 1.21.11.
- `Block#updateEntityAfterFallOn(BlockGetter, Entity)` -> `updateEntityMovementAfterFallOn` (MC 1.21.11; Create Fly overrides use the new name).
- Registrate `BlockEntry#has(state)` -> `state.is(AllBlocks.X)` (AllBlocks fields are the blocks).
- `Direction#getNormal()` -> `getUnitVec3i()`; `NbtUtils.writeBlockPos/readBlockPos` -> `CompoundTag#store/read(key, BlockPos.CODEC)`.
- Create Fly BE renderers no longer query the camera: `extractRenderState(be, state, partialTick, Vec3 cameraPos, crumbling)` gets it (BeltRenderer stores it in `BeltRenderState.camera`), `submit(..., CameraRenderState)` passes `cameraState.pos` on (DepotRenderer.renderItemsOf/renderItem take `Vec3 cameraPos`).
- `ArmInteractionPointHandler` -> `com.zurrtum.create.client.content.kinetics.mechanicalArm`, `flushSettings(LocalPlayer, BlockPos)`.
- `DeployerHandler.activate(DeployerFakePlayer, ...)` -> `activate(DeployerPlayer, Vec3, BlockPos, Vec3, DeployerBlockEntity.Mode)`.
- `AirParticle`/`AirFlowParticle` -> `com.zurrtum.create.client.infrastructure.particle.*`; `AirParticle(ClientLevel, AirParticleData, x, y, z, dx, dy, dz, SpriteSet, RandomSource)`.
- `Level#getCapability(Capabilities.ItemHandler.BLOCK, pos, dir)` -> `ItemHelper.getInventory(level, pos, dir)` (returns `Container`, falls back to Fabric Transfer through `AllTransfer.getInventory`) (`com.zurrtum.create.foundation.item.ItemHelper`).
- `CapManipulationBehaviourBase#findNewCapability`: inventory comes from the abstract `getCapability(Level, BlockPos, BlockEntity, @Nullable Direction)` (InvManipulation/FluidManipulation implement it).
- `BlockEntityBehaviour#getWorld()` -> `getLevel()` (`getPos()` unchanged).
- `HosePulleyFluidHandler`: slot inventory, `getStack(1)` = pull/drain; fluids are `com.zurrtum.create.infrastructure.fluids.FluidStack`.
- `BlazeBurnerBlockEntity#tickAnimation` -> `com.zurrtum.create.client.content.processing.burner.BlazeBurnerRenderer.tickAnimation(be)` (static, client).
- `PipeConnection#isRenderEntityWithinDistance` -> `com.zurrtum.create.client.AllHandle#isRenderEntityWithoutDistance(Minecraft, BlockPos)`.
- Registrate block entity registration -> `Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, Identifier.fromNamespaceAndPath("create", id), FabricBlockEntityTypeBuilder.create(factory, blocks).build())`; vanilla `BlockEntityType(BlockEntitySupplier, Set)` and `BlockEntitySupplier` are inaccessible at compile time (Create Fly's and Fabric's AWs are not transitive). Create Fly registers its BE types in `AllBlockEntityTypes.<clinit>`, triggered by `AllBlockEntityTypes.register()`.
- `NozzleBlockEntity(BlockEntityType, BlockPos, BlockState)` -> `(BlockPos, BlockState)`; most Create Fly BEs drop the type ctor param.
- `SmartBlockEntity#write/read(CompoundTag, HolderLookup.Provider, boolean)` -> `write(ValueOutput, boolean)` / `read(ValueInput, boolean)` (Create-owned, remap = false).
- MC: `Direction.getNearest(double, double, double)` -> `Direction.getApproximateNearest(...)`; `Direction#getNormal()` -> `getUnitVec3i()`.
- `AllBlocks.X.has(state)` -> `state.is(AllBlocks.X)`; `AllBlockEntityTypes.X.get()` -> `AllBlockEntityTypes.X`.
- `SmartBlockEntity#tick/lazyTick`, `KineticBlockEntity#onSpeedChanged` are Create-owned (remap = false); `BlockItem#updateCustomBlockEntityTag` is vanilla (remap).
- catnip outliner -> `com.zurrtum.create.client.catnip.outliner.*`; `Outline.render(PoseStack, SuperRenderTypeBuffer, Vec3, float)` -> `render(Minecraft, PoseStack, SuperRenderTypeBuffer, Vec3, float)` (all outlines; ghost block renderers likewise gained a leading `Minecraft`).
- Outliner, ghost blocks, track/coupling/schematic overlays render in Create Fly's client `mixin/LevelRendererMixin.renderAfterParticles` frame pass with a fresh camera-relative `PoseStack`, same space as upstream's AFTER_PARTICLES stage, so `SublevelRenderOffsetHelper.posePlotToProjected` + `translation()` work unchanged.
- Create Fly BE renderers no longer read the main camera: distances use the `cameraPos` passed to `extractRenderState`, which Sable (`mixin/sublevel_render/BlockEntityRenderDispatcherMixin` + `VanillaSubLevelRenderDispatcher.extractSubLevelBlockEntities`) sets to `renderPose.transformPositionInverse(camera)` for sub-level BEs. Upstream "distanceTo with sub-levels" render redirects into BE renderers are therefore not needed (and would double-project).
- `SafeBlockEntityRenderer` (incl. `shouldCullItem`) does not exist in Create Fly.
- `VecHelper.getCenterOf` -> `com.zurrtum.create.catnip.math.VecHelper` (common); `projectToPlayerView` -> `com.zurrtum.create.client.catnip.math.VecHelper`.
- `Camera.getPosition()` -> `Camera.position()`.
- `dev.engine_room.flywheel.*` -> `com.zurrtum.create.client.flywheel.*` (client only); internals (EngineImpl, LightStorage, LightLut,
  EmbeddedEnvironment, EnvironmentStorage, MatrixBuffer, BlockEntityStorage, VisualManagerImpl, RenderDispatcherImpl) match Flywheel 1.0 for 1.21.1.
- Flywheel shaders: fog distance changed (`in float flw_distance` -> `flw_sphericalDistance` + `flw_cylindricalDistance`, `fogDistance(...)` -> `sphericalDistance/cylindricalDistance`); everything else Sable overrides is unchanged.
- `AbstractContraptionEntity#applyLocalTransforms(PoseStack, float)` (client) -> `ContraptionVisual#transform(PoseStack, float)` (Create Fly client).
- `CarriageContraptionVisual` superclass `ContraptionVisual` -> `OrientedContraptionVisual`.
- Flywheel shader resources must not rely on cross-mod resource order on Fabric: override via `ShaderSources$SourceFinder#readResource`.
- Create Fly's `VisualizationEventHandler.onEntityJoinLevel` is driven by its `ClientLevelMixin#addEntity` HEAD (NeoForge: EntityJoinLevelEvent).
- `SchematicPlacePacket.handle(ServerPlayer)` -> `com.zurrtum.create.AllHandle.onSchematicPlace(ServerGamePacketListenerImpl, SchematicPlacePacket)` (all c2s packet handlers live in `AllHandle`).
- `EntityHats.getLogisticsHatFor` -> gone; logic inlined in Create Fly's `client/mixin/LivingEntityRendererMixin.addHat`; per-entity hat state is `client.content.equipment.hats.HatState` on `LivingEntityRenderState`.
- `SymmetryHandler.onRenderWorld` -> `client.content.equipment.symmetryWand.SymmetryHandlerClient.onRenderWorld(Minecraft, PoseStack, MultiBufferSource, Vec3)`.
- Schematic client classes -> `com.zurrtum.create.client.content.schematics.client[.tools]`; their tick/update/render methods all gained a leading `Minecraft` param (`tick`, `updateSelection`, `renderTool`, `setupRenderer`, `drawLayer`).
- `RaycastHelper` -> `com.zurrtum.create.client.foundation.utility.RaycastHelper`; `Outliner`/`AABBOutline` -> `com.zurrtum.create.client.catnip.outliner`; `AllSpecialTextures` -> `com.zurrtum.create.client.AllSpecialTextures`; `SchematicLevel` stays common (`com.zurrtum.create.catnip.levelWrappers`).
- 1.21.11: `StructureTemplate.fillFromWorld(.., Block)` -> `(.., List<Block>)`; `StructureTemplate.fillEntityList(Level, BlockPos, BlockPos, ProblemReporter)`; `CompoundTag.putUUID/getUUID` -> `store/read(key, UUIDUtil.CODEC)`.
- `ModelBlockRenderer.tesselateBlock(level, BakedModel, state, pos, ps, vc, checkSides, random, seed, overlay, ModelData, RenderType)` -> `tesselateBlock(level, List<BlockModelPart>, state, pos, ps, vc, checkSides, overlay)`; layer filter `ItemBlockRenderTypes.getChunkRenderType(state)` (a `ChunkSectionLayer`).
- `Item.use` returns `InteractionResult` (was `InteractionResultHolder<ItemStack>`).
- Create Fly packets are records whose `handle` calls `AllHandle.onX(ServerGamePacketListenerImpl, packet)`; `BlockEntityConfigurationPacket.applySettings` bodies are lambdas `AllHandle.lambda$onX$N(...)` passed to `onBlockEntityConfiguration` (`lambda$onCurvedTrackDestroy$5`, `lambda$onChainPackageInteraction$16`). Player = `listener.player`.
- Client handlers moved to `com.zurrtum.create.client.*` and take `Minecraft mc` (`ChainConveyorInteractionHandler.clientTick(Minecraft)`, `ChainConveyorRidingHandler.clientTick(Minecraft)`, `ChainPackageInteractionHandler.onUse(Minecraft)`, `TrackBlockOutline.pickCurves(Minecraft)`, `TrackGraphVisualizer.debugViewGraph(Minecraft, TrackGraph, boolean)`, `TrackTargetingClient.render(Minecraft, PoseStack, SuperRenderTypeBuffer, Vec3)`).
- `ChainConveyorPackage.ChainConveyorPackagePhysicsData` -> `com.zurrtum.create.client.content.kinetics.chainConveyor.ChainConveyorPackagePhysicsData`.
- `TrackTargetingBehaviour.render(...)` -> `AllTrackRenders.get(track).getRenderState(level, offset, state, pos, dir, bezier, type, scale).render(ms, buffer)`; `offset` (= pos - camera) is applied first in `TrackBlockRenderState.transform`.
- `SmartBlockEntityRenderer.renderNameplateOnHover` -> `getNameplateRenderState(be, pos, cameraPos, tag, yOffset, light)` + `NameplateRenderState.render(PoseStack, SubmitNodeCollector, CameraRenderState)` -> vanilla `NameTagFeatureRenderer.Storage.add` uses `cameraRenderState.orientation` immediately.
- `FrogportRenderer.renderSafe` -> `extractRenderState` (typed + bridge overload; give the descriptor).
- The remapped Create Fly jar keeps LocalVariableTables, so MixinExtras `@Local(name = ...)` works; prefer type/argsOnly anyway.

## PORT-TODOs / caveats

- (verify in game) contraption-controls floor text on a contraption that sits on a sub-level: upstream also swapped the view projection; Create Fly renders it with the actor's model pose so it should already follow the sub-level.
- (kept upstream quirk) sticker `sable$saveToContraption` saves once with the constraint suppressed and discards the result, then the original save still writes `SableConstraint` into the contraption NBT; `read` drops it again when the position changes.
- (production only, not B1-specific) `@At` targets on Create-owned members whose descriptors contain MC types (`DeployerHandler.activate(...Vec3...BlockPos...)`, `BeltRenderState.camera:Vec3`) use `remap = false`; in an intermediary-mapped production runtime those descriptors would need refmap remapping. Dev runtime (named) is fine; verify the generated refmap if a production jar is ever built.
- Untested in game: needs a client run with Create Fly on the classpath (contraption/BE visuals on a sub-level, light + sky scale, `/flywheel` backend instancing and indirect).
- Production-jar note for the coordinator (applies to all forks): `remap = false` selectors whose descriptors contain MC types (here `relight(Lnet/minecraft/core/BlockPos;...)`, `createRaw(...BlockEntity...)`, `remove(...BlockEntity)`, and the `createVisual` @At) are only correct in the Mojang-named dev runtime; a remapped intermediary jar would need these descriptors remapped (remap = true lets Loom remap the descriptor). Dev/e2e unaffected.
