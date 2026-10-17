# p2 notes: platform

Scope: `simulated/.../{service,multiloader,network,events,api,command,compat,util,mixin_interface}` and the Fabric
platform code under `simulated/.../fabric/` (services, transfer wrappers, events, mixins, menu impl),
`META-INF/services/*`, `simulated-fabric.mixins.json`.

## Entrypoint calls (registry agent owns `fabric/SimulatedFabric*.java`)
- Common: nothing extra. `Simulated.init()` already calls `SimulatedCommonEvents.register()`, which now also registers
  every Fabric API listener (`fabric/events/SimFabricCommonEvents`).
- Client: call `SimulatedCommonClientEvents.register()` from `SimulatedFabricClient#onInitializeClient` (after
  `SimulatedClient.init()`). It registers ticks, HUD overlays, key mappings (`SimKeys.registerTo(KeyBindingHelper::registerKeyBinding)`
  -- don't register them again), use-block/use-item callbacks and fuel value tracking.
- Still needed from the entrypoint (upstream did them in NeoForge events): `SimArmInteractions.init()`,
  `SimAdvancements.register()` + `SimAdvancementTriggers.register()`, stats bootstrap (`NeoForgeSimStats.bootstrap()`),
  config load/reload (`ModConfigEvents` like `SableFabricClient`), config screen.

## Services (`fabric/service`, registered in `META-INF/services/<interface>`)
| Interface | Fabric impl / mapping |
|---|---|
| `SimAssemblyService` | vanilla sticky rule (`PistonStructureResolver#canStickToEachOther`; NeoForge `BlockState#canStickTo`) |
| `SimEntityDataSerialization` | `FabricTrackedDataRegistry.register(simulated:<name>, serializer)` (id-synced) |
| `SimEntityService` | `getCustomData`: persistent data attachment `simulated:custom_data` (NeoForge `getPersistentData`); `isFake`: Create Fly `FakePlayerHandler.has`; `loaderEntityTransform` now takes/returns vanilla `EntityType.Builder<T>` (`alwaysUpdateVelocity` = NeoForge `setShouldReceiveVelocityUpdates`) |
| `SimFluidService` | droplets (`mb * 81`); fluid in item via Create Fly `FluidHelper.getFluidInventory(stack)` (tank 0) |
| `SimInventoryService` | see Transfer below |
| `SimItemService` | `getBurnTime(stack)`: fuel values of the running server / joined client (since 1.21.2 fuel is level data; tracked by the events), new overload `getBurnTime(Level, stack)` = `level.fuelValues().burnDuration`; superheated: Create Fly tag `AllItemTags.BLAZE_BURNER_FUEL_SPECIAL` -> 3200 (replaces Create's data map) |
| `SimMenuService` | `getLoaderLinkedTypewriter(MenuType<LinkedTypewriterBlockEntity>, id, inv, be)` (Create Fly `foundation.gui.menu.MenuType`); `openScreen(player, vanilla MenuProvider, extraDataWriter)` -> Create Fly `MenuProvider.openHandledScreen` (menu must extend `MenuBase`; client needs `AllMenuScreens.register(type, factory)`) |
| `SimPlatformService` | `FabricLoader.isModLoaded` |
| `SimTabService` | creative tab looked up by id `simulated:main_tab` (the registry agent must register it under that id) |
| `SimpleResourceManager.Registry` | `ResourceLoader.get(CLIENT_RESOURCES).registerReloader(id, listener)`; `registerListener` now takes the id |
| `compat.SimPeripheralService` | CC:T `PeripheralLookup.get()` / `WiredElementLookup.get()` `.registerForBlockEntity`; takes `BlockEntityType<T>` (was a supplier) |
| `SimBlockStateService` | deleted (datagen only) |
| `SimConfigService` | not mine: registry agent must add the service file |

## Transfer (Fabric halves of `multiloader/*/neoforge`)
- Items: `registerInventory(type, getter)` -> `ItemStorage.SIDED.registerForBlockEntity(..., type)` with
  `fabric/transfer/AbstractContainerStorage` (a `SlottedStorage<ItemVariant>`, one wrapper per container, slots go
  through `insertSlot/extractSlot` so insert-only/extract-only slots and filters hold). Create Fly finds it through
  `ItemHelper.getInventory -> AllTransfer.getInventory` like any modded storage. Call after the BE type exists (its
  valid blocks are read at registration). Curried form `registerInventory(getter).accept(type)` kept.
- Inside a transaction stacks are set with `AbstractContainer#setItemSilently`; `onTransferCommitted(slot, old, new)`
  fires once on commit (Multi/SingleSlotContainer replay `onStackItemChange` + `setChanged`). Rolled-back simulations
  have no side effects (no `notifyUpdate`, no potato cannon reset). Fabric's own `InventoryStorage.of` would not work:
  it grows stacks in place, which skips `MultiSlotContainer`'s bookkeeping, and ignores extract-only slots.
- Fluids: `registerTank` -> `FluidStorage.SIDED` with `fabric/transfer/SingleTankStorage` (uses `SingleTank`'s snapshot
  hooks). `SingleTank.insert/extract` no longer apply a 0 amount (applyInsert retyped a full tank to the rejected
  fluid, which Fabric's `StorageUtil.move` would commit).
- Energy: no Fabric energy API in our deps -> `registerBattery` only records getters (`ENERGY_GETTERS`). PORT-TODO.
- Other inventories (`getInventory(be, dir)`, contraption storages): Create Fly `ItemHelper.getInventory(...)` /
  `MountedStorageManager#getAllItems()` give `Container`s; `fabric/transfer/FabricInventoryLoaderWrapper` maps the
  NeoForge `IItemHandler` + `ItemHelper.extract` calls to Create Fly's injected `BaseInventory` methods: simulate =
  `count*`/`countSpace`, `ExtractionCountMode.EXACTLY` = `preciseCount/preciseExtract`, slot insert =
  `insert(stack, max, slot, slot + 1)`, slot extract = `removeItem` (storage-backed containers use a transaction there).
- Serialization: containers/tank/battery keep `write(provider)/read(provider, tag)` and gain
  `write(ValueOutput)/read(ValueInput)` (same keys; use `inventory.write(view.child("Inventory"))`).
  `ItemStack.save/parseOptional` -> `tag.store/read("item", ItemStack.CODEC, provider.createSerializationContext(NbtOps.INSTANCE))`.
- `MultiSlotContainer#getSlot(int)` -> `getContainerSlot(int)` (clashes with `Container#getSlot` returning `SlotAccess`).
  `clearContent` clears slots in place (it filled the list with the parentless `ContainerSlot.EMPTY`); `read` updates
  the filtered/populated views instead of marking every listed slot populated.
- `CommonRedstoneBlock` now extends Create Fly `RedStoneConnectBlock` + `WeakPowerControlBlock` (dispatched by Create
  Fly's `RedstoneWireBlockMixin`/`SignalGetterMixin`), replacing the NeoForge `IBlockExtension` mixin. Vanilla's wire
  check has no level/pos, so `commonConnectRedstone` gets `EmptyBlockGetter.INSTANCE`/`BlockPos.ZERO` (no impl uses them).

## Events (NeoForge -> Fabric)
- `ChunkEvent.Load(isNewChunk)` -> `ServerChunkEvents.CHUNK_LOAD` (false) + `CHUNK_GENERATE` (true): new chunks are
  reported twice; only `EndSeaPreset` reads the flag (acts on `true`).
- `PlayerLoggedInEvent` -> `ServerPlayConnectionEvents.JOIN`; `RegisterCommandsEvent` -> `CommandRegistrationCallback`;
  `ServerStoppedEvent` -> `SERVER_STOPPED`; `ServerTickEvent.Post` -> `END_SERVER_TICK` (loop levels as upstream);
  `OnDatapackSyncEvent` -> `SYNC_DATA_PACK_CONTENTS`; `AddReloadListenerEvent` -> `ResourceLoader.get(SERVER_DATA)`.
- `ModifyDefaultComponentsEvent` -> `DefaultItemComponentEvents.MODIFY`; the `DataComponentPatch.Builder` handed to
  `modifyDefaultComponents` is applied onto Fabric's `DataComponentMap.Builder` (removals -> `set(type, null)`).
- `PlayerInteractEvent.RightClickBlock` -> `UseBlockCallback` (common: spring adjust; client: hold interaction + honey
  glue, result SUCCESS = cancel). `RightClickItem` -> `UseItemCallback`.
- Client: `ClientTickEvent.Pre/Post` -> `ClientTickEvents.START/END_CLIENT_TICK`; `RenderGuiEvent.Post` ->
  `HudElementRegistry.addLast`; `RegisterGuiLayersEvent.registerAbove(HOTBAR)` -> `attachElementAfter(VanillaHudElements.HOTBAR)`
  (`LinkedTypewriterItemBindHandler::renderOverlay`; `LayeredDraw` is gone); `RegisterKeyMappingsEvent` -> `KeyBindingHelper`.
- No Fabric API equivalent -> client mixins in `fabric/mixin/client`: `InputEvent.Key` = TAIL of
  `KeyboardHandler#keyPress(long, int, KeyEvent)`; `InteractionKeyMappingTriggered` (use key) = `Minecraft#startUseItem`
  before the first `LocalPlayer#getItemInHand` (cancel = no swing); `UseItemOnBlockEvent` ITEM_AFTER_BLOCK =
  `MultiPlayerGameMode#performUseItemOn` at `new UseOnContext` (CONSUME); `ItemTooltipEvent` = `ItemStack#getTooltipLines`
  RETURN ordinal 1 (Fabric's `ItemTooltipCallback` lacks the player, which is null for the creative search tree).
- `InputEvent.MouseButton.Post` not hooked: `onAfterMouseInput` is empty.

## NeoForge-only mixins (`simulated-neoforge.mixins.json`)
- `harvesters.BlockBreakingTileEntityMixin` -> `BlockBreakingKineticBlockEntityMixin` (Create Fly still compiles the
  drop lambda as `lambda$onBlockBroken$0(Vec3, ItemStack)`, checked with javap); `SawTileEntityMixin` ->
  `SawBlockEntityMixin#dropItemFromCutTree`; `SableHarvesterMixin` -> `HarvesterBlockEntityMixin`.
- `SableHarvesterTickerMixin`: PORT-TODO, Sable's Create harvester compat (`HarvesterTicker`) isn't ported.
- `self_mixins.HoneyGlueEntityMixin` (`IEntityWithComplexSpawn`, spawn data = bounds) -> no mixin:
  `EntityTrackingEvents.START_TRACKING` sends a `HoneyGlueSyncBoundsPacket` right after the add-entity bundle.
  Don't return a bundle from `getAddEntityPacket`: `ServerEntity#addPairing` already puts it in a bundle (nested
  bundles can't be encoded); Create Fly registers a whole new add-entity packet type for super glue instead.
- `self_mixins.PlungerLauncherItemMixin`: `onEntitySwing` is done by `PlungerLauncherItem implements SwingControlItem`
  (items-misc); the renderer half is the item model.
- `multiloader.CommonRedstoneBlockMixin`: see `CommonRedstoneBlock` above (no mixin needed).
- `tooltip_flag.ClientTooltipFlagMixin`: not needed; vanilla/Fabric tooltips use `TooltipFlag.Default`, covered by the
  common `TooltipFlagDefaultMixin`.

## Misc API mappings
- `player.canInteractWithBlock(pos, d)` -> `isWithinBlockInteractionRange(pos, d)`; `ServerPlayer#serverLevel()` -> `level()`.
- `GameProfile#getId()` -> `id()` (record); `SoundEvent#getLocation()` -> `location()`; Create Fly
  `AllSoundEvents.SoundEntry#getId()` -> `getMainEvent().location()`.
- `cmd.requires(s -> s.hasPermission(2))` -> `requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))`;
  catnip `CatnipServices.PLATFORM.isDevelopmentEnvironment()` -> `FabricLoader.getInstance().isDevelopmentEnvironment()`.
- `Direction.fromDelta(x, y, z)` (unit vectors) -> `Direction.getNearest(x, y, z, null)`; `getNormal()` -> `getUnitVec3i()`.
- NeoForge `ILevelReaderExtension#isAreaLoaded(c, r)` -> `level.hasChunksAt(c.offset(-r,-r,-r), c.offset(r,r,r))`;
  `getMaxBuildHeight()` (exclusive) comparisons -> `getMaxY()` (inclusive).
- `KeyMapping#matches(key, scan)` -> `matches(new KeyEvent(key, scan, 0))`; `matchesMouse(b)` ->
  `matchesMouse(new MouseButtonEvent(0, 0, new MouseButtonInfo(b, 0)))`; `ClientInput#shiftKeyDown` -> `keyPresses.shift()`.
- `CreateClient.VALUE_SETTINGS_HANDLER.showHoverTip(tip)` -> `com.zurrtum.create.client.Create.VALUE_SETTINGS_HANDLER.showHoverTip(mc, tip)`.
- `Registry#get(Identifier)` (now `Optional<Reference>`) -> `getValue(Identifier)`.
- Block entity item data: `DataComponents.BLOCK_ENTITY_DATA` is a `TypedEntityData<BlockEntityType<?>>`:
  `copyTag()` -> `copyTagWithoutId()`, `CustomData.set(...)` -> `item.set(..., TypedEntityData.of(type, tag))`.
- Veil `CodecReloadListener` -> vanilla `SimpleJsonResourceReloadListener(codec, FileToIdConverter.json(path))`.
- `SpriteContents.Ticker` -> `SpriteContents.AnimationState` (`SpriteContentsExtension`).
- Compat mods on Fabric: Nature's/Explorer's Compass `FOUND_X/FOUND_Z` components and `NATURES_COMPASS_ITEM` /
  `EXPLORERS_COMPASS_ITEM`; JEI 27 `jei_mod_plugin` (mod aliases now cover Simulated + mods depending on it).
- AW (`# platform`): `Entity#levelCallback` (was in the NeoForge AT, missing from the widener).

## Waiting on other areas (the only compile errors left in these files)
- `fabric/menu/LinkedTypewriterMenuImpl`: needs `LinkedTypewriterMenuCommon(MenuType<LinkedTypewriterBlockEntity>, int,
  Inventory, LinkedTypewriterBlockEntity)` with Create Fly's `MenuType` (like `GhostItemMenu`); register the type as
  `LinkedTypewriterMenuCommon::create` and the screen with `AllMenuScreens.register`.
- `compat/.../TorsionSpringPeripheral`, `network/packets/SteeringWheelPacket`: `angleInput` must become a
  `ServerScrollValueBehaviour` (has `setValue/getValue`).
- `mixin_interface/diagram/VisualManagerExtension`: Sable's Flywheel `SubLevelEmbedding` isn't ported.
- `SimpleSubLevelGroupRenderer.renderChain/renderGroup` take a vanilla `RenderTarget` (callers: `DiagramScreen`,
  `EndSeaShadowRenderer`).

## PORT-TODOs
- `fabric/service/FabricSimInventoryService#registerBattery`: no Fabric energy API (Team Reborn Energy would be the
  equivalent, needs a compileOnly dep).
- `util/SimpleSubLevelGroupRenderer#renderGroup`: offscreen sub-level rendering (contraption diagram, end sea shadow
  map) only clears the target; needs an offscreen entry point in Sable's `SubLevelRenderDispatcher`. Takes a vanilla
  `RenderTarget` now (was Veil `AdvancedFbo`).
- `fabric/mixin/harvesters/HarvesterBlockEntityMixin`: Sable `HarvesterTicker#dropItem` wrap (SableHarvesterTickerMixin).
- `compat/jei/SimulatedJEI`: portable engine dyeing recipes not shown (the special dyeing recipe isn't ported).
