# p2 notes: entities-render

Scope: `content/entities/**` (contraption diagram + screen/sticky note, honey glue, launched plunger),
`content/end_sea/**`, `content/physics_staff/**`, `util/SimpleSubLevelGroupRenderer`, `mixin/{diagram,end_sea,physics_staff}`,
`mixin_interface/diagram`, `fabric/mixin/diagram`. 0 compile errors in these files.

## Client hook to wire (orchestrator)
- `dev.simulated_team.simulated.content.physics_staff.PhysicsStaffClient#registerClient()`: item model type
  `simulated:model/physics_staff` (`ItemModels.ID_MAPPER`) + `WorldRenderEvents.END_MAIN` hook for beams / lock icons /
  hover box (upstream: Veil AFTER_TRANSLUCENT_BLOCKS).
- `SimulatedClient`: drop `onVeilAddShaderProcessors(EndSeaFadeTransformer)` (now a mixin) and
  `onVeilRenderLevelStage(EndSeaShadowRenderer::renderShadowMap)` (now in the end sea frame pass). The veillite-signature
  `EndSeaShadowRenderer.renderShadowMap(stage, ...)` and `PhysicsStaffRenderHandler.renderSelectionBox(stage, ...)` are
  deprecated no-ops kept for compile compatibility: veillite never fires `VeilRenderLevelStageEvent`.

## Rendering (Veil -> 1.21.11)
- `AdvancedFbo` -> `com.mojang.blaze3d.pipeline.TextureTarget(label, w, h, useDepth)`; clear with
  `CommandEncoder#clearColorAndDepthTextures`; free with `destroyBuffers()`.
- Veil shader programs -> `RenderPipeline.builder(...)` + core shaders in `assets/simulated/shaders/core/`
  (`contraption_diagram/outline_diagram`, `end_sea`, `spread_end_sea`; `core/screenquad` for fullscreen). Plain uniforms ->
  std140 blocks (`DiagramInfo`, `EndSeaInfo`, `SpreadInfo`) written with `Std140Builder` into a `MappableRingBuffer`
  (`mapBuffer(..., false, true)`, `rotate()` after the pass). Fullscreen passes: `EMPTY` format, `TRIANGLES`,
  `renderPass.draw(0, 3)`. Location textures: `TextureManager#getTexture(id)` + its own `getSampler()` (mcmeta blur/clamp).
- Veil post pipeline that writes `gl_FragDepth` (spread_end_sea): no "always" depth test in blaze3d, so clear the target
  depth to 1.0 in `createRenderPass(..., OptionalDouble.of(1.0))` and use `LEQUAL_DEPTH_TEST` + depth write.
- Offscreen sub-level rendering (`SimpleSubLevelGroupRenderer.renderGroup(level, subLevels, RenderTarget, modelView,
  projection, cameraPos, viewRotation, pt, renderPlayers)`): no Sable change needed. Sections are drawn with Sable's
  public `SubLevelRenderPipelines.get(layer)`, `VanillaSubLevelRenderDispatcher.getAnchoredTransformation`,
  `VanillaChunkedSubLevelRenderData.allRenderSections()` and our own `SubLevelUniform`s
  (`SubLevelRenderPipelines.createUniformStorage()`, `endFrame()` after each group: the ring is fenced) +
  `RenderSystem.getDynamicUniforms().writeChunkSections`. Upstream "simple" mode = params x (dynamic shading) 0, z (fog) 0,
  w (end sea) 0. Block entities, single-block sub-levels (`VanillaSingleSubLevelRenderData#submitSingleBlock`) and
  entities go through `FeatureRenderDispatcher.getSubmitNodeStorage()` -> `renderAllFeatures()` +
  `bufferSource.endBatch()` with `RenderSystem.outputColor/DepthTextureOverride` = target, own projection
  (`PerspectiveProjectionMatrixBuffer#getBuffer(Matrix4f)` works for any matrix, `setProjectionMatrix(..., ORTHOGRAPHIC)`,
  restore the previous slice/type yourself: `backupProjectionMatrix` is a single slot) and model-view stack.
  Block entities come from the plot chunks (`plot.getLoadedChunks()` -> `LevelChunk#getBlockEntities()`), which also covers
  the ones Flywheel embeds (upstream: `VisualManagerExtension`/`SubLevelEmbedding`, dropped). Plot-space camera for BERs:
  Sable's `BlockEntityRenderDispatcherExtension#sable$setCameraPosition`.
- `Lighting.setupNetherLevel()` -> `gameRenderer.getLighting().updateLevel(CardinalLightType.NETHER)` +
  `setupFor(Entry.LEVEL)`; restore with `updateLevel(level.dimensionType().cardinalLightType())` +
  `RenderSystem.setShaderLights(previous)`.
- Light texture (`mixin/diagram/LightTextureMixin`): no CPU pixels and no COPY_DST on 1.21.11; the diagram curve is
  written into our own 16x16 texture (`writeToTexture`) and blitted over the light texture with
  `RenderPipelines.TRACY_BLIT` (`InSampler`). `updateLightTexture = true` + `updateLightTexture(1.0f)` restores it.
- GUI: `GuiGraphics#pose()` is a `Matrix3x2fStack` (`pushMatrix/translate(x, y)/popMatrix`, no z; submission order
  layers). Draw a GPU texture: `graphics.guiRenderState.submitGuiElement(new BlitRenderState(RenderPipelines.GUI_TEXTURED,
  TextureSetup.singleTexture(view, sampler), new Matrix3x2f(graphics.pose()), x0, y0, x1, y1, 0, 1, 1, 0, -1,
  graphics.scissorStack.peek()))` (v flipped: render target rows start at the bottom). `blit(id, x, y, z, u, v, ...)` ->
  `blit(RenderPipelines.GUI_TEXTURED, id, x, y, u, v, ...)`; `RenderType.gui()` quads -> `graphics.fill`;
  `fill(..., z, color)` -> no z. Widgets: `onClick(double, double)` -> `onClick(MouseButtonEvent, boolean)`; screens:
  `mouseClicked(MouseButtonEvent, boolean)` / `mouseReleased(MouseButtonEvent)`.
- `glReadPixels` -> async `CommandEncoder#copyTextureToBuffer(tex, buf(MAP_READ|COPY_DST), 0, callback, 0)`; the callback
  runs from `RenderSystem.executePendingTasks()` at the start of a later frame (diagram greebles are placed then).
- Level render hook "before renderDebug" -> own frame pass added before `LevelRenderer#addLateDebugPass`
  (`@Local FrameGraphBuilder`, `targets.main = pass.readsAndWrites(targets.main)`), see `mixin/end_sea/LevelRendererMixin`.
- Veil shader pre-processor (end sea fade in the chunk shaders) -> `mixin/end_sea/ShaderManagerMixin`
  (`@ModifyExpressionValue` on `String.join` in `ShaderManager#loadShader`) patches `sable:core/sub_level_terrain.vsh`;
  `mixin/end_sea/VanillaSubLevelRenderDispatcherMixin` puts camera y - end sea y into **`SubLevelParams.w`** (Sable's
  unused component, `@ModifyArg` on `DynamicUniformStorage#writeUniform` in `prepareSections`). Shared-state note: if
  Sable ever uses `SubLevelParams.w`, move this.

## Items / entities
- Create `CustomRenderedItemModelRenderer` + NeoForge `PhysicsStaffItemMixin` -> `PhysicsStaffItemRenderer` is an
  `ItemModel` + `SpecialModelRenderer` (like Create Fly `SymmetryWandModel`); `assets/simulated/items/creative_physics_staff.json`
  = `{"model":{"type":"simulated:model/physics_staff"}}`. The pose handed to `submit` is at the model corner:
  `translate(.5,.5,.5)` restores the old origin. Hand poses start with the inverse view rotation in both 1.21.1 and
  1.21.11, so `ms.last().pose()` math is unchanged. `RenderSystem.getProjectionMatrix()` -> hand projection
  `perspective(getFov(camera, pt, false), w/h, 0.05, 100)`. Iris check -> Create Fly
  `flywheel.lib.util.ShadersModHelper.isShaderPackInUse()`. Staff textures are on the **items** atlas: cutout via
  `RenderTypes.entityCutout(atlas)`, glowing via `SimRenderTypes.itemGlowing*` (registry, items atlas).
- `DiagramEntity` spawn data (NeoForge `DiagramEntityMixin`/`IEntityWithComplexSpawn`) -> `getAddEntityPacket` returns
  Create Fly's `NbtSpawnPacket` with `addAdditionalSaveData`, `recreateFromPacket` reads it (Create Fly `BlueprintEntity`).
  Honey glue bounds are synced by the platform's `START_TRACKING` hook instead.
- `HangingEntity` 1.21.11: direction is synced entity data (`getDirection()`/`setDirectionRaw`); call
  `super.defineSynchedData`; override `onSyncedDataUpdated` without the horizontal-only `setDirection` (diagrams face
  up/down). `dropItem(Entity)` -> `dropItem(ServerLevel, Entity)`, `RULE_DOENTITYDROPS` -> `GameRules.ENTITY_DROPS` via
  `getGameRules().get(..)`, `spawnAtLocation(level, stack)`, `moveTo` -> `snapTo`, `lerpTo` -> `moveOrInterpolateTo(Vec3, f, f)`
  (final: AW `extendable`), `HANGING_ENTITY` predicate -> `e -> e instanceof HangingEntity`.
- `EntityDataSerializers.OPTIONAL_UUID` is gone: launched plunger keeps the partner UUID in a `STRING` accessor ("" = none).
- `Projectile#ownerUUID` -> `owner` `EntityReference`; clear with `setOwner((Entity) null)`. `load(CompoundTag)` -> `load(ValueInput)`.
- NBT -> `ValueOutput/ValueInput` with the same keys: `VecHelper.writeNBT(vec)` = `Vec3.CODEC`, `NbtUtils.writeBlockPos` =
  `BlockPos.CODEC`, `putUUID` = `UUIDUtil.CODEC`, enum -> `putString(name())`. `walkDist/walkDistO` are gone (dropped like
  Create Fly's super glue). `hurt` -> `hurtServer(ServerLevel, ...)`. `makeBoundingBox()` -> `makeBoundingBox(Vec3)`.
- Entity renderers: `EntityRenderer<T, S>` + `createRenderState/extractRenderState/submit`; SuperByteBuffer geometry via
  a state implementing `SubmitNodeCollector.CustomGeometryRenderer` + `submitCustomGeometry`. The plunger rope is built in
  camera-relative world space at extract time and emitted ignoring the pose (as upstream). `RenderType.solid()` ->
  `RenderTypes.solidMovingBlock()`. `EntityRenderDispatcher#cameraOrientation()` -> `camera.rotation()`.
- `SavedData.Factory` -> `SavedDataType(id, Supplier, Codec, null)` (physics staff locks keep the same NBT layout).
- `SimpleJsonResourceReloadListener(Gson, dir)` -> `(ExtraCodecs.JSON, FileToIdConverter.json(dir))`, parse in `apply`.
- `canAttackBlock` -> `canDestroyBlock(stack, state, level, pos, LivingEntity)`; `AllKeys.altDown()` (absent) ->
  `InputConstants.isKeyDown(window, GLFW_KEY_LEFT/RIGHT_ALT)`; `ctrlDown/shiftDown` -> `AllKeys.hasControlDown/hasShiftDown`.
- `GuiGraphicsMixin` (staff clipped to its slot): `renderItem(LivingEntity, Level, ItemStack, III)` (one int fewer), GUI
  scissor stack instead of `RenderSystem.enableScissor`.

## Removed
- `mixin/diagram/VanillaSubLevelRenderDispatcherMixin` (diagram draws its own uniforms), `mixin_interface/diagram/VisualManagerExtension`,
  Veil resources under `assets/simulated/pinwheel/` for the diagram/end sea (replaced by core shaders).

## PORT-TODOs
- Single-block sub-levels don't fade into the end sea (`EndSeaFadeTransformer`).
- Not run in game yet (module does not compile as a whole): diagram/end sea/staff rendering is untested.
