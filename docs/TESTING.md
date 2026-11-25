# Testing

`scripts/validate-all.sh` runs every automated layer and prints one PASS/FAIL summary with the paths of the logs and
screenshots (`--all` adds the Iris, modded blocks and modpack client runs): see [Everything at once](#everything-at-once).

Five automated layers: JUnit unit tests and GameTests run on both loaders on every `./gradlew build`; the GameTests
also run against the release jars on real servers; the production jar smoke test boots the built jars on real servers;
the client smoke test plays a scenario in a real client, with JEI or REI, and takes screenshots. What remains
manual is listed in `docs/MANUAL_VALIDATION.md`.

Minecraft 26.3, the mod and Gradle run on Java 25: the Gradle launcher starts its daemon on a Java 25 found on the
machine, or downloads one (`gradle/gradle-daemon-jvm.properties`, kept in `~/.gradle/jdks`), and compiles and runs the
game with the Java 25 toolchain. The scripts that start real servers and clients outside Gradle need Java 25 on `PATH`.
The first build downloads and decompiles Minecraft for both loaders (several minutes), later runs take about two
minutes.

## Layout

| Project | Contents |
|---|---|
| `common` | all the game logic, assets and data (`src/main`), unit tests (`src/test/java`), GameTest bodies and the client smoke test (`src/gametest/java`, packages `capsule.gametest` and `capsule.clientsmoke`) and test structures (`src/gametest/resources`). Compiled against vanilla Minecraft; `checkLoaderImports` (part of `check`) fails if a common source references `net.neoforged` or `net.fabricmc` |
| `neoforge` | NeoForge glue; compiles the common sources into `Capsule-neoforge-<mc>-<version>.jar`. Its gametest source set is a mod of its own, `capsule_gametest`, with the GameTests (adding the SecurityCraft, Waystones, Sophisticated Storage, WorldEdit and known incompatibility tests) and the client smoke entrypoint |
| `fabric` | Fabric glue (Loom); compiles the common sources into `Capsule-fabric-<mc>-<version>.jar`. Its gametest source set is a mod of its own, `capsule-gametest`, with the GameTests and the client smoke entrypoint |

The test mods are packaged too, in `<loader>/build/test-mod/<release jar name>-gametest.jar` (task `testModJar`, part of
`assemble`), so that the GameTests and the client smoke scenario run next to
the release jar in real servers and clients. They are never published: CI publishes `build/libs` only.

## Commands

| Command | Runs | Exit code |
|---|---|---|
| `./gradlew build` | compile, jars, and `check` of every project: loader import check, unit tests and GameTests of both loaders | non-zero if anything fails |
| `./gradlew check --continue` | every test layer, reporting all of them even if one fails | |
| `./gradlew :neoforge:test` / `:fabric:test` | JUnit tests of `common/src/test/java` on one loader | non-zero if a test fails |
| `./gradlew :neoforge:runGameTestServer` | the 116 NeoForge GameTests (113 common + 2 SecurityCraft + vanilla's `minecraft:always_pass`) on a headless server | number of failed required tests |
| `./gradlew :neoforge:runGameTestServer -PmodCompat` | the same plus the Waystones (2), Sophisticated Storage, WorldEdit, Open Parties and Claims and Flan tests (124) | number of failed required tests |
| `./gradlew :neoforge:runGameTestServer -Pincompat` | the same plus the tests of the [known incompatibilities](#known-incompatibilities) that have a build for the Minecraft version: none on 26.3, so the same 116 | number of failed required tests |
| `./gradlew :fabric:runGameTestServer` | the 114 Fabric GameTests (113 common + `minecraft:always_pass`) on a headless Fabric server | number of failed required tests |
| `./gradlew :fabric:runGameTestServer -PmodCompat` | the same plus the Open Parties and Claims and Flan tests (118) | number of failed required tests |
| `... -PmodCompatLatest` | with `-PmodCompat` or `-Pincompat`: the newest Modrinth versions of their mods instead of the pinned ones | |
| `./gradlew :neoforge:runClient` / `:fabric:runClient` | a dev client with the mod and its GameTests | |
| `scripts/prod-gametest.sh <jar>...` | the GameTests on real dedicated servers with the release jars, see below | non-zero if a test fails |
| `scripts/prod-smoke.sh <jar>...` | the release jars on real dedicated servers, see below | non-zero if a jar fails |
| `scripts/client-smoke.sh neoforge\|fabric` | the client smoke test under Xvfb, see below | non-zero if a check fails |
| `scripts/validate-all.sh` | every layer above, with JEI and REI and with Improved Transparency, and a summary table, see below | non-zero if a step fails |

CI passes the build number with `-Pbuild_id=<n>` (default `SNAPSHOT`), which only changes the jar names
`neoforge/build/libs/Capsule-neoforge-26.3-11.0.<n>.jar` and `fabric/build/libs/Capsule-fabric-26.3-11.0.<n>.jar`.
Neither jar contains GameTest code: `unzip -l <jar> | grep -i gametest` prints nothing.

## Unit tests (JUnit 5)

- Sources: `common/src/test/java`, compiled and run by each loader project. On NeoForge, ModDevGradle's `unitTest`
  runs them inside a loaded NeoForge environment; on Fabric, `fabric-loader-junit` runs them in Fabric Loader. Tests
  using registries call `Bootstrap.bootStrap()` in a `@BeforeAll` (Fabric's launcher does not bootstrap Minecraft).
- Expected output: `BUILD SUCCESSFUL`; failures are listed as `SomeTest > someMethod() FAILED`.
- Reports: `<loader>/build/reports/tests/test/index.html` and `<loader>/build/test-results/test/TEST-*.xml`.
- Use them for logic that needs no level: paths, file names, config parsing.

## GameTests

- Bodies: `common/src/gametest/java`, test structures in `common/src/gametest/resources`. They only use vanilla
  classes (`GameTestHelper`) and Capsule's `@CapsuleGameTest`, so the same tests run on both loaders.
- Registration: since Minecraft 1.21.5 a test is a function of the `test_function` registry and a `GameTestInstance`
  of the datapack registry `test_instance`, and batches are test environments. `CapsuleGameTests.tests` turns the
  `@CapsuleGameTest` methods of the classes listed in `CapsuleGameTests.TEST_CLASSES` into `CapsuleTest`s named after
  the method (lowercase, `capsule:<method>`), using the templates of the `capsule` namespace; each batch is an
  environment `capsule:<batch>` (`prefabPattern` is `capsule:prefab_pattern`), the tests without one are in
  `capsule:default`. NeoForge: `NeoForgeGameTests`, a `@Mod` of `capsule_gametest`, registers the functions with
  `RegisterEvent` and the environments and instances with `RegisterGameTestsEvent`, adding `SecurityCraftTests`.
  Fabric: `FabricGameTestSetup` registers the functions, and the test mod mixin `RegistryDataLoaderMixin` the
  environments and instances (Fabric API only registers annotated methods of `fabric-gametest` entrypoints), adding
  `GetOffMyLawnTests` when Get Off My Lawn is loaded.
- The totals include vanilla's `minecraft:always_pass`: 26.x registers it in every world, and the GameTest server
  runs every registered test.
- `-Dcapsule.gametest.failOnPurpose=true` adds `FailureProofTests.failsOnPurpose`, which always fails: it shows that a
  runner reports failures. It is never registered otherwise.
- `-PclaimBenchmark` (`-Dcapsule.gametest.claimBenchmark=true`) adds `ClaimProbeBenchmark.claimProbeCost`, which logs
  the cost of the generic claim probe per block and per chunk column (`docs/CLAIMS.md`); a few minutes with
  `-PmodCompat` on NeoForge. It is never registered otherwise.
- `runGameTestServer` deletes `<loader>/runs/gameTestServer/world`, `config/capsule`, `initialconfig`, `schematics`
  (and `worldedit-schematics` on NeoForge) first, so every run starts from a fresh world and default config, without the
  template copies of the previous run.
- The GameTest resources fill every modded `c:ingots/*` tag with a placeholder item (`data/c/tags/item/ingots`), so
  every addon capsule recipe loads and `everyCapsuleRecipeLoadsWithResolvedIngredients` checks it. The schematic
  fixtures are in `data/capsule/schematics`.
- The GameTest server has a mock name to id cache (real servers have a cached one, and Open Parties and Claims only
  reads the vanilla one): `GameTestProfiles` gives it an offline vanilla one before the server starts.
- `TestProbe` stands for a claim mod without adapter: the GameTest mods answer the NeoForge placement event and Common
  Protection API with it, so the claim tests see what the generic probe asks.
- Expected output, at the end of the console log:
  ```
  [Server thread/INFO] [net.minecraft.gametest.framework.GameTestServer/]: All 116 required tests passed :)   (NeoForge)
  [Server thread/INFO] (Minecraft) All 114 required tests passed :)                                          (Fabric)
  ```
  On failure each test is reported with its position and message, then the summary:
  ```
  [Server thread/ERROR] [net.minecraft.gametest.framework.LogTestReporter/]: capsule:blueprintconsumesmaterials failed at 62268, -59, -8274054! both materials should be missing, got {} on tick 0
  [Server thread/ERROR] [net.minecraft.gametest.framework.GameTestServer/]: 1 required tests failed :(
  [Server thread/ERROR] [net.minecraft.gametest.framework.GameTestServer/]:    - capsule:blueprintconsumesmaterials: both materials should be missing, got {} on tick 0
  ```
  and the Gradle task fails. It also fails when the server ran no test, for example when a mod crashed at load (the
  NeoForge GameTest server then exits with status 0). The full logs are `<loader>/runs/gameTestServer/logs/latest.log`; on Fabric the failure
  messages are in `fabric/runs/gameTestServer/logs/debug.log` and in the JUnit report
  `fabric/build/gametest/report.xml`.
- In a dev client (`./gradlew :neoforge:runClient` or `:fabric:runClient`, creative world with cheats): `/test runall`
  runs every test, `/test run <name>` one test, `/test runfailed` the failed ones. Tests are placed near the player.

### Writing a GameTest

- Use the vanilla API only, and add the class to `CapsuleGameTests.TEST_CLASSES`:
  ```java
  public class MyTests {
      @CapsuleGameTest(template = "empty")
      public static void somethingWorks(GameTestHelper helper) { ... helper.succeed(); }
  }
  ```
- Templates: `capsule:empty` (9×9×9 of air) and `capsule:empty17` (17×17×17), given without namespace. Build the
  scenario in code with `helper.setBlock`; positions are relative to the test origin.
- `CapsuleTestUtils` has the common steps: `capture`, `deploy`, `template`, `emptyCapsule`, `clear`, and
  `survivalPlayer`. Use `survivalPlayer` rather than `helper.makeMockServerPlayerInLevel()`: the mock player joins the
  player list without the mod network channels, so the first capsule payload broadcast near it throws on NeoForge.
  `survivalPlayer(helper, pos, messages)` collects the player's chat feedback; without a message list it is a plain
  `ServerPlayer`, which some mods (Flan) tell apart from fake players by its class. It is set to survival: a new
  player takes the default game mode of the server, creative on the GameTest server.
- `LogCapture.open()` (try-with-resources) collects the warnings and errors logged meanwhile, for tests on log output
  (`deployedItemFramesHangOnTheirBlocks`, `missingPrefabTemplateIsReported`,
  `prefabBlocksMissingFromBlueprintsAreReported`, `BundledConfigTests`).
- Tests changing global state go in their own batch (`reload` for `/reload`, `prefabPattern` and `bundledConfig` for
  `prefab_blueprint_recipe.json`, `claims` and
  `claimscale` for registered claim adapters, one per claim mod, `claimprobe` for a box wider than the test area,
  `claimtiers` for recipes replaced after a reload, `claimfailures` for failing adapters, which refuse every capture
  and deploy, `flandepth` for Flan's claim depth config), and must restore what they change (see `ConfigTests`).
- A bug fix comes with a test that fails before the fix; record the failure message in the commit.

### Mods on the GameTest runtime

`gametestImplementation` dependencies of the neoforge project are loaded by its dev runs only: SecurityCraft
v1.10.2.1-beta4 for 26.3 (Modrinth maven, version id in `gradle.properties`) for `SecurityCraftTests`, NeoForge only.
The dev runs of both loaders load one recipe viewer, JEI by default: `-PrecipeViewer=jei|rei` picks JEI 31.23.0.129 or
REI 26.3.824 (with Architectury API 22.0.6 and Cloth Config; versions in `gradle.properties`). EMI has no 26.x build
(`BACKLOG.md`). The Fabric dev runs load Fabric API and Forge Config API Port 26.3.1.

## Mod interaction tests

Mods from issues, checked with Capsule (results and versions in `docs/MANUAL_VALIDATION.md`):

- **GameTests** with `-PmodCompat` (Modrinth maven, version ids in `gradle.properties`): the NeoForge GameTest runtime
  also loads Waystones + Balm, Sophisticated Storage + Sophisticated Core, WorldEdit, Open Parties and Claims and
  Flan; the Fabric one Open Parties and Claims and Flan. `NeoForgeGameTests` registers `WaystonesTests` (#121),
  `SophisticatedStorageTests` (#115) and `WorldEditTests` (#70) only when their mod is loaded; they reference the mods'
  blocks by id, so they compile without them. `OpenPartiesAndClaimsTests` and `FlanTests` (#91, common) compile against
  the mods (the `compileOnly` API jars Capsule's adapters use) and are registered on both loaders when the mod is
  loaded. `WorldEditTests` writes
  the Sponge v2 and v3 fixtures with WorldEdit's own clipboard writers. SecurityCraft, always in the NeoForge runtime,
  stops the 1.13 data fixer from upgrading older block entity data, so the MCEdit test skips the item count there.
  ```
  ./gradlew :neoforge:runGameTestServer -PmodCompat
  ...
  [Server thread/INFO] [net.minecraft.gametest.framework.GameTestServer/]: All 124 required tests passed :)
  ```
- **Newest versions**: `-PmodCompatLatest` replaces the pinned version of every mod of `-PmodCompat` and `-Pincompat`
  with its newest Modrinth version for the loader and Minecraft version (`modrinth` of
  `buildSrc/src/main/groovy/multiloader-loader.gradle`), and prints both, to see whether a new mod version changes a
  result:
  ```
  ./gradlew :neoforge:runGameTestServer -PmodCompat -Pincompat -PmodCompatLatest
  ...
  sophisticated-storage: 26.3-1.5.134.2242 (on0Ziwnf), pinned on0Ziwnf
  ...
  [Server thread/INFO] [minecraft/GameTestServer]: All 123 required tests passed :)
  ```
- **GameTests on the release jars** with mods: `EXTRA_MODS` of `scripts/prod-gametest.sh`, see
  [GameTests on the release jars](#gametests-on-the-release-jars). Get Off My Lawn (Fabric, #91) is tested there only.
- **Servers**: `EXTRA_MODS` adds jars to the server of `scripts/prod-smoke.sh`, for example the mods above and JEI,
  downloaded from Modrinth (`https://api.modrinth.com/v2/version/<id>` gives the file URL):
  ```
  EXTRA_MODS="$(ls ~/mods/*.jar)" scripts/prod-smoke.sh neoforge/build/libs/Capsule-neoforge-*.jar
  ```
  Avoid spaces and brackets in the jar names.
- **Modpacks**: `MODPACK=<Modrinth slug>[:<version id>]` (or a `.mrpack` file) plays the client scenario in a
  production NeoForge client with the pack's mods, so packs using Sinytra Connector work (Connector cannot start in a
  dev run). `scripts/prod-client.py` installs the pack's client files and overrides, the NeoForge version the pack names
  (official installer `--installClient`, vanilla libraries, assets without sounds) and starts the client like a launcher,
  offline, with the release jar and the test mod jar of `./gradlew build`:
  ```
  ./gradlew build
  MODPACK=<slug>[:<version id>] scripts/client-smoke.sh neoforge
  ```
  The packs are searched for the `minecraft_version` of `gradle.properties`. On 1.21.1, `validate-all.sh --modpack`
  used Forgeulously Optimized 1.1.4 (49 mods, NeoForge 21.1.218, Connector 2.0.0-beta.12 with Fabric mods, Sodium,
  Iris); it has no 26.x version, so on 26.x `--modpack` needs `MODPACK` and is reported SKIP without it (not run on 26.x,
  `BACKLOG.md`). The game directory is kept in `build/client-smoke/<run>/game`. The recipe
  viewer check is left to the pack: the report has a line for each viewer it brings. `EXTRA_MODS` also adds jars to the
  dev client of `scripts/client-smoke.sh`; a mod crashing at startup leaves the client on its crash screen until
  `TIMEOUT`.

### Known incompatibilities

The mods of the wiki page [Known incompatibilities](https://github.com/Lythom/capsule/wiki/Known-incompatibilities),
loaded by the NeoForge GameTests with `-Pincompat` (Modrinth version ids in `gradle.properties`). `NeoForgeGameTests`
registers the test class of each mod that is loaded. Each test asserts the current behavior, so that a change of the mod
or of Capsule shows as a failure, whether it fixes or breaks something:

| Mod (version) | Reported problem | Test | Asserts |
|---|---|---|---|
| Corail Tombstone 9.9.4 (26.2 only) | moving a player grave duplicates its items | `playerGravesAreNeverCaptured` | standard and overpowered capsules leave graves in place (`tombstone:graves` in `capsule:excluded`, `tombstone:player_graves` excluded by Capsule) |
| Refined Storage 2.0.9 (1.21.1 only) | machines lose their data when moved | `refinedStorageIsExcludedByDefault`, `diskDriveKeepsItsDiskWhenNotExcluded` | `refinedstorage:` of the default config leaves a disk drive and a controller in place; without it, the disk drive moves with its disk and its 16 diamonds |
| Mekanism 10.7.19.85 (1.21.1 only) | a moved Digital Miner cannot be broken and stops working | `digitalMinerIsNeverCaptured`, `mekanismBinMovesWithItsContent` | the miner and its bounding blocks stay in place (`c:relocation_not_supported`); a bin moves with its content |
| Immersive Engineering 12.4.2-194 (1.21.1 only) | wires disappear when deployed elsewhere | `wiredConnectorsAreNeverCaptured` | two LV connectors joined by a copper wire stay in place with the wire (`c:relocation_not_supported`) |
| Super Factory Manager 4.34.0 (1.21.1 only) | the manager crashes when moved | `managerMovesWithItsProgram` | the manager moves with its disk and program, no crash; the program moves nothing until the inventories are labelled again (labels hold positions), then works |

Refined Storage, Mekanism, Immersive Engineering and Super Factory Manager have no 26.x build: their test classes are
out of the build (`neoforge/build.gradle`) and their rows above are the 1.21.1 results. Corail Tombstone has a 26.2
build but none for 26.3: on 26.3 `-Pincompat` loads no mod (Gradle prints it), `TombstoneTests` is not registered, and
`validate-all.sh --incompat` reports its two steps SKIP; add a mod back in the `incompatMods` map of
`neoforge/build.gradle` and `INCOMPAT_MODS` of `validate-all.sh` when it has a 26.3 build. GregTech CEu Modern 7.0.2 (1.21.1) is not loaded: it does not start on a dedicated server ("Attempted to load class
net/minecraft/client/multiplayer/ClientLevel for invalid dist DEDICATED_SERVER" with LDLib 1.0.35.a, a missing LDLib class
with 1.0.41). IndustrialCraft 2 and Blood Magic have no NeoForge 1.21.1 version on Modrinth.

```
./gradlew :neoforge:runGameTestServer -Pincompat
-Pincompat: no mod of the known incompatibilities has a build for Minecraft 26.3, none is loaded
...
[Server thread/INFO] [net.minecraft.gametest.framework.GameTestServer/]: All 116 required tests passed :)
```

`validate-all.sh --incompat` runs them in the dev runtime and on the NeoForge release jar (`EXTRA_MODS`).

### Compatibility matrix (Minecraft 26.3)

Each mod tested on 1.21.1 and 26.2 was tested again with its newest 26.3 build (Modrinth API on 2026-11-23, pinned in
`gradle.properties` and `validate-all.sh`); a mod without a 26.3 build is not tested. Since it worked on 26.2, it is
likely to work once it is published for 26.3, but nothing checks it. Result of `validate-all.sh --iris --incompat` on
2026-11-25 (table in [Everything at once](#everything-at-once)):

| Mod | 26.3 build | Tested by | Result |
|---|---|---|---|
| JEI | 31.23.0.129 (beta), NeoForge + Fabric | client smoke, both loaders, Improved Transparency off and on | pass |
| REI (+ Architectury 22.0.6, Cloth Config) | 26.3.824 (beta on NeoForge) | client smoke, both loaders | pass |
| EMI | none | — | not tested (no 26.x build; plugin out of the build) |
| SecurityCraft | v1.10.2.1-beta4 (beta, NeoForge) | NeoForge GameTests (`SecurityCraftTests`), dev and release jar | pass |
| Waystones + Balm (+ Shogi) | 26.3.0.3 / 26.3.0.3 (26.3.0.3) | `-PmodCompat` NeoForge | pass |
| Sophisticated Storage + Core | 26.3-1.5.134 / 26.3-1.5.42 | `-PmodCompat` NeoForge | pass |
| WorldEdit | 7.4.6 | `-PmodCompat` NeoForge (writes the schematic fixtures) | pass |
| Open Parties and Claims | 0.33.0 (beta), NeoForge + Fabric | `-PmodCompat` both loaders, Fabric release jar | pass |
| Flan | 26.3-1.12.9, NeoForge + Fabric | `-PmodCompat` both loaders, Fabric release jar | pass |
| Get Off My Lawn ReServed (+ Polymer 0.18.7+26.3) | 1.22.0+26.3-rc-1 (Fabric; built for 26.3-rc-1, declares `minecraft >=26.2-`) | Fabric release jar with the claim mods | pass: loads on 26.3, `GetOffMyLawnTests` passes |
| Common Protection API | 2.0.0 | Fabric GameTests (`TestProbe`) | pass |
| Forge Config API Port | 26.3.1 | every Fabric run | pass |
| Iris + Sodium (+ MakeUp Ultra Fast 9.5g) | Iris 1.11.7 / Sodium 0.9.2 (Fabric) | client smoke `--iris`, Fabric | pass on Fabric. NeoForge not tested: no pair loads on NeoForge 26.3.0.115 (Sodium 0.9.3 and later refuse Iris below 1.12.1, which does not exist for 26.3; Sodium 0.9.2 crashes the NeoForge client at startup) |
| Corail Tombstone | none (9.9.4 exists for 26.2) | — | not tested (`--incompat` steps SKIP) |
| Refined Storage | none (3.2.1 exists for 26.1.2) | — | not tested |
| Mekanism | none | — | not tested |
| Immersive Engineering | none | — | not tested |
| Super Factory Manager | none (4.34.0 exists for 26.1.2) | — | not tested |
| Integrated Dynamics, Ad Astra, Farmer's Delight (`--modded`) | not all of them | — | not tested (`--modded` SKIP) |
| Forgeulously Optimized 1.1.4 (modpack) | none for 26.x | — | not tested (`--modpack` SKIP) |

The 26.2 results are in this file on branch `dev-1.26.2`.

## Client smoke test

A dev only harness (`common/src/gametest/java/capsule/clientsmoke`, never in the release jars) plays a scenario in a
real client when the JVM has `-Dcapsule.clientsmoke=true`. The Gradle task `runClientSmoke` of each loader sets it; the
loader entrypoints are `NeoForgeClientSmoke` (a client `@Mod` class) and `FabricClientSmoke` (client entrypoint of
`capsule-gametest`).

```
scripts/client-smoke.sh neoforge
scripts/client-smoke.sh fabric
```

- The script runs the task under `xvfb-run` (unless `DISPLAY` is set) with a `TIMEOUT` (seconds, default 1500), then
  copies the screenshots, `report.txt`, `console.log` and `client.log` to `build/client-smoke/<loader>/` (or `OUT`). In
  this container the client renders with Mesa llvmpipe; a run takes about two minutes once Gradle is warm. Since 26.3
  the game opens its window with SDL and asks for an sRGB framebuffer, which no GLX visual of Xvfb offers: under
  `xvfb-run` the script sets `SDL_VIDEO_FORCE_EGL=1` and needs Mesa's EGL (`libEGL.so.1`, Ubuntu package `libegl1`),
  as does `scripts/showcase.sh`.
- `IMPROVED_TRANSPARENCY=on|off` (default `off`) sets the video option of that name in `options.txt`. With it on, 26.3
  draws the translucent phase with order independent transparency, where every blended render type needs OIT pipelines
  (`CustomRenderType.GHOST_OIT_PIPELINES`, `RenderTypes.linesTranslucentNoDepthWrite()`). The scenario writes
  `INFO Improved Transparency on|off` to the report, as the game ran, and the script fails if it is not the mode asked
  for. `validate-all.sh` runs JEI with it on, on both loaders. Iris turns the option off while a shader pack is on, so
  the Iris runs are always off.
- `RECIPE_VIEWER=jei|rei` (default `jei`) picks the recipe viewer of the dev client (`-PrecipeViewer`), on both
  loaders. EMI has no build for Minecraft 26.x: its plugin and probe stay in the sources, out of the build. `GRADLE_ARGS` adds arguments to the Gradle call, for example `-I mirror.gradle` with an init script putting a
  Maven Central mirror first when Maven Central answers 429. `MODPACK` runs the production client instead, see
  [Mod interaction tests](#mod-interaction-tests).
- `SHADER_PACK=<zip>` turns that shader pack on with Iris (`config/iris.properties`); `EXTRA_MODS` must bring Iris
  and Sodium. `validate-all.sh --iris` uses MakeUp Ultra Fast with `TIMEOUT=3000` (llvmpipe is slow with shaders). On
  NeoForge it also passes `-PdisableGlValidation` (`-Dneoforge.disableGlValidation=true`): NeoForge turns on vanilla's
  GL draw validation in dev runs, and Iris 1.11.4's own pipelines fail it on 26.2 (an `ArrayIndexOutOfBoundsException`
  in `GlCommandEncoder#validateDraw` while Iris draws the hand, as soon as the world loads). Production clients never
  run it. Capsule assigns its preview pipelines to Iris programs (`IrisCompat`), otherwise Iris draws the preview
  without the pack's programs and it is almost invisible. On 26.3 the Iris run is Fabric only (Iris 1.11.7 with
  Sodium 0.9.2): no Iris and Sodium pair loads on NeoForge 26.3.0.115 (see the compatibility matrix).
- `EXTRA_MODS` with the mods of the preview issues (`validate-all.sh --modded`): on NeoForge Integrated Dynamics,
  Cyclops Core, Common Capabilities, Integrated Tunnels, Ad Astra, Resourceful Lib, Common Storage Lib, Resourceful
  Config, Farmer's Delight, and Refined Storage 2.0.9, which Integrated Dynamics needs in the NeoForge dev client; on
  Fabric Ad Astra and its libraries and Farmer's Delight Refabricated 3.2.8 (3.3.x crashes the Fabric dev remapper).
  The scenario places the blocks of each mod that is loaded (vanilla farmland and crops always). These are the 1.21.1
  mods: Ad Astra and Farmer's Delight have no 26.x build, so on 26.x `--modded` is reported SKIP (`BACKLOG.md`).
- The script writes `onboardAccessibility:false` to the run directory's `options.txt`: since 26.1 the client shows
  the accessibility onboarding screen before the first world load otherwise.
- Scenario, on ticks of the client: create a flat creative world (`saves/capsule-smoke`, recreated on each run), build a
  house on a capture base, throw an empty capsule to capture it (with the capture animation, then the fallback box of a
  capture the client did not see), give capsules of every state and color, open the survival inventory with tooltips,
  the creative search, the label screen of the linked capsule and the recipe viewer, then fly to an empty area, preview, rotate, throw, deploy the captured
  house, preview it over the deployed one, undeploy it with `captureAnimation` off, preview and deploy a blueprint of
  the castle wall prefab, preview the house in water, between glass panes and against terrain, and capture, preview
  and deploy the modded blocks.
  Screenshots: `<run dir>/screenshots/capsule-smoke/NN-<step>.png` (1280×720).
- Checks, each a `PASS`/`FAIL` line of `report.txt`:
  - no missing texture: fewer than 64 magenta pixels of the missing texture in every screenshot;
  - every capsule slot of the inventory screenshots differs from the empty slot background;
  - the log has no warning of a `capsule.*` logger, no missing model, texture or blockstate of the `capsule` namespace
    and no exception thrown through capsule code (a log4j appender installed before the models load);
  - the recipe viewer (JEI or REI, through `RecipeViewerProbe`, once it has loaded its plugins) lists crafting
    recipes and information pages of capsules, a recipe for every capsule tier, and every recipe Capsule adds for the
    viewers (`RecipeViewerContent`: upgrades, clear, recovery, blueprints, prefab blueprints from the templates of
    `config/capsule/prefabs`, blueprint change); `SKIP recipe viewers` when none is installed;
  - the deployed chest keeps its 5 diamonds, the blueprint preview has as many blocks as the blueprint, the modded
    blocks deploy;
  - the capture animation draws frames with `captureAnimation` on and none with it off;
  - every step finishes in time (a step waiting for the server, for example the capture, fails after its timeout).
- Exit code: the client exits with status 1 when a check failed; the script fails if the client fails, if
  `RESULT PASS` is missing from the report, or if the chosen recipe viewer was not checked.
- Expected output:
  ```
  == neoforge client smoke test with JEI, Improved Transparency off (log: build/client-smoke/neoforge/console.log)
  INFO Improved Transparency off
  PASS 01-capture-base has no missing texture: 0 missing texture pixels
  ...
  PASS JEI shows the capsule recipes: 9 crafting recipes for Empty Capsule, 14 capsule information pages
  PASS JEI shows a recipe for every capsule tier: 29 tiers, without recipe: []
  PASS JEI shows the upgrade, clear, recovery, blueprint and prefab recipes: 241 recipes including 6 prefabs, without recipe: []
  ...
  PASS no capsule warning, missing model/texture or exception in the log
  RESULT PASS
  OK: neoforge client smoke test passed, screenshots in build/client-smoke/neoforge
  ```
- The screenshots still need a look from a human after visual changes: the checks catch missing textures and errors,
  not wrong colors or misplaced previews. `docs/MANUAL_VALIDATION.md` records the last review.
- To change the scenario: steps are chained in `ClientSmokeTest` (`run`, `sleep`, `await` with a timeout, `async` for
  work on the server thread). Inputs go through the real key mappings (`KeyMapping.click`), server state is set on the
  integrated server thread, and screenshots are the last rendered frame.

### Showcase recordings

`scripts/showcase.sh neoforge|fabric [scene,...]` plays staged scenes of the 9.1 features instead of the smoke scenario
(`-Pshowcase`, `-Dcapsule.showcase=true`, class `Showcase`, never active otherwise and never in the jars) and records
each with ffmpeg (x11grab of the Xvfb display), plus PNG stills, for the illustrated changelog. Scenes: `title`,
`modmenu` (Mod Menu in `EXTRA_MODS`, Fabric), `capture` (always played), `preview`, `deploy`, `loyalty`, `enchanting`,
`claim` (Flan in `EXTRA_MODS`), `blueprint`, `lava`, `viewer`. Output in `build/showcase/<loader>/`: `.mkv` recordings,
a GIF of each (`GIF_WIDTH`, `GIF_FPS`) and the PNGs. Needs ffmpeg.

## Production jar smoke test

`scripts/prod-smoke.sh <jar>...` starts a real dedicated server for each built release jar, outside Gradle:

```
./gradlew build
scripts/prod-smoke.sh neoforge/build/libs/Capsule-neoforge-*.jar fabric/build/libs/Capsule-fabric-*.jar
```

- The loader comes from the jar name, the versions from `gradle.properties`. NeoForge: the official installer
  (`--installServer`) of `neo_version`. Fabric: the Fabric server launcher of `fabric_loader_version`, plus the jar's
  required dependencies, Fabric API (`fabric_api_version`) and Forge Config API Port (Modrinth).
- Each server runs in a temporary directory: EULA accepted, `nogui`, a free port, offline mode. The script waits for
  `Done`, sends `help capsule`, then `stop`.
- It fails if the server does not reach `Done` within `TIMEOUT` seconds (default 600), if the capsule command is not
  registered, if `config/capsule-common.toml` or `config/capsule/loot` are not created, if a log line with `ERROR`,
  `Exception` or `Caused by` mentions capsule or a mixin, or if the server does not stop cleanly. Exit code 0 when every
  jar passes.
- Expected output per jar, for example:
  ```
  == fabric: Capsule-fabric-26.3-11.0.SNAPSHOT.jar in /tmp/smoke-fabric.iXn56m
  [21:46:07] [Server thread/INFO]: Done (3.464s)! For help, type "help"
  	- capsule 26.3-11.0.SNAPSHOT
  	   \-- common-protection-api 2.0.0
  	- fabric-api 0.166.2+26.3
  	- forgeconfigapiport 26.3.1
  [21:46:08] [Server thread/INFO]: /capsule giveEmpty [<size>]
  OK: fabric server booted with Capsule-fabric-26.3-11.0.SNAPSHOT.jar and stopped cleanly
  ```
- It also fails when capsule asks an update checker (`neoforge.mods.toml` declares no `updateJSONURL`).
- `KEEP_SERVER=1` keeps the server directories for inspection. Needs Java 25 on `PATH`, `curl` and `python3`, and network access.
  Downloads (installers, Fabric API, Forge Config API Port, `EXTRA_MODS` of `validate-all.sh`) and the installed NeoForge
  server are cached in `CAPSULE_CACHE` (default `~/.cache/capsule-validation`), outside the repository
  (`scripts/lib.sh`).

## GameTests on the release jars

`scripts/prod-gametest.sh <jar>...` runs every GameTest on a real dedicated server of the jar's loader, with the
release jar and its test mod jar (`<loader>/build/test-mod/<release jar name>-gametest.jar`, made by `./gradlew build`):

```
./gradlew build
scripts/prod-gametest.sh neoforge/build/libs/Capsule-neoforge-*.jar fabric/build/libs/Capsule-fabric-*.jar
```

- Servers are installed like in `prod-smoke.sh`, in a temporary directory. NeoForge starts FML's GameTest server
  entrypoint (`net.neoforged.fml.startup.GameTestServer` instead of `Server` in the installed `unix_args.txt`) with
  `-Dcapsule.gametest.production=true`, and SecurityCraft (Modrinth), for `SecurityCraftTests`. NeoForge never enables
  GameTests in production (`GameTestHooks#isGametestEnabled`, which gates `RegisterGameTestsEvent` and the GameTest
  ticker), so the test mod mixin `GameTestHooksMixin` turns it on with that property. Fabric runs with
  `-Dfabric-api.gametest` and Fabric API's GameTest module, which its release jar does not bundle (the version comes
  from the Fabric API pom).
- Expected output: the same summary as `runGameTestServer`, then `OK`:
  ```
  == neoforge: Capsule-neoforge-26.3-11.0.SNAPSHOT.jar + Capsule-neoforge-26.3-11.0.SNAPSHOT-gametest.jar in /tmp/gametest-neoforge.ccheTQ
  [20:10:16] [Server thread/INFO] [minecraft/GameTestServer]: All 116 required tests passed :)
  OK: All 116 required tests passed on Capsule-neoforge-26.3-11.0.SNAPSHOT.jar
  == fabric: Capsule-fabric-26.3-11.0.SNAPSHOT.jar + Capsule-fabric-26.3-11.0.SNAPSHOT-gametest.jar in /tmp/gametest-fabric.LHm14n
  [20:10:41] [Server thread/INFO]: All 114 required tests passed :)
  OK: All 114 required tests passed on Capsule-fabric-26.3-11.0.SNAPSHOT.jar
  ```
- Exit code 0 when every jar passes. A failure prints the failed tests and keeps the server directory:
  ```
  FAIL_ON_PURPOSE=1 scripts/prod-gametest.sh neoforge/build/libs/Capsule-neoforge-*.jar
  [20:11:07] [Server thread/ERROR] [minecraft/GameTestServer]: 1 required tests failed :(
  [20:11:07] [Server thread/ERROR] [minecraft/GameTestServer]:    - capsule:failsonpurpose: failed on purpose (-Dcapsule.gametest.failOnPurpose=true) on tick 0
  [20:11:03] [Server thread/ERROR] [minecraft/LogTestReporter]: capsule:failsonpurpose failed at 629416, -59, -13486610! failed on purpose (-Dcapsule.gametest.failOnPurpose=true) on tick 0
  FAIL: neoforge GameTests on the release jar (exit status 1), log: /tmp/gametest-neoforge.Mpd6LY/console.log
  ```
  `FAIL_ON_PURPOSE=1` registers `FailureProofTests` (`-Dcapsule.gametest.failOnPurpose=true`), never registered otherwise.
- `TIMEOUT` (seconds, default 1200), `EXTRA_MODS` and `KEEP_SERVER` work as in `prod-smoke.sh`.
- With the claim mods in `EXTRA_MODS`, their tests run against the release jar too. Get Off My Lawn is tested this way
  only: it nests 8 libraries in its jar (Cardinal Components, sgui, placeholder-api, rtree, Common Protection API, …) and
  requires Polymer, which Fabric Loader loads on a real server but the Loom dev runs do not, so it cannot start in
  `runGameTestServer`. The Fabric test mod compiles against it (`compileOnly`) and `FabricGameTests` registers
  `GetOffMyLawnTests` only when it is loaded, so the test mod never loads its classes otherwise. `validate-all.sh`
  downloads Get Off My Lawn ReServed 1.22.0+26.3-rc-1, Polymer 0.18.7+26.3, Open Parties and Claims 0.33.0 and Flan
  26.3-1.12.9 for Fabric (by sha1 from Modrinth, into `CAPSULE_CACHE/modcompat/fabric`):
  ```
  EXTRA_MODS="$(ls ~/.cache/capsule-validation/modcompat/fabric/*.jar | tr '\n' ' ')" scripts/prod-gametest.sh fabric/build/libs/Capsule-fabric-*.jar
  ...
  OK: All 119 required tests passed on Capsule-fabric-26.3-11.0.SNAPSHOT.jar
  ```
  119: the 114 Fabric GameTests, 3 Flan, 1 Open Parties and Claims and 1 Get Off My Lawn test.

## Everything at once

`scripts/validate-all.sh` runs every automated layer, one step at a time, and prints one summary:

```
scripts/validate-all.sh            # build, unit tests, GameTests, mod-compat GameTests, release-jar GameTests (Fabric
                                   # also with the claim mods) and their failure proof, prod smoke,
                                   # client smoke × {NeoForge, Fabric} × {JEI, REI, JEI with Improved Transparency}
scripts/validate-all.sh --iris     # + client smoke with Iris, Sodium and MakeUp Ultra Fast on both loaders
scripts/validate-all.sh --modded   # + client smoke with the mods of #81, #94, #117 and #76 (SKIP on 26.x)
scripts/validate-all.sh --modpack  # + client smoke in a production NeoForge client with MODPACK (SKIP without)
scripts/validate-all.sh --incompat # + GameTests with the mods of the known incompatibilities, dev runtime and release jar (SKIP on 26.3)
scripts/validate-all.sh --all      # everything
```

- It works from a clean clone with Java 25 on `PATH` (checked first: the release jars run on real servers with it),
  `python3`, `curl`, `xvfb-run`, `flock`, Mesa's EGL (`libegl1`, for the clients under Xvfb) and network access. Everything downloaded outside Gradle (servers, installers, mods of
  the variants, the modpack, the shader pack, by sha1 from Modrinth) is cached in `CAPSULE_CACHE` (default
  `~/.cache/capsule-validation`). `GRADLE_ARGS` is added to every Gradle call, `MODPACK` overrides the pack.
  `MODCOMPAT_LATEST=1` runs the mod-compat and incompatibility steps with the newest Modrinth versions of their mods
  (`-PmodCompatLatest` for Gradle, the newest files for the release jar runs).
- Every step takes the lock `/tmp/capsule-heavy.lock` (as `flock /tmp/capsule-heavy.lock <command>` does), so it
  waits for any other Minecraft run of the machine and lets others run between its steps.
- Logs: `build/validate-all/<step>.log` and `summary.txt`; each client smoke run keeps its screenshots, report and logs
  in `build/client-smoke/<step>/`. Exit code 0 when every step passes.
- Do not run `./gradlew --stop` while another run uses Gradle on the machine: it kills that run's daemon too.
- On 26.2, `--iris --incompat` (the variants that have 26.2 builds) takes about 20 minutes once Gradle is warm. Its
  summary on 2026-11-23, without the log column (`--modded` and `--modpack` would add SKIP rows):
  ```
  RESULT STEP                                         TIME  DETAIL
  PASS   build: jars and loader import check         0m10s  BUILD SUCCESSFUL
  PASS   unit tests (both loaders)                   0m22s  BUILD SUCCESSFUL
  PASS   GameTests NeoForge                          0m30s  All 114 required tests passed
  PASS   GameTests Fabric                            0m25s  All 112 required tests passed
  PASS   mod-compat GameTests NeoForge               0m35s  All 122 required tests passed
  PASS   mod-compat GameTests Fabric                 0m27s  All 116 required tests passed
  PASS   GameTests on the NeoForge release jar       0m25s  OK: All 114 required tests passed on Capsule-neoforge-26.2-10.0.SNAPSHOT.jar
  PASS   GameTests on the Fabric release jar         0m22s  OK: All 112 required tests passed on Capsule-fabric-26.2-10.0.SNAPSHOT.jar
  PASS   Fabric release jar with the claim mods      0m25s  OK: All 117 required tests passed on Capsule-fabric-26.2-10.0.SNAPSHOT.jar
  PASS   NeoForge release jar: failure reported      0m23s  OK: the failing test was reported (exit status 1)
  PASS   Fabric release jar: failure reported        0m21s  OK: the failing test was reported (exit status 1)
  PASS   release jars on dedicated servers           0m36s  OK: fabric server booted with Capsule-fabric-26.2-10.0.SNAPSHOT.jar and stopped
  PASS   client smoke neoforge-jei                   1m22s  47 checks passed, 0 failed, 0 skipped
  PASS   client smoke neoforge-rei                   1m21s  47 checks passed, 0 failed, 0 skipped
  PASS   client smoke fabric-jei                     1m16s  47 checks passed, 0 failed, 0 skipped
  PASS   client smoke fabric-rei                     1m16s  47 checks passed, 0 failed, 0 skipped
  PASS   client smoke neoforge-iris                  1m27s  47 checks passed, 0 failed, 0 skipped
  PASS   client smoke fabric-iris                    1m20s  47 checks passed, 0 failed, 0 skipped
  PASS   incompatibility GameTests NeoForge          0m33s  All 115 required tests passed
  PASS   NeoForge release jar, incompatible mods     0m25s  OK: All 115 required tests passed on Capsule-neoforge-26.2-10.0.SNAPSHOT.jar
  ```
- On 1.21.1, `--all` (with EMI, the modded blocks, the modpack and the four other incompatible mods) passed every step;
  see this file on branch `1.21.1`.

## Not covered

See `docs/MANUAL_VALIDATION.md`: multiplayer with latency, sounds and controls, modpacks beyond the one tested, and the
TEST.md items marked manual.
