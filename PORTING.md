# Create Aeronautics → Fabric 1.21.11 port

> **Start with [ROADMAP.md](ROADMAP.md)** for setup, workflow, status and the task list.
> Absolute paths below (`/Users/akshatkhair/Documents/Projects/minecraft-create-fly/...`) mean `<your clone>/...`;
> run `tools/setup-reference.sh` to create `reference/`.

Goal: run Create Aeronautics (Simulated Project) on **Minecraft 1.21.11 + Fabric**, on top of
**Create Fly** (Fabric port of Create, `maven.modrinth:create-fly:1.21.11-6.0.9-5`).

Stack being ported, in order:

1. `sable/` — Sable 2.0.6 physics engine (common + fabric + rapier sources merged into one Fabric mod,
   plus vendored `sable-companion` 1.6.0). Rust natives are prebuilt in
   `sable/src/main/resources/natives/` — never touch them, and never rename a Java class/method that
   declares `native` methods (JNI binds by name; see `physics/impl/rapier/Rapier3D.java`).
2. `simulated/` — later.
3. `aeronautics/` — later.

Veil (rendering/networking lib) does **not** exist for 1.21.11. It is replaced by an in-repo shim,
`dev.ryanhcode.sable.veillite` (see "Veil replacement").

## Build

```
export JAVA_HOME=$(/usr/libexec/java_home -v 21)
./gradlew :sable:compileJava --console=plain 2>&1 | tee /tmp/<yourname>-build.log
```

`-Xmaxerrs 100000` is set, so one compile reports every error. javac reports errors in waves
(missing symbols first, then type errors), so the count can go up after a fix — that is normal.

To see only errors in files you own:

```
grep -A4 "error:" build.log | grep -E "^/.*(sublevel/render|mixin/sublevel_render)/" ...
```

`validateAccessWidener` fails until `sable.accesswidener` is fixed; it is not part of `compileJava`.

## Reference sources (read-only, absolute paths — they are gitignored, so use these paths from worktrees)

| What | Path |
|---|---|
| Minecraft **1.21.11** decompiled (Mojang names) — the target | `/Users/akshatkhair/Documents/Projects/minecraft-create-fly/reference/mc-1.21.11` |
| Minecraft **1.21.1** decompiled — what the original code was written against | `/Users/akshatkhair/Documents/Projects/minecraft-create-fly/reference/mc-1.21.1` |
| Fabric API 0.141.6+1.21.11 sources | `/Users/akshatkhair/Documents/Projects/minecraft-create-fly/reference/fabric-api-1.21.11` |
| Original Sable (incl. neoforge module) | `/Users/akshatkhair/Documents/Projects/minecraft-create-fly/reference/sable-1.21.1` |
| Veil 1.21.1 (LGPL-3.0) — to understand what replaced APIs did | `/Users/akshatkhair/Documents/Projects/minecraft-create-fly/reference/veil-1.21.1` |
| Create Fly 1.21.11 (CC0) — has a working Flywheel/contraption renderer on 1.21.11; great reference for the new render pipeline | `/Users/akshatkhair/Documents/Projects/minecraft-create-fly/reference/create-fly-1.21.11` |
| Original Simulated/Aeronautics | `/Users/akshatkhair/Documents/Projects/minecraft-create-fly/reference/simulated-1.21.1` |

The fastest way to port a broken call: find the 1.21.1 method in `mc-1.21.1`, then look at the same
class in `mc-1.21.11` (or `diff` the two files) to see what replaced it.

## Rules

- **Mixins don't fail at compile time.** For every mixin you touch or own, open the target class in
  `mc-1.21.11` and verify: the target class still exists, every `method = "..."` name/descriptor exists,
  every `@At` target (`INVOKE`/`FIELD` descriptor, `ordinal`) still matches, and `@Shadow` members exist.
  Fix or rewrite them; if a feature's injection point no longer exists, re-implement it at the
  closest equivalent point. Only remove a mixin as a last resort, and list it in your report.
- Remember mixin config JSON (`sable.mixins.json`, `sable-fabric.mixins.json`) entries when you
  add/remove/rename mixin classes.
- Access widener entries (`sable.accesswidener`) must point at members that exist in 1.21.11
  (Mojang names, `accessWidener v2 named`).
- Keep behaviour. This is a port, not a rewrite: preserve the original logic wherever 1.21.11 allows.
- Don't stub things out just to compile. If something truly can't be ported yet, leave it compiling
  with a `// PORT-TODO(1.21.11): <what and why>` comment and list it in your final report.
- Stay inside the files/directories you own. If you need a change elsewhere (e.g. a shared util or the
  shim), make the minimal change, and call it out explicitly in your report so it can be merged carefully.
- Commit to your branch often with clear messages. No Claude/Anthropic attribution in commits.
- Match the existing code style (4-space indent, `final` params, existing naming).

## Common 1.21.1 → 1.21.11 changes (add to this list when you find more)

- `ResourceLocation` → `net.minecraft.resources.Identifier` (already applied by `tools/port-renames.sh`).
- `net.minecraft.Util` → `net.minecraft.util.Util` (applied).
- `RenderType` → `net.minecraft.client.renderer.rendertype.RenderType` (applied); rendering moved to
  `RenderPipeline`/`GpuDevice`/`GpuBuffer` (blaze3d) — `ShaderInstance`, `VertexBuffer`,
  `RenderSystem.setShader` etc. are gone.
- `CompoundTag.getX(key)` now returns `Optional<X>`; use `getXOr(key, default)`; `putUUID`/`getUUID` gone
  (use codecs: `UUIDUtil.CODEC` with `store`/`read`); NBT persistence largely moved to `ValueInput`/`ValueOutput`.
- `Level.isClientSide` is a method `isClientSide()`.
- `getMinBuildHeight()/getMaxBuildHeight()` → `getMinY()/getMaxY()`; `getMinSection()` → `getMinSectionY()`.
- `ClickEvent`/`HoverEvent` are now records per action (`new ClickEvent.RunCommand(...)`, `new HoverEvent.ShowText(...)`).
- `TicketType` / `Ticket` are no longer generic.
- `level.getProfiler()` → `Profiler.get()`.
- `registryOrThrow` → `lookupOrThrow`.

## Phase 2: Simulated + Aeronautics on Create Fly (not in the Gradle build yet)

`simulated/` and `aeronautics/` contain the 1.21.1 **common** sources, with the `src/generated/resources`
datagen output merged into `src/main/resources`. Already applied: `tools/port-renames.sh`,
`com.simibubi.create` → `com.zurrtum.create`, client-only Create classes → `com.zurrtum.create.client.*`
(Create Fly splits client code), a few known class moves, and `foundry.veil` → `dev.ryanhcode.sable.veillite`.
Offroad is out of scope for now.

What's left, by area:
- **Registration.** Create Fly has no Registrate (`CreateRegistrate`, `BlockStateGen`, `TagGen`,
  `AssetLookup`, `SharedProperties`, `BlockEntry`... don't exist). Replace `SimulatedRegistrate`/builders
  with plain static `register(...)` calls like `reference/create-fly-1.21.11/.../AllBlocks.java`.
  Datagen is dropped: we ship the already-generated JSON instead.
- **Resources → 1.21.11 formats.** Item model definitions (`assets/<ns>/items/*.json`, since 1.21.4),
  recipe JSON changes (ingredients are plain strings/tags, `result` uses `id`), equipment assets, etc.
- **Platform layer.** NeoForge impls in `reference/simulated-1.21.1/*/neoforge` (~80 files) need Fabric
  equivalents: the `service/Sim*Service` interfaces (13), energy/inventory/fluid wrappers (Fabric
  Transfer API), menus, creative tab, config (Forge Config API Port), events.
- **Client split.** Renderers, visuals, ponder and screens go into a client entrypoint, the way Create Fly
  registers them in `client/.../AllBlockEntityRenders.java`.
- **Create API differences.** 37 Create classes have no direct Create Fly equivalent
  (e.g. `AllTags` → `AllBlockTags`/`AllItemTags`, `CreateClient`, custom item renderers, `SafeBlockEntityRenderer`).
