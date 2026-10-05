# Testing

Two automated layers run on every `./gradlew build`: JUnit unit tests and GameTests. Both need Java 21; the first
build downloads and decompiles Minecraft (a few minutes), later runs take about a minute.

| Command | Runs | Exit code |
|---|---|---|
| `./gradlew test` | JUnit tests of `src/test/java` | non-zero if a test fails |
| `./gradlew runGameTestServer` | every GameTest of the `capsule` namespace on a headless server | number of failed required tests |
| `./gradlew build` | compile, jar, `test` and `runGameTestServer` (both are part of `check`) | non-zero if anything fails |
| `./gradlew check --continue` | both test layers, reporting both even if the first fails | |

CI passes the build number with `-Pbuild_id=<n>` (default `SNAPSHOT`), which only changes the jar name
`build/libs/Capsule-1.21.1-9.0.<n>.jar`.

## Unit tests (JUnit 5)

- Sources: `src/test/java`. ModDevGradle's `unitTest` runs them inside a loaded NeoForge environment, so vanilla
  classes, registries and the capsule mod are available (no world).
- Expected output: `BUILD SUCCESSFUL`; failures are listed as `SomeTest > someMethod() FAILED`.
- Reports: `build/reports/tests/test/index.html` and `build/test-results/test/TEST-*.xml`.
- Use them for logic that needs no level: paths, file names, config parsing.

## GameTests

- Sources: `src/gametest/java`, resources (test structures) in `src/gametest/resources`. This source set is part of
  the capsule mod in the dev runs (`runClient`, `runServer`, `runGameTestServer`) but is never packaged in the release
  jar.
- `runGameTestServer` deletes `runs/gameTestServer/world`, `runs/gameTestServer/config/capsule` and
  `runs/gameTestServer/initialconfig` first, so every run starts from a fresh world and default config.
- Expected output, at the end of the console log:
  ```
  [Server thread/INFO] [minecraft/GameTestServer]: All 42 required tests passed :)
  ```
  On failure each test is reported with its position and message, then the summary:
  ```
  [Server thread/ERROR] [minecraft/LogTestReporter]: blindthrowdeploysontheground failed at ...! Expected Block of Gold, got Air at ... (relative: 4,1,4) (t=61)
  [Server thread/INFO] [minecraft/GameTestServer]: 1 required tests failed :(
  [Server thread/INFO] [minecraft/GameTestServer]:    - blindthrowdeploysontheground
  ```
  and the Gradle task fails. The full log is `runs/gameTestServer/logs/latest.log`.
- In a dev client (`./gradlew runClient`, creative world with cheats): `/test runall` runs every test,
  `/test run <name>` one test (names are the lowercase method names), `/test runfailed` the failed ones. Tests are
  placed near the player.

### Writing a GameTest

- Use the vanilla `GameTestHelper` API (these tests are meant to run on Fabric too). The only NeoForge parts are the
  class annotations:
  ```java
  @GameTestHolder(CapsuleMod.MODID)
  @PrefixGameTestTemplate(false)
  public class MyTests {
      @GameTest(template = "empty")
      public static void somethingWorks(GameTestHelper helper) { ... helper.succeed(); }
  }
  ```
- Templates: `capsule:empty` (9×9×9 of air) and `capsule:empty17` (17×17×17). Build the scenario in code with
  `helper.setBlock`; positions are relative to the test origin.
- `CapsuleTestUtils` has the common steps: `capture`, `deploy`, `template`, `emptyCapsule`, `clear`, and
  `survivalPlayer`. Use `survivalPlayer` rather than `helper.makeMockServerPlayerInLevel()`: the mock player joins the
  player list without the mod network channels, so the first capsule payload broadcast near it throws.
- Tests changing global state go in their own batch (`batch = "reload"` for `/reload`), and must restore what they
  change (see `ConfigTests`).
- A bug fix comes with a test that fails before the fix; record the failure message in the commit and in
  `docs/ISSUE_TRIAGE.md`.

### Mods on the GameTest runtime

`gametestImplementation` dependencies are loaded by the dev runs only: SecurityCraft 1.21.1 (Modrinth maven, version
id in `gradle.properties`) for `SecurityCraftTests`. JEI is loaded in the dev runs as well (`localRuntime`).

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

## Not covered yet

Client rendering (previews, item models, colors) has no automated test; it is planned as a client smoke test under
Xvfb (see `docs/PLAN.md`, phase 6). Interactions with third-party mods other than SecurityCraft are manual.
