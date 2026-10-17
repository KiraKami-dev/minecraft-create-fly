# gametest (branch `p2-gametest`): headless GameTest suite

Owner files: `e2e/**`, `tools/e2e.sh`.

## How to run
- `tools/e2e.sh gametest [selector]` -> Loom run `gametest` (`:e2e:runGametest`, run dir `e2e/run/gametest`, fresh world
  every run). Prints `PASS/FAIL <test id>` per test + vanilla's `N GAME TESTS COMPLETE` line, writes
  `e2e/run/gametest/gametest-report.xml` (JUnit), exits 1 if a required test failed or the run did not finish.
  Selector example: `tools/e2e.sh gametest 'mcf_e2e:sable_physics_test_*'` (`-Pe2eFilter=` on the Gradle task);
  `tools/e2e.sh gametest <selector> verify` (`-Pe2eVerify`) runs each selected test 100x per rotation and prints counts.
- `all` now runs server, gametest, client. `GRADLEW="python3 -I .../gw.py" nice -n 19 tools/e2e.sh gametest` routes the
  Gradle call through the shared build-slot wrapper (default `./gradlew --console=plain`).
- Logs: `e2e/run/gametest/logs/latest.log`; every result line is `E2E: GAMETEST ...` (from `E2ETestReporter`), tests that
  throw get their stack trace logged as `E2E: GAMETEST <id> threw`.

## Upstream (NeoForge 1.21.1) -> Fabric 1.21.11 GameTest mapping
- `@GameTestHolder(modid)` + static `@net.minecraft.gametest.framework.GameTest(template = "x")` ->
  `@net.fabricmc.fabric.api.gametest.v1.GameTest(structure = "sable:<class>.x")` on **public non-static** methods of a class
  listed under the `fabric-gametest` entrypoint (`e2e/src/main/resources/fabric.mod.json`). NeoForge's implicit template id
  was `<modid>:<classname lowercase>.<template>`, hence `sable:assemblytest.brittlebreak` etc.; the structures resolve from
  `sable/src/main/resources/data/sable/structure/*.nbt` (vanilla `StructureTemplateManager` resource source; DataVersion
  3955 files are upgraded by DFU on load).
- Test id = `<entrypoint mod id>:<snake_case(SimpleClassName + "_" + method)>`, e.g. `mcf_e2e:sable_physics_test_snag`.
  Class names are prefixed (`Sable*Test`) so later `Simulated*`/`Aeronautics*` classes can't collide.
- `timeoutTicks` (NeoForge default 100) -> `maxTicks` (**Fabric default 20**, so always set it); `attempts` -> `maxAttempts`;
  `requiredSuccesses`, `required`, `manualOnly` unchanged; batches are now per `environment` (default `minecraft:default`).
- **Relative coordinates moved one block down.** 1.21.1 `absolutePos` was relative to the structure block, placed one below
  the structure (`StructureUtils.prepareTestStructure(...).below()`); 1.21.11 is relative to
  `TestInstanceBlockEntity.getStartCorner()`, the structure's first block. Every upstream local Y is ported as `y - 1`
  (brittlebreak `(0,1,0)..(2,3,2)` -> `(0,0,0)..(2,2,2)`, spawn `(2.5,12,2.5)` -> `(2.5,11,2.5)`, ...).
- `GameTestHelper.testInfo` is private now: origin = `helper.absolutePos(BlockPos.ZERO)` (was
  `testInfo.getStructureBlockPos()`); `helper.getTick()` returns `long`.
- `helper.fail(String, BlockPos)` -> `fail(Component, BlockPos)`, the pos is **relative** (upstream passed absolute ones);
  `new GameTestAssertPosException(String, abs, rel, tick)` -> `(Component, abs, rel, int)`; assertions throw
  `GameTestAssertException(Component, tick)`.
- `Rotation.rotation().transformation()` returns `Matrix3fc` (`getNormalizedRotation(Quaterniond)` still there);
  `ResourceKey.location()` -> `identifier()`.
- NeoForge `level.getCapability(Capabilities.ItemHandler.BLOCK, pos, state, be, side)` + `IItemHandler(Modifiable)` ->
  `ItemStorage.SIDED.find(level, pos, state, be, @Nullable side)` + `SlottedStorage.getSlot(i).insert(variant, n, tx)` in a
  `Transaction.openOuter()`; slot contents via `StorageView.getResource().toStack(amount)`.
- GameTest server (`-Dfabric-api.gametest`, Fabric `MainMixin` replaces the dedicated server): flat world, tests at random
  x/z in +-15M at y -59 (moved to the origin here, see below), every test's chunks force-loaded (`TestInstanceBlockEntity.forceLoadChunks`), structure encased in
  barriers (walls, floor, ceiling unless `skyAccess`). It exits with the number of failed required tests. `manualOnly`
  tests are never run by it (only by `/test run` in a world); `-Dfabric-api.gametest.filter=<selector>` runs a subset.
- Fabric's `GameTestServerMixin` makes `isDedicatedServer()` return true on the GameTest server, so `E2E`'s scene logic
  (stop at tick 700) would kill it: the run uses `-De2e.gametest` instead of `-De2e.enabled`.
- In a dev environment Fabric registers the `@GameTest` functions in every run (client/server too) and instantiates the
  entrypoint classes: keep test-class constructors side-effect free.
- Tick order: `ServerLevel.tick` (Sable container + physics step, injected at HEAD) runs before `GameTestTicker`, so a check
  scheduled N ticks after spawning a sub-level sees exactly N physics steps (2 substeps each by default).
- Sable's `mixin/game_test/*` (remove sub-levels intersecting a test on `GameTestInfo.succeed`, on
  `StructureUtils.clearSpaceForStructure` and on `/test reset`) all match 1.21.11 targets (`defaultRequire: 1`, audit passes).

## Sable facts the tests rely on
- A sub-level pose's `position` is the world position of its centre of mass (`rotationPoint` is set to the COM when blocks are
  added, `MergedMassTracker.uploadData`), so a single-block sub-level's position is the block centre.
- Placing blocks one by one moves the COM and teleports the body with it; `/sable spawn` resets only `logicalPose`, which the
  next physics step overwrites. `SableTestHelper.spawnBox` teleports the body (`PhysicsPipeline.teleport` updates both).
- `SubLevel.boundingBox()` is refreshed in `ServerSubLevel.tick()` before the physics step (one step behind the pose):
  `SableTestHelper.worldBounds` recomputes it from the current pose.
- S8a cause: `PhysicsChunkTicketManager.update` moves a sub-level that is not force-loaded into a holding chunk as soon as
  its bounds (+1 block, + velocity prediction) touch a chunk that is not `inBlockTickingRange`. 1.21.11 has no spawn chunks,
  so without a player nothing ticks. GameTest areas are force-loaded, so test sub-levels simulate; `SubLevelProbe` fails a
  test as soon as its sub-level is no longer loaded. The plain server scene now `forceload`s its area too and logs
  `N sub-levels loaded` every 100 ticks: 38 of 38 stay loaded for the 600 ticks (was 0), so S8a can be closed.
- Defaults: gravity (0, -11, 0) m/s^2, universal drag 0.09, block mass about 1 (`physics_block_properties`).
- Rapier damps every substep (`v = (v + g dt) / (1 + drag dt)`, matches to 1e-3) but steps positions more finely than one
  step per Sable substep: after 1 s the drop is 5.347 m, between the exact drag solution (5.338) and one-step-per-substep
  Euler (5.476). Upstream's `gravity` test expects `v = g * 1 s` and `drop = g / 2` (no drag), so it cannot pass with
  Sable's own default drag (it fails by 0.49 m/s; the "inconsistent distance" FIXME is the same thing, not spawn timing:
  the velocity proves all 40 substeps ran).

## Harness fixes for the 1.21.11 GameTest server (`e2e/.../mixin/GameTestServerMixin`)
- **Tests run next to the origin.** Vanilla places the grid at a random x/z within +-15M. Sable's Rapier is the f32 build
  (`sable_rapier/Cargo.toml`: `rapier3d`, not `rapier3d-f64`), which resolves 1 m at 1.2e7: sub-levels snapped to whole
  metres in x (+0.5 m jumps) and fell through floor, barriers and bedrock (first run: 2 required failures). That is what
  upstream's "allow manual tests to run automatically when rapier is set to 64-bit mode" FIXME is about. Gameplay far from
  spawn has the same limit (0.06 m at 1M blocks, 1 m at 10M+).
- **Multi-attempt tests finish.** Vanilla `GameTestServer` tracks only the first attempt of each test and halts while the
  re-runs of `maxAttempts > 1` tests are still queued, so `snag` (10/10 required) never reported. The mixin waits for the
  runner's re-runs (gives up after 5 min); `tools/e2e.sh` fails if fewer tests report than ran.
- Vanilla `GameTestHelper.relativePos` mirrors positions for unrotated (and 180 deg) tests; failure messages compute local
  positions with `SableTestHelper.localPosition` instead.

## Tests (ids `mcf_e2e:<class>_<method>`) and results (2026-10-17, `tools/e2e.sh gametest`)
| Test | Origin | Asserts | Result |
|---|---|---|---|
| `sable_assembly_test_brittle_breaking` | upstream | 3x3x3 of comparator/torch/wire/lever/rails/button/plates/tripwire assembles with every state unchanged | PASS |
| `sable_assembly_test_all_blocks` | upstream (manual-only there, optional here; own batch, env `mcf_e2e:isolated`) | 202 vanilla blocks with a block entity or inventory: placed, every slot filled, assembled with a stone base: block survives, nothing drops, every slot keeps its item | PASS |
| `sable_assembly_test_assembled_blocks_settle` | new | 3x1x3 deck + chest with 7 diamonds: world left empty, 10 blocks in a 3x2x3 plot, cargo kept, falls 1 block, rests on the floor (bottom y 1 +-0.1), still 10 blocks + cargo | PASS (400/400 in verify mode; failed 1 of 10 full runs before `all_blocks` got its own batch, see the plot cache bug) |
| `sable_assembly_test_plot_reuse_in_one_tick` | new, optional (known Sable bug) | remove a sub-level and create another in the same tick: the new one's block must be in its own chunk and readable through the level | FAIL (bug below) |
| `sable_physics_test_continuous_collision` | upstream (+ must stay loaded and actually move) | glass block thrown at a brick wall never passes it | PASS |
| `sable_physics_test_snag` | upstream, optional, 10/10 attempts | diamond block pushed along a 1x1 channel slides through without snagging | PASS (10/10) |
| `sable_physics_test_gravity` | upstream, optional | v = g after 1 s (no drag) | FAIL (expected: drag, see above) |
| `sable_physics_test_free_fall_with_drag` | new | after 20 ticks v matches the per-substep damped model to 0.01 m/s, drop within [exact, coarse] bounds, no sideways drift | PASS |
| `sable_physics_test_falls_and_comes_to_rest` | new (S8a) | 3x2x3 box: stays loaded every tick, falls straight at 50-120% of free-fall speed, rests on the floor (bottom y 1 +-0.1) for 10 ticks, no drift, 18 blocks | PASS |
(plus vanilla's own optional `minecraft:always_pass`). Suite: 10 tests, about 2-7 s of game time, about 2 min wall time with
Gradle. Required tests: 5/5 pass; optional: `all_blocks`, `snag` (10/10), `always_pass` pass; `gravity` (upstream test ignores
drag) and `plot_reuse_in_one_tick` (Sable bug) fail as documented. `tools/e2e.sh gametest` exits 0.
`tools/e2e.sh gametest <selector> verify` runs a test 100x in each rotation (also exercises the rotation-aware helpers).

## Findings (not fixed here; Sable is read-only for this area)
- **Sable bug (upstream too): stale plot chunks in `ServerChunkCache`'s lookup cache.** Vanilla
  `ServerChunkCache.getChunk(x, z, status, load)` returns any non-null chunk from its 4-entry `lastChunk` cache without
  checking it; the cache is cleared only once per level tick (end of `tick()`) or when `runDistanceManagerUpdates` changes
  something. Sable's `mixin/plot/ServerChunkCacheMixin` redirects `getChunkNow`, `getChunkFutureMainThread` (cache misses),
  `getVisibleChunkIfPresent` etc. for plot positions, but not the cache hit in `getChunk`, and plot chunk add/remove
  (`ServerLevelPlot.addChunkHolder` / `onRemoveChunkHolder`) never clears it. So when a sub-level is removed and another is
  created in the same tick (it reuses the freed plot index), `level.getBlockState/setBlock/getBlockEntity` in the new plot hit
  the removed plot's dead `LevelChunk` until the next tick: the new sub-level's blocks are written into the dead chunk and
  vanish (evidence: `plot_reuse_in_one_tick` -> "air in its own chunk, diamond_block through the level"). The same happens to
  a plot chunk that was looked up before it existed (the miss path returns the shared `EmptyLevelChunk`, which is then
  cached). In gameplay: disassemble/destroy one ship and assemble another in the same tick -> blocks or block-entity data of
  the new one lost (`SubLevelAssemblyHelper.moveBlocks` loads block entities via `resultingLevel.getBlockEntity`). Seen in
  this suite as `assembled_blocks_settle` reading 1 of its 10 blocks when `all_blocks` had removed a sub-level that tick.
  Suggested fix (Sable owner): `@Inject` at HEAD of `ServerChunkCache.getChunk(IILnet/minecraft/world/level/chunk/status/ChunkStatus;Z)`
  returning the container chunk (or the empty chunk) for `container.inBounds(x, z)` on the main thread, or clear the cache
  (`clearCache()`, accessor needed) in `addChunkHolder`/`onRemoveChunkHolder`. Then make the test required.
- Upstream: `SubLevelAssemblyHelper.moveBlocks` registers POIs twice for every moved POI block
  (`level.updatePOIOnBlockStateChange(newPos, air, state)` in the move loop, then again in `markAndNotifyBlock`; upstream
  calls `onBlockStateChange` at the same two places). Each assembled bed/bell/lectern/workstation/beehive logs
  `POI data mismatch: already registered at <plot pos>` (16 times in `all_blocks`). Harmless log noise; fix = drop the call in
  the move loop.
- `Failed to apply tag physics properties. Unknown block: create:flywheel` at start-up: expected while Create is absent.

## Helpers for new tests (`e2e/src/main/java/dev/mcf/e2e/gametest/`)
- `SableTestHelper`: `container/physics/handle`, `isLoaded`, `subLevelsInTest`; `spawnSubLevel/spawnSingleBlockSubLevel/
  spawnBox` (exact COM placement), `assemble(localBlocks)` / `assembleRegion(min, max)` (what a physics assembler ends up
  calling: `SubLevelAssemblyHelper.assembleBlocks`), `teleport`, `removeSubLevel`; `countBlocks`, `countWorldBlocks`,
  `worldBounds`, `localPosition/localVelocity/localAngularVelocity`, `describe/describePlot`; `fill`, `power/unpower`
  (redstone block; vanilla has `pulseRedstone`, `pullLever`, `pressButton`, `useBlock`); rotation-aware
  `absolutePosition/localPosition/absoluteDirection/localDirection/testOrientation`.
- `SubLevelProbe`: follows one sub-level: `sample()` per tick (or `sampleEveryTick()`), fails the test the moment the
  sub-level is unloaded, exposes local `position/velocity/angularVelocity/displacement/min/max/maxSpeed/bottom()`,
  `restingTicks()`, `assertResting(n)`.
- Ticking N times: `helper.runAfterDelay(n, ...)`, `helper.startSequence().thenIdle(n).thenExecute(...)`,
  `thenExecuteFor(n, check)`, `thenWaitUntil(check)` (retried every tick until it stops throwing or the test times out).
- New test class: add it to the `fabric-gametest` list in `e2e/src/main/resources/fabric.mod.json`, prefix its name
  (`SimulatedXTest`, `AeronauticsXTest`), always set `maxTicks`. Tests of one environment run concurrently in batches of
  50; `environment = "mcf_e2e:isolated"` (empty `all_of`, `e2e/src/main/resources/data/mcf_e2e/test_environment/`) gives
  a test its own batch. Build structures in code on top of an existing floor
  structure (`sable:assemblytest.allblocks` 5x4x5, `sable:physicstest.gravity` 5x13x5 shaft) or ship a new `.nbt` under
  `e2e/src/main/resources/data/mcf_e2e/structure/`. The e2e module will need `implementation project(path: ':simulated',
  configuration: 'namedElements')` (and `:aeronautics`, Create Fly runtime) once those compile.

## Design note: Simulated/Aeronautics content tests (T1/T2), from upstream behaviour
Forces only exist on sub-levels (`ServerSubLevel.prePhysicsTick`, balloon `applyForces`), so every test assembles first.
Propeller and steering tests also need Create kinetics working on sub-levels (ROADMAP C10).
- **Physics assembler -> ship** (`simulated:physics_assembler`, `SimulatedAssemblyTest`): build a 3x1x3 deck that sticks
  together (slime/honey blocks, or a `SuperGlueEntity`/`HoneyGlueEntity` box; plain blocks don't attach) with the assembler
  on top (FACE=FLOOR assembles the block below + everything attached, the assembler rides along). Trigger without a player:
  `SimAssemblyHelper.assembleFromSingleBlock(level, asm, asm.relative(PhysicsAssemblerBlock.getStickyFacing(state)), true,
  true)` or `PhysicsAssemblerBlockEntity.assembleOrDisassemble()` (no redstone input; players use a packet). Assert: result
  sub-level holds deck + assembler, world region is air, no item entities, `getLastAssemblyException() == null`, ship
  falls and rests (`SubLevelProbe`). Second test: trigger again once resting near terrain -> disassembles (needs
  speed < 5 m/s, angular speed < pi/2, aligns to 4 deg, places after > 5 ready ticks, gives up after 100): blocks back in the
  world, sub-level removed.
- **Propeller thrust** (`aeronautics:wooden_propeller`/`andesite_propeller`/`smart_propeller`, `AeronauticsPropellerTest`):
  creative motor -> shaft -> small propeller on a glued deck, all assembled into one sub-level; point the axis down at the
  deck centre (no torque) so thrust fights gravity. Thrust per small propeller T = RPM (1.0 * RPM, sign from REVERSED and
  the axis), scaled by air pressure and airflow (caps climb speed at about 0.1 * RPM m/s); spin-up about 20 ticks. At
  256 RPM a 10-block ship (weight 110) must climb: assert COM y rises > 1 block within 60 ticks and climb speed stays
  <= 0.1 * RPM. Confirm the thrust sign empirically once, then lock it. Bearing variant (`propeller_bearing` + >= 2
  `#create:windmill_sails`, T = 0.2 * S^1.5 * RPM): assemble the bearing after the ship (assembly turns running bearing
  contraptions back into blocks).
- **Hot-air balloon lift** (`aeronautics:adjustable_burner` + `#aeronautics:envelope`/wool, `AeronauticsBalloonTest`,
  `skyAccess = true`, `maxTicks` >= 800): burner on a deck under an envelope closed on top and sides (bottom may be open;
  burner raycasts up to 80 blocks to the first airtight block), redstone block glued next to the burner (power must be on
  the same sub-level; output = 500 * signal / 15). Gas fills toward min(output, capacity) with a ~180-tick time constant
  (balloon detection on the 20-tick lazy tick); lift = 1.5 * g * V_filled * pressure. Size V so 1.5 * V > ship mass
  (envelopes weigh 0.25): assert the ship leaves the floor within 600 ticks and rises at a bounded rate (envelope drag).
  Control: same build unpowered stays on the floor.
- **Levitite** (`aeronautics:levitite`, `AeronauticsLevititeTest`; easiest, no kinetics or block entity): glued group of
  N levitite + planks spawned mid-air. With 10 * N >= mass (each levitite cancels gravity for 10 mass units, lift capped
  at gravity, never upward) assert it hovers: vertical speed -> ~0 and y within 0.5 of start over 100 ticks. Control with
  10 * N < mass: sinks at about 11 * (1 - 10N/m) m/s^2. Keep levitite on the COM or the ship tilts.
- **Steering wheel** (`simulated:steering_wheel`, `SimulatedSteeringTest`): it is a kinetic generator, not a torque source:
  set `be.targetAngleToUpdate = 90f` server-side (players send a packet), it emits +-16 RPM for ceil(delta/4.8)+2 ticks.
  Test 1: wheel -> shaft -> Create mechanical bearing with sails (rudder): `be.getAngle()` and the bearing angle reach 90
  within ~30 ticks. Test 2 (full steering, needs propeller + sail lift from Sable's `SailBlockMixin`): a ship pushed
  forward by a propeller yaws (local angular velocity y) with the sign of the wheel angle.
