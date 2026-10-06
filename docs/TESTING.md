# Testing

`scripts/validate-all.sh` runs every automated layer and prints one PASS/FAIL summary with the paths of the logs and
screenshots (`--all` adds the Iris, modded blocks and modpack client runs): see [Everything at once](#everything-at-once).

Five automated layers: JUnit unit tests and GameTests run on both loaders on every `./gradlew build`; the GameTests
also run against the release jars on real servers; the production jar smoke test boots the built jars on real servers;
the client smoke test plays a scenario in a real client, with JEI, REI or EMI, and takes screenshots. What remains
manual is listed in `docs/MANUAL_VALIDATION.md`.

Minecraft and the mod run on Java 21. Gradle itself runs on Java 25 (Fabric Loom 1.18 needs it): the Gradle launcher
starts its daemon on a Java 25 found on the machine, or downloads one (`gradle/gradle-daemon-jvm.properties`, kept in
`~/.gradle/jdks`), and still compiles and runs the game with the Java 21 toolchain. The first build downloads and
decompiles Minecraft for both loaders (several minutes), later runs take about two minutes.

## Layout

| Project | Contents |
|---|---|
| `common` | all the game logic, assets and data (`src/main`), unit tests (`src/test/java`), GameTest bodies and the client smoke test (`src/gametest/java`, packages `capsule.gametest` and `capsule.clientsmoke`) and test structures (`src/gametest/resources`). Compiled against vanilla Minecraft; `checkLoaderImports` (part of `check`) fails if a common source references `net.neoforged` or `net.fabricmc` |
| `neoforge` | NeoForge glue; compiles the common sources into `Capsule-neoforge-<mc>-<version>.jar`. Its gametest source set is a mod of its own, `capsule_gametest`, with the GameTests (adding the SecurityCraft, Waystones, Sophisticated Storage and WorldEdit tests) and the client smoke entrypoint |
| `fabric` | Fabric glue (Loom); compiles the common sources into `Capsule-fabric-<mc>-<version>.jar`. Its gametest source set is a mod of its own, `capsule-gametest`, with the GameTests and the client smoke entrypoint |

The test mods are packaged too, in `<loader>/build/test-mod/<release jar name>-gametest.jar` (task `testModJar`, part of
`assemble`, remapped like the release jar on Fabric), so that the GameTests and the client smoke scenario run next to
the release jar in real servers and clients. They are never published: CI publishes `build/libs` only.

## Commands

| Command | Runs | Exit code |
|---|---|---|
| `./gradlew build` | compile, jars, and `check` of every project: loader import check, unit tests and GameTests of both loaders | non-zero if anything fails |
| `./gradlew check --continue` | every test layer, reporting all of them even if one fails | |
| `./gradlew :neoforge:test` / `:fabric:test` | JUnit tests of `common/src/test/java` on one loader | non-zero if a test fails |
| `./gradlew :neoforge:runGameTestServer` | the 85 NeoForge GameTests (83 common + 2 SecurityCraft) on a headless server | number of failed required tests |
| `./gradlew :neoforge:runGameTestServer -PmodCompat` | the same plus the Waystones (2), Sophisticated Storage, WorldEdit, Open Parties and Claims and Flan tests (91) | number of failed required tests |
| `./gradlew :fabric:runGameTestServer` | the 83 common GameTests on a headless Fabric server | number of failed required tests |
| `./gradlew :fabric:runGameTestServer -PmodCompat` | the same plus the Open Parties and Claims and Flan tests (85) | number of failed required tests |
| `./gradlew :neoforge:runClient` / `:fabric:runClient` | a dev client with the mod and its GameTests | |
| `scripts/prod-gametest.sh <jar>...` | the GameTests on real dedicated servers with the release jars, see below | non-zero if a test fails |
| `scripts/prod-smoke.sh <jar>...` | the release jars on real dedicated servers, see below | non-zero if a jar fails |
| `scripts/client-smoke.sh neoforge\|fabric` | the client smoke test under Xvfb, see below | non-zero if a check fails |
| `scripts/validate-all.sh` | every layer above, with JEI, REI and EMI, and a summary table, see below | non-zero if a step fails |

CI passes the build number with `-Pbuild_id=<n>` (default `SNAPSHOT`), which only changes the jar names
`neoforge/build/libs/Capsule-neoforge-1.21.1-9.0.<n>.jar` and `fabric/build/libs/Capsule-fabric-1.21.1-9.0.<n>.jar`.
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
  classes (`GameTestHelper`, `@GameTest`), so the same tests run on both loaders.
- Registration: `CapsuleGameTests` is a `@GameTestGenerator` that turns the `@GameTest` methods of the classes listed
  in `CapsuleGameTests.TEST_CLASSES` into test functions named after the method (lowercase), using the templates of
  the `capsule` namespace. NeoForge registers it from `NeoForgeGameTests` (`RegisterGameTestsEvent`), adding
  `SecurityCraftTests`; Fabric through the `fabric-gametest` entrypoint of `fabric/src/gametest/resources/fabric.mod.json`.
- `-Dcapsule.gametest.failOnPurpose=true` adds `FailureProofTests.failsOnPurpose`, which always fails: it shows that a
  runner reports failures. It is never registered otherwise.
- `runGameTestServer` deletes `<loader>/runs/gameTestServer/world`, `config/capsule`, `initialconfig`, `schematics`
  (and `worldedit-schematics` on NeoForge) first, so every run starts from a fresh world and default config, without the
  template copies of the previous run.
- The GameTest resources fill every modded `c:ingots/*` tag with a placeholder item (`data/c/tags/item/ingots`), so
  every addon capsule recipe loads and `everyCapsuleRecipeLoadsWithResolvedIngredients` checks it. The schematic
  fixtures are in `data/capsule/schematics`.
- The GameTest server has no profile cache (real servers do, claim mods need it): `GameTestProfiles` gives it an
  offline one before the server starts.
- Expected output, at the end of the console log:
  ```
  [Server thread/INFO] [minecraft/GameTestServer]: All 85 required tests passed :)      (NeoForge)
  [Server thread/INFO] (Minecraft) All 83 required tests passed :)                      (Fabric)
  ```
  On failure each test is reported with its position and message, then the summary:
  ```
  [Server thread/ERROR] [minecraft/LogTestReporter]: blindthrowdeploysontheground failed at ...! Expected Block of Gold, got Air at ... (relative: 4,1,4) (t=61)
  [Server thread/INFO] [minecraft/GameTestServer]: 1 required tests failed :(
  [Server thread/INFO] [minecraft/GameTestServer]:    - blindthrowdeploysontheground
  ```
  and the Gradle task fails. The full logs are `<loader>/runs/gameTestServer/logs/latest.log`; on Fabric the failure
  messages are in `fabric/runs/gameTestServer/logs/debug.log` and in the JUnit report
  `fabric/build/gametest/report.xml`.
- In a dev client (`./gradlew :neoforge:runClient` or `:fabric:runClient`, creative world with cheats): `/test runall`
  runs every test, `/test run <name>` one test, `/test runfailed` the failed ones. Tests are placed near the player.

### Writing a GameTest

- Use the vanilla API only, and add the class to `CapsuleGameTests.TEST_CLASSES`:
  ```java
  public class MyTests {
      @GameTest(template = "empty")
      public static void somethingWorks(GameTestHelper helper) { ... helper.succeed(); }
  }
  ```
- Templates: `capsule:empty` (9×9×9 of air) and `capsule:empty17` (17×17×17), given without namespace. Build the
  scenario in code with `helper.setBlock`; positions are relative to the test origin.
- `CapsuleTestUtils` has the common steps: `capture`, `deploy`, `template`, `emptyCapsule`, `clear`, and
  `survivalPlayer`. Use `survivalPlayer` rather than `helper.makeMockServerPlayerInLevel()`: the mock player joins the
  player list without the mod network channels, so the first capsule payload broadcast near it throws on NeoForge.
  `survivalPlayer(helper, pos, messages)` collects the player's chat feedback; without a message list it is a plain
  `ServerPlayer`, which some mods (Flan) tell apart from fake players by its class.
- `LogCapture.open()` (try-with-resources) collects the warnings and errors logged meanwhile, for tests on log output
  (`deployedItemFramesHangOnTheirBlocks`, `missingPrefabTemplateIsReported`).
- Tests changing global state go in their own batch (`reload` for `/reload`, `prefabPattern`, `claims` and
  `claimscale` for registered claim adapters, one per claim mod), and must restore what they change (see
  `ConfigTests`).
- A bug fix comes with a test that fails before the fix; record the failure message in the commit and in
  `docs/ISSUE_TRIAGE.md`.

### Mods on the GameTest runtime

`gametestImplementation` dependencies of the neoforge project are loaded by its dev runs only: SecurityCraft 1.21.1
(Modrinth maven, version id in `gradle.properties`) for `SecurityCraftTests`, NeoForge only. The dev runs of both loaders
load one recipe viewer, JEI by default: `-PrecipeViewer=jei|rei|emi` picks JEI 19.57.0.451, REI 16.0.799 (with
Architectury API and Cloth Config) or EMI 1.1.24 (versions in `gradle.properties`). The Fabric dev runs load Fabric API
and Forge Config API Port.

## Mod interaction tests

Mods from issues, checked with Capsule (results and versions in `docs/MANUAL_VALIDATION.md`):

- **GameTests** with `-PmodCompat` (Modrinth maven, version ids in `gradle.properties`): the NeoForge GameTest runtime
  also loads Waystones + Balm, Sophisticated Storage + Sophisticated Core, WorldEdit, Open Parties and Claims and
  Flan; the Fabric one Open Parties and Claims and Flan. `NeoForgeGameTests` registers `WaystonesTests` (#121),
  `SophisticatedStorageTests` (#115) and `WorldEditTests` (#70) only when their mod is loaded; they reference the mods'
  blocks by id, so they compile without them. `OpenPartiesAndClaimsTests` and `FlanTests` (#91, common) compile against
  the mods (`gametestCompileOnly`) and are registered on both loaders when the mod is loaded. `WorldEditTests` writes
  the Sponge v2 and v3 fixtures with WorldEdit's own clipboard writers. SecurityCraft, always in the NeoForge runtime,
  stops the 1.13 data fixer from upgrading older block entity data, so the MCEdit test skips the item count there.
  ```
  ./gradlew :neoforge:runGameTestServer -PmodCompat
  ...
  [Server thread/INFO] [minecraft/GameTestServer]: All 91 required tests passed :)
  ```
  Get Off My Lawn (Fabric) is not in the runtime: the Loom dev runs do not load the mods nested in its jar.
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
  MODPACK=forgeulously-optimized:mPRwXMh4 scripts/client-smoke.sh neoforge
  ```
  Forgeulously Optimized 1.1.4 (49 mods, NeoForge 21.1.218, Connector 2.0.0-beta.12 with Fabric mods, Sodium, Iris) is
  the pack `validate-all.sh --modpack` uses. The game directory is kept in `build/client-smoke/<run>/game`. The recipe
  viewer check is left to the pack: the report has a line for each viewer it brings. `EXTRA_MODS` also adds jars to the
  dev client of `scripts/client-smoke.sh`; a mod crashing at startup leaves the client on its crash screen until
  `TIMEOUT`.

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
  this container the client renders with Mesa llvmpipe; a run takes about two minutes once Gradle is warm.
- `RECIPE_VIEWER=jei|rei|emi` (default `jei`) picks the recipe viewer of the dev client (`-PrecipeViewer`), on both
  loaders. `GRADLE_ARGS` adds arguments to the Gradle call, for example `-I mirror.gradle` with an init script putting a
  Maven Central mirror first when Maven Central answers 429. `MODPACK` runs the production client instead, see
  [Mod interaction tests](#mod-interaction-tests).
- `SHADER_PACK=<zip>` turns that shader pack on with Iris (`config/iris.properties`); `EXTRA_MODS` must bring Iris
  and Sodium. `validate-all.sh --iris` uses MakeUp Ultra Fast with `TIMEOUT=3000` (llvmpipe is slow with shaders).
- `EXTRA_MODS` with the mods of the preview issues (`validate-all.sh --modded`): on NeoForge Integrated Dynamics,
  Cyclops Core, Common Capabilities, Integrated Tunnels, Ad Astra, Resourceful Lib, Common Storage Lib, Resourceful
  Config, Farmer's Delight, and Refined Storage 2.0.9, which Integrated Dynamics needs in the NeoForge dev client; on
  Fabric Ad Astra and its libraries and Farmer's Delight Refabricated 3.2.8 (3.3.x crashes the Fabric dev remapper).
  The scenario places the blocks of each mod that is loaded (vanilla farmland and crops always).
- Scenario, on ticks of the client: create a flat creative world (`saves/capsule-smoke`, recreated on each run), build a
  house on a capture base, throw an empty capsule to capture it (with the capture animation, then the fallback box of a
  capture the client did not see), give capsules of every state and color, open the survival inventory with tooltips,
  the creative search and the recipe viewer, then fly to an empty area, preview, rotate, throw, deploy the captured
  house, preview it over the deployed one, undeploy it with `captureAnimation` off, preview and deploy a blueprint of
  the castle wall prefab, preview the house in water, between glass panes and against terrain, and capture, preview
  and deploy the modded blocks.
  Screenshots: `<run dir>/screenshots/capsule-smoke/NN-<step>.png` (1280×720).
- Checks, each a `PASS`/`FAIL` line of `report.txt`:
  - no missing texture: fewer than 64 magenta pixels of the missing texture in every screenshot;
  - every capsule slot of the inventory screenshots differs from the empty slot background;
  - the log has no warning of a `capsule.*` logger, no missing model, texture or blockstate of the `capsule` namespace
    and no exception thrown through capsule code (a log4j appender installed before the models load);
  - the recipe viewer (JEI, REI or EMI, through `RecipeViewerProbe`, once it has loaded its plugins) lists crafting
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
  == neoforge client smoke test with JEI (log: build/client-smoke/neoforge/console.log)
  PASS 01-capture-base has no missing texture: 0 missing texture pixels
  ...
  PASS JEI shows the capsule recipes: 9 crafting recipes for Empty Capsule, 14 capsule information pages
  PASS JEI shows a recipe for every capsule tier: 29 tiers, without recipe: []
  PASS JEI shows the upgrade, clear, recovery, blueprint and prefab recipes: 249 recipes including 6 prefabs, without recipe: []
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
  == fabric: Capsule-fabric-1.21.1-9.0.SNAPSHOT.jar in /tmp/smoke-fabric.QnmLgQ
  [16:05:43] [Server thread/INFO]: Done (8.999s)! For help, type "help"
  	- capsule 1.21.1-9.0.SNAPSHOT
  	   \-- common-protection-api 1.0.0
  	- fabric-api 0.116.17+1.21.1
  	- forgeconfigapiport 21.1.6
  [16:05:44] [Server thread/INFO]: /capsule giveEmpty [<size>]
  OK: fabric server booted with Capsule-fabric-1.21.1-9.0.SNAPSHOT.jar and stopped cleanly
  ```
- It also fails when capsule asks an update checker (the dead `updateJSONURL` removed in 760456c).
- `KEEP_SERVER=1` keeps the server directories for inspection. Needs Java 21, `curl` and `python3`, and network access.
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

- Servers are installed like in `prod-smoke.sh`, in a temporary directory. NeoForge runs with
  `-Dneoforge.gameTestServer=true` and SecurityCraft (Modrinth), for `SecurityCraftTests`. NeoForge registers and ticks
  GameTests outside production only, so the test mod does it itself on a production GameTest server
  (`ProductionGameTests`). Fabric runs with `-Dfabric-api.gametest` and Fabric API's GameTest module, which its release
  jar does not bundle (the version comes from the Fabric API pom); the test mod jar is remapped like the release jar.
- Expected output: the same summary as `runGameTestServer`, then `OK`:
  ```
  == neoforge: Capsule-neoforge-1.21.1-9.0.SNAPSHOT.jar + Capsule-neoforge-1.21.1-9.0.SNAPSHOT-gametest.jar in /tmp/gametest-neoforge.dMMlJn
  [11:06:18] [Server thread/INFO] [minecraft/GameTestServer]: All 85 required tests passed :)
  OK: All 85 required tests passed on Capsule-neoforge-1.21.1-9.0.SNAPSHOT.jar
  == fabric: Capsule-fabric-1.21.1-9.0.SNAPSHOT.jar + Capsule-fabric-1.21.1-9.0.SNAPSHOT-gametest.jar in /tmp/gametest-fabric.eYuJF4
  [11:07:27] [Server thread/INFO]: All 83 required tests passed :)
  OK: All 83 required tests passed on Capsule-fabric-1.21.1-9.0.SNAPSHOT.jar
  ```
- Exit code 0 when every jar passes. A failure prints the failed tests and keeps the server directory:
  ```
  FAIL_ON_PURPOSE=1 scripts/prod-gametest.sh neoforge/build/libs/Capsule-neoforge-*.jar
  [11:07:56] [Server thread/INFO] [minecraft/GameTestServer]: 1 required tests failed :(
  [11:07:56] [Server thread/INFO] [minecraft/GameTestServer]:    - failsonpurpose
  [11:07:51] [Server thread/ERROR] [minecraft/LogTestReporter]: failsonpurpose failed at 10190940, -60, 14580310! failed on purpose (-Dcapsule.gametest.failOnPurpose=true)
  FAIL: neoforge GameTests on the release jar (exit status 1), log: /tmp/gametest-neoforge.mqlxI2/console.log
  ```
  `FAIL_ON_PURPOSE=1` registers `FailureProofTests` (`-Dcapsule.gametest.failOnPurpose=true`), never registered otherwise.
- `TIMEOUT` (seconds, default 1200), `EXTRA_MODS` and `KEEP_SERVER` work as in `prod-smoke.sh`.

## Everything at once

`scripts/validate-all.sh` runs every automated layer, one step at a time, and prints one summary:

```
scripts/validate-all.sh            # build, unit tests, GameTests, mod-compat GameTests, release-jar GameTests and
                                   # their failure proof, prod smoke, client smoke × {NeoForge, Fabric} × {JEI, REI, EMI}
scripts/validate-all.sh --iris     # + client smoke with Iris, Sodium and MakeUp Ultra Fast on both loaders
scripts/validate-all.sh --modded   # + client smoke with the mods of #81, #94, #117 and #76 on both loaders
scripts/validate-all.sh --modpack  # + client smoke in a production NeoForge client with a Connector modpack
scripts/validate-all.sh --all      # everything
```

- It works from a clean clone with Java 21, `python3`, `curl`, `xvfb-run`, `flock` and network access. Gradle
  downloads its own Java 25 if the machine has none. Everything downloaded outside Gradle (servers, installers, mods of
  the variants, the modpack, the shader pack, by sha1 from Modrinth) is cached in `CAPSULE_CACHE` (default
  `~/.cache/capsule-validation`). `GRADLE_ARGS` is added to every Gradle call, `MODPACK` overrides the pack.
- Every step takes the lock `/tmp/capsule-heavy.lock` (as `flock /tmp/capsule-heavy.lock <command>` does), so it
  waits for any other Minecraft run of the machine and lets others run between its steps.
- Logs: `build/validate-all/<step>.log` and `summary.txt`; each client smoke run keeps its screenshots, report and logs
  in `build/client-smoke/<step>/`. Exit code 0 when every step passes.
- Do not run `./gradlew --stop` while another run uses Gradle on the machine: it kills that run's daemon too.
- A full run (`--all`) takes about 22 to 25 minutes once Gradle is warm. Its summary, without the log column:
  ```
  RESULT STEP                                         TIME  DETAIL
  PASS   build: jars and loader import check         0m17s  BUILD SUCCESSFUL
  PASS   unit tests (both loaders)                   0m20s  BUILD SUCCESSFUL
  PASS   GameTests NeoForge                          0m34s  All 85 required tests passed
  PASS   GameTests Fabric                            0m31s  All 83 required tests passed
  PASS   mod-compat GameTests NeoForge               0m41s  All 91 required tests passed
  PASS   mod-compat GameTests Fabric                 0m31s  All 85 required tests passed
  PASS   GameTests on the NeoForge release jar       0m25s  OK: All 85 required tests passed on Capsule-neoforge-1.21.1-9.0.SNAPSHOT.jar
  PASS   GameTests on the Fabric release jar         0m27s  OK: All 83 required tests passed on Capsule-fabric-1.21.1-9.0.SNAPSHOT.jar
  PASS   NeoForge release jar: failure reported      0m24s  OK: the failing test was reported (exit status 1)
  PASS   Fabric release jar: failure reported        0m28s  OK: the failing test was reported (exit status 1)
  PASS   release jars on dedicated servers           1m00s  OK: fabric server booted with Capsule-fabric-1.21.1-9.0.SNAPSHOT.jar and stopped
  PASS   client smoke neoforge-jei                   1m24s  46 checks passed, 0 failed, 0 skipped
  PASS   client smoke neoforge-rei                   1m25s  46 checks passed, 0 failed, 0 skipped
  PASS   client smoke neoforge-emi                   1m23s  46 checks passed, 0 failed, 0 skipped
  PASS   client smoke fabric-jei                     1m22s  46 checks passed, 0 failed, 0 skipped
  PASS   client smoke fabric-rei                     1m21s  46 checks passed, 0 failed, 0 skipped
  PASS   client smoke fabric-emi                     1m22s  46 checks passed, 0 failed, 0 skipped
  PASS   client smoke neoforge-iris                  1m32s  46 checks passed, 0 failed, 0 skipped
  PASS   client smoke fabric-iris                    1m37s  46 checks passed, 0 failed, 0 skipped
  PASS   client smoke neoforge-modded                1m37s  46 checks passed, 0 failed, 0 skipped
  PASS   client smoke fabric-modded                  1m39s  46 checks passed, 0 failed, 0 skipped
  PASS   client smoke neoforge-modpack               1m22s  42 checks passed, 0 failed, 1 skipped
  ```

## Not covered

See `docs/MANUAL_VALIDATION.md`: multiplayer with latency, sounds and controls, modpacks beyond the one tested, and the
TEST.md items marked manual.
