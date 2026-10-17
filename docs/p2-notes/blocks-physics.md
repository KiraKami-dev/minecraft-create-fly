# p2 notes: blocks-physics

Scope: `simulated/.../content/blocks/{rope,spring,docking_connector,merging_glue,swivel_bearing,physics_assembler}`
(rope winch/connector + strands, springs, docking connectors, merging glue, swivel bearing, physics assembler).
0 compile errors in these files against the current tree (also checked with `-XDshould-stop.ifError=FLOW`).
Physics/assembly logic is unchanged from upstream; only API call sites moved.

## Client registration (orchestrator: call from the client entrypoint)
- `dev.simulated_team.simulated.content.blocks.swivel_bearing.SwivelBearingClient#registerClient()`: locking-setting
  value box (`LockingScrollBehaviour`) and `SwivelBearingTooltipBehaviour` for `SimBlockEntityTypes.SWIVEL_BEARING`.
  The tooltip behaviour **replaces** the default `KineticTooltipBehaviour` (same `TooltipBehaviour.TYPE`, last
  registration wins), so call it after the default kinetic behaviours, or register only `KineticAudioBehaviour` for
  `SWIVEL_BEARING` in `SimBlockEntityBehaviours`.
- `dev.simulated_team.simulated.content.blocks.physics_assembler.PhysicsAssemblerTooltipBehaviour#registerClient()`:
  assembly-exception goggle tooltip for `SimBlockEntityTypes.PHYSICS_ASSEMBLER`.
- Renderers (all `(BlockEntityRendererProvider.Context)`): `RopeWinchRenderer`, `RopeConnectorRenderer`,
  `SpringRenderer`, `DockingConnectorRenderer`, `MergingGlueRenderer`, `SwivelBearingRenderer`,
  `SwivelBearingPlateBlockRenderer`, `PhysicsAssemblerRenderer`. Visuals as upstream: `SwivelBearingVisual::new`,
  `OrientedRotatingVisual.of(SimPartialModels.SHAFT_SIXTEENTH)` for the link block.

## Block API (1.21.11)
- `Block.onRemove` is gone. Overrides that only called `IBE.onRemove` are deleted: Create Fly's
  `SmartBlockEntity.preRemoveSideEffects` calls `destroy()` (server, block type changed; same cases as before since
  1.21.1 `onRemove` was server-only too). Custom logic (`DockingConnectorBlock`) is split into
  `affectNeighborsAfterRemoval(oldState, ServerLevel, pos, movedByPiston)` (block changed), `onPlace(newState, level,
  pos, oldState, movedByPiston)` (same block, new state: gets the old state) and the block entity's
  `preRemoveSideEffects(pos, oldState)` for dropping its inventory (vanilla container pattern).
- `updateShape(state, dir, neighbor, LevelAccessor, pos, neighborPos)` ->
  `updateShape(state, LevelReader, ScheduledTickAccess, pos, dir, neighborPos, neighbor, RandomSource)`.
- `neighborChanged(..., BlockPos fromPos, boolean)` -> `(..., @Nullable Orientation, boolean)`;
  `getAnalogOutputSignal(state, level, pos)` -> `+ Direction`; `getCloneItemStack(level, pos, state)` -> `+ boolean includeData`.
- `Direction#getNormal()` -> `getUnitVec3i()`; `Direction.getNearest(x, y, z)` -> `getApproximateNearest(x, y, z)`.
- `Shulker.getProgressAabb(1, dir, p)` -> `getProgressAabb(1, dir, p, new Vec3(0.5, 0, 0.5))` (box is now built around
  x/z = 0; this offset gives the old 0..1 box, as vanilla `ShulkerBoxBlockEntity#getBoundingBox` does).
- `level.getMaxBuildHeight()` (exclusive) -> `level.getMaxY() + 1`; `getMinBuildHeight()` -> `getMinY()`.
- `level.getGameRules()` exists only on `ServerLevel`; `getBoolean(RULE_DOBLOCKDROPS)` -> `get(GameRules.BLOCK_DROPS)`
  (`net.minecraft.world.level.gamerules.GameRules`).
- `Create.RANDOM` does not exist in Create Fly -> `level.random` (as Create Fly's `IWrenchable#playRotateSound`).
- catnip `PlacementOffset#placeInWorld(level, item, player, hand, ray)` -> `(level, item, player, hand)`.
- Registrate `BlockEntry::has` method refs in a block's static init -> `state -> state.is(SimBlocks.X)` (lambda: the
  `SimBlocks` field is still null while that block class initializes).

## Block entities / behaviours
- `BlockEntityBehaviour` is generic (`BlockEntityBehaviour<SmartBlockEntity>`) and its `getLevel()` is public (a
  private redeclaration no longer compiles). `addBehaviours(List<BlockEntityBehaviour>)` -> `List<BlockEntityBehaviour<?>>`.
- Save/load keys unchanged. `putUUID/getUUID/hasUUID` -> `store/read(key, UUIDUtil.CODEC)`;
  `NbtUtils.write/readBlockPos` -> `store/read(key, BlockPos.CODEC)` (both the same int-array NBT); `contains(k) +
  getLong(k)` -> `view.getLong(k)` (`Optional`); codecs that went through `NbtOps` -> `view.store/read(key, CODEC)`.
- `AssemblyException.write(tag, registries, e)/read(tag, registries)` -> `write(view, e)/read(view)`.
- Multiloader containers (`SingleSlotContainer`, `SingleTank`, `SingleBattery`) still serialize to `CompoundTag`:
  `view.store("Inventory", CompoundTag.CODEC, inventory.write(level.registryAccess()))`, read with `view.lookup()`.
  (p2-platform also adds `write(ValueOutput)/read(ValueInput)` overloads; switching `DockingConnectorBlockEntity` to
  `inventory.write(view.child("Inventory"))` etc. would be equivalent once that branch is in.)
- Create's common `ScrollOptionBehaviour` -> `ServerScrollOptionBehaviour<E>(Class<E>, be)` on the block entity plus a
  client `ScrollOptionBehaviour<E>` subclass (label, slot, icons) attached through `CLIENT_REGISTRY`. Its `value` field
  is protected: `option.value = 1` -> `option.setValue(1)` (no side effects while constructing). `INamedIconOptions` /
  `AllIcons` are client-only, so the common enum drops its icons and the client maps them (`LockingSettingIcon`).
- `ValueBoxTransform#getLocalOffset(LevelAccessor, BlockPos, BlockState)` -> `getLocalOffset(BlockState)`.
- `IDisplayAssemblyExceptions`, `addToTooltip`, `addToGoggleTooltip` and `addStressImpactStats` live on client
  `TooltipBehaviour`s in Create Fly (`KineticTooltipBehaviour` for kinetic BEs; see `MechanicalBearingTooltipBehaviour`).
  The block entity keeps a public `getLastAssemblyException()`.
- `SimLang` (catnip `LangBuilder`) is client-only, so common classes use
  `Component.translatable("simulated." + key, args)`, `.color(c)` -> `.withColor(c)` (same component tree).
- Veil `CodecUtil.VECTOR3D_CODEC` -> `Vec3.CODEC.xmap(JOMLConversion::toJOML, JOMLConversion::toMojang)` (same list of
  three doubles; used for saved rope strands).

## Rendering (render-state model)
- `SafeBlockEntityRenderer` is gone: implement `BlockEntityRenderer<T, S>`; `renderSafe(be, pt, ms, buffers, light,
  overlay)` -> read everything from the block entity in `extractRenderState` (fill a `BlockEntityRenderState`
  subclass, `BlockEntityRenderState.extractBase` gives pos/state/light), then in `submit` call
  `queue.submitCustomGeometry(poseStack, renderType, renderer)`. The pose is copied at submit time, so push/translate
  before and pop after. PoseStack-heavy upstream code runs unchanged inside `render(PoseStack.Pose, VertexConsumer)`
  with `PoseStack ps = new PoseStack(); ps.last().set(pose);`.
- Meshes computed from live state (springs, merging glue strands) are computed in `extractRenderState` into a
  `FloatArrayList` and replayed in `render`.
- `SuperByteBuffer#renderInto(PoseStack, vc)` -> `renderInto(PoseStack.Pose, vc)`.
- `RenderType.solid()` -> `RenderTypes.solidMovingBlock()`, `cutout()` -> `cutoutMovingBlock()`, `lines()` ->
  `RenderTypes.lines()`, `entityCutout(id)` -> `RenderTypes.entityCutout(id)`.
- Line vertices need `.setLineWidth(w)`; the width 1.21.1's `RenderType.lines()` used is
  `Minecraft.getInstance().getWindow().getAppropriateLineWidth()`.
- `KineticBlockEntityRenderer<T>` -> `<T, S extends KineticRenderState>`; `getRotatedModel(be, state)` reads
  `state.blockState`; super's extract sets `state.support` (Flywheel active: draw nothing).
  `kineticRotationTransform(buf, be, axis, angle, light)` -> `buf.light(l).rotateCentered(angle,
  Direction.fromAxisAndDirection(axis, POSITIVE)).color(KineticBlockEntityRenderer.getColor(be))`;
  `renderRotatingBuffer` is the same with `getAngleForBe(be, pos, axis)`.
- `AbstractPulleyRenderer.scrollCoil(buf, shift, offset, speed)` -> `if (offset != 0)
  buf.shiftUVScrolling(shift, AbstractPulleyRenderer.getCoilVScroll(shift, offset, speed))`.
- `FilteringRenderer.renderOnBlockEntity` on a block entity without a filtering behaviour was a no-op: dropped.
- `shouldRenderOffScreen(be)` -> `shouldRenderOffScreen()`; `getRenderBoundingBox()` overrides are kept but nothing in
  1.21.11 / Create Fly reads them.
- GUI overlays: `graphics.pose()` is a `Matrix3x2fStack` (`pushMatrix/translate(x, y)/popMatrix`);
  `blit(id, x, y, u, v, w, h, tw, th)` -> `blit(RenderPipelines.GUI_TEXTURED, id, x, y, u, v, w, h, tw, th)`.

## Packets used only by these blocks (`network/packets`)
- `rope/ClientboundRopeDataPacket`, `rope/ClientboundRopeStoppedPacket`, `RopeBreakPacket`,
  `physics_assembler/*` compile unchanged.
- `RopeRidingPacket`: Create Fly's `ServerChainConveyorHandler.handleTTLPacket/handleStopRidingPacket(player)` take
  the server first -> `(ctx.server(), player)` (veillite `ServerPacketContext#server()`).
- `AssemblePacket` (physics assembler) is being fixed on p2-platform (`player.level()`,
  `isWithinBlockInteractionRange`); left alone here to avoid a conflict.

## Expectations on other areas
- `SimRenderTypes.spring(Identifier)` keeps the upstream vertex layout (Position, Color "Stress", UV0, UV2, Normal):
  `SpringRenderer` writes `setColor/setUv/setLight/setNormal` per vertex.
- `HoldTipBehaviour(SmartBlockEntity, MutableComponent)`, `MagnetBehaviour(SmartBlockEntity, MagnetMap)`,
  `SimAssemblyHelper`, `SimMovementContext`, `ExtraKinetics`, `BlockHoldInteraction#renderOverlay(GuiGraphics, int,
  int, boolean)`, `InteractCallback` and the rope / assembler packets are used with their upstream signatures.

## PORT-TODOs
- None.
