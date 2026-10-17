# Create Aeronautics on Fabric 1.21.11: roadmap and progress

_Last updated: 2026-10-17 (Phase 1 S8a/S10/S12/S14 merged; Phase 2 ported: Simulated + Aeronautics compile and run on
server and client with Create Fly, automated playtest passes; a hands-on playtest is left). Keep this file current:
update the status tables when you claim, finish or discover a task._

## Goal

Run **Create Aeronautics** (the [Simulated Project](https://github.com/Creators-of-Aeronautics/Simulated-Project):
Simulated + Aeronautics) on **Minecraft 1.21.11 with Fabric**. Upstream only supports NeoForge 1.21.1, and none of its
dependencies existed for 1.21.11, so we port the whole stack ourselves:

| Layer | Upstream (1.21.1 NeoForge) | Here (1.21.11 Fabric) |
|---|---|---|
| Create | Create 6.0.10 | **[Create Fly](https://github.com/ZurrTum/Create-Fly) 1.21.11-6.0.9-5** (existing Fabric port; used as a dependency, not modified) |
| Physics | Sable 2.0.6 (+ Rust/Rapier natives) | **`sable/`**: ported, natives reused as-is |
| Rendering/network lib | Veil 4.3.2 | **replaced** by an in-repo shim, `dev.ryanhcode.sable.veillite` |
| Content | Simulated + Aeronautics 1.3.2 | **`simulated/`, `aeronautics/`** (Phase 2) |
| Offroad | Offroad | out of scope for now |

### Licensing: keep this repo private
- Simulated/Aeronautics **code is MIT**, but their **assets are All Rights Reserved**.
- Sable is **PolyForm Shield 1.0.0**; Veil-derived code in `veillite` is **LGPL-3.0**.
- Personal use is fine; **don't publish builds or make the repo public**. Collaborators are added by the repo owner
  (GitHub → Settings → Collaborators).

## Getting started (new contributor)

Requirements: JDK 21 (point `JAVA_HOME` at it; Gradle won't download one), git, ~16 GB RAM, macOS or Linux.

```bash
git clone https://github.com/KiraKami-dev/minecraft-create-fly && cd minecraft-create-fly
git checkout integration                  # the working trunk (see Workflow); Phase 2 work in progress is on p2-port
tools/setup-reference.sh                  # ~5-10 min: decompiles MC 1.21.11 + 1.21.1, clones upstream sources into reference/
./gradlew :sable:compileJava              # should be 0 errors
tools/e2e.sh server                       # headless smoke test (see Testing)
```

Then read **`PORTING.md`**: the porting rules, the 1.21.1 → 1.21.11 API cheat sheet, and where everything is.

`reference/` (gitignored) holds what you need to port anything:
`mc-1.21.11` (target, decompiled), `mc-1.21.1` (original), `fabric-api-1.21.11`, `sable-1.21.1`,
`simulated-1.21.1` (including the NeoForge modules), `veil-1.21.1`, `create-fly-1.21.11`.
The fastest way to port a call: find it in `mc-1.21.1`, then diff against the same class in `mc-1.21.11`.

## Workflow

- **`integration`** is the trunk. Branch from it, and merge back with a PR or a no-ff merge once your branch compiles.
- **`p2-port`** collects the Phase 2 work (one `p2-<area>` branch per area, merged into it) until Simulated and
  Aeronautics compile and pass e2e; then it merges into `integration`. Per-area porting notes (API mappings, PORT-TODOs)
  are in `docs/p2-notes/`.
- **`main`** only moves when a milestone passes (compiles + e2e). Don't commit to it directly.
- **One task per branch** (`<area>-<short-name>`). Claim a task by putting your name in the table below and pushing
  that change first, so two people don't do the same thing.
- **Never commit the `reference` folder or a `reference` symlink.** A tracked `reference` deletes everyone's local
  reference dir on merge.
- Before merging: `./gradlew :sable:compileJava` (and the Phase 2 modules once they're in the build),
  `./gradlew :sable:validateAccessWidener`, `tools/e2e.sh server`.
- Shared files that conflict often: `sable.accesswidener` (it's a list, so take the union and re-validate) and
  the `*.mixins.json` files.
- Commit messages: imperative, describe the change. No AI attribution trailers.

## Testing

`tools/e2e.sh [server|gametest|client|all|play]` drives the dev-only harness in `e2e/` (which loads Sable, Create Fly
and Simulated; Aeronautics joins once it compiles):
- **server**: boots a dedicated 1.21.11 server, runs a **mixin audit** (forces every mixin to apply), spawns
  sub-levels with `/sable spawn jenga|sphere|platform`, and runs 600 ticks. Logs: `e2e/run/server/logs/latest.log`.
- **gametest**: headless GameTest server running the `@GameTest` suite in `e2e/src/main/java/dev/mcf/e2e/gametest`
  (Sable assembly + physics tests ported from upstream), one PASS/FAIL line per test, non-zero exit if a required test
  fails, JUnit report in `e2e/run/gametest/gametest-report.xml`. `tools/e2e.sh gametest '<selector>' verify` reruns the
  selected tests 100× per rotation to catch flakiness.
- **flight**: real game window: builds an airship in view (deck, physics assembler, propeller bearing, burner,
  levitite), assembles it, lets it fly and takes screenshots `e2e/run/client/screenshots/flight-*.png`, then quits.
- **client**: opens the game in a flat world, spawns sub-levels in front of the camera, and saves 5 screenshots to
  `e2e/run/client/screenshots/` over ~20 s, then quits. Look at them.

- `tools/e2e.sh client flywheel` (or `server flywheel`): opt-in scene with Create Fly in the runtime (`-Pe2eScenario=flywheel`); the default scene stays Create-free.
- **play**: the same scene as `client`, but no screenshots and the game stays open, for manual testing.

Compiling proves nothing about mixins; the e2e run is the real check.

## Progress

Legend: ✅ done · 🔄 in progress · ⬜ todo · ⏸ deferred/gated

### Phase 1: Sable on 1.21.11

| ID | Task | Status | Owner | Notes |
|---|---|---|---|---|
| S1 | Import Sable + companion, Gradle/Loom 1.21.11 setup | ✅ | | Loom 1.16, Gradle 9.5.1, Mojang mappings |
| S2 | `veillite` shim: networking, registration, events | ✅ | | Fabric payloads; API mirrors Veil |
| S3 | `veillite` render shim + core sub-level rendering on the blaze3d pipeline | ✅ (untested in game) | | vanilla `SectionRenderDispatcher` per sub-level, custom `sub_level_terrain.vsh`, 4 pipelines in `SubLevelRenderPipelines` |
| S4 | World/plots/chunk tickets/storage/physics port | ✅ | | |
| S5 | Entity/camera/particle mixins (71+) | ✅ | | |
| S6 | Water occlusion (no water inside hulls) | ✅ (untested in game) | | depth-volume discard in custom translucent pipeline |
| S7 | **`:sable:compileJava` = 0 errors**, access widener validates | ✅ | | 2026-10-14 |
| S8 | **Runtime: dedicated server e2e passes** (all mixins apply, sub-levels simulate) | ✅ | | 2026-10-14: mixin audit passes; jenga/sphere/platform spawn, simulate 600 ticks and save; 13 mixins re-targeted (explosion → `ServerExplosion`, thunder → `tickThunder`, leash physics, …) |
| S9 | **Runtime: client e2e**: sub-levels visible, correct pose/lighting in screenshots | ✅ | | 2026-10-14: all client mixins apply; tower, platform and sphere render textured and lit in their physics poses (`tools/e2e.sh client`, screenshots in `e2e/run/client/screenshots/`) |
| S9a | ~~Bug: `/sable spawn sphere` hangs in mid-air~~ **Not a bug** | ✅ | | 2026-10-15: server and client poses both show the sphere falling from y −39.7 to rest at y −56.5 (bb min −60.0 = ground top) in under 2 s, before the first screenshot at tick 80. From eye height with no contact shadow it only looks like it floats. Side note: the sphere lands 3 blocks +x of the requested position, because the first block placed (−3,0,0) anchors the pose (same code upstream; jenga/platform reset their pose after placing blocks) |
| S8a | Dedicated-server e2e has **0 loaded sub-levels** during the 600 ticks (no player, so nothing keeps their chunks loaded); it checks mixins and spawning, not physics | ✅ | | 2026-10-17: the scene is force-loaded (38/38 sub-levels loaded); physics is also covered headlessly by `tools/e2e.sh gametest` (grid moved to the origin: Rapier is single-precision, so sub-levels at vanilla's ±15M test placement fell through floors); the e2e runs `forceload add -16 0 15 31`, and both server and client runs log `N sub-levels loaded, M moved since spawn` every 100 ticks (38 loaded, 38 moved through tick 600) |
| S8b | **Bug (upstream too):** a sub-level created in the same tick another is removed reuses the freed plot, but `ServerChunkCache.getChunk`'s 4-entry lookup cache still served the removed plot's chunk, so the new sub-level's blocks and block entity data were lost | ✅ | | 2026-10-17: plot chunks bypass that cache (`ServerChunkCacheMixin.getChunk`); regression test `plot_reuse_in_one_tick` is required. Minor, not fixed: `SubLevelAssemblyHelper.moveBlocks` registers POIs twice (log noise only) |
| S10 | Playtest: assemble, collide, stand on, ride a sub-level | ✅ | | 2026-10-17: automated as `tools/e2e.sh playtest` (`-Pe2eScenario=playtest`, `e2e/.../Playtest.java`, client e2e with a real player); 14 PASS/FAIL checks all pass: `sable assemble area` makes one sub-level and removes the world blocks, it falls 10 blocks and rests with bb min y −60.003 (ground top −60), a second one rests on the first (−57.003 vs −57.002) without pushing it down, the player stands on a resting platform (y = top, onGround), and is carried by a linear impulse (1.36 blocks, same as the platform), a 90° rotation (−90.0°, radius 2.00) and a 5-block translation. Riding: Sable has no seat/mount mechanism (seats are Simulated/Create, Phase 2). **Remaining manual checks:** walking on and off a moving/tilted sub-level, jumping and sprinting on it, camera feel while it rotates, ride/mount (Phase 2), multiplayer view of another player on a moving sub-level |
| S10b | **Bug:** a player standing on a sub-level that is teleported/translated (`/sable physics translation`) was not carried along until the client's next position reminder (~20 ticks) | ✅ | | 2026-10-17: the teleport changed the pose after the player ticked and `updateLastPose()` ran before the next player tick, so the delta was lost; `ServerPlayerMixin` now carries from the pose it last carried the player from. Found when the S10 playtest ran with Create Fly + Simulated loaded (timing-dependent) |
| S11 | Sodium support (sub-levels drawn under Sodium 0.8) | ✅ | | 2026-10-17: sub-levels still compile/draw through Sable's vanilla-based path; `impl.sodium` hooks `SodiumWorldRenderer` (`drawChunkLayer`, `scheduleRebuildForChunk`, `extractBlockEntities`, camera for the compile queue) and gives the vanilla `SectionRenderDispatcher` its own buffer pool (Sodium empties the vanilla pool, so async compiles never ran). `LevelRendererMixin` moved to `impl.shared`. `breaks sodium` removed. Verified with `E2E_SODIUM=1 tools/e2e.sh client`: tower/sphere/platform render textured and lit in the same poses as without Sodium. 2026-10-17 follow-up (`sable-sodium-2`): Flywheel visuals follow a yawed sub-level and S14 shading darkens a flipped platform under Sodium (`tools/e2e.sh client flywheel|shading` with `E2E_SODIUM=1`); Sodium no longer allocates sections for plot chunks (`RenderSectionManagerMixin`). Gaps under Sodium: no sky-light shadows (the shadow pass is skipped; Sodium's terrain shader has no shadowed variant) and no water occlusion in the world's water (S6 needs the vanilla translucent pipeline) |
| S12 | Flywheel (inside Create Fly) block entities on sub-levels | ✅ | | 2026-10-17: done by the Flywheel port of C10 (`sable-create.mixins.json`, `fabric/mixin/compatibility/create/flywheel_lib/`, `fabric/compatibility/create/flywheel/`): embedding of plot block entity visuals, plus light storage and shader overrides (`SableFlywheelLightStorage`, `SableFlywheelShaderOverrides`). The separate minimal integration from the Phase 1 branch was dropped in the merge; its `tools/e2e.sh client flywheel` scenario (motor + shafts + cogwheel assembled, yawed 45°, next to an un-assembled control line) stays as the visual check |
| S13 | Sky-light shadows from sub-levels | ✅ | | Top-down ortho depth pass of sub-level SOLID/CUTOUT sections (256x256 target, `SableSkyLightShadows`) before the opaque group; the terrain pipelines are swapped for `sable:core/terrain_shadowed` variants (also with the water-occlusion translucent one) by `water_occlusion.ChunkSectionsToRenderMixin`. On by default (`sub_level_skylight_shadows`). Gaps: single-block sub-levels cast none, sub-levels do not receive shadows from each other or the world; skipped under Sodium (no shadowed Sodium terrain shader). e2e: `tools/e2e.sh client shadows` |
| S14 | Dynamic directional shading of rotated sub-levels | ✅ | | 2026-10-17: Indigo replaces `SectionCompiler.compile`, so the `ModelBlockRenderer` mixins never ran. Fix: `RenderSectionRegionMixin` makes `getShade` return 1.0 while a sub-level section compiles, and `IndigoRenderContextMixin` gives shade-less quads the up normal so the shader re-shades them; `SUPPORTED=true`, config default on. Verified in client e2e with a platform flipped 180° overhead: ON shows a dark underside, OFF (`sub_level_dynamic_shading=false`) keeps the original bright top |
| S15 | Water occlusion near-plane clipping (no `GL_DEPTH_CLAMP` in blaze3d) | ⏸ | | minor |
| S16 | ImGui inspector windows | ✅ | | 2026-10-17: upstream has one inspector (`SubLevelContainerInspector`: client/server plot grid with loaded counts). Rendered with the imguimc mod (imgui-java + its own GL backend working on the 1.21.11 blaze3d device, FoundryMC, client-only), bundled as jar-in-jar with the imgui-java binding and natives (Loom strips imguimc's own nested jars, so they are nested explicitly; +~11 MB). `ImGuiEditorBackend` draws the shown inspectors from imguimc's post-render event; `VeilRenderSystem.hasImGui()` is true only on a client with imguimc. Off by default; `/sableinspector` (client command) toggles. Fixed an upstream bug in the inspector (`getAllSubLevels().get(index)` threw on the client; now `getSubLevel(x, z)`). Verified: `tools/e2e.sh client inspector` shows the window with Client/Server panels (38 loaded each); default client render and server audit unchanged. Not checked: Windows/Linux natives, mouse interaction (hover tooltip) with a free cursor |
| S17 | Review third-party compat mixins (sodium-extras, distant horizons, shoulder surfing, exposure, …) | ✅ | | 2026-10-17: inventory vs upstream. **Present and verified** (client e2e with `-Pe2eCompat`; mixin audit passed, 0 errors): shoulder surfing, jade (both loaded in client e2e); CC: Tweaked is compile-only checked (its Cobalt dependency is not on the dev runtime classpath, so it can't be loaded in e2e) (shoulder-surfing fix: a stale `ShoulderSurfingImplMixin` descriptor: `api.model.Perspective`/`api.client.IPerspectiveConfig` moved to `api.client.Perspective`/`api.config.IPerspectiveConfig`, which crashed the game on launch with Shoulder Surfing installed). **No 1.21.11 Fabric version, left out:** exposure (≤1.21.1), vista (≤1.21.1), sodium-extras (≤1.21.5), jade-addons (Fabric build is 26.x only; 1.21.1 then 26.1), etched (Forge/NeoForge only; mixin kept, never applies). **Obsolete:** iris `ExtendedShaderMixin` (served the `ShaderInstance` uniform refresh of the 1.21.1 vanilla chunk renderer; `ShaderInstance` no longer exists in 1.21.11), any Iris/Sodium sub-level rendering compat belongs to S11. **Nothing to port:** distant horizons (no upstream Sable mixin). NeoForge-only upstream compat (Create, pmweather, backpacks, flywheel) is Phase 2 / S12 |

### Phase 2: Simulated + Aeronautics on Create Fly

Both mods are in the Gradle build (`:simulated`, `:aeronautics`; 765 Java files, 86 + 29 mixin classes), depending on
`:sable` and Create Fly from Modrinth maven. Work happens on `p2-port` (see Workflow). **2026-10-17: both mods compile
(0 errors), boot on a dedicated server and on the client together with Sable and Create Fly (all mixins apply, no
data errors), the GameTest playtest suite passes (22/22 required), and an airship flies in the real client
(`tools/e2e.sh flight`).** Next: merge into `integration` (after Akshat's `merge-integration-p2`), then a human playtest.

| ID | Task | Status | Owner | Notes |
|---|---|---|---|---|
| R1 | Convert all JSON resources to 1.21.11 formats | ✅ | | 1,467 files, 0 validator errors (`tools/validate_resources.py`) |
| R1a | Datapacks load on a dedicated server (Create Fly's recipe formats differ from Create 6.0.10's) | ✅ | | 2026-10-17: 0 data errors with both mods. Fixed a `create:cutting` step without `processing_time`, a loot table dropping air, advancement backgrounds; `tools/validate_resources.py` now checks Create Fly recipe formats and that every referenced item/block/tag/recipe exists (conversion table in `docs/p2-notes/sim-data.md`) |
| R2 | OBJ models: `mecanum_wheel`, `plunger_wheel`, **`steering_wheel`** | ✅ | | Create Fly bundles NeoForge's OBJ loader (`"loader": "neoforge:obj"` works in any namespace), so restoring the loader keys was enough; mecanum/plunger/omni wheels are Offroad's |
| R3 | Register custom advancement triggers (`*_builtin`) | ✅ | | `SimAdvancementTriggers` |
| R4 | Check `c:` convention tags exist on Fabric (`c:armors`, `c:music_discs`, …) | ✅ | | all exist in convention tags v2; code uses `ConventionalItemTags` (see `docs/p2-notes/registry.md`) |
| R5 | Blocks whose models lost `render_type` need render layers set in code | ✅ | | Create Fly honours `render_type` in model JSON, so it was restored in the 13 models (4 Simulated, 9 Aeronautics) |
| C1 | Add `simulated` + `aeronautics` to `settings.gradle` (depends on `:sable` + Create Fly from Modrinth maven) | ✅ | | `fabric.mod.json` per mod; optional compat (JEI, CC: Tweaked, Nature's/Explorer's Compass, Sodium, Iris) compileOnly |
| C2 | Replace Registrate (`SimulatedRegistrate`, builders, `BlockEntry`) with static registration in Create Fly's style | ✅ | | registry fields are the objects themselves (`SimBlocks.X` is the block); a small `Simulated.getRegistrate()` helper keeps creative-tab sections, display targets, nav targets |
| C3 | Fabric impls of the 13 `service/Sim*Service` interfaces (inventory/fluid via Transfer API, menus, tabs, config, …) | ✅ | | `fabric/service`; item/fluid storages via Fabric Transfer API. Energy isn't exposed (no Fabric energy API among our deps) |
| C4 | Port `META-INF/accesstransformer.cfg` → access wideners | ✅ | | `:simulated:validateAccessWidener` and `:aeronautics:validateAccessWidener` pass; the `.cfg` files are deleted |
| C5 | Client entrypoints: block entity renderers, visuals, screens, ponder | ✅ | | render-state BERs, Flywheel visuals, client behaviours per BE type, menus, particles, ponder on Create Fly's ponder; the client e2e scene places a row of Simulated/Aeronautics blocks and they render (steering wheel OBJ, levitite, burner, propellers, …) |
| C6 | Create API gaps: the 37 Create classes with no direct Create Fly equivalent | ✅ | | mappings per area in `docs/p2-notes/` |
| C7 | 1.21.11 API fixes in Simulated (same patterns as Sable; see PORTING.md) | ✅ | | Veil render effects (contraption diagram, End Sea, lasers, physics staff) re-implemented on the 1.21.11 pipeline |
| C8 | Simulated mixins (86) verified against 1.21.11 **and Create Fly** | ✅ | | checked against the bytecode with javap; 5 removed as obsolete, several retargeted; dedicated server applies all (0 mixin errors) |
| C9 | Aeronautics: same as C2–C8 (181 files, 29 mixins) | ✅ | | 2026-10-17: compiles, all 29 mixins checked against the bytecode (7 removed: their targets, mostly Sodium/Iris chunk-layer hooks, don't exist on 1.21.11); Levitite Blend is a Create Fly fluid; levitite shimmer/ghost layers on sub-levels and the balloon hot-air effect re-implemented with their own pipelines; burner flame shader |
| C10 | Sable ↔ Create integration (contraptions on sub-levels, kinetics across sub-levels) | ✅ | | 2026-10-17: 139 of upstream's Create-compat mixins ported against Create Fly (`sable-create.mixins.json`, applied only when `create` is loaded); 8 skipped as unnecessary on Create Fly (`docs/p2-notes/sable-create.md`). Server and client e2e pass with Create. Fixed: Create block entities (deployer, item drain, ejector, arm, …) duplicated their contents when assembled into a sub-level |
| T1 | Server e2e with Sable + Simulated + Aeronautics + Create Fly | ✅ | | 2026-10-17: `tools/e2e.sh server` and `client`: mixin audit passes, 0 mixin errors, 0 data errors, 600 ticks with 38 sub-levels plus 14 mod blocks placed. Found and fixed a client crash when placing a torsion spring (inner "extra kinetics" block entities picked up the parent's Create Fly behaviours) |
| T2 | Playtest checklist: build a ship, assemble, propeller flight, balloon lift, levitite, steering | ✅ | | 2026-10-17, every checklist item exercised in the game: `tools/e2e.sh gametest` (22/22 required): physics assembler builds a ship that falls and rests, disassembles it again; steering wheel turned by a player drives its shaft to 90° and back (400/400 runs); torsion spring driven by a motor on a moving ship; propeller bearing lifts a ship (vs unpowered control); powered hot-air burner lifts a balloon (unpowered stays down); levitite raft hovers, an overloaded one sinks; upstream's extra-kinetics structure tests. `tools/e2e.sh flight` (real client): an airship (deck, physics assembler, propeller bearing + sail rotor, burner, levitite) is built, assembled and flies from y −59 to y 137 in ~9 s, rendering in the air with propeller airflow particles ("In Thrust We Trust" is awarded). Thrust direction checked against upstream's code: on that build −256 RPM lifts, as upstream (the default "pull when clockwise" mode negates thrust; a wrench toggles it; `docs/p2-notes/aero-propeller.md`). How flying feels is still worth a human playtest |

**Known Phase 2 gaps (PORT-TODOs):** energy storage not exposed (docking connector battery); server config not synced
to clients (Simulated/Aeronautics use Create Fly's config system); JEI doesn't show the portable engine dyeing recipe;
single-block sub-levels don't fade into the End Sea; two Create-compat mixins add a `write` override Create Fly lacks
(revisit if Create Fly adds it); levitite placed in the world (not on a ship) has no shimmer layer; the Levitite Blend
fog isn't cylindrical (1.21.11 fog has no shape option); filter items on brass tunnels/rollers and extra factory gauges
still duplicate on assembly; the client logs 38 harmless "movement packet for a non-existent sub-level" errors at
disconnect. `VoidAnchor` stays unregistered, as upstream.

## Suggested split for two people

- **Person A, physics/runtime:** S8 → S9 → S10, then S12/S11.
- **Person B, content:** C1 → C2 → C3/C4 → C5, then C6–C9. This can start on C2–C4 now (they don't need the game to
  boot), but needs S8 before anything can run.
- Ping each other before touching the other's area (`sable/` vs `simulated/`/`aeronautics/`).

## Decisions so far (don't undo without discussing)

- **Create Fly is a dependency, not forked.** If Create Fly lacks something, add a mixin/accessor on our side.
- **Veil is replaced, not ported**: only the subset Sable/Simulated use lives in `veillite`.
- **Rust natives are reused.** Never rename or re-sign Java classes/methods that declare `native` methods
  (`physics/impl/rapier/Rapier3D.java`); JNI binds by name.
- **No datagen.** We ship the 1.21.1 generated JSON, converted to 1.21.11 formats.
- **Fluids:** Create Fly uses Fabric droplets (81 per mB); recipe fluid amounts were multiplied by 81.
- Sable supports Sodium 0.8 (S11); sub-levels are drawn by Sable's own renderer, not by Sodium.
- **Registration is Create Fly style**: no Registrate; registry fields hold the objects themselves.
- **Configs use Create Fly's config system** (catnip `ConfigBase`), not Forge Config API Port, because Create's config
  classes only work with it.
- **Single source set**: client classes stay in their packages, but common code must not load them, so the dedicated
  server keeps booting. Create Fly's lang helpers (`LangBuilder`, so also `SimLang`) are client-only.
- **Mixins into Minecraft code keep remapping** (production jars are intermediary-named) and capture locals by type
  and position, never by name. Mixins into Create Fly classes use `remap = false` on Create-owned members.

## Running agents in Claude Code cloud sessions

If you use Claude Code cloud sessions (claude.ai/code) on this repo, the environment needs network access to:
`maven.fabricmc.net, api.modrinth.com, cdn.modrinth.com, piston-meta.mojang.com, piston-data.mojang.com,
libraries.minecraft.net, resources.download.minecraft.net, services.gradle.org, plugins.gradle.org,
repo.maven.apache.org, repo1.maven.org, maven.ryanhcode.dev, raw.githubusercontent.com` (or "Full" access), and the
Claude GitHub App must be installed on the repo to push. Ready-made task prompts are in `tools/cloud-prompts/`.
