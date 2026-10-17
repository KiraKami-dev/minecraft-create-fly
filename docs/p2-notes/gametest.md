# gametest (branch `p2-gametest`): headless GameTest suite

Owner files: `e2e/**`, `tools/e2e.sh`.

## How to run
- `tools/e2e.sh gametest [selector]` -> Loom run `gametest` (`:e2e:runGametest`, run dir `e2e/run/gametest`, fresh world
  every run). Prints `PASS/FAIL <test id>` per test + vanilla's `N GAME TESTS COMPLETE` line, writes
  `e2e/run/gametest/gametest-report.xml` (JUnit), exits 1 if a required test failed or the run did not finish.
  Selector example: `tools/e2e.sh gametest 'mcf_e2e:sable_physics_test_*'` (`-Pe2eFilter=` on the Gradle task).
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
  x/z in +-15M at y -59, every test's chunks force-loaded (`TestInstanceBlockEntity.forceLoadChunks`), structure encased in
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
  `N sub-levels loaded` every 100 ticks.
- Defaults: gravity (0, -11, 0) m/s^2, universal drag 0.09, block mass about 1 (`physics_block_properties`).
