# Testing

Four automated layers: JUnit unit tests and GameTests run on both loaders on every `./gradlew build`; the production
jar smoke test runs the built jars on real servers; the client smoke test plays a scenario in a real client and takes
screenshots. What remains manual is listed in `docs/MANUAL_VALIDATION.md`. Everything needs Java 21; the first build downloads and
decompiles Minecraft for both loaders (several minutes), later runs take about two minutes.

## Layout

| Project | Contents |
|---|---|
| `common` | all the game logic, assets and data (`src/main`), unit tests (`src/test/java`), GameTest bodies and the client smoke test (`src/gametest/java`, packages `capsule.gametest` and `capsule.clientsmoke`) and test structures (`src/gametest/resources`). Compiled against vanilla Minecraft; `checkLoaderImports` (part of `check`) fails if a common source references `net.neoforged` or `net.fabricmc` |
| `neoforge` | NeoForge glue; compiles the common sources into `Capsule-neoforge-<mc>-<version>.jar`. Its gametest source set adds the SecurityCraft, Waystones and Sophisticated Storage tests and the client smoke entrypoint |
| `fabric` | Fabric glue (Loom); compiles the common sources into `Capsule-fabric-<mc>-<version>.jar`. Its gametest source set is a dev only mod, `capsule-gametest`, with the GameTests and the client smoke entrypoint |

## Commands

| Command | Runs | Exit code |
|---|---|---|
| `./gradlew build` | compile, jars, and `check` of every project: loader import check, unit tests and GameTests of both loaders | non-zero if anything fails |
| `./gradlew check --continue` | every test layer, reporting all of them even if one fails | |
| `./gradlew :neoforge:test` / `:fabric:test` | JUnit tests of `common/src/test/java` on one loader | non-zero if a test fails |
| `./gradlew :neoforge:runGameTestServer` | the 43 NeoForge GameTests (41 common + 2 SecurityCraft) on a headless server | number of failed required tests |
| `./gradlew :neoforge:runGameTestServer -PmodCompat` | the same plus the Waystones and Sophisticated Storage tests (46) | number of failed required tests |
| `./gradlew :fabric:runGameTestServer` | the 41 common GameTests on a headless Fabric server | number of failed required tests |
| `./gradlew :neoforge:runClient` / `:fabric:runClient` | a dev client with the mod and its GameTests | |
| `scripts/prod-smoke.sh <jar>...` | the release jars on real dedicated servers, see below | non-zero if a jar fails |
| `scripts/client-smoke.sh neoforge\|fabric` | the client smoke test under Xvfb, see below | non-zero if a check fails |

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
- `runGameTestServer` deletes `<loader>/runs/gameTestServer/world`, `config/capsule` and `initialconfig` first, so every
  run starts from a fresh world and default config.
- Expected output, at the end of the console log:
  ```
  [Server thread/INFO] [minecraft/GameTestServer]: All 43 required tests passed :)      (NeoForge)
  [Server thread/INFO] (Minecraft) All 41 required tests passed :)                      (Fabric)
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
- Tests changing global state go in their own batch (`batch = "reload"` for `/reload`), and must restore what they
  change (see `ConfigTests`).
- A bug fix comes with a test that fails before the fix; record the failure message in the commit and in
  `docs/ISSUE_TRIAGE.md`.

### Mods on the GameTest runtime

`gametestImplementation` dependencies of the neoforge project are loaded by its dev runs only: SecurityCraft 1.21.1
(Modrinth maven, version id in `gradle.properties`) for `SecurityCraftTests`, NeoForge only. JEI is loaded in the
NeoForge dev runs as well (`localRuntime`); JEI for Fabric is not (its jars need a newer Loom, which needs Java 25).
The Fabric dev runs load Fabric API and Forge Config API Port.

## Mod interaction tests

Mods from issues, checked with Capsule (results and versions in `docs/MANUAL_VALIDATION.md`):

- **GameTests** with `-PmodCompat`: the NeoForge GameTest runtime also loads Waystones + Balm and Sophisticated
  Storage + Sophisticated Core (Modrinth maven, version ids in `gradle.properties`). `NeoForgeGameTests` registers
  `WaystonesTests` (#121) and `SophisticatedStorageTests` (#115) only when their mod is loaded; they reference the mods'
  blocks by id, so they compile without them.
  ```
  ./gradlew :neoforge:runGameTestServer -PmodCompat
  ...
  [Server thread/INFO] [minecraft/GameTestServer]: All 46 required tests passed :)
  ```
- **Servers**: `EXTRA_MODS` adds jars to the server of `scripts/prod-smoke.sh`, for example the mods above and JEI,
  downloaded from Modrinth (`https://api.modrinth.com/v2/version/<id>` gives the file URL):
  ```
  EXTRA_MODS="$(ls ~/mods/*.jar)" scripts/prod-smoke.sh neoforge/build/libs/Capsule-neoforge-*.jar
  ```
  Avoid spaces and brackets in the jar names.
- **Modpacks**: `EXTRA_MODS` also adds jars to the client of `scripts/client-smoke.sh`. NeoForge 1.21.1 dev runs load
  production mod jars from the mods folder, so the mods of a Modrinth pack (`modrinth.index.json` of the `.mrpack`
  lists their URLs) can run the whole client scenario. Leave out JEI (already in the dev runs), Sinytra Connector (it
  cannot start in a dev run) and the Fabric mods it would load. A mod crashing at startup leaves the client on its crash
  screen until `TIMEOUT`.

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
  copies the screenshots, `report.txt`, `console.log` and `client.log` to `build/client-smoke/<loader>/`. In this
  container the client renders with Mesa llvmpipe; a run takes 3 to 4 minutes once Gradle is warm.
- Scenario, on ticks of the client: create a flat creative world (`saves/capsule-smoke`, recreated on each run), build a
  house on a capture base, throw an empty capsule to capture it, give capsules of every state and color, open the
  survival inventory with tooltips, the creative search and the JEI recipes, then fly to an empty area, preview, rotate,
  throw, deploy and undeploy the captured house, and preview and deploy a blueprint of the castle wall prefab.
  Screenshots: `<run dir>/screenshots/capsule-smoke/NN-<step>.png` (1280×720).
- Checks, each a `PASS`/`FAIL` line of `report.txt`:
  - no missing texture: fewer than 64 magenta pixels of the missing texture in every screenshot;
  - every capsule slot of the inventory screenshots differs from the empty slot background;
  - the log has no warning of a `capsule.*` logger, no missing model, texture or blockstate of the `capsule` namespace
    and no exception thrown through capsule code (a log4j appender installed before the models load);
  - JEI lists crafting recipes and information pages of capsules (NeoForge; `SKIP JEI` when JEI is not installed);
  - the deployed chest keeps its 5 diamonds, the blueprint preview has as many blocks as the blueprint;
  - every step finishes in time (a step waiting for the server, for example the capture, fails after its timeout).
- Exit code: the client exits with status 1 when a check failed; the script fails if the client fails, if
  `RESULT PASS` is missing from the report, or, on NeoForge, if JEI was not checked.
- Expected output:
  ```
  == neoforge client smoke test (log: build/client-smoke/neoforge/console.log)
  PASS 01-capture-base has no missing texture: 0 missing texture pixels
  ...
  PASS JEI shows the capsule recipes: 3 crafting recipes for Empty Capsule, 14 capsule information pages
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
- `KEEP_SERVER=1` keeps the server directories for inspection. Needs Java 21, `curl` and `python3`, and network access.
- GameTests cannot run against the release jars: NeoForge only registers GameTests outside production, and the tests
  are not packaged in the jars.

## Not covered

See `docs/MANUAL_VALIDATION.md`: multiplayer with latency, sounds and controls, shaders, JEI on Fabric, modpacks
beyond the one tested, and the TEST.md items marked manual.
