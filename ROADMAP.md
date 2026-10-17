# Create Aeronautics on Fabric 1.21.11: roadmap and progress

_Last updated: 2026-10-17 (Phase 2: Simulated compiles and boots on a dedicated server with Create Fly; Sable ↔ Create
integration ported; Aeronautics in progress). Keep this file current: update the status tables when you claim, finish
or discover a task._

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
- **client**: opens the game in a flat world, spawns sub-levels in front of the camera, and saves 5 screenshots to
  `e2e/run/client/screenshots/` over ~20 s, then quits. Look at them.

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
| S8a | Dedicated-server e2e has **0 loaded sub-levels** during the 600 ticks (no player, so nothing keeps their chunks loaded); it checks mixins and spawning, not physics | ✅ | | 2026-10-17: the scene is force-loaded (38/38 sub-levels loaded); physics is also covered headlessly by `tools/e2e.sh gametest` (grid moved to the origin: Rapier is single-precision, so sub-levels at vanilla's ±15M test placement fell through floors) |
| S8b | **Bug (upstream too):** a sub-level created in the same tick another is removed reuses the freed plot, but `ServerChunkCache.getChunk`'s 4-entry lookup cache still served the removed plot's chunk, so the new sub-level's blocks and block entity data were lost | ✅ | | 2026-10-17: plot chunks bypass that cache (`ServerChunkCacheMixin.getChunk`); regression test `plot_reuse_in_one_tick` is required. Minor, not fixed: `SubLevelAssemblyHelper.moveBlocks` registers POIs twice (log noise only) |
| S10 | Manual playtest: assemble, collide, stand on, ride a sub-level | ⬜ | | after S9 |
| S11 | Sodium support (sub-levels currently not drawn under Sodium; Sable `breaks` Sodium) | ⬜ | | needs a Sodium 0.8 chunk-render integration |
| S12 | Flywheel (inside Create Fly) block entities on sub-levels | 🔄 | | 2026-10-17: upstream's Flywheel embedding mixins ported against Create Fly's bundled Flywheel as part of C10 (`sable-create.mixins.json`); not yet seen in game |
| S13 | Sky-light shadows from sub-levels | ⏸ | | hard-off; needs a shadow-map pass + terrain pipeline variant (see `SableSkyLightShadows`) |
| S14 | Dynamic directional shading of rotated sub-levels | ⬜ | | **visible in e2e:** a flipped sub-level keeps its original face shading (an upside-down platform shows a dark top). The render-core shader supports it; render-fx's mesh half is gated (`SUPPORTED=false`); check the Indigo renderer path. Priority for ships that tilt |
| S15 | Water occlusion near-plane clipping (no `GL_DEPTH_CLAMP` in blaze3d) | ⏸ | | minor |
| S16 | ImGui inspector windows | ⏸ | | Veil's ImGui overlay doesn't exist on 1.21.11 |
| S17 | Review third-party compat mixins (sodium-extras, distant horizons, shoulder surfing, exposure, …) | ⬜ | | some were dropped or left uncompiled during the port; re-add those whose 1.21.11 versions exist |

### Phase 2: Simulated + Aeronautics on Create Fly

Both mods are in the Gradle build (`:simulated`, `:aeronautics`; 765 Java files, 86 + 29 mixin classes), depending on
`:sable` and Create Fly from Modrinth maven. Work happens on `p2-port` (see Workflow). **`:simulated:compileJava` = 0
errors** (2026-10-17); Aeronautics is being ported.

| ID | Task | Status | Owner | Notes |
|---|---|---|---|---|
| R1 | Convert all JSON resources to 1.21.11 formats | ✅ | | 1,467 files, 0 validator errors (`tools/validate_resources.py`) |
| R1a | Datapacks load on a dedicated server (Create Fly's recipe formats differ from Create 6.0.10's) | 🔄 | agent `sim-data` | 2026-10-17: the server stops at datapack load (`create:cutting` step without `processing_time`, a loot table dropping air, an empty world-preset layer); fixing the JSON and extending the validator. Aeronautics data after |
| R2 | OBJ models: `mecanum_wheel`, `plunger_wheel`, **`steering_wheel`** | ✅ | | Create Fly bundles NeoForge's OBJ loader (`"loader": "neoforge:obj"` works in any namespace), so restoring the loader keys was enough; mecanum/plunger/omni wheels are Offroad's |
| R3 | Register custom advancement triggers (`*_builtin`) | ✅ | | `SimAdvancementTriggers` |
| R4 | Check `c:` convention tags exist on Fabric (`c:armors`, `c:music_discs`, …) | ✅ | | all exist in convention tags v2; code uses `ConventionalItemTags` (see `docs/p2-notes/registry.md`) |
| R5 | Blocks whose models lost `render_type` need render layers set in code | ✅ | | Create Fly honours `render_type` in model JSON, so it was restored in the 13 models (4 Simulated, 9 Aeronautics) |
| C1 | Add `simulated` + `aeronautics` to `settings.gradle` (depends on `:sable` + Create Fly from Modrinth maven) | ✅ | | `fabric.mod.json` per mod; optional compat (JEI, CC: Tweaked, Nature's/Explorer's Compass, Sodium, Iris) compileOnly |
| C2 | Replace Registrate (`SimulatedRegistrate`, builders, `BlockEntry`) with static registration in Create Fly's style | ✅ Simulated | | registry fields are the objects themselves (`SimBlocks.X` is the block); a small `Simulated.getRegistrate()` helper keeps creative-tab sections, display targets, nav targets |
| C3 | Fabric impls of the 13 `service/Sim*Service` interfaces (inventory/fluid via Transfer API, menus, tabs, config, …) | ✅ | | `fabric/service`; item/fluid storages via Fabric Transfer API. Energy isn't exposed (no Fabric energy API among our deps) |
| C4 | Port `META-INF/accesstransformer.cfg` → access wideners | ✅ Simulated | | `:simulated:validateAccessWidener` passes; the `.cfg` is deleted |
| C5 | Client entrypoints: block entity renderers, visuals, screens, ponder | ✅ Simulated | | render-state BERs, Flywheel visuals, client behaviours per BE type, menus, particles, ponder on Create Fly's ponder |
| C6 | Create API gaps: the 37 Create classes with no direct Create Fly equivalent | ✅ Simulated | | mappings per area in `docs/p2-notes/` |
| C7 | 1.21.11 API fixes in Simulated (same patterns as Sable; see PORTING.md) | ✅ | | Veil render effects (contraption diagram, End Sea, lasers, physics staff) re-implemented on the 1.21.11 pipeline |
| C8 | Simulated mixins (86) verified against 1.21.11 **and Create Fly** | ✅ | | checked against the bytecode with javap; 5 removed as obsolete, several retargeted; dedicated server applies all (0 mixin errors) |
| C9 | Aeronautics: same as C2–C8 (181 files, 29 mixins) | 🔄 | agents `aero-core`, `aero-content`, `aero-render` | started 2026-10-17; levitite shader and balloon effects need 1.21.11 re-implementations |
| C10 | Sable ↔ Create integration (contraptions on sub-levels, kinetics across sub-levels) | ✅ | | 2026-10-17: 139 of upstream's Create-compat mixins ported against Create Fly (`sable-create.mixins.json`, applied only when `create` is loaded); 8 skipped as unnecessary on Create Fly (`docs/p2-notes/sable-create.md`). Server e2e passes with and without Create; client side not yet run |
| T1 | Server e2e with Sable + Simulated + Aeronautics + Create Fly | 🔄 | | Sable + Create Fly + Simulated: all mixins apply; blocked on R1a. Aeronautics after C9 |
| T2 | Playtest checklist: build a ship, assemble, propeller flight, balloon lift, levitite, steering | ⬜ | | GameTest harness ready; test design in `docs/p2-notes/gametest.md`. Then a manual playtest |

**Known Phase 2 gaps (PORT-TODOs):** energy storage not exposed (docking connector battery); server config not synced
to clients (Simulated/Aeronautics use Create Fly's config system); JEI doesn't show the portable engine dyeing recipe;
single-block sub-levels don't fade into the End Sea; two Create-compat mixins add a `write` override Create Fly lacks
(revisit if Create Fly adds it). `VoidAnchor` stays unregistered, as upstream.

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
- Sable `breaks` Sodium until S11 is done (otherwise ships would silently be invisible).
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
