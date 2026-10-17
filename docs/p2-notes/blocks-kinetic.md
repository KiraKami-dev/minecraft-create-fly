# blocks-kinetic (branch `p2-blocks-kinetic`)

Packages: `content/blocks/{steering_wheel,torsion_spring,auger_shaft,directional_gearshift,analog_transmission,throttle_lever,handle,portable_engine,symmetric_sail,absorber,void_anchor,behaviour,util}`,
`content/blocks/SimKineticBlocksClient`, packets `SteeringWheelPacket`, `ThrottleLeverSignalPacket`, `UpdatePlayerUsingHandlePacket`, `handle/ClientboundPlayersHoldingHandlePacket`.

## R2: OBJ models (steering wheel)
- Create Fly already ships NeoForge's OBJ loader: `client/model/obj/ObjLoader`, hooked by `client/mixin/BlockModelMixin` (wraps `BlockModel.GSON`).
  Any model JSON (any namespace) with `"loader": "neoforge:obj"` is parsed by it, so the fix is resource-only:
  restored `loader`/`flip_v`/`model` (from `_obj_model`) in `models/block/steering_wheel/{wheel,item}.json`.
  Works for partial models (`SimPartialModels.STEERING_WHEEL.get()` is a `SimpleModelWrapper`) and for the item model chain.
  Offroad's mecanum/plunger OBJs can be fixed the same way (move `_obj_model` back to `model`, add `"loader": "neoforge:obj", "flip_v": true`).
- `tools/validate_resources.py` now accepts `neoforge:obj` (one-line change).
- R5 hint: Create Fly also reads `render_type` from ordinary block model JSON (`client/mixin/BlockModelDeserializerMixin` -> `LayerUnbakedModel`),
  so the stripped `render_type` keys can simply be restored. Done for `throttle_lever/button.json` (cutout).

## API mappings (1.21.1 / Create -> 1.21.11 / Create Fly)
- Value boxes are split: server `ServerScrollValueBehaviour`/`ServerScrollOptionBehaviour` stays in the BE (`between`, `setValue`, `getValue`,
  `withCallback`); client `ScrollValueBehaviour<B, ServerBehaviour>` (label, `ValueBoxTransform`, `createBoard`, formatter, `onlyActiveWhen`, `testHit`)
  is registered through `BlockEntityBehaviour.CLIENT_REGISTRY` (`SimKineticBlocksClient`). Client behaviour finds the server one by `ServerScrollValueBehaviour.TYPE`.
  Examples: `SteeringWheelScrollValueBehaviour`, `TorsionSpringScrollValueBehaviour`, portable engine uses Create Fly's public
  `RotationDirectionScrollBehaviour(be, title, slot)` with `PortableEngineValueBoxTransform`.
- Clamp-on-read for a scroll value: subclass `ServerScrollValueBehaviour` and clamp the protected `value` in `read` (`SteeringWheelBlockEntity.AngleLimitBehaviour`).
- `ValueBoxTransform` (client): `getLocalOffset(level,pos,state)` -> `getLocalOffset(state)`, `rotate(level,pos,state,ms)` -> `rotate(state,ms)`; `testHit(level,pos,state,hit)` unchanged.
- Goggle/hover text: BEs no longer implement `IHaveGoggleInformation`/`addToGoggleTooltip`/`addToTooltip`; Create Fly reads `TooltipBehaviour.TYPE`
  client behaviours. Subclass `KineticTooltipBehaviour`/`GeneratingKineticTooltipBehaviour`/`TooltipBehaviour implements IHaveGoggleInformation`
  (`AugerShaftTooltipBehaviour`, `AnalogTransmissionTooltipBehaviour` (overrides `addToTooltip`), `PortableEngineTooltipBehaviour`, `ThrottleLeverTooltipBehaviour`).
- `KineticBlockEntity#tickAudio` -> client `KineticAudioBehaviour#tickAudio` (`SteeringWheelAudioBehaviour`).
- `IHaveBigOutline` -> `com.zurrtum.create.client.AllExtensions.BIG_OUTLINE.add(block)` (client).
- `SimLang`/`CreateLang`/catnip `LangBuilder` are client-only (import `net.minecraft.client.*`): in common code use `Component.translatable("simulated." + key)`.
- `SafeBlockEntityRenderer`/`renderSafe` -> `BlockEntityRenderer<T, S>`: `createRenderState`, `extractRenderState(be, state, pt, cameraPos, crumbling)`
  (copy everything you need, incl. partial ticks results), `submit(state, poseStack, SubmitNodeCollector, camera)` ->
  `queue.submitCustomGeometry(poseStack, renderType, (pose, consumer) -> ...)`. Apply `SuperByteBuffer` transforms inside the geometry callback
  (buffers are cached/shared; `renderInto` resets them). `renderInto(PoseStack, ...)` -> `renderInto(PoseStack.Pose, ...)`.
  Several layers = several `submitCustomGeometry` calls (`PortableEngineRenderer`). The pose is copied at submit time, so you can push/transform
  the `PoseStack` around a submit (`ThrottleLeverRenderer#renderOutline`).
- Kinetic renderers: extend `KineticBlockEntityRenderer<T, S extends KineticRenderState>`, `updateBaseRenderState`, `getRotatedModel(be, state)`,
  `getAngleForBe`, `getColor(be)`; `kineticRotationTransform(buf, be, axis, angle, light)` = `light; rotateCentered(angle, positive(axis)); color(getColor(be))`.
- Render types: `RenderType.solid()/cutout()/cutoutMipped()/translucent()/lines()` -> `RenderTypes.solidMovingBlock()/cutoutMovingBlock()/translucentMovingBlock()/lines()`.
- `LevelRenderer.renderShape` (via accessor) -> `ShapeRenderer.renderShape(ps, vc, shape, x, y, z, argb, lineWidth)`; line width `Minecraft.getWindow().getAppropriateLineWidth()`.
- Create Fly renderer registration: `visual(...)` skips the BER when Flywheel is on, `normal(...)` keeps it. Upstream Registrate `.visual(..)` kept the BER
  (renderNormally = true) and the BERs check `VisualizationManager.supportsVisualization` themselves, so use `normal` (auger cog redstone,
  directional gearshift barrel and throttle lever outline are BER-only).
- `SplitShaftRenderer` is now render-state based; `DirectionalGearshiftRenderer` subclasses its state (shafts only without Flywheel).
- BakedModel -> `SimpleModelWrapper`; `BakedModelHelper.generateModel(SimpleModelWrapper, UnaryOperator<TextureAtlasSprite>)`; `new BakedModelBuilder(SimpleModelWrapper).build()`;
  sprite lookups: `BlockStateModel#collectParts(random)`, `BlockModelPart#getQuads(side)`, `BakedQuad#sprite()/direction()`, `particleIcon()`.
- `SuperBufferFactory.createForBlock(SimpleModelWrapper, BlockState, PoseStack)`; `CachedBuffers.partialDirectional(partial, state, dir, Supplier<PoseStack>)` unchanged.
- BE NBT: `write/read(ValueOutput/ValueInput, boolean)`; optional keys: `view.read(key, Codec.FLOAT)` / `view.getInt(key)` (Optional) / `view.contains(key)` (Fabric);
  `NbtUtils.writeBlockState/readBlockState` -> `view.store/read("Material", BlockState.CODEC)` (same NBT shape).
  NBT-based helpers (multiloader inventories): `view.store(key, CompoundTag.CODEC, helper.write(level.registryAccess()))`,
  `helper.read(view.lookup(), view.read(key, CompoundTag.CODEC).orElseGet(CompoundTag::new))`.
- Copying BE data across a block swap: `TagValueOutput.createWithContext(ProblemReporter.DISCARDING, registries)` -> `be.write(out,false)` ->
  `TagValueInput.create(ProblemReporter.DISCARDING, registries, out.buildResult())` (`AugerShaftBlock#transformAuger`).
- Block API: `updateShape(state, LevelReader, ScheduledTickAccess, pos, dir, neighborPos, neighborState, RandomSource)`;
  `ProperWaterloggedBlock#updateWater(level, tickAccess, state, pos)`; `neighborChanged(..., @Nullable Orientation, boolean)` (no neighbour pos anymore, `HandleBlock`);
  `getAnalogOutputSignal(state, level, pos, Direction)`; `getCloneItemStack(level, pos, state, boolean)`; `fallOn(..., double)`;
  `updateEntityAfterFallOn` -> `updateEntityMovementAfterFallOn`; `DirectionProperty` -> `EnumProperty<Direction>`; `Direction#getNormal()` -> `getUnitVec3i()`;
  `MethodsReturnNonnullByDefault` gone; `Player#canInteractWithBlock` -> `isWithinBlockInteractionRange`; `ItemStack#getDescriptionId` -> `getHoverName().getString()`;
  `Item#hasCraftingRemainingItem/getCraftingRemainingItem` -> `getCraftingRemainder()` (ItemStack); `SoundEvents.GENERIC_EAT` is a Holder (`.value()`);
  `new DustParticleOptions(Vector3f, alpha)` -> `new DustParticleOptions(0xRRGGBB, alpha)`; `LevelAccessor#getMaxBuildHeight()` -> `getMaxY() + 1`.
- Comparators: 1.21.11 passes a direction (`getAnalogOutputSignal(level, pos, comparatorFacing.getOpposite())`), so `SteeringWheelBlock`/`TorsionSpringBlock`
  override it and call `getAnalogOutputSignalFrom(..., dir.getOpposite())`; `mixin/torsion_spring/ComparatorBlockMixin` is redundant.
- Removal: `onRemove` is gone. Keep the BE across a block swap with `shouldChangedStateKeepBlockEntity(oldState)` (portable engine dyeing,
  `PORTABLE_ENGINE.isValid(oldState)`); drops/neighbour updates go in the BE's `preRemoveSideEffects(pos, oldState)` (server, before the BE is removed;
  `affectNeighborsAfterRemoval` runs after the BE is gone). `SmartBlockEntity#preRemoveSideEffects` also calls `destroy()`.
- `PlacementOffset#placeInWorld(level, item, player, hand, hit)` -> `(level, item, player, hand)`; `BlockEntry::has` -> `state.is(block)` (use lambdas: method refs
  on static registry fields evaluate during class init); `AllItems.X.isIn(stack)` -> `stack.is(AllItems.X)`; `getDefaultState()` -> `defaultBlockState()`.
- GUI (1.21.11 is 2D): `graphics.pose()` is a `Matrix3x2fStack` (`pushMatrix/translate(x,y)/rotate(rad)/popMatrix`, no z); `blit(tex, x, y, u, v, w, h, tw, th)` ->
  `blit(RenderPipelines.GUI_TEXTURED, tex, x, y, u, v, w, h, tw, th)`; text colours need alpha (`0xFF......`) or nothing is drawn;
  Create Fly `UIRenderHelper.drawStretched/drawCropped` dropped the z argument.
- Client: `Minecraft#getTimer()` -> `getDeltaTracker()`; `Minecraft#cameraEntity` -> `getCameraEntity()`; `player.input.up/down` -> `player.input.keyPresses.forward()/backward()`.
- `CatnipServices.NETWORK.sendToAllClients(p)` -> `VeilPacketManager.all(server).sendPacket(p)`.
- `Create.RANDOM` does not exist in Create Fly (used `level.getRandom()` for client particles).
- Loom: an interrupted first build can leave `.gradle/loom-cache/remapped_mods` half remapped (Create Fly methods named `method_xxxx`,
  e.g. "does not override abstract method newBlockEntity"); delete that folder and rebuild.

## Behaviour notes
- Steering wheel: `redraw()` dropped NeoForge `requestModelDataUpdate` (the wheel model is keyed on the material by renderer/visual).
- Throttle lever: neighbour update on removal moved to `ThrottleLeverBlockEntity#preRemoveSideEffects`.
- Handle: survival check now runs on any neighbour change (1.21.11 has no neighbour position); `Variant` keeps a `TagKey<Item>`
  (`getIngredientTag()`, was a datagen-only `Ingredient`); `PlayerHoldingHandleRenderer#afterSetupAnim(UUID, HumanoidModel)` for render-state based models;
  `ServerHandleHoldingHandler.tick(MinecraftServer)` (not called, as upstream).
- Analog transmission renderer draws its shaft once (upstream drew it twice). Portable engine renderer dropped `FilteringRenderer` (no filter on the engine).
- Void anchor: reports to `EndSeaShadowRenderer.addVoidAnchor` from `extractRenderState` (was `render`).
- Absorber is still unregistered upstream (commented out); ported to compile only.

## PORT-TODOs
- none in these packages.

## Structure-loaded kinetics and extra-kinetics inner BEs (sim-structure-kinetics)
- Inner "extra kinetics" BEs (torsion spring `Output`, analog transmission cogwheel, swivel bearing cog) are built with
  the parent's `BlockEntityType`, so Create Fly's `BlockEntityBehaviour.FIRST_READ_REGISTRY`/`CLIENT_REGISTRY` applied
  the parent's factories to them (client crash: `Output cannot be cast to TorsionSpringBlockEntity`).
  `mixin/extra_kinetics/SmartBlockEntityMixin` redirects both `Multi.get(type)` calls in `SmartBlockEntity.initialize`
  to `List.of()` for `ExtraKinetics.ExtraKineticsBlockEntity`.
- BE ids already matched upstream (`simulated:simple` for the analog transmission is upstream's id too).
- Upstream `extrakineticstest.*` structures store absolute `Source` positions / network ids from their save world;
  Create Fly's `validateKinetics` finds the stale source missing and drops speed to 0 with nothing re-propagating
  (upstream passed only because that position was in an unloaded chunk). `mixin/extra_kinetics/StructureTemplateMixin`
  calls `warnOfMovement()` (BE and its extra kinetics) before `loadWithComponents` in `StructureTemplate.placeInWorld`,
  like Create's schematic placement. This also skips the torsion spring's upstream `Sequence {Mode:"TURN_ANGLE"}`
  (Create Fly's `SequencerInstructions.CODEC` rejects it; only matters for in-world saves from upstream, left as is).
- The four `simulated_extra_kinetics_test_*` GameTests are now required and pass.
