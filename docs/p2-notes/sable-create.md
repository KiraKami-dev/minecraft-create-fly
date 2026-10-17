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
- `remap`: `remap = false` on injectors/shadows of Create-owned members, `@At(remap = true)` for Minecraft targets inside
  them; MC-inherited members keep the default.
- Class mapping: `com.simibubi.create` -> `com.zurrtum.create` (client classes `com.zurrtum.create.client`),
  `net.createmod.catnip` -> `com.zurrtum.create.(client.)catnip`, `dev.engine_room.flywheel` ->
  `com.zurrtum.create.client.flywheel`.

## Platform
- `SablePlatformImpl.isWrappedLevel`: `level instanceof com.zurrtum.create.catnip.levelWrappers.WrappedServerLevel` when
  `create` is loaded (via `fabric.compatibility.create.CreateLevelWrappers`), as upstream NeoForge did; was `false`.
