# p2 notes: items-misc

Scope: `simulated/.../content/{items,particle,display_sources,navigation_targets,item_attributes,components,worldgen}`.
0 compile errors in these files (verified with the registry fields stubbed the way requested below).

## Client registration (orchestrator: call from the client entrypoint)
- `dev.simulated_team.simulated.content.items.plunger_launcher.PlungerLauncherItemRenderer#registerClient()`:
  item model type + crossbow arm pose. Must run before the first resource reload.
- `dev.simulated_team.simulated.content.particle.SimParticleProviders#registerClient()`: particle providers (Fabric
  `ParticleFactoryRegistry`, call in `onInitializeClient`).
- `dev.simulated_team.simulated.content.display_sources.SimDisplaySourceRenders#registerClient()`: display link config
  widgets for every `SimDisplaySources` entry (including `AUGER_DISPLAY` -> Create Fly `ItemThroughputDisplaySourceRender`).

## Registry shape this code expects (`index/*`, registry agent)
- `SimParticleTypes`: plain fields, e.g.
  `public static final ParticleType<MagnetFieldParticleData> MAGNET_FIELD = register("magnet_field", MagnetFieldParticleData.CODEC, MagnetFieldParticleData.STREAM_CODEC);`
  same for `MAGNET_FIELD2` ("magnet_field2") and `AUGER_INDICATOR` ("auger_indicator"), with
  `Registry.register(BuiltInRegistries.PARTICLE_TYPE, id, FabricParticleTypes.complex(codec, streamCodec))`.
  The enum / `ParticleEntry` / `registerClientParticles` go away.
- `SimItemAttributeTypes.BLOCK_MASS/BLOCK_FRICTION/BLOCK_BOUNCINESS`: instances (`new BlockXItemAttribute.Type()`)
  registered into `com.zurrtum.create.api.registry.CreateRegistries.ITEM_ATTRIBUTE_TYPE` (not `CreateBuiltInRegistries`).
- `SimEntityTypes.PLUNGER`: `EntityType<LaunchedPlungerEntity>`; `SimItems.ROPE_COUPLING`: an `Item` (`RopeItem`).
- `SimItems.PLUNGER_LAUNCHER`: `stacksTo(1).durability(200)` as upstream; `enchantable(1)` is added by the item constructor.

## API mappings (old -> new)
Particles (`content/particle`)
- Create `ICustomParticleData(WithSprite)` / `getCodec` / `getStreamCodec` / `getMetaFactory` -> gone. Options classes
  only expose `static MapCodec CODEC` + `static StreamCodec STREAM_CODEC` and `getType()`; client providers are registered
  separately (Create Fly: mixin on `ParticleResources.registerProviders`; we use Fabric `ParticleFactoryRegistry`,
  `Factory(SpriteSet)` accepts the `FabricSpriteProvider`).
- `ParticleProvider#createParticle(..., double zSpeed)` -> extra trailing `RandomSource` param.
- `getRenderType()` + `ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT` -> `getLayer()` returning
  `SingleQuadParticle.Layer.TRANSLUCENT` (`getGroup()` is now the `ParticleRenderType`).
- `render(VertexConsumer, Camera, float)` -> `extract(QuadParticleRenderState, Camera, float)`;
  `renderRotatedQuad(buffer, quat, x, y, z, pt)` -> `extractRotatedQuad(state, quat, x, y, z, pt)`; `Camera#getPosition` -> `position`.
- `CatnipStreamCodecs.VEC3` -> `Vec3.STREAM_CODEC`; `StreamCodec.composite` now goes up to 12 fields.
- `new DustParticleOptions(Vector3f, scale)` -> `new DustParticleOptions(int rgb, scale)` (`Color#getRGB()`, alpha ignored).
- AW `ParticleEngine$SpriteParticleRegistration` was stale (class gone) and is removed.

Display sources (`content/display_sources`)
- `DisplaySource#initConfigurationWidgets` -> client `com.zurrtum.create.client.api.behaviour.display.DisplaySourceRender`,
  attached with `source.attachRender = render` (Create Fly `AllDisplaySourceRenders`). Label box:
  `SingleLineDisplaySourceRender`; extend it for extra widgets (`SimDisplaySourceRenders.NumericRender/SelectionRender`).
- `SingleLineDisplaySource#allowsLabeling` is `public abstract` in Create Fly: overrides must be `public`.
- `context.sourceConfig().getInt(k)` -> `getIntOr(k, 0)`.
- catnip `Lang`/`LangBuilder`/`LangNumberFormat` and `CreateLang` are **client-only** in Create Fly
  (`com.zurrtum.create.client.catnip.lang`), so `SimLang` is client-only too. Server-side text (display sources, item
  messages) builds `Component`s directly: `DisplaySourceLang` (same component tree as `LangBuilder`; numbers use a
  `Locale.ROOT` format with max 2 fraction digits).

Item attributes: `ItemAttributeType#streamCodec()` -> `packetCodec()`.

Items (`content/items`)
- `use` returns `InteractionResult` (`InteractionResultHolder.success/fail(stack)` -> `InteractionResult.SUCCESS/FAIL`);
  `UseAnim` -> `ItemUseAnimation`; `getEnchantmentValue()` -> `Item.Properties#enchantable(n)`;
  `canAttackBlock(state, level, pos, player)` -> `canDestroyBlock(stack, state, level, pos, LivingEntity)`;
  `LivingEntity.getSlotForHand(hand)` -> `hand.asEquipmentSlot()`; `EntityType#create(level)` -> `create(level, EntitySpawnReason.SPAWN_ITEM_USE)`.
- NeoForge `IItemExtension#onEntitySwing` -> Create Fly `com.zurrtum.create.foundation.item.SwingControlItem` (generic mixin on `LivingEntity#swing`).
- Create `CustomArmPoseItem#getArmPose` -> client map `com.zurrtum.create.client.AllExtensions.ARM_POSE.put(item, ArmPose.X)`.
- Create `CustomRenderedItemModelRenderer` + NeoForge `IClientItemExtensions` -> an `ItemModel` + `SpecialModelRenderer<T>`
  (`PlungerLauncherItemRenderer`, modelled on Create Fly `PotatoCannonModel`/`WrenchModel`): codec registered in
  `ItemModels.ID_MAPPER`, item definition `assets/simulated/items/plunger_launcher.json` = `{"model":{"type":"simulated:model/plunger_launcher"}}`.
  In `submit` the pose is at the model corner (`ItemTransform#apply` now includes the `-0.5` translate): `translate(.5,.5,.5)`
  gives the old `renderByItem` origin. Extra parts are baked in `Unbaked#bake` (`baker.getModel(id).bakeTopGeometry(...)`)
  and drawn with `SubmitNodeCollector#submitItem`. Textures under `textures/item/` live in the **items atlas** in 1.21.11:
  use `Sheets.translucentItemSheet()` (block atlas: `translucentBlockItemSheet()`); `RenderType.solid()` would sample the wrong atlas.
- `RenderSystem.getProjectionMatrix()` is gone; the first-person hand projection is
  `perspective(getFov(camera, pt, false), w/h, 0.05, 100)` (`GameRenderer` "3d hud" buffer). `GameRenderer#getFov` is private
  and returns `float` now: AW added under `# items-misc` (the old `# Physics Staff` line has descriptor `...)D` and matches nothing).
- `Minecraft#getTimer()` -> `getDeltaTracker()`; `ItemCooldowns#getCooldownPercent(Item, pt)` -> `(ItemStack, pt)`.
- `ShootableGadgetRenderHandler`: upstream listened to NeoForge `RenderHandEvent`; Create Fly hardcodes its own handlers in
  its `ItemInHandRendererMixin`, so ours is hooked by `mixin/plunger_launcher/ItemInHandRendererMixin`
  (HEAD of `renderArmWithItem`, cancellable; client list of `simulated.mixins.json`).
- Create `RaycastHelper` is client-only in Create Fly; `PlungerLauncherItem` keeps a copy of `rayTraceRange` (runs on the server).

Saved data / level caches (`navigation_targets/lodestone_compass_compatability`)
- `SavedData.Factory` + `@Override save(CompoundTag, Provider)` -> `new SavedDataType<>(id, () -> new X(level),
  CompoundTag.CODEC.xmap(tag -> load(level, tag), x -> x.save(new CompoundTag(), ...)), null)` (Sable's pattern for
  level-bound data). `null` DataFixTypes is fine: Fabric API's `DimensionDataStorageMixin` skips datafixing (Create Fly does the same).
- Flywheel `LevelAttached` is client-only (`com.zurrtum.create.client.flywheel.lib.util`); common code uses
  `com.zurrtum.create.catnip.data.WorldAttached`.
- `putUUID/getUUID` -> `store/read(key, UUIDUtil.CODEC)` (same int-array NBT); `getList(key, TAG_COMPOUND)` -> `getListOrEmpty(key)`.

Misc
- `Level#getSharedSpawnPos()` -> `getRespawnData().pos()`; `Direction#getNormal()` -> `getUnitVec3i()`.
- Game rules: `getRule(RULE_X).set(v, server)` -> `set(GameRules.X, v, server)`; `RULE_DOMOBSPAWNING` -> `SPAWN_MOBS`,
  `RULE_DO_TRADER_SPAWNING` -> `SPAWN_WANDERING_TRADERS`, `RULE_WEATHER_CYCLE` -> `ADVANCE_WEATHER`, `RULE_DAYLIGHT` -> `ADVANCE_TIME`.
- `DimensionTransition` -> `TeleportTransition`, `changeDimension` -> `teleport`;
  `setRespawnPosition(dim, pos, angle, forced, msg)` -> `setRespawnPosition(new ServerPlayer.RespawnConfig(LevelData.RespawnData.of(dim, pos, yaw, pitch), forced), msg)`.

## Notes / not done
- No PORT-TODOs. Nothing here has been run in game yet (item renderer, particles, display link GUI are untested).
- Kept upstream bug: `BlockBouncinessItemAttribute#appliesTo` compares FRICTION while `getAllAttributes` reads RESTITUTION.
- `aeronautics/.../GasDisplaySource` still overrides `initConfigurationWidgets`; port it the same way (a `DisplaySourceRender`).
