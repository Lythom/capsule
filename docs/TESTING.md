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

## Not covered yet

Client rendering (previews, item models, colors) has no automated test; it is planned as a client smoke test under
Xvfb (see `docs/PLAN.md`, phase 6). Interactions with third-party mods other than SecurityCraft are manual.
