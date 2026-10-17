# minecraft-create-fly progress

_Last updated: 2026-10-17_

Port of Create Aeronautics to Minecraft 1.21.11 Fabric, on top of Create Fly 1.21.11-6.0.9-5. Task table in `ROADMAP.md`, porting rules in `PORTING.md`.

```mermaid
flowchart LR
  B["✅ Base: Create Fly<br/>1.21.11 Fabric"] --> P1["✅ Phase 1<br/>port Sable physics<br/>(veillite shim for Veil)"]
  P1 --> P2["✅ Phase 2<br/>Simulated + Aeronautics"]
  P2 --> R["⏳ Playtest +<br/>private release"]
```

## Now
- Phase 1 code complete (all ROADMAP S rows done, incl. Sodium water occlusion + shadows). Open: hands-on checks by a person (walk/jump on moving sub-levels, camera feel, multiplayer) via `tools/e2e.sh play`.
- Phase 2 is merged into integration (p2-port fast-forwarded with integration). Per-area notes in `docs/p2-notes/`.

## Next
- Hands-on playtest of Simulated + Aeronautics (`tools/e2e.sh play`, `tools/e2e.sh flight`), then a private release build.

## Done
- 2026-10-14: `:sable` compiles, server + client e2e pass, sub-levels render on 1.21.11.
- 2026-10-15: S9a closed as not a bug (the sphere lands before the first screenshot); `tools/e2e.sh` now exits non-zero when Gradle fails instead of reporting the previous run's log.
- 2026-10-17: S8a fixed: the server e2e force-loads the spawn area and logs loaded/moved sub-level counts (38/38 through 600 ticks).
- 2026-10-17: S14 fixed: Indigo path now re-shades rotated sub-levels (`getShade` neutral + up normal on shade-less quads); verified with a flipped platform, shading on vs off.
- 2026-10-17: S12 merged: Create Fly Flywheel visuals on sub-levels follow the sub-level pose (`tools/e2e.sh client flywheel`); lighting is S12a.
- 2026-10-17: S10 merged: `tools/e2e.sh playtest` checks assemble, fall/rest, stacking, standing on, carried by push/rotate/translate: 14/14 pass on integration.
- 2026-10-17: S17 merged: compat mixin review; fixed Shoulder Surfing descriptor (crashed on launch), `-Pe2eCompat` loads Shoulder Surfing + Jade; mods without a 1.21.11 Fabric build left out.
- 2026-10-17: S13 done: sub-levels darken the world sky light below them (shadow map pass + shadowed terrain pipelines, on by default); `tools/e2e.sh client shadows`.
- 2026-10-17: Phase 2 ported and merged into integration: Simulated + Aeronautics compile; server/client e2e pass with Create Fly, 22/22 GameTests, playtest 14/14, client `flight` scene flies an assembled airship. Sable fix on the way: players are carried across sub-level teleports (the S10 "carried by translate" check was racing the client's position reminder).

- 2026-10-17: S16 merged: ImGui inspector (bundled imguimc, `/sableinspector`, `tools/e2e.sh client inspector`). S11 merged: sub-levels render under Sodium 0.8 (`E2E_SODIUM=1`). S13 merged: sub-levels cast sky-light shadows (`tools/e2e.sh client shadows`). Full suite on integration: server 38/38, gametest 22 + 1 optional fail (gravity), playtest, flight (y=121), shadows, Sodium client all pass.
- 2026-10-17: S6 water occlusion verified (fixed a render-pass crash), S15 near-plane fixed with GL_DEPTH_CLAMP, S3 verified (`tools/e2e.sh client water`). S11 follow-ups: Flywheel visuals and S14 shading work under Sodium, Sodium skips plot sections, shadow pass is off under Sodium.
- 2026-10-17: Sodium water occlusion + sky-light shadows (patched Sodium chunk shaders). Full suite with and without Sodium passes (server, gametest 22+1 optional, playtest, flight, water, shadows, client).
- 2026-10-17: release jars verified in a real Fabric 1.21.11 install (server: 53 mods, 38 sub-levels simulate; offline client joins and renders). Fixed Sable UDP on macOS clients (EISCONN). Zip: `~/Documents/Projects/minecraft-create-fly-release/create-aeronautics-1.21.11-fabric.zip` (3 jars + INSTALL.txt; Fabric API 0.141.6+1.21.11 and Create Fly 1.21.11-6.0.9-5 installed separately).

## Blockers
- None. Note: this shell's `JAVA_HOME` is JDK 17 and the build needs 21; `e2e.sh` picks 21 itself; for Gradle use `JAVA_HOME=$(/usr/libexec/java_home -v 21)`.

## Decisions
- Keep the GitHub repo private. Why: ARR assets and the PolyForm Shield license on Sable.
- Push only when the merged state compiles.
- Cap concurrent Gradle builds at about 4. Why: 16 GB RAM with heavy swap.
- Never commit the `reference` symlink. Why: a merge that tracks it deletes the real ignored `reference/` folder (rebuild with `tools/setup-reference.sh`).
- Agents work in clones, not `isolation: worktree`, which attaches to the parent Projects repo.
- An old `e2e/run/client/config/sable-client.toml` keeps stale config values; delete it after changing a config default. Why: S13 looked broken until it was deleted.
- Parallel agents run e2e only through a shared lock (one game at a time). Why: 16 GB RAM.
