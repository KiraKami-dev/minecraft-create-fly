# Create Aeronautics on Fabric 1.21.11: roadmap and progress

_Last updated: 2026-10-17 (S14 fixed). Keep this file current: update the status tables when you claim, finish or
discover a task._

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

Requirements: JDK 21, git, ~16 GB RAM, macOS or Linux.

```bash
git clone https://github.com/KiraKami-dev/minecraft-create-fly && cd minecraft-create-fly
git checkout integration                  # the working trunk (see Workflow)
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

`tools/e2e.sh [server|client|all]` drives the dev-only harness in `e2e/`:
- **server**: boots a dedicated 1.21.11 server, runs a **mixin audit** (forces every mixin to apply), spawns
  sub-levels with `/sable spawn jenga|sphere|platform`, and runs 600 ticks. Logs: `e2e/run/server/logs/latest.log`.
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
| S8a | Dedicated-server e2e loaded **0 sub-levels** (no player keeps their chunks loaded) | ✅ | | 2026-10-17: e2e now runs `forceload add -16 0 15 31`; sub-levels unload with their holding chunk. Both runs now log `N sub-levels loaded, M moved since spawn` every 100 ticks: 38 loaded and 38 moved through tick 600 |
| S10 | Playtest: assemble, collide, stand on, ride a sub-level | ✅ | | 2026-10-17: automated as `tools/e2e.sh playtest` (`-Pe2eScenario=playtest`, `e2e/.../Playtest.java`, client e2e with a real player); 14 PASS/FAIL checks all pass: `sable assemble area` makes one sub-level and removes the world blocks, it falls 10 blocks and rests with bb min y −60.003 (ground top −60), a second one rests on the first (−57.003 vs −57.002) without pushing it down, the player stands on a resting platform (y = top, onGround), and is carried by a linear impulse (1.36 blocks, same as the platform), a 90° rotation (−90.0°, radius 2.00) and a 5-block translation. Riding: Sable has no seat/mount mechanism (seats are Simulated/Create, Phase 2). **Remaining manual checks:** walking on and off a moving/tilted sub-level, jumping and sprinting on it, camera feel while it rotates, ride/mount (Phase 2), multiplayer view of another player on a moving sub-level |
| S11 | Sodium support (sub-levels currently not drawn under Sodium; Sable `breaks` Sodium) | ⬜ | | needs a Sodium 0.8 chunk-render integration |
| S12 | Flywheel (inside Create Fly) block entities on sub-levels | ⬜ | | upstream support lived only in the NeoForge module; likely invisible today |
| S13 | Sky-light shadows from sub-levels | ⏸ | | hard-off; needs a shadow-map pass + terrain pipeline variant (see `SableSkyLightShadows`) |
| S14 | Dynamic directional shading of rotated sub-levels | ✅ | | 2026-10-17: Indigo replaces `SectionCompiler.compile`, so the `ModelBlockRenderer` mixins never ran. Fix: `RenderSectionRegionMixin` makes `getShade` return 1.0 while a sub-level section compiles, and `IndigoRenderContextMixin` gives shade-less quads the up normal so the shader re-shades them; `SUPPORTED=true`, config default on. Verified in client e2e with a platform flipped 180° overhead: ON shows a dark underside, OFF (`sub_level_dynamic_shading=false`) keeps the original bright top |
| S15 | Water occlusion near-plane clipping (no `GL_DEPTH_CLAMP` in blaze3d) | ⏸ | | minor |
| S16 | ImGui inspector windows | ⏸ | | Veil's ImGui overlay doesn't exist on 1.21.11 |
| S17 | Review third-party compat mixins (sodium-extras, distant horizons, shoulder surfing, exposure, …) | ⬜ | | some were dropped or left uncompiled during the port; re-add those whose 1.21.11 versions exist |

### Phase 2: Simulated + Aeronautics on Create Fly

Sources are imported (`simulated/`, `aeronautics/`; 765 Java files, 86 + 29 mixin classes), with imports retargeted
to Create Fly (`com.zurrtum.create`, client classes under `.client`) and `veillite`. **Not in the Gradle build yet.**

| ID | Task | Status | Owner | Notes |
|---|---|---|---|---|
| R1 | Convert all JSON resources to 1.21.11 formats | ✅ | | 1,467 files, 0 validator errors (`tools/validate_resources.py`) |
| R2 | OBJ models: `mecanum_wheel`, `plunger_wheel`, **`steering_wheel`** | ⬜ | | were `neoforge:obj`; loader stripped and path kept under `_obj_model`; need a Fabric OBJ loader or code rendering |
| R3 | Register custom advancement triggers (`*_builtin`) | ⬜ | | |
| R4 | Check `c:` convention tags exist on Fabric (`c:armors`, `c:music_discs`, …) | ⬜ | | |
| R5 | Blocks whose models lost `render_type` need render layers set in code | ⬜ | | 13 models; see the p2-resources commit |
| C1 | Add `simulated` + `aeronautics` to `settings.gradle` (depends on `:sable` + Create Fly from Modrinth maven) | ⬜ | | after S8 |
| C2 | Replace Registrate (`SimulatedRegistrate`, builders, `BlockEntry`) with static registration in Create Fly's style | ⬜ | | see `reference/create-fly-1.21.11/.../AllBlocks.java`; datagen is dropped (we ship the JSON) |
| C3 | Fabric impls of the 13 `service/Sim*Service` interfaces (inventory/fluid via Transfer API, menus, tabs, config, …) | ⬜ | | NeoForge originals: `reference/simulated-1.21.1/*/neoforge` |
| C4 | Port `META-INF/accesstransformer.cfg` → access wideners | ⬜ | | both mods |
| C5 | Client entrypoints: block entity renderers, visuals, screens, ponder | ⬜ | | like Create Fly's `client/AllBlockEntityRenders` |
| C6 | Create API gaps: the 37 Create classes with no direct Create Fly equivalent | ⬜ | | e.g. `AllTags` → `AllBlockTags`/`AllItemTags`, `CreateClient`, custom item renderers, `SafeBlockEntityRenderer` |
| C7 | 1.21.11 API fixes in Simulated (same patterns as Sable; see PORTING.md) | ⬜ | | |
| C8 | Simulated mixins (86) verified against 1.21.11 **and Create Fly** | ⬜ | | many target Create internals, which differ in Create Fly |
| C9 | Aeronautics: same as C2–C8 (181 files, 29 mixins) | ⬜ | | propellers, hot-air balloons, levitite |
| C10 | Sable ↔ Create integration (contraptions on sub-levels, kinetics across sub-levels) | ⬜ | | upstream lived in Sable's NeoForge module, not ported |
| T1 | Server e2e with Sable + Simulated + Aeronautics + Create Fly | ⬜ | | |
| T2 | Playtest checklist: build a ship, assemble, propeller flight, balloon lift, levitite, steering | ⬜ | | |

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

## Running agents in Claude Code cloud sessions

If you use Claude Code cloud sessions (claude.ai/code) on this repo, the environment needs network access to:
`maven.fabricmc.net, api.modrinth.com, cdn.modrinth.com, piston-meta.mojang.com, piston-data.mojang.com,
libraries.minecraft.net, resources.download.minecraft.net, services.gradle.org, plugins.gradle.org,
repo.maven.apache.org, repo1.maven.org, maven.ryanhcode.dev, raw.githubusercontent.com` (or "Full" access), and the
Claude GitHub App must be installed on the repo to push. Ready-made task prompts are in `tools/cloud-prompts/`.
