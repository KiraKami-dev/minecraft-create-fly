# aero-propeller (branch `p2-aero-propeller`)

Packages: `aeronautics/.../content/blocks/propeller/**` (propeller + gyroscopic propeller bearings, their contraption
entity, small andesite/wooden/smart propellers, `PropellerActorBehaviour`, sounds) and
`aeronautics/.../content/blocks/mounted_potato_cannon/**`.
Started from the propeller/potato-cannon part of the aero-content WIP commit `9b3b41e` (render-state renderers, client
split), then finished the potato cannon and the client registration. 0 compile errors in these packages
(checked with `-XDshould-stop.ifError=FLOW`). Thrust/airflow/physics code is unchanged from upstream.

## Client registration (orchestrator: call from the client entrypoint, after the default kinetic behaviours)
- `dev.eriksonn.aeronautics.content.blocks.propeller.PropellerClient#registerClient()`: thrust-direction value box
  (`PropellerBearingClient.ThrustDirectionScrollBehaviour`) + `PropellerBearingTooltipBehaviour` for `PROPELLER_BEARING`
  and `GYROSCOPIC_PROPELLER_BEARING`; `SmallPropellerTooltipBehaviour` for `ANDESITE_PROPELLER`, `WOODEN_PROPELLER`,
  `SMART_PROPELLER`.
- `dev.eriksonn.aeronautics.content.blocks.mounted_potato_cannon.MountedPotatoCannonClient#registerClient()`: goggle
  tooltip (`MountedPotatoCannonTooltipBehaviour`) for `MOUNTED_POTATO_CANNON`.
- The tooltip behaviours replace the default `KineticTooltipBehaviour` (same `TooltipBehaviour.TYPE`, last one wins),
  so call both after `AeroBlockEntityBehaviours.register()` (which gives every one of these types the default
  `KineticAudioBehaviour` + `KineticTooltipBehaviour`).

## Renderers / visuals (registration: aero-core)
All renderer ctors are `(BlockEntityRendererProvider.Context)`; BE ctors stay `(BlockEntityType, BlockPos, BlockState)`.
| BE / entity | renderer | visual | BER with Flywheel on? |
|---|---|---|---|
| `PROPELLER_BEARING` | `PropellerBearingRenderer` | `PropellerBearingVisual::new` | either (BER draws nothing then) |
| `GYROSCOPIC_PROPELLER_BEARING` | `GyroscopicPropellerBearingRenderer` | `GyroscopicPropellerBearingVisual::new` | either |
| `ANDESITE_PROPELLER` / `WOODEN_PROPELLER` | `AndesitePropellerRenderer` / `WoodenPropellerRenderer` | `AndesitePropellerVisual::new` / `WoodenPropellerVisual::new` | skip (upstream `visual(.., false)`) |
| `SMART_PROPELLER` | `SmartPropellerRenderer` | none | BER only |
| `MOUNTED_POTATO_CANNON` | `MountedPotatoCannonRenderer` | `MountedPotatoCannonVisual::new` | **keep** (`normal`): barrel, bellows and the loaded item are only drawn by the BER |
| entity `PROPELLER_CONTROLLED_CONTRAPTION` | `PropellerBearingContraptionEntityRenderer::new` (`EntityRendererProvider.Context`) | `PropellerBearingContraptionVisual::new` | upstream used Create's `ContraptionEntityRenderer`/`ContraptionVisual`; these subclasses carry the tilt + spin transform |

`AeroBlockEntityRenders` (aero-core, merged) matches the table. **`AeroEntityRenders` still registers Create Fly's
`ControlledContraptionEntityRenderer::new` / `ControlledContraptionVisual::new`** for `PROPELLER_CONTROLLED_CONTRAPTION`:
those drop the gyroscopic bearing's tilt (`tiltQuat` about the bearing face) and the renderer also ignores the
disassembly-slowdown angle (it lerps `entity.angle` instead of the overridden `getAngle`). Swap the two factories for
`PropellerBearingContraptionEntityRenderer::new` / `PropellerBearingContraptionVisual::new`
(`dev.eriksonn.aeronautics.content.blocks.propeller.bearing.contraption`), keeping `skipVanillaRender(entity -> false)`.

## API mappings (1.21.1 / Create -> 1.21.11 / Create Fly)
- `AbstractContraptionEntity#applyLocalTransforms(PoseStack, float)` (client) is gone: Create Fly puts the transform in
  `ContraptionEntityRenderer#transform(state, PoseStack)` (values copied in `extractRenderState(entity, state, pt)`,
  state extends `AbstractContraptionState`) and `ContraptionVisual#transform(PoseStack, float)`. See
  `PropellerBearingContraptionEntityRenderer` / `PropellerBearingContraptionVisual` (same pattern as Create Fly's
  `ControlledContraptionEntityRenderer`/`ControlledContraptionVisual`).
- `ControlledContraptionEntity(EntityType<?>, Level)` -> `(EntityType<? extends ControlledContraptionEntity>, Level)`.
- Bearing value box: the BE keeps a `ServerScrollOptionBehaviour<ThrustDirection>(ThrustDirection.class, be)` (common enum,
  no icons); the client `ScrollOptionBehaviour<ThrustDirection>` subclass maps icons and uses
  `ScrollOptionBehaviour.getMovementModeSlot()` (client static). `option.value` -> `option.getValue()`.
  `movementMode.setValue(2)` + `behaviours.remove(movementMode)` in `addBehaviours` still works (null-level safe).
- `MechanicalBearingBlockEntity#addToGoggleTooltip` -> client `MechanicalBearingTooltipBehaviour` (subclassed by
  `PropellerBearingTooltipBehaviour`); `BlockEntityBehaviour implements IHaveGoggleInformation` -> static helper
  `PropellerTooltipBehaviours.addPropellerInfo(behaviour, tooltip, sneaking)`.
- `ServerSpeedProvider.get()` -> `AllClientHandle.INSTANCE.getServerSpeed()` (common).
- `BearingSlowdownController.serializeIntoNBT/deserializeFromNBT(tag)` -> `serialize(ValueOutput)/deserialize(ValueInput)`.
- `NbtUtils.readBlockState(holderGetter, info.nbt().getCompound("Material"))` -> `getCompoundOrEmpty("Material")`.
- `BearingRenderer` (Create Fly) is generic (`BearingRenderer<T extends KineticBlockEntity & IBearingBlockEntity>`) with a
  `BearingRenderState.top` you can swap in `extractRenderState` (propeller bearing plate).
- Potato cannon: `CreateRegistries.POTATO_PROJECTILE_TYPE` -> `CreateRegistryKeys.POTATO_PROJECTILE_TYPE`
  (`com.zurrtum.create.api.registry`); `AllEntityTypes.POTATO_PROJECTILE.create(level)` ->
  `.create(level, EntitySpawnReason.TRIGGERED)`; `PotatoCannonItem.Ammo(stack, type)` and the projectile type record
  accessors are unchanged.
- `NBTHelper.writeEnum/readEnum(CompoundTag, ...)` with a `ValueOutput`: `view.putString(key, e.name())` and a
  name lookup falling back to the first constant (same NBT, the ponder's `modifyBlockEntityNBT` still matches).
- `SingleSlotContainer`: `inventory.write(view.child("inventory"))` / `inventory.read(view.childOrEmpty("inventory"))`
  (same NBT as the old `write(registries)` compound); `view.lookup()` for the `HolderLookup.Provider`.
- `ItemRenderer#renderStatic(stack, FIXED, light, overlay, ms, buffer, level, seed)` in a BER ->
  `context.itemModelResolver().updateForTopItem(itemState, stack, FIXED, level, null, seed)` in `extractRenderState`,
  then `itemState.submit(ms, queue, light, OverlayTexture.NO_OVERLAY, 0)` in `submit`. `ItemStackRenderState.displayContext`
  is package-private (Create Fly widens it; its AW is not transitive), so use `updateForTopItem` rather than
  `appendItemLayers`.
- Static `KineticBlockEntityRenderer.renderRotatingKineticBlock(be, shaftState, ms, vb, light)` in a non-kinetic BER ->
  extract `CachedBuffers.block(KINETIC_BLOCK, shaft(axis))`, `getAngleForBe`, `getColor(be)`, then
  `light; rotateCentered(angle, positive(axis)); color; renderInto(pose, vb)` in the geometry callback.
- `Block#neighborChanged(.., BlockPos fromPos, boolean)` -> `(.., @Nullable Orientation, boolean)`.
- `PropellerAirParticle.frictionScale/lifeTime` are compile-time constants, so the common `PropellerActorBehaviour`
  never loads the client particle class.

## Expectations on other areas
- aero-core: only the `AeroEntityRenders` factory swap above is missing (BE types, renders, default behaviours and the
  cannon's `SimInventoryService` registration are wired); `AeroLang`, `AeroSoundDistUtil.tickPropellerSounds(PropellerBearingBlockEntity, Object)`,
  `AeroConfig.server().physics.*`, `AeroAdvancements.*.awardToNearby(pos, level)`, `AeroBlockShapes.*`,
  `AeroPartialModels.*` used with their upstream signatures.
- aero-render: `BearingContraptionExtension#aeronautics$setPropeller` (mixin on `BearingContraption`) and
  `PotatoProjectileEntityExtension` setters used unchanged.

## PORT-TODOs
- none in these packages.
