# p2 notes: aero-render

Scope: `aeronautics/.../{mixin/**, content/ponder/**, index/client/AeroRenderTypes, content/blocks/levitite/LevititeShaderManager,
content/blocks/hot_air/balloon/effect/**}`, `aeronautics.mixins.json`, `aeronautics.accesswidener`, `assets/aeronautics/shaders/**`.
Also touched (outside the list, single consumer): `index/AeroPonderScenes` (ponder registration), new
`mixinterface/BoilerDataExtension`, new `content/blocks/levitite/render/LevititeRenderer`. 0 compile errors in these files;
`:aeronautics:validateAccessWidener` passes. Not run in game yet (module does not compile as a whole).

## Client hooks to wire / remove (aero-core, `AeronauticsClient`)
- Remove `onVeilRenderLevelStage(... ClientBalloonEffectRenderer.onRenderLevelStage ...)`: the balloon effect is drawn from
  `mixin/levitite/VanillaSubLevelRenderDispatcherMixin` (after the opaque section group = upstream `AFTER_SOLID_BLOCKS`).
- Remove `onVeilRegisterBlockLayers` / `onVeilRegisterFixedBuffers` (`AeroRenderTypes.levitite()/levititeGhosts()` are gone:
  there are no custom chunk layers on 1.21.11). Keep `LevititeShaderManager.tick()` in the client tick.
- `AeroMixinPlugin`: the `render.vanilla/sodium/iris` packages are empty now (see below); the gating can stay.

## Levitite rendering (Veil tessellation shader -> blaze3d)
- `ChunkSectionLayer` is a closed enum (Sable's `SubLevelRenderPipelines.get` and Sodium switch over it), so no levitite
  chunk layers: levitite blocks stay in SOLID (upstream's fallback when the shader was off) and the effect is extra draws.
- `LevititeRenderer` (hooked into Sable's `VanillaSubLevelRenderDispatcher`: `prepareSections` TAIL, `renderSectionLayerGroup`
  RETURN, `free` TAIL) re-draws the compiled SOLID `SectionBuffers` of every visible chunked sub-level with
  `AeroRenderTypes.LEVITITE` (layer 0, after the opaque group, LEQUAL + depth write, z nudged towards the camera) and, while
  `LevititeShaderManager#needsLayers`, twice with `LEVITITE_GHOSTS` (layers +1/-1, after the translucent group, no cull, pass
  **without depth attachment** so the scene depth can be sampled as `DepthSampler` for the soft fade; upstream disabled the
  depth test). Uniforms: own `SableSubLevel` ring (`SubLevelRenderPipelines.createUniformStorage()` +
  `VanillaSubLevelRenderDispatcher.getAnchoredTransformation`), `writeChunkSections`, and a `LevititeInfo` std140 block
  (`LevititeUniform`, mat4/vec4 only).
- `aeronautics:core/levitite.{vsh,fsh}` + `include/levitite.glsl`: no tessellation in blaze3d. The vertex shader drops every
  quad whose UV is outside the 4 levitite atlas sprites (`levitite`, `pearlescent_levitite`, `*_connected`; rects in the UBO)
  by moving it out of the clip volume, and does the old vertex + tessellation-evaluation work per vertex (velocity trail,
  noise wobble); the layer-0 shimmer noise (`ghostNoiseMagnitude`) moved to the fragment shader (per pixel instead of the
  4x4 tessellated grid). Local position = anchor-relative plot position + (anchor - rotation point).
- Skipped while a shader pack is active (`ShadersModHelper.isShaderPackInUse()`).
- `LevititeShaderManager`: `ShaderInstance` uniforms -> `createUniform(subLevel, anchor, layer, spriteRects, pt)`; the
  gravity vector is per sub-level now (upstream: one static, overwritten by every sub-level).
- `Block#shouldRenderFace(state, neighbour, dir)` lost the per-state-pair cache (keyed by face shapes now, full-block
  neighbours return first), so `BlockMixin` decides up front at HEAD: levitite next to non-levitite -> drawn unless
  `skipRendering` (same result as upstream's `joinIsNotEmpty || l1 ^ l2`).

## Hot air balloon effect
- `AdvancedFbo` -> two `TextureTarget`s (overlay with depth, scene colour copy). Overlay pass: `ClientBalloonEffectRenderer.HOT_AIR_OVERLAY`
  (`POSITION_TEX_COLOR_NORMAL` quads, `DynamicTransforms` via `writeTransform` per balloon, `HotAirInfo` UBO = scroll/cutoff,
  `withDepthBias(-0.5, -30)` for the old polygon offset). blaze3d only culls back faces: `HeatedCulledRenderRegionBuilder`
  winds its quads the other way round instead of `glCullFace(GL_FRONT)`.
- Veil post `aeronautics:soft_light` -> `SOFT_LIGHT` fullscreen pass (`core/screenquad`, EMPTY/TRIANGLES, `draw(0, 3)`) into the
  main target without depth attachment, reading a `copyTextureToTexture` copy of the main colour + both depth textures.
- `VertexBuffer` -> `GpuBuffer` from `MeshData#vertexBuffer()` + shared sequential quad index buffer.

## Other API mappings
- `LevelChunk#setBlockState(pos, state, boolean)` -> `(pos, state, int flags)`; `LevelChunk.level` is package-private.
- `Minecraft#getSituationalMusic`: no `Musics.GAME` GETSTATIC; music comes from the `BACKGROUND_MUSIC` environment attribute
  (`BackgroundMusic#select(..).orElse(null)`); the overworld default is `Musics.GAME` (records: compare with `equals`).
- `TerrainParticle` extends `SingleQuadParticle` (mixin superclass changed, ctor `(level, x, y, z, sprite)`).
- `Entity#hurt` -> `hurtServer(ServerLevel, DamageSource, float)` (Create Fly potato projectile).
- `Tool.Rule.overrideSpeed(TagKey, f)` -> `(HolderSet, f)`: use the bootstrap `HolderGetter<Block>` local of
  `ShearsItem#createToolProperties` (`getOrThrow(tag)`).
- Create Fly moved `BoilerData#addToGoggleTooltip` into client `FluidTankTooltipBehaviour#addBoilerDataTooltip(data, tooltip, size)`
  (same field/call order); `BoilerData#write(ValueOutput)` / `read(ValueInput, int)` write into the boiler's own view.
- Ponder: `getWorld` -> `getLevel`, `Direction#getNormal` -> `getUnitVec3i`, `Create.RANDOM` -> local `Random`,
  `EntityType#create(level)` -> `create(level, EntitySpawnReason.LOAD)`, `getCompound(k)` -> `getCompoundOrEmpty(k)`,
  BE NBT round trip via `TagValueOutput`/`TagValueInput` (as Simulated), scenes keyed by `RegisteredObjectsHelper::getKeyOrThrow`.

## Mixins (aeronautics.mixins.json)
| mixin | target (1.21.11 / Create Fly / Sable) | status |
|---|---|---|
| balloon.LevelChunkMixin (client) | `LevelChunk#setBlockState(BlockPos,BlockState,I)` HEAD, `LevelChunkSection#setBlockState(IIIBlockState)` | params ported |
| balloon.SimAssemblyHelperMixin | Sim `disassembleSubLevel` `SubLevel#getPlot` ord 1, local `AssemblyTransform` by type | verified, remap=false |
| balloon.SubLevelAssemblyHelperMixin | Sable `needsBitSet` HEAD, `moveOtherStuff` TAIL (local `BoundedBitVolume3i` live at last return) | verified, remap=false |
| custom_situational_music.MinecraftMixin (client) | `getSituationalMusic` `Optional#orElse` | **retargeted** (was GETSTATIC `Musics.GAME`) |
| custom_situational_music.MusicManagerAccessor (client) | `nextSongDelay`, `currentMusic` | verified |
| flywheel_block_entity.FlywheelBlockEntityMixin | Create Fly `read(ValueInput,Z)` HEAD | retargeted, remap=false |
| item_converter.ItemEntityMixin | `ItemEntity#tick` TAIL | verified |
| levitite.BlockMixin | `Block#shouldRenderFace(BlockState,BlockState,Direction)` HEAD | **rewritten** (see above) |
| levitite.ItemEntityMixin | `getDefaultGravity` HEAD, `tick` `ItemEntity#move` | verified |
| levitite.TerrainParticleMixin (client) | `<init>(ClientLevel,DDDDDD,BlockState,BlockPos)` TAIL + `tick` override | superclass -> `SingleQuadParticle` |
| levitite.WaterWheelBlockEntityMixin | Create Fly `getFlowVectorAtPosition` RETURN, local `fluid` (names kept) | verified, remap=false |
| levitite.VanillaSubLevelRenderDispatcherMixin (client) | Sable `prepareSections` TAIL, `renderSectionLayerGroup` RETURN, `free` TAIL | **new** (replaces VanillaChunkedSubLevelRenderDataMixin) |
| levitite.VanillaChunkedSubLevelRenderDataMixin | `renderChunkedSubLevel(RenderType,ShaderInstance,..)` | **removed** (gone in Sable's port) |
| levitite.ChunkRenderTypeSetAccessor | NeoForge `ChunkRenderTypeSet` | **removed** |
| levitite.PonderUIMixin | `PonderUI#renderScene` | **removed**: no global levitite shader state; ponder shows solid levitite (= upstream with the shader off) |
| ponder.DisplayWorldSectionInstructionMixin (client) | `firstTick` `Optional.ofNullable`, `mergeOnto` | verified, moved to client |
| ponder.TickingInstructionAccessor / TickingInstructionMixin (client) | `remainingTicks`; `isComplete` HEAD | verified, moved to client |
| ponder.WorldSectionElementImplAccessor (client) | `centerOfRotation` | verified, moved to client |
| potato_cannon_projectiles.PotatoProjectileEntityMixin | `onHitEntity` `Entity#hurtServer(ServerLevel,DamageSource,F)` AFTER; `additionalDamageMult` | retargeted |
| propeller_bearing.BearingContraptionMixin | Create Fly `assemble` `expandBoundsAroundAxis(Direction$Axis)` AFTER; `sailBlocks`, `isWindmill` | descriptor package fixed |
| propeller_bearing.PropellerBearingContraptionEntityMixin | Sable `KinematicContraption#sable$liftProviders` | verified |
| propeller_collision.ContraptionColliderMixin | Create Fly `collideEntities`: `isAlive` x1, `getDeltaMovement` x5, `setDeltaMovement` x3, `collide` x2 | retargeted; no overlap with Sable's redirects |
| sable_hooks.SableCommonEventsMixin | Sable `handleBlockChange` HEAD | verified, remap=false |
| shears_break_envelopes.ShearsItemMixin | `createToolProperties` `List.of(4)` + local `HolderGetter` | ported |
| steam_vent.BoilerDataMixin | Create Fly `evaluate` (`BlockState#is(Block)` ord 0, local BlockState ord 1), `isActive`, `getEngineEfficiency`/`updateOcclusion` FIELD `attachedEngines`, `clear`, `write(ValueOutput)`, `read(ValueInput,I)` | retargeted (key `SimVents` kept); implements `BoilerDataExtension` |
| steam_vent.FluidTankTooltipBehaviourMixin (client) | Create Fly `addBoilerDataTooltip`: FIELD `attachedEngines` ord 0/2, `getEngineEfficiency`, `Math.max(II)`, `forGoggles(List)` ord 2 | **new** (tooltip half of upstream BoilerDataMixin) |
| render.vanilla.LevelRendererMixin | `LevelRenderer#renderSectionLayer` + `ShaderInstance` | **removed** (gone; world levitite is not drawn, see TODO) |
| render.sodium.DefaultMaterialsMixin / SodiumWorldRendererMixin | Sodium material mapping of the levitite chunk layers | **removed**: no levitite chunk layers exist (and Sable doesn't render under Sodium yet, S11) |
| render.iris.WorldRenderingPhaseMixin | Iris phase of the levitite chunk layers | **removed** (same reason; the effect is skipped under shader packs) |

Remap policy as in ponder-mixins.md: Create Fly / Sable / Sim name-only selectors and shadows `remap = false`; `@At`s whose
descriptor names Minecraft classes `remap = true`.

## Access widener
- `META-INF/accesstransformer.cfg` deleted: the particle registration line is in the AW
  (`ParticleResources$SpriteParticleRegistration`), the datagen and `RenderType#create` lines are obsolete.

## PORT-TODOs
- World (non sub-level) levitite has no shimmer layer: needs per-section levitite meshes (compile hook) for world sections;
  re-drawing every world section would be too expensive. Ships (sub-levels) get all three layers.
- The levitite pass re-draws all SOLID sections of visible chunked sub-levels (cheap reject in the vertex shader), 3x while moving.
- Not run in game: levitite, balloon overlay, ponder and the mixins are compile/bytecode-verified only.
