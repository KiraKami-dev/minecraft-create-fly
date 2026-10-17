# p2 notes: blocks-redstone

Scope: `simulated/.../content/blocks/{redstone,lasers,nameplate,gimbal_sensor,altitude_sensor,velocity_sensor,nav_table,redstone_magnet}`.
0 compile errors; with p2-port merged, `:simulated:compileJava` is green.

## Client registration (orchestrator: wire into `SimulatedClient.init`)
- `dev.simulated_team.simulated.content.blocks.redstone.SimRedstoneBlocksClient#registerClient()`:
  - client behaviour halves via `AllBlockEntityBehaviours.add` (`BlockEntityBehaviour.CLIENT_REGISTRY`): frequency slots
    (`LinkedReceiverLinkBehaviour`, both receivers), value boxes (accumulator, inductor, laser pointer, optical sensor,
    velocity, gimbal), filter slot (`LaserSensorFilterBehaviour::create`, laser + optical sensor), goggle tooltips
    (velocity, gimbal, altitude; Create Fly's goggle overlay looks up `TooltipBehaviour.TYPE` on the BE);
  - `SimMovementBehaviours.ALTITUDE_SENSOR.attachRender = new AltitudeSensorMovementRenderBehaviour()`;
  - `SpecialGuiElementRegistry.register(ctx -> new ModulatingLinkedReceiverGuiRenderer(ctx.vertexConsumers()))`
    (Fabric API; must run before the GUI renderer exists, i.e. from the client entrypoint).
- The block entities add only the server halves (`ServerScrollValueBehaviour`, `ServerBrassDiodeScrollValueBehaviour`,
  server filtering/link behaviours). Common code mentions client halves only in `{@link}` javadoc.

## API mappings
- Value boxes: Create's `ScrollValueBehaviour(label, be, slot)` in `addBehaviours` -> server `ServerScrollValueBehaviour`
  (BE) + client `ScrollValueBehaviour<SmartBlockEntity, ServerScrollValueBehaviour>` subclass holding label, `ValueBoxTransform`
  and formatting (`*ScrollValueBehaviour`/`*ValueBehaviour` classes here). Brass-diode style: `ServerBrassDiodeScrollValueBehaviour`.
- Linked receivers: Create Fly hard-codes the redstone link slots in client `LinkBehaviour`; `LinkedReceiverLinkBehaviour`
  swaps in `LinkedReceiverFrequencySlot`s through client mixin `mixin/accessor/LinkBehaviourAccessor` (`remap = false`).
- Goggle info: BE `implements IHaveGoggleInformation` -> client `TooltipBehaviour<BE> implements IHaveGoggleInformation`.
- Movement render: `MovementBehaviour#renderInContraption` -> `MovementRenderBehaviour#getRenderState(...)` on `attachRender`.
- `Block#onRemove` gone: drops go in the BE's `preRemoveSideEffects(pos, oldState)` (nav table held item);
  `SmartBlockEntity#preRemoveSideEffects` already calls `destroy()`.
- Save/load: `ItemStack#saveOptional`/`parseOptional` -> `view.store/read(key, ItemStack.OPTIONAL_CODEC)`;
  `VecHelper.writeNBT/readNBT` (double list) -> `view.store/read(key, Vec3.CODEC)` (same NBT shape).
- `Direction#getNormal()` -> `getUnitVec3i()`; `Direction.getNearest(float x3)` -> `getApproximateNearest(...)`.
- Renderers are `BlockEntityRenderer<T, S>`: partials as `SuperByteBuffer` in the state, drawn by a
  `SubmitNodeCollector.CustomGeometryRenderer` (`queue.submitCustomGeometry(ps, RenderTypes.cutoutMovingBlock(), ...)`);
  the buffers reset their transform after `renderInto`, so set transforms inside `render`. Renderers with a Flywheel
  visual skip the partials when `VisualizationManager.supportsVisualization(level)`.
- Items in BERs: `ItemRenderer#renderStatic` -> `context.itemModelResolver().updateForTopItem(state, stack, FIXED, level, null, 0)`
  in extract, `state.submit(ps, queue, light, OverlayTexture.NO_OVERLAY, 0)` in submit; `BakedModel#isGui3d()` ->
  `ItemStackRenderState#usesBlockLight()` (as Create Fly's `DepotRenderer`).
- `RenderableNavigationTarget#renderInNavTable(..., MultiBufferSource, ...)` -> `extractInNavTable(self, navBE, navState,
  partialTicks, itemState)` returning a `NavTableItemRender` submitted later (no implementors upstream).
- Text: `font.drawInBatch`/`drawInBatch8xOutline` -> `queue.submitText(ps, x, y, seq, shadow, mode, light, color, bg, outline)`
  (outline != 0 draws the 8x outline). Colors are ARGB: an alpha of 0 is transparent now (was "opaque"), so dark
  nameplate colors use `ARGB.color(255, r, g, b)`; `DyeColor#getTextColor()` is already opaque.
- Lasers: Veil laser/lens programs -> `SimRenderTypes.laser()/lens()` (core shaders; `LaserRenderTypes` deleted).
  Accumulator diode: own `RenderPipeline` + core shader `assets/simulated/shaders/core/redstone_accumulator_diode`.
- GUIs: 1.21.11 GUIs are 2D. 3D block previews -> picture-in-picture (`ModulatingLinkedReceiverGuiRenderer`, Create Fly's
  `EntityBlockRenderState` in the typewriter screen); gradient quads -> `GradientBlitRenderState` (GUI element state);
  z offsets -> strata; `keyPressed(int,int,int)`/`charTyped(char,int)` -> `KeyEvent`/`CharacterEvent`;
  `pose()` is a `Matrix3x2fStack`; `blit(id, ...)` -> `blit(RenderPipelines.GUI_TEXTURED, id, ...)`;
  `fill(RenderType.guiTextHighlight(), ...)` -> `textHighlight(...)`; the background comes from `renderWithTooltipAndSubtitles`.
- Menus: typewriter menu is a Create Fly `GhostItemMenu` (`LinkedTypewriterMenuCommon.create(id, inv, be)` = menu factory);
  screen factory `LinkedTypewriterScreen.create(mc, type, syncId, inv, title, buf)`; ghost slots override `isFake()`.
- Lang: `CreateLang.translateDirect("x")` in common code -> `Component.translatable("create.x")`.
- Create's `RaycastHelper#rayTraceRange` is client-only in Create Fly -> copied into `LinkedTypewriterItem`.
- Gimbal sensor: `DimensionType#natural` gone -> compare with the respawn dimension (needle spins elsewhere, like the compass).
- Display target: `NameplateBlockTarget.NAMEPLATE = Simulated.getRegistrate().displayTarget("nameplate", NameplateBlockTarget::new)`.

## Changes outside the area
- `registrate/SimulatedRegistrate#onAddDefaultComponents` takes `Consumer<DataComponentPatch.Builder>` again (upstream
  signature; its only caller `SimulatedCommonEvents#modifyDefaultComponents` passes that). The registry note's
  `DataComponentMap.Builder` is outdated.
- `network/packets/honey_glue/HoneyGlueSpawnPacket`: `HONEY_GLUE.create(level, EntitySpawnReason.SPAWN_ITEM_USE)`.
- `fabric/menu/LinkedTypewriterMenuImpl`: ghost slot `isFake()`. `simulated.mixins.json` client: `accessor.LinkBehaviourAccessor`.

## PORT-TODOs
- None in the area.
