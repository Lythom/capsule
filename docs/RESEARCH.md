# Capsule: state of Minecraft mod tooling (researched 2026-10-05)

Legend: **[V]** = verified on that date (URL / maven metadata / git ls-remote / API query). **[I]** = my inference.

---

## 0. Context the caller should know first

- **Mojang changed version numbering** after 1.21.11. Releases are now `YY.drop.hotfix`: 26.1, 26.1.1, 26.1.2, 26.2 and 26.3 are stable, and 26.4-snapshot-2 is current. [V] https://meta.fabricmc.net/v2/versions/game, https://neoforged.net/news/26.1release/
- **26.1 and later ship unobfuscated, need Java 25, and use NeoForge 4-part versions** (for example `26.1.2.114`). With no obfuscation there is no intermediary or reobf step, so Parchment matters less. [V] neoforged.net/news/26.1release, MultiLoader-Template branch 26.3 (`java_version=25`)
- **1.21.11 renamed `ResourceLocation` to `Identifier`.** The Stonecutter template applies this as a string replacement. [V] codeberg.org/stonecutter/template-multiloader `stonecutter.gradle.kts`
- **Capsule's own numbers on Modrinth** (API query, all-time downloads by version):

  | Build | Downloads |
  |---|---|
  | 1.20.1 Forge | 369,091 |
  | 1.12.2 Forge | 3,501 |
  | 1.21.1 NeoForge | 3,233 |
  | 1.16.5 Forge | 907 |
  | 1.18.2 Forge | 239 |
  | 1.19.2 Forge | 179 |
  | 1.20.4 NeoForge | 106 |

  The latest file is `1.21.1-9.0.117` (2026-03-24). [V] https://api.modrinth.com/v2/project/capsule/version
- **The current repo is tied closely to NeoForge.** 26 of the 55 Java files import `net.neoforged.*`: bus/events, networking, registries, capabilities, fluids, config and client. It uses NeoGradle 7.0.165, NeoForge 21.1.77, JEI 19.21.0.247 and Parchment 2024.11.17. [V] local repo

---

## 1. Options for building several loaders from one codebase (1.21.1)

### 1a. Architectury (Loom + API)
- **Architectury API on 1.21.x supports Fabric and NeoForge only.** On branch `1.21` of github.com/architectury/architectury-api, `gradle.properties` has `platforms=fabric,neoforge`, and the `forge` subprojects are commented out in `settings.gradle`. [V]
  - The last `architectury-forge` artifact is 10.1.20 (2024-03, the 1.20.4 line). [V] maven.architectury.dev
  - API 13.0.11 for 1.21.1 (Fabric and NeoForge) was released 2026-07-23. The newest line is 22.0.3 for 26.3. [V] Modrinth API
- **Architectury Loom can still build Forge 1.21.1, NeoForge and Fabric.**
  - The latest stable is 1.17.493 (2026-09-12). [V] maven.architectury.dev
  - Its source has explicit handling for Forge 1.21.1+ and for unobfuscated Forge and NeoForge (`MinecraftPatchedProvider.java`, `ForgeProvider.usesMojangAtRuntime`). [V] github.com/architectury/architectury-loom
  - Loom docs say it needs Gradle 8.1+. [V] docs.architectury.dev/loom/introduction/
  - If you target Forge 1.21.1 you cannot use the Architectury API runtime there. You would write your own abstraction. [I]

### 1b. jaredlll08 MultiLoader-Template (`common`, `fabric`, `neoforge`, `forge` subprojects)
- **One branch per MC version**, from 1.17 to 1.21.11 and 26.1 to 26.3. The default branch is 26.3. [V] `git ls-remote`
- **Branch 1.21.1** (last commit 2026-09-30):
  - `common` uses ModDevGradle in vanilla mode (`neoForge { neoFormVersion = ... }`).
  - `neoforge` uses `net.neoforged.moddev`.
  - `forge` uses **ForgeGradle `[6.0.24,6.2)`** plus `org.spongepowered.mixin`, with `reobf = false`.
  - `fabric` uses `fabric-loom`.
  - Gradle 8.10. The pinned versions are old (MDG 2.0.49-beta, NeoForge 21.1.80, Forge 52.0.28). [V]
- **Branch 1.20.1** has `common`, `fabric` and `forge`, with **MDG `net.neoforged.moddev.legacyforge`** for Forge and common. It uses Gradle 8.11. [V]
- **Branch 26.3** has `common`, `fabric` and `neoforge` (**no Forge**), with MDG and `net.fabricmc.fabric-loom` on Gradle 9.7.1 and Java 25. [V]
- **How loader code is abstracted.** `common` compiles against vanilla only. Shared code calls `Services.PLATFORM`, which looks up an interface (`IPlatformHelper`) through `java.util.ServiceLoader`. Each loader ships its own implementation and registers it in `META-INF/services/<interface FQN>`. Mixins in `common` are allowed, because Fabric and NeoForge both bundle MixinExtras. [V] template sources
- **Limitation:** supporting a new MC version means a new branch, so code is copied between versions rather than kept in one tree. [I]

### 1c. ModDevGradle Legacy Forge (`net.neoforged.moddev.legacyforge`)
- It is released alongside MDG with the same version number. The latest is **2.0.148** (2026-09-29). [V] maven.neoforged.net
- **Scope: MinecraftForge and vanilla 1.17 to 1.20.1 only.** LEGACY.md states: "adds support for developing mods against MinecraftForge and Vanilla Minecraft versions 1.17 up to 1.20.1". [V] github.com/neoforged/ModDevGradle/blob/main/LEGACY.md
- **It cannot build Forge 1.21.1.** For Forge 1.20.6 and later you need ForgeGradle 6 or 7 (FG 7.0.40 is on the Gradle plugin portal) or Architectury Loom. [V]/[I]

### 1d. Multi-version tools
- **Stonecutter** (KikuGie) is a Gradle plugin and comment preprocessor.
  - Conditions look like `//? if >=1.21.11 {` and `//? if neoforge {`.
  - Version and loader "nodes" are separate Gradle subprojects that share one `src`. One node is "active" in the IDE at a time.
  - Latest **0.9.8** (2026-08-31). [V] maven.kikugie.dev
- **Official Stonecutter Fabric + NeoForge template**: https://codeberg.org/stonecutter/template-multiloader (last commit 2026-09-24, Gradle 9.8.0). [V]
  - Uses `match("1.21.1","fabric","neoforge")`, `match("1.21.11",…)`, `match("26.2",…)` and `match("26.3",…)`.
  - Each loader has its own build script: `build.neoforge.gradle.kts` uses MDG 2.0.147 and `build.fabric.gradle.kts` uses Loom.
  - Loom back-compat for pre-26.1 versions comes from `dev.kikugie.loom-back-compat` 0.4.2.
  - It sets Java per version (25 for ≥26.1, 21 for ≥1.20.5, 17 for ≥1.18, and so on).
  - Adding a Forge 1.20.1 node means adding `build.forge.gradle.kts` with MDG legacyforge. [I]
- **Stonecraft** (meza, `gg.meza.stonecraft`) combines Stonecutter with Architectury for Fabric, Forge and NeoForge. [V] github.com/meza/Stonecraft
- **Alternatives:** the ReplayMod preprocessor (`//#if MC>=11605`) and its Essential fork in essential-gradle-toolkit (github.com/EssentialGG/essential-gradle-toolkit), and the Manifold preprocessor. Both are older and less widely adopted for new projects in 2026. Stonecutter is now the de-facto choice. [I]

### 1e. Is Forge 1.21.1 relevant, and is Connector viable?
- **Modrinth modpacks on 1.21.1: NeoForge 2,111, Forge 25, Fabric 3,032.** On 1.21.1 mods: NeoForge 21,121, Forge 5,356, Fabric 19,386. [V] Modrinth search API, faceted counts, 2026-10-05
- **Forge 1.21.1 is still maintained** (52.1.16, 2026-07-21; "recommended" 52.1.0), but packs have largely abandoned it. Forge has also kept porting to 26.x (latest 26.3-66.0.9). [V] files.minecraftforge.net promotions_slim.json
- **Major 1.21.1 packs use NeoForge or Fabric:** ATM10 (NeoForge 1.21.1), Better MC 5 (NeoForge 1.21.1), the Cobblemon packs (Fabric 1.21.1). [V] search results / CurseForge pages
- **Recommendation:** do not ship a Forge 1.21.1 build. [I]
- **Sinytra Connector** is `2.0.0-beta.17+1.21.1` (2026-08-15) and `3.0.0-beta.6+26.1.2`, still beta. **Forgified Fabric API** is `0.116.15+2.3.5+1.21.1`. [V] Modrinth API
  - Connector runs Fabric mods on NeoForge. Capsule is already native to NeoForge, so Connector does not help here, and it should not be the official way to distribute a Fabric build. [I]

### 1f. Recommendation (one approach)
**Use the Stonecutter multiloader layout with one build script per loader.**
- `build.neoforge.gradle.kts` uses MDG `net.neoforged.moddev`, for 1.21.1 and 26.x.
- `build.forge.gradle.kts` uses MDG `net.neoforged.moddev.legacyforge`, for 1.20.1 (and 1.18.2 or 1.19.2 later if wanted).
- `build.fabric.gradle.kts` uses Loom, for 1.21.1 Fabric, optional.

All the Gradle plugins come from the same MDG family, so the DSL is the same everywhere. [I]

Why this instead of the alternatives [I]:
- One source tree covers every MC version and loader. MultiLoader-Template needs a branch per version.
- It avoids depending on the Architectury API runtime, which has no Forge on 1.21 anyway.
- It is what the community template now does for 1.21.1 to 26.3.

Suggested target matrix [I]:

| Priority | Target | Reason |
|---|---|---|
| 1 | 1.20.1 Forge | Where Capsule's downloads come from |
| 2 | 1.21.1 NeoForge | Current branch, ATM10-era packs |
| 3 | 26.x NeoForge | Once ATM11-class packs mature |
| Optional | 1.21.1 Fabric | Cobblemon packs |
| Skip | Forge 1.21.1 | Packs have abandoned it |
| Skip | 1.16.5 | Java 8; mappings, capability and networking APIs differ widely; small audience |

How to abstract the loader-specific code [I]:
1. Put the logic in loader-neutral classes that only touch vanilla.
2. For each concern, define a small platform interface in common: registration, networking (payload send/receive), config, capability/attachment access, events-to-callbacks, client hooks (keybinds, renderers, tooltips). Back it with a per-loader implementation, loaded through `ServiceLoader` as in MultiLoader-Template, or chosen with Stonecutter `//? if neoforge` blocks for small differences.
3. Prefer an event-to-callback bridge in each loader's entrypoint over mixins. Use mixins only where no loader hook exists. Mixins are portable (all three loaders bundle Mixin and MixinExtras), but they are fragile across MC versions.
4. Keep data-driven content (recipes, loot, tags) generated per version.

Alternative if Forge 1.21.1 ever becomes a hard requirement: Architectury Loom with Stonecutter (Stonecraft), and your own abstraction instead of Architectury API on Forge. [I]

---

## 2. Which MC versions modpacks target in 2026

Modrinth search API counts, 2026-10-05 [V]:

| MC | Modpacks | NeoForge packs | Forge packs | Fabric packs | Mods (all) |
|---|---:|---:|---:|---:|---:|
| 1.12.2 | 194 | 0 | 191 | 3 | 2,820 |
| 1.16.5 | 296 | 0 | 142 | 181 | 5,814 |
| 1.18.2 | 437 | 0 | 168 | 269 | 8,778 |
| 1.19.2 | 1,068 | 1 | 363 | 629 | 12,230 |
| **1.20.1** | **7,331** | 263 | **3,015** | **4,290** | **34,584** |
| 1.20.4 | 1,259 | 41 | 13 | 1,199 | 13,562 |
| **1.21.1** | **5,015** | **2,111** | 25 | 3,032 | **31,847** |
| 1.21.4 | 1,872 | 75 | 13 | 1,825 | 17,554 |
| 1.21.11 | 2,883 | 82 | 19 | 2,834 | 19,441 |
| 26.1.2 | 1,343 | 65 | 7 | 1,313 | 14,651 |
| 26.2 | 1,421 | 52 | 3 | 1,400 | 13,241 |
| 26.3 | 377 | 10 | 2 | 373 | 6,765 |

- **Top 300 Modrinth modpacks by downloads, grouped by highest supported version:** 1.20.1 87, 1.21.1 57, 26.2 37, 26.3 33, 1.21.11 24, 1.12.2 11, 1.19.2 10. [V]
- **The 26.x packs are mostly Fabric performance or vanilla+ packs** (Fabulously Optimized and similar). The large content packs sit on 1.20.1 (Forge) and 1.21.1 (NeoForge or Fabric). [V] list; [I] characterization
- **Modrinth under-represents CurseForge-heavy 1.12.2 and 1.16.5**, which are still alive on CurseForge. CurseForge's site API blocked scripted access. Modpack Index reports about 149.8k modpacks tracked (132.7k on CurseForge) as of 2026-10-01, but its per-version stats page returned 403. [V]/[I]
- **Flagship packs** [V] via search results and CurseForge pages:
  - ATM9: Forge 1.20.1.
  - ATM10: NeoForge 1.21.1, over 20M downloads.
  - ATM11: NeoForge 26.1.2, Java 25, 0.8.0-beta on 2026-09-07, still alpha/beta.
  - Better MC: BMC4 on Forge 1.20.1, BMC5 on NeoForge 1.21.1.
  - Cobblemon Official pack: Fabric 1.21.1. Cobblemon 1.8.1 (2026-09-12) is on 1.21.1 for Fabric and NeoForge.
  - Create: 6.0.10 is still on 1.21.1 NeoForge (2026-04-21), with no 26.x build yet.
- **Conclusion** [I]:
  - The two dominant targets are **1.20.1 Forge** and **1.21.1 NeoForge**.
  - 1.21.1 Fabric is a solid third.
  - 26.x NeoForge is emerging (ATM11 beta) but the content ecosystem (Create and others) has not moved yet.
  - For 1.20.1, NeoForge builds are a rounding error; use Forge (47.4.x; latest 47.4.26, recommended 47.4.10).

---

## 3. Testing without a human

### 3a. NeoForge GameTest on 1.21.1
- **Registering tests** [V] https://docs.neoforged.net/docs/1.21.1/misc/gametest/:
  - `@GameTestHolder(MODID)` on a class registers its `@GameTest` methods.
  - Alternatively, listen for `RegisterGameTestsEvent` on the mod bus, call `event.register(Cls.class)`, and set `@GameTest(templateNamespace = MODID)`.
  - `@PrefixGameTestTemplate(false)` drops the class-name prefix from template paths.
- **Enabling namespaces:** `neoforge.enabledGameTestNamespaces=modid1,modid2` (no spaces). [V]
- **Running headless:** `./gradlew runGameTestServer`. "The build server returns an exit code of the number of required, failed Game Tests", so CI fails on a non-zero exit. [V]
- **Gradle settings:** the docs mention `setForceExit false` for NeoGradle. With MDG, declare `runs { gameTestServer { type = "gameTestServer"; systemProperty 'neoforge.enabledGameTestNamespaces', 'capsule' } }`, which creates the task `runGameTestServer`. [V] MDG README
- **Capsule today** sets the namespace property only on the client and server runs. A gameTestServer run needs adding. [V] repo build.gradle
- **1.21.5 and later rewrote GameTest** as data-driven `test_instance` registry entries plus test environments, with a new `/test` command. NeoForge registers these via `DeferredRegister`. Expect a test-API rewrite when porting to 26.x. [V] fabricmc.net/2025/03/24/1215.html, gist.github.com/meza/b3c3dde10c6cc24cd53c9aa7f554f7e1

### 3b. Unit tests (JUnit) with NeoForge
- **MDG setup:** `neoForge { unitTest { enable(); testedMod = mods.capsule } }`, plus `testImplementation 'org.junit.jupiter:junit-jupiter'` and `testRuntimeOnly 'org.junit.platform:junit-platform-launcher'` and `test { useJUnitPlatform() }`. Tests run inside FML, so Minecraft classes and registries are usable. [V] MDG README
- **Tests with a running server:** add `testImplementation "net.neoforged:testframework:<neoforge version>"` (21.1.255 exists) and use `@ExtendWith(EphemeralTestServerProvider.class)`, which injects a `MinecraftServer` parameter. [V] MDG README, maven
- **Plain-Fabric equivalent:** `fabric-loader-junit`, calling `SharedConstants.tryDetectVersion(); Bootstrap.bootStrap();`. [V] https://docs.fabricmc.net/develop/automatic-testing

### 3c. Fabric gametest and client gametest
- **Setup:** `fabricApi { configureTests { createSourceSet = true; enableGameTests = true; enableClientGameTests = true; eula = true } }`. Tests live in `src/gametest`. Client tests run via `runProductionClientGameTest`, with Xvfb on Linux CI. [V] docs.fabricmc.net/develop/automatic-testing
- **Client gametest (`fabric-client-gametest-api-v1`) needs 1.21.4 or later.** It first shipped in Fabric API 0.112.1+1.21.4; the module's 1.0.0 is dated 2024-12-16. It is **not available on 1.21.1**: the 0.116.17+1.21.1 and 0.92.12+1.20.1 POMs do not contain it. It supports screenshots and input simulation. [V] maven.fabricmc.net POMs

### 3d. Running a real client headless with screenshots: HeadlessMC and mc-runtime-test
- **HeadlessMC** (github.com/headlesshq/headlessmc) is a terminal launcher for Fabric, Forge and NeoForge.
  - `launch neoforge 1.21.1`.
  - `--headless` / `-lwjgl` patches LWJGL so nothing is rendered: no GPU needed, but no meaningful screenshots.
  - `--offline` uses an offline account, which is allowed only for CI.
  - `hmc.xvfb.check` setting.
  - Latest tags: 2.10.0 and 3.0.0-RC1. Last commit 2026-10-04. [V] docs/launch.md, commands.md, configuration.md
- **hmc-specifics** are per-version Fabric/Forge/NeoForge mods covering 1.21–1.21.11 and 26.2. They add console commands: `gui`, `click`, `text`, `render` (dumps on-screen text), `key`, `connect`, `quit`. **`key f2` takes a vanilla screenshot**, which needs real rendering, so run under Xvfb with Mesa (llvmpipe) rather than `-lwjgl`. [V] docs/specifics.md, github.com/headlesshq/hmc-specifics; [I] for llvmpipe
- **mc-runtime-test** (`uses: headlesshq/mc-runtime-test@4.5.2`) is a GitHub Action built on HeadlessMC and Xvfb.
  - It launches the client with your jar in `run/mods`, joins a single-player world, waits for chunks, and exits. It also runs `/test runall` to execute registered GameTests.
  - Inputs: `mc: 1.21.1`, `modloader: neoforge`, `regex: .*neoforge.*`, `mc-runtime-test: neoforge`, `java: 21`, `xvfb`, `cache-mc`, `headlessmc-command` (for example `-DMcRuntimeGameTestMinExpectedGameTests=1`).
  - **NeoForge 1.21–1.21.11 is supported (✔️).** The README notes that Forge/NeoForge GameTest discovery "may require additional setup" for structure templates.
  - [V] https://github.com/headlesshq/mc-runtime-test README
- **Local use:** run the same steps with `xvfb-run -s "-screen 0 1920x1080x24" java -jar headlessmc-launcher.jar --command launch neoforge 1.21.1 --offline …` plus hmc-specifics. Then take screenshots with `key f2`, or grab the X display (`import -window root` / `xwd`). [I]
- **No NeoForge equivalent of Fabric's client gametest** turned up for 1.21.1. The NeoForge testframework is server/gametest-oriented. Not verified exhaustively. [I]
- **Practical stack for Capsule** [I]:
  1. JUnit through MDG `unitTest`.
  2. `runGameTestServer` in CI for gameplay logic: capsule capture and deploy in a structure.
  3. mc-runtime-test, or HeadlessMC with Xvfb and hmc-specifics, as a client smoke test (does the client boot with the mod and JEI, and join a world), plus optional screenshots. The Docker image `3arthqu4ke/headlessmc` exists.

---

## 4. Build tooling

| Item | Latest / recommended | Source |
|---|---|---|
| ModDevGradle (`net.neoforged.moddev`) | **2.0.148** (2026-09-29) | maven.neoforged.net [V] |
| MDG legacyforge | 2.0.148 (same release train) | maven.neoforged.net [V] |
| NeoGradle userdev | 7.1.39 (2026-09-15). The 7.0.x line ended at 7.0.192 (2025-07-28). Capsule is on 7.0.165. | maven.neoforged.net [V] |
| NeoForge for 1.21.1 | **21.1.255** (2026-10-04). The MDK pins 21.1.252. | maven [V] |
| NeoForge testframework | 21.1.255 | maven [V] |
| Gradle | MDK-1.21.1-ModDevGradle wrapper: **9.2.1**. MDG README says compatible with 8.8+. Gradle current is 9.8.0 (2026-09-24). | NeoForgeMDKs repo, services.gradle.org [V] |
| Parchment 1.21.1 | **2024.11.17** (final; matches the MDK and Capsule) | maven.parchmentmc.org [V] |
| JEI 1.21.1 | **19.57.0.451** (2026-10-05) for `jei-1.21.1-neoforge`, `-forge`, `-fabric`, `-common-api`, `-neoforge-api` | maven.blamejared.com [V] |
| JEI 1.20.1 | 15.62.0.219 (forge, fabric, common-api) | maven.blamejared.com [V] |
| Forge 1.21.1 / 1.20.1 | 52.1.16 (recommended 52.1.0) / 47.4.26 (recommended 47.4.10) | promotions_slim.json [V] |
| ForgeGradle | 6.0.54; FG7 7.0.40 on the plugin portal | maven [V] |
| Fabric Loom / Loader / API 1.21.1 | Loom 1.18.2; Loader 0.19.5; Fabric API 0.116.17+1.21.1 | maven.fabricmc.net [V] |
| Architectury Loom / API (1.21.1) | 1.17.493 / 13.0.11 | maven.architectury.dev [V] |
| Stonecutter | 0.9.8 | maven.kikugie.dev [V] |

- **Recommendation: move from NeoGradle 7.0.165 to ModDevGradle 2.0.148.**
  - The official MDKs, MultiLoader-Template, the Stonecutter template, and MDG legacyforge (needed for Forge 1.20.1) all use MDG.
  - MDG has first-class `unitTest`, `gameTestServer` runs and Parchment.
  - MDG's own README describes NeoGradle's niche as multiple NeoForge/MC versions in one project, which Stonecutter now covers. [V] MDG README; [I] for the recommendation
- **Mapping Capsule's current settings to MDG:**
  - `minecraft.accessTransformers.file` becomes the default `META-INF/accesstransformer.cfg`, which MDG picks up automatically.
  - The `neogradle.subsystems.parchment.*` properties become `neoForge { parchment { minecraftVersion = '1.21.1'; mappingsVersion = '2024.11.17' } }`.
  - `runs {}` moves under `neoForge {}`.
