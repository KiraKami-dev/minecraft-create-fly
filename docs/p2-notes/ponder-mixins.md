# p2 notes: ponder-mixins

Scope: `simulated/.../{ponder,client,mixin (minus diagram/end_sea/physics_staff),index/SimPonder*,index/ponder}`,
`simulated.accesswidener` (C4), `simulated.mixins.json`. ROADMAP C8 (Simulated mixins), C4, ponder.
0 compile errors in these files; `:simulated:validateAccessWidener` passes. Every mixin was checked against the
1.21.11 / Create Fly **bytecode** (javap): target class, selected method names, `@At` INVOKE/FIELD/NEW targets with
enough occurrences for the ordinal, `@Shadow`/`@Accessor`/`@Invoker` members. Nothing has been run in game yet (the
module does not compile as a whole until every area is merged).

## Mixin rules that bit (read before writing new mixins)
- **Minecraft jars have no local variable table** (Loom/Mojang mappings): `@Local(name = ...)` never works on MC
  classes, and type-based locals see the *inferred* type: booleans are `int`, `list2 = Lists.newArrayList(..)` is an
  `ArrayList`, `TooltipFlag.Default.NORMAL.asCreative()` is a `TooltipFlag.Default`. Capture by inferred type or use
  `argsOnly`, `ModifyExpressionValue`, `WrapOperation` args instead. Create Fly's jar *has* an LVT (names work there).
- **Lambdas keep intermediary names** under Mojang mappings, in dev and prod: `lambda$updateCreativeTooltips$15` ->
  `method_60369` (find them with `javap -p` on the Loom merged jar).
- **remap** (no mixin AP here: Loom remaps mixins with tiny-remapper at `remapJar`; `remap = false` means "leave this
  string alone"): name-only selectors and `@Shadow`/`@Accessor`/`@Invoker` of Create Fly / Sable members get
  `remap = false`; any `@At` (or selector) whose target/descriptor mentions Minecraft classes (`net/minecraft/`,
  `com/mojang/`) keeps `remap = true` (explicitly when the injector is `remap = false`); Minecraft targets keep the
  default. Same policy as sable-create. Upstream's `remap = false` on Minecraft members (PrimaryLevelData `parse` /
  `setTagData`, GoggleOverlayRenderer `Mth.clamp`, auto-orientation `IRotate` descriptors) only worked on NeoForge.
- Descriptor strings: `com/simibubi/create` -> `com/zurrtum/create`, `net/createmod/catnip` ->
  `com/zurrtum/create/(client/)catnip`, `net/createmod/ponder` -> `com/zurrtum/create/client/ponder`,
  `dev/engine_room/flywheel` -> `com/zurrtum/create/client/flywheel`.
- Client-only targets must be in the `client` list (the server e2e mixin audit loads every common target): moved
  `ParrotElementAccessor`, `WorldSectionElementAccessor`, `ponder.TextWindowElementMixin`, `dynamic_stress.KineticStatsMixin`
  (Create Fly `client.foundation.item.KineticStats`), goggle tooltip mixin.

## API mappings found on the way
- Goggle info: `KineticBlockEntity#addToGoggleTooltip` -> client `KineticTooltipBehaviour#addToGoggleTooltip`
  (`BlockEntityBehaviour.CLIENT_REGISTRY`); other BEs' lines via `be.getBehaviour(TooltipBehaviour.TYPE)` as `IHaveGoggleInformation`.
- Ponder (Create Fly `com.zurrtum.create.client.ponder`): `PonderScene#getWorld` -> `getLevel`; `renderScene(buffer, graphics, pt)`
  -> `renderScene(Minecraft, SuperRenderTypeBuffer, SubmitNodeStorage, PoseStack, pt)`; `PonderUI#renderScene` only submits a
  PiP `SceneRenderState`, the scene + base plate shadow are drawn in `foundation.render.SceneRenderer#renderScene` (static);
  `AnimatedSceneElementBase#renderLast(world, buffer, GuiGraphics, fade, pt)` -> `renderLast(EntityRenderDispatcher,
  ItemModelResolver, PonderLevel, MultiBufferSource, SubmitNodeCollector, Camera, CameraRenderState, PoseStack, fade, pt)`;
  `Outline#render(ms, buffer, camera, pt)` -> `render(Minecraft, ms, buffer, camera, pt)`; ponder text/GUI is 2D
  (`Matrix3x2fStack`), scaled items are PiP `GuiGameElement.GuiItemRenderBuilder`s that must be `clear()`ed (see
  `KeybindWindowElement`, like `InputWindowElement`). Scenes keyed by id: `withKeyFunction(RegisteredObjectsHelper::getKeyOrThrow)`
  with `ItemLike`s (`SimPonderScenes`, `SimPonderTags`).
- `modifyBlockEntityNBT` still hands a `CompoundTag` (Create Fly saves with `saveWithFullMetadata`, loads with
  `loadWithComponents`): `getCompound(k)` -> `getCompoundOrEmpty(k)`, `getFloat` -> `getFloatOr`; item stacks:
  `stack.saveOptional(provider)` -> `tag.store(key, ItemStack.OPTIONAL_CODEC, provider.createSerializationContext(NbtOps.INSTANCE), stack)`
  (Create Fly `Frequency.CODEC` / filters use `ItemStack.OPTIONAL_CODEC`). BE NBT round trip:
  `TagValueOutput.createWithContext(ProblemReporter, registries)` -> `be.write(out, false)` -> `TagValueInput.create(..., out.buildResult())`.
- Fluids in ponder: `IFluidHandler#fill(stack, EXECUTE)` -> Create Fly `FluidInventory#insert(stack)`, amounts x81.
- Extra kinetics save/load: `write/read(ValueOutput/ValueInput, boolean)`; child compound `view.child(name)` /
  `view.childOrEmpty(name)`; `saveAdditional/loadCustomOnly(tag, provider)` -> public `saveCustomOnly(view)` / `loadCustomOnly(view)`;
  `writeClient/readClient(view)`.
- `SpriteContents.Ticker#tickAndUpload` -> `SpriteContents.AnimationState#tick` (`createAnimationState`); GUI atlas:
  `Minecraft#getAtlasManager().getAtlasOrThrow(AtlasIds.GUI)`.
- Input: `KeyboardHandler#keyPress(long, int key, int scan, int action, int mods)` -> `keyPress(long, int action, KeyEvent)`;
  `MouseHandler#onPress(long, button, action, mods)` -> `onButton(long, MouseButtonInfo, action)` (2nd `MouseButtonInfo` local is
  the macOS-adjusted one); `turnPlayer` lost the invert-Y int local (both axes inverted inline in `LocalPlayer#turn` args);
  shift is sent to the server inside `ServerboundPlayerInputPacket(input.keyPresses)` (`ClientInput#keyPresses`, `Input#shift()`).
- `Minecraft#startUseItem`: `InteractionResult#shouldSwing()` -> `InteractionResult.Success#swingSource()`.
- `HumanoidModel#setupAnim(T entity, ...)` -> `setupAnim(HumanoidRenderState)`; players are `AvatarRenderState`
  (Create Fly tags it with the UUID: `SkyhookRenderState#create$getUuid`). Use the exact descriptor (bridge method).
- `Item#canAttackBlock(state, level, pos, Player)` -> `canDestroyBlock(stack, state, level, pos, LivingEntity)`;
  `CompassItem#inventoryTick(stack, ServerLevel, entity, EquipmentSlot)` (server only);
  `CompassItemPropertyFunction#getCompassRotation` -> `client.renderer.item.properties.numeric.CompassAngleState#calculate(stack, level, seed, ItemOwner)`.
- `Block#updateEntityAfterFallOn` -> `updateEntityMovementAfterFallOn`; `BlockState#getAnalogOutputSignal(level, pos, Direction)`
  (comparators pass `facing.getOpposite()`); `LevelRenderer#renderHitOutline(ps, vc, camX, camY, camZ, BlockOutlineRenderState, color, width)`;
  `LevelRenderer.renderShape` -> `ShapeRenderer.renderShape(ps, vc, shape, x, y, z, argb, lineWidth)`.
- World creation: `LevelSettings`/`PrimaryLevelData` are built in `CreateWorldScreen#onCreate()` (not `createNewWorld`);
  `CreateWorldScreen$WorldTab` gained locals after the world type button (capture by type, not `LocalCapture`).
- `ResourceKey#location()` -> `identifier()`; `Registry#getTag(key)` -> `get(key)`; `Registry#get(id)` -> `getValue(id)`;
  `Direction#getNormal()` -> `getUnitVec3i()`; `DustParticleOptions(Vector3f, scale)` -> `(int rgb, scale)`;
  `SimpleJsonResourceReloadListener(Gson, dir)` -> `<T>(Codec<T>, FileToIdConverter.json(dir))`.
- GUI: `GuiGraphics#fillGradient` lost the z argument; text colours need alpha (`0xFF000000 | rgb`, e.g. Create Fly `Label#colored`).

## Mixins (simulated.mixins.json; diagram/end_sea/physics_staff are entities-render's)
| mixin | target (1.21.11 / Create Fly) | status |
|---|---|---|
| aabb.AABBMixin | `AABB#getDirection(AABB,Vec3,double[],Direction,DDD)` | verified |
| accessor.BlockBehaviourAccessor | `BlockBehaviour#hasCollision` | verified |
| accessor.ContraptionAccessor | Create Fly `Contraption#superglue` | verified, remap=false |
| accessor.ControlledContraptionEntityAccessor | `controllerPos`, `makeStructureTransform()` | verified, remap=false |
| accessor.CreativeModeInventoryScreenAccessor | `AbstractContainerScreen#leftPos/topPos` | verified |
| accessor.CreativeModeInventoryScreenSelectedTabAccessor | `CreativeModeInventoryScreen.selectedTab` | verified |
| accessor.KeyMappingsAccessor | `KeyMapping#key` | verified |
| accessor.ParrotElementAccessor | ponder `ParrotElementImpl#entity` | verified, moved to client |
| accessor.RedstoneLinkBlockEntityAccessor | `RedstoneLinkBlockEntity#link` | type `LinkBehaviour` -> `ServerLinkBehaviour` |
| accessor.WorldSectionElementAccessor | ponder `WorldSectionElementImpl#centerOfRotation` | verified, moved to client |
| accessor.CreateBlockEntityBuilderAccessor | Registrate `CreateBlockEntityBuilder` | **removed** (no Registrate) |
| accessor.LevelRendererAccessor | `LevelRenderer.renderShape` (gone) | **removed** -> `ShapeRenderer.renderShape` |
| assembly_preventer.ServerSubLevelMixin | Sable `ServerSubLevel` (interface only) | verified |
| auger_shaft.GirderBlockMixin | `GirderBlock.isConnected` TAIL, LVT local `blockState` | verified |
| conditional_display_target.DisplayLinkBlockEntityMixin | `updateGatheredData` before `DisplaySource#transferData` | descriptor retargeted, local by type |
| conditional_display_target.DisplayLinkScreenMixin | `initGathererOptions` `DisplayTarget#getLineOptionText` | retargeted; label colour gets alpha |
| create_assembly.* (4) | `disassemble()` of clockwork/elevator/actuator/bearing BEs | verified |
| creative_tab_sections.AbstractContainerScreenMixin | `AbstractContainerScreen#renderContents` TAIL | **new**: banners (was `render` TAIL + depth test; carried item is a later GUI stratum now) |
| creative_tab_sections.CreativeModeInventoryScreenMixin | `getTooltipFromContainerItem` before `CreativeModeTabs.tabs()` | locals by inferred type (`ArrayList`), index 1 |
| creative_tab_sections.CreativeModeTabMixin | `CreativeModeTab#buildContents` (WrapMethod) | verified |
| creative_tab_sections.ItemPickerMenuMixin | `ItemPickerMenu#scrollTo` | verified (+`@Final` shadow) |
| creative_tab_sections.SpriteContentsMixin | `SpriteContents#createAnimationState` RETURN | retargeted from `createTicker` |
| creative_tab_sections.SpriteContentsAnimationStateMixin | `SpriteContents$AnimationState#tick` PUTFIELD `frame` | renamed from SpriteContentsTickerMixin (`Ticker#tickAndUpload`) |
| diving_boots.AirCurrentMixin | `AirCurrent.isPlayerCreativeFlying` | verified |
| dynamic_stress.KineticStatsMixin | client `KineticStats.getKineticStats` (`LangBuilder#add` 0/2, `#translate` 0) | targets retargeted, moved to client |
| extendo_grip.ExtendoGripItemMixin | override `Item#canDestroyBlock` | ported (`canAttackBlock` gone) |
| extra_kinetics.EncasedCogwheelBlockMixin | `handleEncasing` `IRotate#hasShaftTowards` | verified |
| extra_kinetics.GeneratingKineticBlockEntityMixin | `setSource` `Level#getBlockEntity` | verified |
| extra_kinetics.KineticBlockEntityMixin | `switchToBlockState`, `validateKinetics`, `setSource`, `remove`, `removeSource`, `write`, `read` | write/read ported to ValueOutput/ValueInput (same keys) |
| extra_kinetics.KineticBlockMixin | `KineticBlock#updateIndirectNeighbourShapes` TAIL | verified |
| extra_kinetics.KineticNetworkMixin | `calculateCapacity/Stress` | verified |
| extra_kinetics.RotationPropagatorMixin | `getRotationSpeedModifier` (isSmallCog 0-3, isLargeCog 0-2, hasShaftTowards 0-1, getRotationAxis 0-2), neighbour lists | verified, ordinals unchanged |
| extra_kinetics.auto_orientation.FixAutoOrientation(Static)Mixin | `IRotate#hasShaftTowards` in kinetic base blocks | verified, remap fixed |
| extra_kinetics.goggle_tooltips.KineticTooltipBehaviourMixin | client `KineticTooltipBehaviour#addToGoggleTooltip` RETURN | **retargeted** from `KineticBlockEntity`, client; extra BEs share the parent type, so their (Generating)KineticTooltipBehaviour is created per class |
| flicker_tally_removal.RotationPropagatorMixin | `propagateNewSource` `getFlickerScore` | verified |
| handle.ExtendoGripRenderHandlerMixin | client `ExtendoGripRenderHandler.tick(Minecraft)` PUTSTATIC `holding` (was `pose`) | retargeted |
| handle.HumanoidModelMixin | `HumanoidModel#setupAnim(HumanoidRenderState)` RETURN | ported to render states (UUID from `SkyhookRenderState`) |
| handle.LocalPlayerMixin | `LocalPlayer#isMovingSlowly` | verified |
| handle.MinecraftMixin | `startUseItem` `InteractionResult$Success#swingSource` | retargeted from `shouldSwing` |
| hold_interaction.GoggleOverlayRendererMixin | `renderOverlay(Minecraft, GuiGraphics, DeltaTracker)` `Mth.clamp` | args fixed; pops the pushed pose when cancelling |
| hold_interaction.KeyMappingInvoker | `KeyMapping#release` | verified |
| hold_interaction.KeyboardHandlerMixin | `keyPress(long, int, KeyEvent)` first GETFIELD `screen` | ported |
| hold_interaction.LocalPlayerMixin | `isShiftKeyDown`; `tick` GETFIELD `ClientInput#keyPresses` | shift masking moved to the input packet |
| hold_interaction.MouseHandlerMixin | `turnPlayer` (WrapOperation `LocalPlayer#turn`), `onButton`, `onScroll` | ported |
| honey_glue.ArmorStandItemMixin | `ArmorStandItem#useOn` `List#isEmpty` | verified |
| linked_controller_binding.LinkedControllerItemMixin | static `onItemUseFirst` `BlockState#is(Block)` ordinal 1 | retargeted from `BlockEntry#has` |
| lodestone_compat.CompassItemMixin | `inventoryTick(ItemStack,ServerLevel,Entity,EquipmentSlot)`, `useOn` | ported |
| lodestone_compat.CompassAngleStateMixin | `CompassAngleState#calculate` (WrapMethod) | **retargeted** from CompassItemPropertyFunction |
| nav_table_compat.NixieTubeBlockMixin / RedstoneLinkBlockMixin | `getPower` 2nd `Iterate.directions` | verified (still needed in Create Fly) |
| new_ponder.PonderSceneRegistryMixin | `compile(Identifier)` RETURN | verified (empty list guarded) |
| new_ponder.PonderTooltipHandlerAccessor | `PonderTooltipHandler.trackingStack` | verified |
| new_ponder.PonderTooltipHandlerMixin | `makeProgressBar` `LangBuilder#component` | retargeted |
| new_ponder.PonderUIMixin | `PonderUI#scroll` `PonderScene#begin` | retargeted |
| ponder.PonderSceneMixin | `begin`, `tick`, `renderScene` `SceneCamera#set(FF)V` | redirect args ported (camera pitch fix still needed) |
| ponder.PonderSceneTransformMixin | `SceneTransform#apply(PoseStack,F)` GETFIELD `scaleFactor/yOffset` | retargeted |
| ponder.SceneRendererMixin | `foundation.render.SceneRenderer.renderScene` (`0x66000000`, `translate(FFF)` 1) | **retargeted** from PonderUIMixin |
| ponder.TextWindowElementMixin | `render` `fillGradient(IIIIII)` 0/1, `Math.min(FF)` | descriptors ported, moved to client |
| ponder.WorldSectionElementImplMixin | `transformMS` `PoseTransformStack#rotateY/ZDegrees` 0/1 | retargeted (flywheel package) |
| player.PlayerMixin | `Player` (interfaces only) | verified |
| quiet_use.MultiPlayerGameModeMixin | `useItemOn` `MutableObject.<init>` | verified |
| rope.ClientSubLevelContainerMixin | Sable `ClientSubLevelContainer#tick` | verified |
| rope.LevelRendererMixin | `renderHitOutline(.., BlockOutlineRenderState, int, float)` HEAD | ported |
| sable_hooks.SableCommonEventsMixin | Sable `SableCommonEvents.handleBlockChange` | verified |
| search_alias.SessionSearchTreesMixin | `ItemStack#getTooltipLines` (static lambda) | verified; copies immutable lists |
| sounds.SoundEngineMixin / SoundManagerMixin | `instanceToChannel`, `soundEngine` | verified |
| spring_item_bounce.EntityMixin | `Entity#move` `updateEntityMovementAfterFallOn`, `checkFallDamage` | retargeted |
| throttle_lever.GameRendererMixin | `GameRenderer#pick(F)` TAIL | verified |
| tooltip_flag.SessionSearchTreesMixin | `method_60369` STORE `TooltipFlag$Default` | retargeted (lambda name + inferred type) |
| tooltip_flag.TooltipFlagDefaultMixin | `TooltipFlag$Default` | verified |
| torsion_spring.MechanicalBearingBlockEntityMixin | `onSpeedChanged` `ControlledContraptionEntity#isStalled` | retargeted |
| torsion_spring.ComparatorBlockMixin | `ComparatorBlock#getInputSignal` | **removed**: blocks override the new directional `getAnalogOutputSignal` (blocks-kinetic) |
| world_presets.CreateWorldScreenMixin | `onCreate` HEAD + `new PrimaryLevelData` | **retargeted** from `createNewWorld` |
| world_presets.PrimaryLevelDataMixin | `parse` RETURN, `setTagData` TAIL | verified; wrong `remap = false` removed |
| world_presets.WorldTabMixin | `CreateWorldScreen$WorldTab.<init>` TAIL | locals by type |
| linked_typewriter.PonderTooltipHandlerMixin | `makeProgressBar` `withStyle` | **removed**: Create Fly already copies the key message |
| schematicannon_fix.SchematicPrinterMixin | `getCurrentRequirement` | **removed**: Create Fly already passes `blockReader.getBlockEntity(target)` |
| plunger_launcher.ItemInHandRendererMixin (items-misc) | `ItemInHandRenderer#renderArmWithItem` HEAD | checked |

## Access widener (C4)
- Removed stale/unused: `RenderStateShard`/`RenderType$CompositeState` (no longer exist), `Projectile#ownerUUID` (now an
  `EntityReference`), `Projectile.<init>`, `Slot#x/y`, `AbstractContainerMenu#lastSlots/remoteSlots`, `RecipeProvider#getName`
  (datagen, malformed), `GameRenderer#getFov ...D`, duplicate `Camera` lines.
- Ported from `META-INF/accesstransformer.cfg` (deleted): `Entity#levelCallback`, `Camera#eyeHeight/eyeHeightOld`; the rest of
  the AT targets are gone in 1.21.11 (`LevelRenderer#renderEntity`, `ModelBlockRenderer` cache, `Ingredient` values,
  `SpriteContents$Ticker`) or datagen-only.

## Ponder
- Scenes, tags and shared text registered by `index/ponder/SimPonderPlugin` (`SimulatedClient`: `PonderIndex.addPlugin`).
- Behaviour kept; the scene camera pitch fix (`PonderSceneMixin`) is still needed in Create Fly (`xRot + 90`).
- Create Fly loads ponder schematics without datafixing (same as its own): 1.21.1 `.nbt` files load as-is.

## PORT-TODOs
- none in these files.
