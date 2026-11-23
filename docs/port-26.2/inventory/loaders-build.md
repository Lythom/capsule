# Capsule 1.21.1 -> 26.2: loader APIs and build tooling inventory

Scope: NeoForge (21.1 -> 26.2) and Fabric (Loader, API, Loom) API changes that hit Capsule's loader glue, the
build, CI, publishing and the scripts. Vanilla API changes (rendering, item models JSON, recipes, gametest helpers,
`ResourceLocation`→`Identifier` everywhere, etc.) belong to the other agents. Where loader code and vanilla code meet,
this file names the contract and leaves the vanilla body to them.

How this was checked (late Nov 2026): maven metadata of NeoForged, Fabric, Parchment, BlameJared, Shedaniel, Terraformers,
Nucleoid and Modrinth; the **NeoForge 26.2.0.95 sources, userdev patches, moddev-config and installer**; **FancyModLoader
11.0.16 sources**; the **sources of every Fabric API 0.161.0+26.2 module**; `javap` on the **vanilla 26.2 client jar**
(unobfuscated); the **Forge Config API Port 26.2.1**, **Common Protection API 2.0.0**, **GOML 1.21.0+26.2** and
**REI 26.2.820** jars; the NeoForge MDK and Fabric example mod for 26.2; the MultiLoader-Template 26.2 branch. Anything I
could not check that way is marked **UNVERIFIED**.

Line numbers are those of the files at commit `5cebb7c`.

---

## 1. Target versions

| Property / plugin | Now | Target | Source |
|---|---|---|---|
| Minecraft | 1.21.1 | **26.2** (only 26.2 release, 2026-06-16. 26.3 is also out, but it is not the target) | https://piston-meta.mojang.com/mc/game/version_manifest_v2.json |
| Java (toolchain + runtime) | 21 | **25** (`javaVersion.majorVersion: 25` in the 26.2 version json, `"java": ">=25"` in Fabric API) | 26.2 version json; https://neoforged.net/news/26.1release/ |
| `neo_version` | 21.1.255 | **26.2.0.95** (latest 26.2, 2026-11-17, stable since 26.2.0.57) | https://maven.neoforged.net/releases/net/neoforged/neoforge/maven-metadata.xml |
| `neo_form_version` | 1.21.1-20240808.144430 | **26.2-2** (what NeoForge 26.2.0.95 is built on: `"mcp": "net.neoforged:neoform:26.2-2@zip"`. New scheme `<mc>-<build>`) | https://maven.neoforged.net/releases/net/neoforged/neoform/maven-metadata.xml, neoforge-26.2.0.95-moddev-config.json |
| ModDevGradle `net.neoforged.moddev` | 2.0.148 | **2.0.148** (still the latest, 2026-09-29; no change) | https://maven.neoforged.net/releases/net/neoforged/moddev/net.neoforged.moddev.gradle.plugin/maven-metadata.xml |
| Fabric Loom plugin | `net.fabricmc.fabric-loom-remap` 1.18.2 | **`net.fabricmc.fabric-loom` 1.19.4** (non-remapping plugin, required for unobfuscated MC; 1.19.4 published 2026-11-12, needs Gradle ≥ 9.8.0 (`org.gradle.plugin.api-version: 9.8.0`) and JVM 25) | https://maven.fabricmc.net/net/fabricmc/fabric-loom/maven-metadata.xml, https://docs.fabricmc.net/26.1.2/develop/porting/ |
| `fabric_loader_version` | 0.19.5 | **0.20.1** (latest stable, also what the 26.2 example mod uses) | https://meta.fabricmc.net/v2/versions/loader, https://raw.githubusercontent.com/FabricMC/fabric-example-mod/26.2/gradle.properties |
| `fabric_api_version` | 0.116.17+1.21.1 | **0.161.0+26.2** (latest for 26.2, 2026-09-18; needs loader ≥0.18.4, java ≥25) | https://maven.fabricmc.net/net/fabricmc/fabric-api/fabric-api/maven-metadata.xml |
| Gradle wrapper | 9.7.1 | **9.8.2** (the version in Fabric's 26.2 example mod; the minimum for Loom 1.19.x. The latest Gradle, 9.9.2 from 2026-11-19, is too new to use). Fallback if MDG misbehaves on 9.8: keep 9.7.1 and use Loom **1.18.4**, which declares no Gradle minimum and is newer than the Loom 1.17 Fabric recommends for 26.2. **UNVERIFIED**: MDG 2.0.148 on Gradle 9.8.2 | https://services.gradle.org/versions/all, fabric-example-mod 26.2 `gradle-wrapper.properties` |
| Gradle daemon JVM (`gradle/gradle-daemon-jvm.properties`) | 25 | 25 (no change) | |
| foojay-resolver-convention | 1.0.0 | 1.0.0 (latest) | plugin portal |
| Parchment | 1.21.1 / 2024.11.17 | **remove**: no `parchment-26.*` artifacts exist (the newest is parchment-1.21.11). The 26.x game ships Mojang's parameter names | https://maven.parchmentmc.org/org/parchmentmc/data/, https://neoforged.net/news/26.1release/ |
| `forge_config_api_port_version` | v21.1.6-1.21.1-Fabric | **rSd3GiG8** (Modrinth version id of FCAP 26.2.1 Fabric, 2026-06-21. Use the id because the number `26.2.1` is shared by the fabric/neoforge/forge files). It bundles night-config 3.8.4 | Modrinth API `project/forge-config-api-port/version?game_versions=["26.2"]` |
| `common_protection_api_version` | 1.0.0 | **2.0.0** (Mojang names, `Identifier`, `NameAndId`; also what GOML 26.2 nests) | https://maven.nucleoid.xyz/eu/pb4/common-protection-api/maven-metadata.xml |
| `night_config_version` | 3.8.0 | **3.8.4** (what FCAP 26.2.1 nests; NeoForge 26.2 ships 3.9.0, and that is backward compatible for compiling `common`) | FCAP jar `META-INF/jars`, NeoForge moddev-config |
| `jei_version` | 19.57.0.451 | **30.48.0.244** (`mezz.jei:jei-26.2-common-api` / `-neoforge` / `-fabric`; Modrinth says beta) | https://maven.blamejared.com/mezz/jei/jei-26.2-common-api/maven-metadata.xml |
| JEI's config lib (Fabric dev run) | `net.mezzdev.config:mezz_config-1.21.1-fabric:0.5.12` | **`net.mezzdev.config:mezz_config-26.2-fabric:0.6.6`** (the version JEI 30.48.0.244's pom asks for; the pom also lists `mezz_config_gui-26.2-fabric:mc26.2-0.5.10`) | JEI pom |
| `rei_version` | 16.0.799 | **26.2.820** (all REI artifacts used: `RoughlyEnoughItems-api-neoforge`, `-api-fabric`, `-default-plugin-neoforge`, `-neoforge`, `-fabric`) | https://maven.shedaniel.me/me/shedaniel/ |
| `emi_version` | 1.1.24+1.21.1 | **none: EMI has no 26.x build** (on Modrinth or on maven.terraformersmc.com, the newest is +1.21.1) → drop EMI, see §2.9 | Modrinth, Terraformers maven |
| `securitycraft_version` | X9nq0PvS | **kCkFPvNl** (v1.10.2.1 for 26.2, NeoForge) | Modrinth |
| `opac_neoforge_version` / `opac_fabric_version` | qMR7tHTs / YnCCrnDV | **5nVoZqbg** / **Z5TkbNzv** (0.33.0 for 26.2, beta) | Modrinth |
| `flan_neoforge_version` / `flan_fabric_version` | Z6X7Mj4L / 1SJYtmxm | **H9NbCnik** / **qJDOZxe5** (1.12.8 for 26.2) | Modrinth |
| `goml_version` | N4XW15zD | **ttqMQa3Y** (1.21.0+26.2; `ClaimUtils` API unchanged, still uses rtree-3i-lite-fabric 0.3.0) | Modrinth, jar |
| `rtree_3i_version` | 0.3.0 | 0.3.0 | |
| `worldedit_version` | WTAFvuRx | **6YnCYPwc** (7.4.5 for 26.2, NeoForge) | Modrinth |
| `waystones_version` / `balm_version` | 6Z6MQ6os / CquiaiDj | **W4wbeQe7** / **5wreNQrF** | Modrinth |
| `sophisticated_storage_version` / `sophisticated_core_version` | Hhf1IKFI / nSoNwJfm | **8NBkbIrX** / **vfOZTdtV** | Modrinth |
| `tombstone_version` | e68NhQJm | **I34Nt5pp** (tombstone-neoforge-26.2-9.9.4) | Modrinth |
| `refined_storage_version`, `mekanism_version`, `immersive_engineering_version`, `sfm_version` | … | **none for 26.2** (Refined Storage and SFM stop at 26.1.2, Mekanism and IE at 1.21.1) → drop from the incompat set, see §2.4 | Modrinth |
| `junit_version` | 5.11.4 | 5.11.4 (keep) | |

Notes
- No "NeoForge 26.2" post exists on https://neoforged.net/news/ (checked pages 1–3 and the RSS feed). The NeoForge-specific
  breaking changes for 26.2 come from the 26.2.0.95 changelog
  (https://maven.neoforged.net/releases/net/neoforged/neoforge/26.2.0.95/neoforge-26.2.0.95-changelog.txt) and from the
  sources. The posts that matter on the way from 21.1: 21.2, 21.4, 21.5, 21.6, 21.9, 21.9-transfer-rework, 21.11, 26.1
  (`https://neoforged.net/news/<n>release/`).
- Fabric posts read: https://fabricmc.net/2024/10/14/1212.html, …/2024/12/02/1214.html, …/2025/03/24/1215.html,
  …/2025/06/15/1216.html, …/2025/09/23/1219.html, …/2025/10/31/obfuscation.html, …/2025/12/05/12111.html,
  …/2026/03/14/261.html, …/2026/06/15/262.html. API rename list: https://docs.fabricmc.net/26.1.2/develop/porting/fabric-api.
  Loom changes: https://docs.fabricmc.net/26.1.2/develop/porting/.

---

## 2. Build changes, file by file

### 2.1 `gradle.properties`
Replace the values as in §1. Then:
- `minecraft_version=26.2`
- `neo_version=26.2.0.95`, `neo_form_version=26.2-2`
- **delete** `parchment_minecraft_version` and `parchment_mappings_version` (and their comment)
- `fabric_loader_version=0.20.1`, `fabric_api_version=0.161.0+26.2`
- `forge_config_api_port_version=rSd3GiG8` (comment: `# Modrinth version id of Forge Config API Port 26.2.1 for Fabric 26.2, provides NeoForge's ModConfigSpec`)
- `common_protection_api_version=2.0.0`, `night_config_version=3.8.4`
- `jei_version=30.48.0.244`, `rei_version=26.2.820`; **delete `emi_version`** and change the recipe viewer comment to `jei|rei`
- Mod ids: `securitycraft_version=kCkFPvNl`, `waystones_version=W4wbeQe7`, `balm_version=5wreNQrF`,
  `sophisticated_storage_version=8NBkbIrX`, `sophisticated_core_version=vfOZTdtV`, `worldedit_version=6YnCYPwc`,
  `opac_neoforge_version=5nVoZqbg`, `opac_fabric_version=Z5TkbNzv`, `flan_neoforge_version=H9NbCnik`,
  `flan_fabric_version=qJDOZxe5`, `goml_version=ttqMQa3Y`, `tombstone_version=I34Nt5pp`
- **delete** `refined_storage_version`, `mekanism_version`, `immersive_engineering_version`, `sfm_version` (no 26.2 builds), and update the comment above them
- Update the version comments ("for NeoForge 1.21.1", "7.3.8 for 1.21.1", …) to 26.2.

### 2.2 `gradle/wrapper/gradle-wrapper.properties`
`distributionUrl=https\://services.gradle.org/distributions/gradle-9.8.2-bin.zip`. Running
`./gradlew wrapper --gradle-version 9.8.2` updates `gradlew`/`gradlew.bat`/the wrapper jar as well, which is better.
Leave `gradle/gradle-daemon-jvm.properties` alone (it is already Java 25).

### 2.3 Root `build.gradle`
```groovy
plugins {
    // https://projects.neoforged.net/neoforged/moddevgradle
    id 'net.neoforged.moddev' version '2.0.148' apply false
    // https://maven.fabricmc.net/net/fabricmc/fabric-loom/ : the non-remapping Loom of unobfuscated Minecraft (26.1+)
    id 'net.fabricmc.fabric-loom' version '1.19.4' apply false
}
```
`settings.gradle` needs no change: `includeGroupAndSubgroups 'net.fabricmc'` already covers the
`net.fabricmc.fabric-loom` plugin marker. The NeoForged maven stays.

### 2.4 `buildSrc/src/main/groovy/multiloader-common.gradle`
- Line 9: `java.toolchain.languageVersion = JavaLanguageVersion.of(25) // Minecraft 26.1+ requires Java 25`
- Repositories: in `mavenCentral { content { … } }`, `excludeGroupByRegex 'remapped\\..*'` can go (non-remap Loom does
  not create `remapped.*` coordinates). It does no harm if left.
- If EMI is dropped, delete the `TerraformersMC (EMI)` `exclusiveContent` block.

### 2.5 `buildSrc/src/main/groovy/multiloader-loader.gradle`
No change is needed. The `modrinth()` helper already queries `game_versions=[minecraft_version]`.

### 2.6 `common/build.gradle`
- Line 8 stays as it is: `neoFormVersion = neo_form_version` (MDG vanilla mode; the MultiLoader-Template 26.2 branch does the same with `26.2-1`).
- **Delete lines 10-13** (the `parchment { … }` block).
- Line 19: `compileOnly "mezz.jei:jei-${minecraft_version}-common-api:${jei_version}"` stays as it is (it resolves to `jei-26.2-common-api`).
- Lines 21-22: keep the two REI `-neoforge` compileOnly lines (they exist at 26.2.820). The line 20 comment about intermediary
  names is obsolete: there are no intermediary names in 26.x. **Delete line 23** (`emi-xplat-mojmap`).
- Lines 26-28 (OPAC, Flan, SecurityCraft NeoForge jars): keep, with the new ids.
- `compileOnly "com.electronwill.night-config:core:${night_config_version}"` stays.
- `common/accesstransformer.cfg` (AT format unchanged):
  - delete `public net.minecraft.world.level.block.entity.BlockEntityType$BlockEntitySupplier`. It is public in vanilla 26.2 (javap), and so is the `BlockEntityType(BlockEntitySupplier, Set)` constructor.
  - `RenderType.create`: the 26.2 signature is
    `public net.minecraft.client.renderer.rendertype.RenderType create(Ljava/lang/String;Lnet/minecraft/client/renderer/rendertype/RenderSetup;)Lnet/minecraft/client/renderer/rendertype/RenderType;`.
    NeoForge's own AT makes it public, and so does Fabric API's transitive class tweaker. Keep an AT line with this
    exact signature only if the ported `common` code still calls `RenderType.create` (rendering agent's call). Delete the
    `RenderType$CompositeState` line (that class no longer exists in that form; RenderSetup replaces it).

### 2.7 `neoforge/build.gradle`
- **Delete lines 9-12** (`parchment { … }`).
- Lines 28, 33: `systemProperty 'neoforge.enabledGameTestNamespaces', 'capsule'`. Nothing reads this property in NeoForge
  26.2.0.95 or FML 11.0.16 (grep over the sources and the userdev patches). Delete both lines. Since 1.21.5 only
  registered tests exist, so no namespace filter is needed. (The 26.2 MDK still sets it; it is stale there too.)
- Line 50 (`gameTestServer { type = "gameTestServer" … }`): keep. Its moddev-config run sets `neoforge.enableGameTest=true`
  and the main class `net.neoforged.fml.startup.GameTestServer`. Delete line 52 (the namespaces property).
- Line 87: delete `compileOnly("dev.emi:emi-neoforge…")`. Line 91: remove the `emi :` entry from the `localRuntime([...])` map (lines 88-92).
- Lines 84-86: JEI/REI coordinates are unchanged apart from the versions (`jei-26.2-neoforge`, `RoughlyEnoughItems-neoforge:26.2.820`).
- Lines 115-125 (`-Pincompat`): remove `refined-storage`, `mekanism`, `immersiveengineering`, `super-factory-manager`
  from both the `gametestCompileOnly` lines (lines 116-118) and the `gametestRuntimeOnly` block (lines 120-123); keep `corail-tombstone`.
  Delete the matching test classes in `neoforge/src/gametest/java/capsule/gametest/`: `RefinedStorageTests.java`,
  `MekanismTests.java`, `ImmersiveEngineeringTests.java`, `SuperFactoryManagerTests.java`, and their entries in
  `NeoForgeGameTests` (lines 33-36).
- `runGameTestServer` doLast check (`'required tests passed'`): keep. Vanilla 26.2 `GameTestServer` still logs
  `All {} required tests passed :)` and `{} required tests failed :(` (strings in the 26.2 class file).
- `testModJar` (MDG, plain `Jar`): unchanged.
- `unitTest { enable(); testedMod = mods.capsule }`: unchanged.

### 2.8 `fabric/build.gradle` (largest change: remapping → non-remapping Loom)
1. Line 3: `id 'net.fabricmc.fabric-loom'` (instead of `net.fabricmc.fabric-loom-remap`).
2. Lines 7-13: **delete the ParchmentMC repository**.
3. Line 31: **delete** `loom.createRemapConfigurations(sourceSets.gametest)` (non-remap Loom has no `mod*` configurations).
4. Lines 35-38: delete the whole `mappings loom.layered { … }` statement. Keep `minecraft "com.mojang:minecraft:${minecraft_version}"`.
5. Rename every configuration: `modImplementation` → `implementation`, `modCompileOnly` → `compileOnly`,
   `modLocalRuntime` → `localRuntime`, `modGametestRuntimeOnly` → `gametestRuntimeOnly`. Concretely:
   ```groovy
   implementation "net.fabricmc:fabric-loader:${fabric_loader_version}"
   implementation "net.fabricmc.fabric-api:fabric-api:${fabric_api_version}"
   implementation "maven.modrinth:forge-config-api-port:${forge_config_api_port_version}"
   implementation include("eu.pb4:common-protection-api:${common_protection_api_version}")
   // bundled in Forge Config API Port (jar-in-jar)
   implementation "com.electronwill.night-config:core:${night_config_version}"
   runtimeOnly "com.electronwill.night-config:toml:${night_config_version}"
   compileOnly 'com.google.code.findbugs:jsr305:3.0.2'

   compileOnly "mezz.jei:jei-${minecraft_version}-common-api:${jei_version}"
   compileOnly("me.shedaniel:RoughlyEnoughItems-api-fabric:${rei_version}") { transitive = false }
   compileOnly("me.shedaniel:RoughlyEnoughItems-default-plugin-fabric:${rei_version}") { transitive = false }
   localRuntime([
           jei : ["mezz.jei:jei-${minecraft_version}-fabric:${jei_version}", "net.mezzdev.config:mezz_config-${minecraft_version}-fabric:0.6.6"],
           rei : ["me.shedaniel:RoughlyEnoughItems-fabric:${rei_version}"],
   ][findProperty('recipeViewer') ?: 'jei'])
   testImplementation "net.fabricmc:fabric-loader-junit:${fabric_loader_version}"
   compileOnly("maven.modrinth:open-parties-and-claims:${opac_fabric_version}") { transitive = false }
   compileOnly("maven.modrinth:flan:${flan_fabric_version}") { transitive = false }
   compileOnly("maven.modrinth:goml-reserved:${goml_version}") { transitive = false }
   compileOnly "com.jamieswhiteshirt:rtree-3i-lite-fabric:${rtree_3i_version}"
   compileOnly("maven.modrinth:security-craft:${securitycraft_version}") { transitive = false }
   if (project.hasProperty('modCompat')) {
       gametestRuntimeOnly modrinth('open-parties-and-claims', opac_fabric_version)
       gametestRuntimeOnly modrinth('flan', flan_fabric_version)
   }
   ```
   (Delete the line for `emi-fabric:…:api` and the `emi :` entry.) The Loom docs say `modImplementation` "has any nested
   jars removed". With plain `implementation`, the FCAP jar keeps its nested night-config. The explicit night-config lines
   are kept and pinned to the same 3.8.4 so that a duplicate is identical. **UNVERIFIED** whether they are still needed;
   if the dev run starts without them, they can go. JEI's pom already pulls `mezz_config` transitively, so the explicit
   `mezz_config` entry can probably go too (**UNVERIFIED**).
6. Line 82: `accessWidenerPath = file('src/main/resources/capsule.accesswidener')` stays (the name is kept for class tweakers too).
7. Lines 145-156 (`gametestJar` + `testModJar` `RemapJarTask`): replace them with a single plain jar, because nothing is remapped any more:
   ```groovy
   // The GameTests as a mod jar, for real servers and clients next to the release jar (scripts/prod-gametest.sh)
   tasks.register('testModJar', Jar) {
       from sourceSets.gametest.output
       archiveClassifier = 'gametest'
       destinationDirectory = layout.buildDirectory.dir('test-mod')
   }
   ```
   Release jar: in non-remap Loom, `jar` (not `remapJar`) produces `build/libs/Capsule-fabric-<version>.jar`, and
   `include` jars are nested into `jar`. CI's `find … ! -name "*-sources.jar"` still finds it (no `-dev` jar any more).
8. Runs (lines 92-126): unchanged (`-Dfabric-api.gametest` and `-Dfabric-api.gametest.report-file` are still the
   property names, see `GameTestSystemProperties` in fabric-gametest-api-v1 4.0.22).

### 2.9 Recipe viewers: EMI
EMI has no 26.x build. Options: (a) drop the integration: delete `common/src/main/java/capsule/plugins/emi/`, the
`"emi"` entrypoint in `fabric/src/main/resources/fabric.mod.json` (lines 31-33), the EMI deps (§2.6-2.8), the `emi`
recipe viewer choice in `scripts/client-smoke.sh` (lines 19, 41-42) and `scripts/validate-all.sh` line 146
(`for viewer in jei rei emi` → `jei rei`), and any EMI checks in the client smoke test (`common/src/gametest/.../clientsmoke`);
or (b) keep the sources outside compilation until EMI ports. (a) is recommended. **Decision for the user.**

### 2.10 `neoforge/src/main/resources/META-INF/neoforge.mods.toml`
- Lines 1-2: `modLoader="javafml"` / `loaderVersion="[4,)"` still work (FML 11 defaults to javafml, and the
  language-provider version is the FML version ≥ 4). The 26.2 MDK leaves both lines out. Optional: delete both; if
  only one is deleted, delete `loaderVersion` (FML refuses `loaderVersion` without `modLoader`).
- Line 22: `versionRange="[26.2,)"` for `neoforge`.
- Line 29: `versionRange="[26.2,26.3)"` for `minecraft`.
- `neoforge/src/gametest/resources/META-INF/neoforge.mods.toml`: same loader lines. If the production GameTest mixin
  from §3.10 is used, add:
  ```toml
  [[mixins]]
  config="capsule_gametest.mixins.json"
  ```

### 2.11 `fabric/src/main/resources/fabric.mod.json` (lines 39-45)
```json
"depends": {
  "fabricloader": ">=0.19.0",
  "minecraft": "~26.2",
  "java": ">=25",
  "fabric-api": ">=0.161.0",
  "forgeconfigapiport": ">=26.2.0"
}
```
(FCAP 26.2.1 itself requires loader ≥0.19.0 and fabric-api ≥0.152.0.) Delete the `"emi"` entrypoint if EMI is dropped.
`fabric/src/gametest/resources/fabric.mod.json`: the `fabric-gametest` entrypoint changes (§4.9). Remove the
`jei_mod_plugin` entry only if `JeiSmokePlugin` is dropped (it is not affected otherwise).

### 2.12 `fabric/src/main/resources/capsule.accesswidener`
- Line 1: `accessWidener v2 official` (was `named`; Fabric docs: "update the header of your access widener or class
  tweaker to replace `named` with `official`". The `classTweaker v1 official` header is the new equivalent.)
- Delete line 4 (`RenderType create …CompositeState…`) and line 5 (`RenderType$CompositeState`). Fabric API's
  transitive class tweaker already opens `net/minecraft/client/renderer/rendertype/RenderType create (Ljava/lang/String;Lnet/minecraft/client/renderer/rendertype/RenderSetup;)…`.
- Delete line 6 (`BlockEntityType$BlockEntitySupplier`), which is public in 26.2.
- Delete line 7 (`ItemProperties PROPERTIES`). `ItemProperties` no longer exists (1.21.4 item models).
- The file ends up holding only the header plus comments. Leave it in place: the rendering agent may need entries. Or
  delete it together with `accessWidenerPath` and the `"accessWidener"` key.

### 2.13 `fabric/src/main/resources/capsule.mixins.json`
- `"compatibilityLevel": "JAVA_25"` (Mixin 0.17.4/0.17.5 bundled by both loaders has `JAVA_25`).
- Remove `"ClientPacketListenerMixin"` from `"client"` (see §4.8).

---

## 3. NeoForge API changes affecting Capsule (21.1 → 26.2.0.95)

Global renames that touch every loader file: `ResourceLocation` → `net.minecraft.resources.Identifier` (1.21.11;
`ResourceLocation.fromNamespaceAndPath` → `Identifier.fromNamespaceAndPath`); `ResourceKey.location()` →
`ResourceKey.identifier()`; `javax.annotation.Nullable` → `org.jspecify.annotations.Nullable`. JSpecify is type-use, so
`Map.@Nullable Entry` is the spelling inside qualified types (NeoForge 21.11 post). `EventBusSubscriber.Bus` is gone:
mod-bus events are recognised by `IModBusEvent` (FML 11 `EventBusSubscriber` only has `value()` (dists) and `modid()`).

### 3.1 `neoforge/src/main/java/capsule/neoforge/CapsuleNeoForge.java`
| Line | Now | 26.2 |
|---|---|---|
| 24, 57 | `AddReloadListenerEvent` / `event.addListener(CapsuleMod.reloadListener())` | `net.neoforged.neoforge.event.AddServerReloadListenersEvent` (game bus) → `bus.addListener((AddServerReloadListenersEvent event) -> event.addListener(Identifier.fromNamespaceAndPath(CapsuleMod.MODID, "templates"), CapsuleMod.reloadListener()));` (`SortedReloadListenerEvent#addListener(Identifier, PreparableReloadListener)`) |
| 25, 64-67 | `LootTableLoadEvent` `event.getName()`, `event.getTable().addPool(pool.name("capsulePool").build())` | Unchanged API (`getName()` returns `Identifier` now; `LootPool.Builder#name` and `LootTable#addPool` are still NeoForge patches). `CapsuleLootTableHook.capsulePool` must take `Identifier`. |
| 26, 56 | `RegisterCommandsEvent#getDispatcher()` | unchanged |
| 27-28, 58-62 | `EntityJoinLevelEvent` (cancellable), `EntityLeaveLevelEvent` | unchanged |
| 29, 68-70 | `PlayerEvent.PlayerLoggedInEvent` | unchanged |
| 30, 75-77 | `PlayerInteractEvent.LeftClickBlock` (`isCanceled`/`setCanceled`) | unchanged |
| 31, 63 | `LevelEvent.Unload` | unchanged (since 26.1.2.46 it also fires from `Minecraft#clearClientLevel`; `ThrownCapsules.onLevelUnload` must cope with client levels, or check `event.getLevel().isClientSide()`) |
| 32-35, 54-55, 71-74 | `ServerStartingEvent`, `ServerStoppedEvent`, `PlayerTickEvent.Post`, `LevelTickEvent.Post` | unchanged |
| 36, 50-51 | `RegisterPayloadHandlersEvent#registrar(String).versioned(...)` | unchanged (handlers now run on the main thread by default: `HandlerThread.MAIN`) |
| 41-46 | `@Mod` ctor `(IEventBus, ModContainer, Dist)`, `modContainer.registerConfig(ModConfig.Type.COMMON, spec)`, `ModConfigEvent` | unchanged |
| new | (recipes reach the client) | Since 1.21.2 the server no longer sends recipes. Add `bus.addListener((OnDatapackSyncEvent event) -> event.sendRecipes(RecipeType.CRAFTING));` (`net.neoforged.neoforge.event.OnDatapackSyncEvent`, game bus) so that the client's `RecipesReceivedEvent` (§3.2) carries the capsule crafting recipes. |

`Dist#isClient()` (line 79): unchanged. Mod constructors now run **before** `Minecraft` exists (21.5), so they must not
call `Minecraft.getInstance()`. Neither entrypoint does today.

### 3.2 `neoforge/src/main/java/capsule/neoforge/CapsuleNeoForgeClient.java`
| Line | Now | 26.2 replacement |
|---|---|---|
| 12, 40-44 | `FMLClientSetupEvent` + `ItemProperties.register(CAPSULE, capsule:state, …)` | `ItemProperties` was removed in 1.21.4. Register a range-select property codec on the mod bus: `modEventBus.addListener((RegisterRangeSelectItemModelPropertyEvent event) -> event.register(Identifier.fromNamespaceAndPath(CapsuleMod.MODID, "state"), CapsuleStateProperty.MAP_CODEC));` (`net.neoforged.neoforge.client.event.RegisterRangeSelectItemModelPropertyEvent#register(Identifier, MapCodec<? extends RangeSelectItemModelProperty>)`). `CapsuleStateProperty` is a common class implementing vanilla `net.minecraft.client.renderer.item.properties.numeric.RangeSelectItemModelProperty` (`float get(ItemStack, ClientLevel, ItemOwner, int)` returning `CapsuleItem.getState(stack).getValue()`, `MapCodec<…> type()`). The model selection moves to `assets/capsule/items/capsule.json` (`minecraft:range_dispatch` with `"property": "capsule:state"`). This is vanilla/asset work for the rendering agent. |
| 22, 45-46 | `RegisterColorHandlersEvent.Item` + `event.register(CapsuleItem::getColorFromItemstack, CAPSULE)` | Gone. Use `RegisterColorHandlersEvent.ItemTintSources#register(Identifier, MapCodec<? extends ItemTintSource>)`: `modEventBus.addListener((RegisterColorHandlersEvent.ItemTintSources event) -> event.register(Identifier.fromNamespaceAndPath(CapsuleMod.MODID, "capsule_color"), CapsuleTint.MAP_CODEC));`. `CapsuleTint` is a common class implementing vanilla `ItemTintSource` (`int calculate(ItemStack, ClientLevel, LivingEntity)` and `type()`), referenced from the item model JSON `tints` list. One tint source per tint index (`getColorFromItemstack(stack, renderPass)` → one `CapsuleTint(int pass)` record with a codec field). Colours must be ARGB with full alpha. |
| 20, 47-48 | `EntityRenderersEvent.RegisterRenderers#registerBlockEntityRenderer(MARKER_TE.get(), CaptureBER::new)` | Same call; the provider is now `BlockEntityRendererProvider<T, S extends BlockEntityRenderState>` and `CaptureBER` must become a render-state BER (`createRenderState`/`extractRenderState`/`submit`, 21.9 post). `IBlockEntityRendererExtension#getRenderBoundingBox(T)` still exists, so `CaptureBER#getRenderBoundingBox` keeps overriding it. |
| 21, 51 | `RecipesUpdatedEvent` + `event.getRecipeManager()` | Gone. Use `net.neoforged.neoforge.client.event.RecipesReceivedEvent` (game bus, client): `bus.addListener((RecipesReceivedEvent event) -> CapsuleItems.registerRecipesClient(event.getRecipeMap().values()));`. This needs the server side `OnDatapackSyncEvent#sendRecipes` from §3.1. `CapsuleItems.registerRecipesClient` must change from `(RecipeManager)` to `(Iterable<RecipeHolder<?>>)` or `(RecipeMap)` (`RecipeMap#values()`, `#byType(RecipeType)` exist in vanilla 26.2). The Fabric call passes `SynchronizedRecipes#getAllOfType(RecipeType.CRAFTING)` (§4.8). |
| 23, 52-56 | `RenderLevelStageEvent` + `getStage() == Stage.AFTER_TRANSLUCENT_BLOCKS`, `getPartialTick()` | The stage enum is gone; each stage is a subclass. `bus.addListener((RenderLevelStageEvent.AfterTranslucentBlocks event) -> CapsulePreviewHandler.onLevelRenderLast(event.getLevelRenderer(), event.getPoseStack(), Minecraft.getInstance().getDeltaTracker().getGameTimeDeltaPartialTick(true)));`. `getPartialTick()` is gone; the event has `getLevelRenderer()`, `getLevelRenderState()`, `getPoseStack()`, `getModelViewMatrix()`, `getRenderableSections()`. The draw-in-place model is deprecated in favour of extraction: `ExtractLevelRenderStateEvent` (has `getDeltaTracker()`, `getCamera()`, `getLevel()`, stores data with `LevelRenderState#setRenderData(ContextKey, Object)`) + `SubmitCustomGeometryEvent` (`getSubmitNodeCollector()`, `getPoseStack()`). The preview renderer's rewrite is the rendering agent's job: keep `AfterTranslucentBlocks` if it still draws immediately (it uses buffer sources), or move to extract+submit. |
| 19, 57 | `ClientChatReceivedEvent#getMessage()` | unchanged |
| 27, 58-60 | `PlayerTickEvent.Pre` with `LocalPlayer` | unchanged |
| 26, 61 | `PlayerInteractEvent.LeftClickEmpty` | unchanged |
| 32-36 | client `ModConfigSpec` registration | unchanged |
| 13 | `ResourceLocation` import | `Identifier` |

### 3.3 `neoforge/src/main/java/capsule/neoforge/NeoForgePlatform.java`
| Line | Now | 26.2 |
|---|---|---|
| 23, 51, 56-58 | `ModList.get().isLoaded`, `getModContainerById(...).getModInfo().getDisplayName()/getVersion()` | unchanged (FML 11) |
| 24, 63 | `FMLPaths.CONFIGDIR.get()` | unchanged |
| 32, 70-72 | `DeferredRegister.create(ResourceKey, MODID)`, `register(name, Supplier)` | unchanged (also `register(String, Function<Identifier, ? extends I>)`). Since 1.21.2 items/blocks need `Properties#setId(ResourceKey)` before construction. If common moves to `register(registry, name, Function<Identifier, T>)`, map it to `register.register(name, factory)` with the Function overload. |
| 76-78 | `CreativeModeTab.builder().withTabsBefore(CreativeModeTabs.SPAWN_EGGS)` | unchanged (`CreativeModeTab.builder()` is still a NeoForge patch). 26.2.0.76: in dev, adding a duplicate stack to a tab throws. |
| 26, 81-83 | `CommonHooks.getCraftingRemainingItem(stack)` | **Removed.** Use `ItemStackTemplate remainder = stack.getCraftingRemainder(); return remainder == null ? ItemStack.EMPTY : remainder.create();` (NeoForge `ItemInstanceExtension#getCraftingRemainder()` returns `@Nullable ItemStackTemplate`; Fabric has the same method, §4.4). The `Platform` interface keeps the method because `common` (NeoForm) cannot see the injected method. |
| 28, 30, 86-89 | `BlockSnapshot.create(dim, level, pos)` + `new BlockEvent.EntityPlaceEvent(snapshot, Blocks.DIRT.defaultBlockState(), player)` + `NeoForge.EVENT_BUS.post(...).isCanceled()` | unchanged |
| 29, 92-94 | `FakePlayerFactory.get(level, profile)` | unchanged |
| 25, 31, 98-101, 113-133 | `Capabilities.ItemHandler.BLOCK` + `IItemHandler` | **`Capabilities.ItemHandler` is gone** (21.9 transfer rework; `IItemHandler` is deprecated for removal). Replace with `ResourceHandler<ItemResource> handler = level.getCapability(Capabilities.Item.BLOCK, pos, null);` and rewrite `ItemHandlerSource`: `size()` → `handler.size()`; `getItem(slot)` → `ItemUtil.getStack(handler, slot)` (`net.neoforged.neoforge.transfer.item.ItemUtil`) or `handler.getResource(slot).toStack(handler.getAmountAsInt(slot))`; `extract(slot, count)` → `ItemResource r = handler.getResource(slot); if (r.isEmpty()) return ItemStack.EMPTY; try (Transaction tx = Transaction.openRoot()) { int n = handler.extract(slot, r, count, tx); tx.commit(); return r.toStack(n); }`; `insert(slot, stack)` → `try (Transaction tx = Transaction.openRoot()) { handler.insert(slot, ItemResource.of(stack), stack.getCount(), tx); tx.commit(); }`. Imports: `net.neoforged.neoforge.transfer.ResourceHandler`, `net.neoforged.neoforge.transfer.item.ItemResource`, `net.neoforged.neoforge.transfer.transaction.Transaction`. |
| 104-106 | `state.rotate(LevelAccessor, BlockPos, Rotation)` | unchanged (`IBlockStateExtension#rotate`) |
| 109-111 | `player.getPersistentData()` | unchanged (`IEntityExtension`) |
| 34 | `javax.annotation.Nullable` | `org.jspecify.annotations.Nullable` |

### 3.4 `neoforge/src/main/java/capsule/neoforge/NeoForgeNetwork.java`
- Line 15: `PacketDistributor.sendToServer` **moved** to `net.neoforged.neoforge.client.network.ClientPacketDistributor.sendToServer(payload)`.
  It is a client class, so call it only on the client (it is only reached from client code paths; the class is loaded
  lazily, which is fine as long as `NeoForgeNetwork` does not import it in a way that loads it on servers. A static
  call inside the method is OK).
- Lines 20, 25: `PacketDistributor.sendToPlayer(player, payload)` and `sendToPlayersNear(level, excluded, x, y, z, radius, payload)`: unchanged.

### 3.5 `neoforge/src/main/java/capsule/neoforge/NeoForgePayloadRegistrar.java`
- Lines 18-33: `registrar.playToServer(type, codec, handler)` / `playToClient(type, codec, handler)` and
  `IPayloadContext#enqueueWork`/`player()`: unchanged and compile as is. Optional simplification: handlers already run
  on the main thread (`HandlerThread.MAIN`), so `enqueueWork` can be replaced by a direct call wrapped in try/catch.
  NeoForge also offers `playToClient(type, codec)` + `RegisterClientPayloadHandlersEvent` for client-only handlers.
  This is optional; the 3-arg form still exists.

### 3.6 `neoforge/src/main/java/capsule/neoforge/NeoForgeClientPlatform.java`
The whole `ClientPlatform` interface is vanilla-only in 26.2, so it can move to common (coordinate with the rendering agent):
- Lines 22-25 `tesselateWithAO(..., ModelData.EMPTY, null)`: `BakedModel` is gone (1.21.5 `BlockStateModel`), and
  NeoForge 26.2 no longer adds a `ModelData` parameter: its `ModelBlockRenderer` patch passes `level, pos, state` to
  `collectParts` internally. The public method is vanilla `ModelBlockRenderer#tesselateBlock(BlockQuadOutput, float, float, float, BlockAndTintGetter, BlockPos, BlockState, BlockStateModel, long)`
  on both loaders. Note `BlockAndTintGetter` is now `net.minecraft.client.renderer.block.BlockAndTintGetter`.
- Lines 28-35 `IClientFluidTypeExtensions#getStillTexture/getTintColor`: **removed** in 26.x (fluid textures moved to
  vanilla `FluidModel`). Loader-independent replacement, verified against vanilla 26.2 and identical on Fabric:
  ```java
  FluidModel model = Minecraft.getInstance().getModelManager().getFluidStateModelSet().get(fluid);
  TextureAtlasSprite still = model.stillMaterial().sprite();
  BlockTintSource tint = model.tintSource();   // NeoForge keeps tintSource() (deprecated, returns its FluidTintSource)
  int color = tint == null ? -1 : tint.colorInWorld(fluid.createLegacyBlock(), level, pos);
  ```
  Pass `fluid.createLegacyBlock()`, not `Blocks.AIR`: NeoForge's `FluidTintSource#colorInWorld(BlockState, …)` reads
  `state.getFluidState()`. Then delete `getFluidStillSprite`/`getFluidTint` from `ClientPlatform` and both
  implementations (or keep both implementations with this identical body).
- Imports `ModelData`, `IClientFluidTypeExtensions`, `InventoryMenu.BLOCK_ATLAS`, `BakedModel`: delete.

### 3.7 `neoforge/src/main/java/capsule/neoforge/ModConfigSpecBuilder.java`
Compiles unchanged. `defineList(String, List, Predicate)` (line 61) is `@Deprecated` (it was in 21.1 too). The
non-deprecated form is `builder.defineList(path, defaultValue, () -> "", item -> item instanceof String)`. That form
also gives the config UI a new-element supplier. Since 26.1.2.27, configs whose values reject their own defaults fail
to load, and Capsule's validators accept their defaults.

### 3.8 `neoforge/src/main/java/capsule/neoforge/NeoForgeReiPlugin.java`
`me.shedaniel.rei.forge.REIPluginClient` still exists in `RoughlyEnoughItems-api-neoforge:26.2.820`. No change.

### 3.9 GameTests (NeoForge)
The vanilla GameTest framework was rewritten in 1.21.5. `GameTestRegistry`, `TestFunction`, `@GameTestGenerator` and
the vanilla `@GameTest` annotation are gone from the 26.2 jar. Tests are entries of two registries: built-in
`Registries.TEST_FUNCTION` (`Consumer<GameTestHelper>`) and datapack `Registries.TEST_INSTANCE` (`GameTestInstance`,
e.g. `new FunctionGameTestInstance(ResourceKey<Consumer<GameTestHelper>>, TestData<Holder<TestEnvironmentDefinition<?>>>)`,
`TestData(env, Identifier structure, int maxTicks, int setupTicks, boolean required[, Rotation[, boolean manualOnly, int maxAttempts, int requiredSuccesses, boolean skyAccess, int padding]])`).
Batches are now environments.

Proposed contract with the vanilla/gametest agent: common exposes
`CapsuleGameTests.tests(Stream<Class<?>>) -> List<CapsuleTest>` where
`record CapsuleTest(Identifier id, Consumer<GameTestHelper> function, String batch, Identifier structure, int maxTicks, int setupTicks, boolean required, int maxAttempts, int requiredSuccesses)`.
Common needs its own annotation (for example `capsule.gametest.GameTest` with the old attribute names `template`,
`batch`, `timeoutTicks`, …) so that the 30 test files only change an import.

- `neoforge/src/gametest/java/capsule/gametest/NeoForgeGameTests.java`
  - Line 14: `@EventBusSubscriber(modid = NeoForgeGameTests.MODID, bus = EventBusSubscriber.Bus.MOD)` → `@EventBusSubscriber(modid = NeoForgeGameTests.MODID)` (`Bus` no longer exists; `RegisterGameTestsEvent` is an `IModBusEvent`).
  - Lines 3-4, 30-40: replace the `@GameTestGenerator` with:
    1. test functions: `DeferredRegister<Consumer<GameTestHelper>> FUNCTIONS = DeferredRegister.create(Registries.TEST_FUNCTION, MODID);`
       register each `CapsuleTest` (`FUNCTIONS.register(t.id().getPath(), () -> t.function())`), and call
       `FUNCTIONS.register(modEventBus)` from a new common (both dists) `@Mod(NeoForgeGameTests.MODID)` class.
       (FML allows several `@Mod` entrypoints per id; `NeoForgeClientSmoke` is the client-only one.) Optional-mod test
       classes are chosen with `ModList.get().isLoaded(...)` as today (line 37). ModList is available in the mod constructor.
    2. instances, in `@SubscribeEvent static void register(RegisterGameTestsEvent event)`: for each batch name
       `Holder<TestEnvironmentDefinition<?>> env = event.registerEnvironment(Identifier.fromNamespaceAndPath(MODID, batch));`
       (zero definitions → `AllOf()`), then
       `event.registerTest(t.id(), new FunctionGameTestInstance(ResourceKey.create(Registries.TEST_FUNCTION, t.id()), new TestData<>(env, t.structure(), t.maxTicks(), t.setupTicks(), t.required(), Rotation.NONE, false, t.maxAttempts(), t.requiredSuccesses(), false, 1)));`.
       `RegisterGameTestsEvent` fires from NeoForge's `RegistryDataLoader` patch only when `GameTestHooks.isGametestEnabled()`
       (`!production && (IDE || gameTestServer || -Dneoforge.enableGameTest)`). The MDG client and gameTestServer runs set `neoforge.enableGameTest=true`.
- `neoforge/src/gametest/java/capsule/gametest/NeoForgeGameTestSetup.java` line 9: `@EventBusSubscriber(modid = …)`.
  It is unchanged; `ServerAboutToStartEvent` and `BlockEvent.EntityPlaceEvent` still exist.
- `neoforge/src/gametest/java/capsule/gametest/ProductionGameTests.java`:
  - line 8/17 `FMLLoader.isProduction()` (static) → `net.neoforged.fml.loading.FMLEnvironment.isProduction()` (21.9).
  - line 3/21 `GameTestRegistry.register(...)` is gone. In production NeoForge neither fires `RegisterGameTestsEvent` nor
    ticks `GameTestTicker` (its `MinecraftServer` patch gates `GameTestTicker.SINGLETON.tick()` on `isGametestEnabled()`,
    and `isGametestEnabled()` is false in production). So `-Dneoforge.gameTestServer=true` no longer does anything.
    Recommended replacement (**UNVERIFIED**, needs a run): delete `ProductionGameTests` and add a test-mod-only mixin
    `capsule.gametest.mixin.GameTestHooksMixin` on `net.neoforged.neoforge.gametest.GameTestHooks`:
    `@Inject(method = "isGametestEnabled", at = @At("HEAD"), cancellable = true) private static void capsule$prod(CallbackInfoReturnable<Boolean> cir) { if (Boolean.getBoolean("capsule.gametest.production")) cir.setReturnValue(true); }`.
    That turns on both the registration event and the ticker. Declare it in `capsule_gametest.mixins.json` (package
    `capsule.gametest.mixin`, `"compatibilityLevel": "JAVA_25"`, `"mixins": ["GameTestHooksMixin"]`) and in the test
    mod's `neoforge.mods.toml` (`[[mixins]] config=…`). Start the server with the GameTest entrypoint (§5.5).
- `NeoForgeTestUtils.java` lines 41-42, 57-66 and `SophisticatedStorageTests.java` lines 15-16: `Capabilities.ItemHandler.BLOCK`/`IItemHandler` → `Capabilities.Item.BLOCK`/`ResourceHandler<ItemResource>`
  (`getSlots()` → `size()`, `getStackInSlot(i)` → `ItemUtil.getStack(handler, i)`).
- `MekanismTests.java` (uses `ItemHandlerHelper`), `RefinedStorageTests`, `ImmersiveEngineeringTests`, `SuperFactoryManagerTests`: delete (no 26.2 mods).
- `neoforge/src/gametest/java/capsule/clientsmoke/NeoForgeClientSmoke.java`: `@Mod(value=…, dist=Dist.CLIENT)` and `ClientTickEvent.Post` are unchanged.

### 3.10 Other NeoForge facts checked (no Capsule change)
`IItemExtension#onEntityItemUpdate(ItemStack, ItemEntity)`, `#supportsEnchantment(ItemStack, Holder<Enchantment>)`,
`#isPrimaryItemFor(ItemStack, Holder<Enchantment>)` keep their signatures, so the plain methods in `CapsuleItem`
(lines 437, 444, 736) still override them on NeoForge. `ModConfigSpec`, `ModConfig.Type`, `ModContainer#registerConfig`
are unchanged. `BlockEvent.BreakEvent` → `BreakBlockEvent` (26.1.2.21) and the merged `PlayerEvent.EntityInteract*`
(26.2.0.43) are not used by Capsule.

---

## 4. Fabric API / Loader changes affecting Capsule (0.116.17+1.21.1 → 0.161.0+26.2)

Fabric API was renamed to Mojang names in 26.1 (full list: https://docs.fabricmc.net/26.1.2/develop/porting/fabric-api).
`ResourceLocation` → `Identifier` applies here too.

### 4.1 `fabric/src/main/java/capsule/fabric/CapsuleFabric.java`
| Line | Now | 26.2 |
|---|---|---|
| 13-14, 46-52 | `fuzs.forgeconfigapiport.fabric.api.neoforge.v4.NeoForgeConfigRegistry` / `NeoForgeModConfigEvents` | FCAP 26.2.1: `fuzs.forgeconfigapiport.fabric.api.v5.ConfigRegistry.INSTANCE.register(String modId, ModConfig.Type, IConfigSpec)` and `fuzs.forgeconfigapiport.fabric.api.v5.ModConfigEvents.loading(modId)` / `.reloading(modId)` (listener `(net.neoforged.fml.config.ModConfig modConfig) -> …`). So: `NeoForgeModConfigEvents.` → `ModConfigEvents.` and `NeoForgeConfigRegistry.INSTANCE` → `ConfigRegistry.INSTANCE`. `net.neoforged.fml.config.ModConfig` and `net.neoforged.neoforge.common.ModConfigSpec` are still in the FCAP jar. |
| 16, 61 | `CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> …)` | unchanged (params now named `buildContext, selection`) |
| 17, 70-71 | `ServerEntityEvents.ENTITY_LOAD/ENTITY_UNLOAD` `(entity, level)` | unchanged |
| 18, 59-60 | `ServerLifecycleEvents.SERVER_STARTING/STOPPED` | unchanged |
| 19, 68 | `ServerTickEvents.END_SERVER_TICK` | unchanged |
| 19, 69 | `ServerTickEvents.END_WORLD_TICK` | **`ServerTickEvents.END_LEVEL_TICK`** (`EndLevelTick#onEndTick(ServerLevel)`) |
| 20, 72 | `ServerWorldEvents.UNLOAD.register((server, level) -> …)` | **`net.fabricmc.fabric.api.event.lifecycle.v1.ServerLevelEvents.UNLOAD`** (`onLevelUnload(MinecraftServer, ServerLevel)`) |
| 21, 75-76 | `AttackBlockCallback.EVENT` `(player, level, hand, pos, direction)` | unchanged |
| 22, 27, 73-74 | `EnchantmentEvents.ALLOW_ENCHANTING` `(Holder<Enchantment>, ItemStack, EnchantingContext) -> TriState` | unchanged |
| 23, 63-66 | `LootTableEvents.MODIFY` `(key, tableBuilder, source, registries)`, `key.location()` | event unchanged; `key.location()` → **`key.identifier()`**; `tableBuilder.withPool(pool)` unchanged |
| 24, 67 | `ServerPlayConnectionEvents.JOIN` `(handler, sender, server)`, `handler.player` | unchanged |
| 25-26, 62, 79-90 | `ResourceManagerHelper.get(PackType.SERVER_DATA).registerReloadListener(new TemplatesReloadListener(...))` + `IdentifiableResourceReloadListener` | v0 lives on only in `fabric-api-deprecated` (still bundled). Move to resource-loader **v1**: `net.fabricmc.fabric.api.resource.v1.ResourceLoader.get(PackType.SERVER_DATA).registerReloadListener(Identifier.fromNamespaceAndPath(CapsuleMod.MODID, "templates"), CapsuleMod.reloadListener());`, then **delete the `TemplatesReloadListener` record (lines 79-90)** and imports 25-26, 30-32, 38-39. |
| new | (recipes to client, Fabric side) | In `onInitialize` add `RecipeSynchronization.synchronizeRecipeSerializer(...)` (`net.fabricmc.fabric.api.recipe.v1.sync.RecipeSynchronization`) for every serializer of Capsule's recipes: the 7 from `capsule.recipes.CapsuleRecipes` (`BLUEPRINT_CAPSULE_SERIALIZER.get()`, …, `PREFABS_AGGREGATOR_SERIALIZER.get()`) and the vanilla serializers of `data/capsule/recipe/*.json` (e.g. `RecipeSerializer.SHAPED`/`SHAPELESS` constants: check their 26.2 names). Call it after `CapsuleMod.init()`. |

### 4.2 `fabric/src/main/java/capsule/fabric/CapsuleFabricClient.java`
| Line | Now | 26.2 |
|---|---|---|
| 11-12, 31-37 | FCAP v4 config API | v5 as in §4.1 |
| 19, 42-46 | `ItemProperties.PROPERTIES.computeIfAbsent(...)` (accesswidener) | `ItemProperties` is gone. Register the same common `CapsuleStateProperty` codec as NeoForge: `RangeSelectItemModelProperties.ID_MAPPER.put(Identifier.fromNamespaceAndPath(CapsuleMod.MODID, "state"), CapsuleStateProperty.MAP_CODEC);` (`net.minecraft.client.renderer.item.properties.numeric.RangeSelectItemModelProperties`; the private `ID_MAPPER` is opened by Fabric API's transitive class tweaker, so no Capsule AW entry is needed). Fabric API has no wrapper for this. |
| 16, 47 | `ColorProviderRegistry.ITEM.register(...)` | Gone (`ColorProviderRegistry` → `BlockColorRegistry`, blocks only). Use `ItemTintSources.ID_MAPPER.put(Identifier.fromNamespaceAndPath(CapsuleMod.MODID, "capsule_color"), CapsuleTint.MAP_CODEC);` (`net.minecraft.client.color.item.ItemTintSources`, opened transitively by Fabric API) and the item model JSON. |
| 18, 48 | `BlockEntityRenderers.register(MARKER_TE.get(), CaptureBER::new)` | unchanged (Fabric API opens it transitively; `BlockEntityRendererRegistry` is `@Deprecated`). The provider is the render-state one, as on NeoForge. |
| 17, 50-51 | `WorldRenderEvents.AFTER_TRANSLUCENT` + `context.worldRenderer()`, `context.matrixStack()`, `context.tickCounter()` | **`net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderEvents`**. The NeoForge `AfterTranslucentBlocks` equivalent is `AFTER_TRANSLUCENT_TERRAIN` (`afterTranslucentTerrain(LevelRenderContext)`); `AFTER_TRANSLUCENT_FEATURES` is the alternative. `LevelRenderContext` has `levelRenderer()`, `poseStack()`, `submitNodeCollector()`, `levelState()`, `gameRenderer()`, `sectionsToRender()`. It has **no tick counter**, so use `Minecraft.getInstance().getDeltaTracker().getGameTimeDeltaPartialTick(true)`. For extraction: `LevelExtractionEvents.END_EXTRACTION` + `FabricRenderState` data keys, and `LevelRenderEvents.COLLECT_SUBMITS` to add submits. Which one depends on the rendering agent's rewrite of `CapsulePreviewHandler.onLevelRenderLast`. |
| 15, 52 | `ClientReceiveMessageEvents.GAME` `(message, overlay)` | unchanged |
| 14, 53-55 | `ClientTickEvents.START_CLIENT_TICK` | unchanged |
| new | recipe sync, client side | `ClientRecipeSynchronizedEvent.EVENT.register((client, recipes) -> CapsuleItems.registerRecipesClient(recipes.getAllOfType(RecipeType.CRAFTING)));` (`net.fabricmc.fabric.api.client.recipe.v1.sync.ClientRecipeSynchronizedEvent`, `SynchronizedRecipes#getAllOfType(RecipeType)`). It replaces `ClientPacketListenerMixin` (§4.8). |

### 4.3 `fabric/src/main/java/capsule/fabric/FabricPayloadRegistrar.java`
- Line 32: `PayloadTypeRegistry.playC2S()` → **`PayloadTypeRegistry.serverboundPlay()`**.
- Line 38: `PayloadTypeRegistry.playS2C()` → **`PayloadTypeRegistry.clientboundPlay()`**.
- `ServerPlayNetworking.registerGlobalReceiver(type, (payload, context) -> … context.player())` and
  `ClientPlayNetworking.registerGlobalReceiver`: unchanged.

### 4.4 `fabric/src/main/java/capsule/fabric/FabricPlatform.java`
| Line | Now | 26.2 |
|---|---|---|
| 8-9, 40-44 | `AttachmentRegistry.<CompoundTag>builder().persistent(CompoundTag.CODEC).copyOnDeath().initializer(CompoundTag::new).buildAndRegister(ResourceLocation…)` | unchanged API; `Identifier.fromNamespaceAndPath` |
| 10, 87 | `FakePlayer.get(level, profile)` | unchanged |
| 11, 72 | `net.fabricmc.fabric.api.itemgroup.v1.FabricItemGroup.builder()` | **`net.fabricmc.fabric.api.creativetab.v1.FabricCreativeModeTab.builder()`** |
| 12-17, 93-148 | Transfer API: `ItemStorage.SIDED.find`, `ItemVariant.of/toStack`, `StorageView`, `SingleSlotStorage`, `Transaction.openOuter()` | unchanged (fabric-transfer-api-v1 8.0.13) |
| 7, 82 | `CommonProtection.canPlaceBlock(level, pos, player.getGameProfile(), player)` | Common Protection API 2.0.0: **`CommonProtection.canPlaceBlock(level, pos, player.nameAndId(), player)`** (`net.minecraft.server.players.NameAndId`) |
| 77 | `stack.getRecipeRemainder()` | **`ItemStackTemplate r = stack.getCraftingRemainder(); return r == null ? ItemStack.EMPTY : r.create();`** (`FabricItemStack#getCraftingRemainder()` returns `@Nullable ItemStackTemplate`) |
| 66 | `Registry.register(registry, ResourceLocation…, factory.get())` | `Identifier.fromNamespaceAndPath`; the item/block id-in-properties requirement is the same as on NeoForge |
| 99 | `state.rotate(rotation)` | vanilla; unchanged |
| 18, 48-60 | `FabricLoader` | unchanged |

### 4.5 `fabric/src/main/java/capsule/fabric/FabricClientPlatform.java`
- Lines 6-7, 27-38: `FluidRenderHandlerRegistry` **no longer exists** and `FluidRenderHandler` lost `getFluidSprites`/`getFluidColor`.
  26.2 Fabric has `FluidRenderingRegistry` + vanilla `FluidModel`. Use the common vanilla code from §3.6, which is
  identical on both loaders.
- Lines 21-24 `tesselateWithAO`: vanilla `ModelBlockRenderer#tesselateBlock(...)` as in §3.6.

### 4.6 `fabric/src/main/java/capsule/fabric/FabricNetwork.java`
Unchanged: `ClientPlayNetworking.send`, `ServerPlayNetworking.send`, `ServerPlayNetworking.canSend(ServerPlayer, CustomPacketPayload.Type)`.

### 4.7 `fabric/src/main/java/capsule/fabric/GetOffMyLawnAdapter.java` / `ModConfigSpecBuilder.java`
- GOML 1.21.0+26.2 keeps `ClaimUtils.isInAdminMode(Player)` and `ClaimUtils.getClaimsInBox(LevelReader, BlockPos, BlockPos)`, so no change (apart from any vanilla `BoundingBox` renames).
- `ModConfigSpecBuilder` (Fabric): unchanged (FCAP's copy of `ModConfigSpec`); same `defineList` deprecation note as §3.7.

### 4.8 Mixins (`fabric/src/main/java/capsule/fabric/mixin/`)
- `ClientPacketListenerMixin.java`: **delete.** In 26.2 `ClientPacketListener` has `ClientRecipeContainer recipes`
  and no `RecipeManager`, and `handleUpdateRecipes` no longer receives the recipes. It is replaced by
  `ClientRecipeSynchronizedEvent` (§4.2) + `RecipeSynchronization` (§4.1). Remove it from `capsule.mixins.json`.
- `MinecraftMixin.java`: the target still exists (`private boolean startAttack()` calls `LocalPlayer.resetAttackStrengthTicker()V` once, javap). No change.
- `ItemEntityMixin.java` (`ItemEntity#tick()V`) and `ServerLevelMixin.java` (`ServerLevel#addFreshEntity(Entity)Z`): targets exist. No change.
  (Optional alternative for `ServerLevelMixin`: `ServerEntityEvents.ALLOW_LOAD` `(entity, level, spawnReason, isLoadedFromDisk) -> boolean`,
  new in Fabric API. The mixin is fine as it is.)

### 4.9 GameTests (Fabric)
fabric-gametest-api-v1 4.0.22: tests are **instance (non-static) public methods** annotated
`net.fabricmc.fabric.api.gametest.v1.GameTest` (`environment`, `structure`, `maxTicks`, `setupTicks`, `required`,
`rotation`, `manualOnly`, `maxAttempts`, `requiredSuccesses`, `skyAccess`, `padding`), on classes listed under the
`fabric-gametest` entrypoint. It has no generator. Test ids are `<modid>:<snake_case(Class_method)>`. The default
structure lookup is `<ns>/gametest/structure/*.snbt`.
- `fabric/src/gametest/java/capsule/gametest/FabricGameTests.java` lines 4-5, 16-20 (`@GameTestGenerator`, `TestFunction`):
  replace them with the common `CapsuleTest` list (§3.9):
  1. functions: in `FabricGameTestSetup.onInitialize()` (the gametest mod's `main` entrypoint):
     `for (CapsuleTest t : tests) Registry.register(BuiltInRegistries.TEST_FUNCTION, t.id(), t.function());`
  2. instances: there is no Fabric event. Add a test-mod mixin `capsule.gametest.mixin.RegistryDataLoaderMixin` that
     copies Fabric's own `net.fabricmc.fabric.mixin.gametest.RegistryDataLoaderMixin`. That mixin injects at HEAD of
     `load(Lnet/minecraft/server/packs/resources/ResourceManager;Ljava/util/List;Ljava/util/List;Ljava/util/concurrent/Executor;)Ljava/util/concurrent/CompletableFuture;`
     to detect the `TEST_INSTANCE` load, then at HEAD of
     `lambda$load$2(Ljava/util/List;Ljava/util/Map;Ljava/lang/Void;)Lnet/minecraft/core/RegistryAccess$Frozen;`,
     reading `RegistryLoadTask#registry` for each task. In the second injection, register the environments
     (`Registry.register(envRegistry, Identifier(MODID, batch), new TestEnvironmentDefinition.AllOf(List.of()))`) and the
     `FunctionGameTestInstance`s into the `TEST_INSTANCE` registry. These targets are valid for 26.2 because Fabric API
     0.161.0 uses them. Declare it in a `capsule-gametest.mixins.json` referenced from
     `fabric/src/gametest/resources/fabric.mod.json` (`"mixins": [...]`), and remove the `fabric-gametest` entrypoint.
  The GOML-only class is still added when `FabricLoader.getInstance().isModLoaded("goml")` (line 18).
- `FabricGameTestSetup.java` line 21: `CommonProtection.register(ResourceLocation…, provider)` →
  `CommonProtection.register(Identifier.fromNamespaceAndPath("capsule-gametest", "test_probe"), …)`. Line 33: the
  override becomes `canPlaceBlock(Level, BlockPos, NameAndId, @Nullable Player)` (CPA 2.0.0). Line 20
  `ServerLifecycleEvents.SERVER_STARTING`: unchanged.
- `GetOffMyLawnTests.java` line 35: `BuiltInRegistries.BLOCK.get(ResourceLocation…)` → `getValue(Identifier…)` (vanilla 1.21.2 rename); its `@GameTest` import follows the common annotation.
- `FabricClientSmoke.java`: `ClientTickEvents.END_CLIENT_TICK` unchanged.
- Fabric's GameTest runner (`-Dfabric-api.gametest`) has no NeoForge-style production gate, so `scripts/prod-gametest.sh` keeps working there (§5.5).

### 4.10 Fabric Loader
0.19 → 0.20 brings nothing Capsule-specific. Class tweakers (Loader ≥0.18) and enum extensions (0.19) are new but unused.
`ModInitializer`/`ClientModInitializer`, `FabricLoader#getModContainer/getConfigDir/isModLoaded` are unchanged.

---

## 5. CI, publishing, scripts

### 5.1 `.circleci/config.yml`
- Line 7: `- image: cimg/openjdk:25.0.5` (Docker Hub, 2026-11-18; `25.0.4` from 2026-10-22 if an older image is preferred).
  Update the comment on line 6: Java 25 builds and runs the mod, and Gradle's daemon also runs on 25, so no extra toolchain download is needed.
- Lines 20-21, 31: bump the cache key prefix `v4-` → `v5-` so that the 1.21.1 Loom/MDG caches are not restored.
- `store_artifacts`/`persist_to_workspace`: unchanged. The branch filter `/[0-9]+\..*/` matches `26.2`.

### 5.2 `Jenkinsfile` (stale since 1.12)
It builds branch `1.12`, seds `BUILD_ID` into `build.gradle`, runs `/gradlew` and archives `build/libs/Capsule-${BRANCH}*`, so it no longer matches the multiloader build. Either delete it (CircleCI is the live CI) or rewrite it:
```groovy
environment { BRANCH = '26.2'; GRADLE_OPTS = '-Dfile.encoding=UTF-8' }
// Build stage:
sh './gradlew build -Pbuild_id=${BUILD_ID} --stacktrace'
// Archive stage:
archiveArtifacts artifacts: "neoforge/build/libs/Capsule-neoforge-*.${env.BUILD_ID}.jar, fabric/build/libs/Capsule-fabric-*.${env.BUILD_ID}.jar", fingerprint: true
```
and drop the `sed` line and the `/data/capsulebuilds:/build/libs` mount (it pointed at the old single-project output).

### 5.3 `Dockerfile`
`FROM java:8-jdk` → `FROM eclipse-temurin:25-jdk`. Drop `MAINTAINER` (deprecated) or replace it with
`LABEL maintainer="…"`. `ADD . /` + `chmod +x build.sh gradlew` can stay.

### 5.4 `publish.sh`
- Version parsing (lines 56-60) works for `Capsule-neoforge-26.2-9.1.42.jar` → `VERSION=26.2-9.1.42`, `MINECRAFT_VERSION=26.2`.
  (A future `26.2.1` also works.)
- Lines 19-20, 43 (usage examples): `1.21.1` → `26.2`.
- Line 216: the fallback listing filter `if '1.21' in v['name']` → `if v['name'].startswith('26.')`.
- CurseForge game-version lookup (lines 191-198) matches `v['name'] == '26.2'`. **UNVERIFIED** that CurseForge names
  the version exactly `26.2` (the script already exits with a listing when it does not match). Modrinth's `game_versions: ["26.2"]` is correct (Modrinth lists `26.2`).
- Line 126: the changelog link uses branch `$MINECRAFT_VERSION`, so a `26.2` branch must exist on GitHub.
- Dependencies (lines 70-73): Modrinth ids `P7dR8mSH` (Fabric API) and `ohNO6lps` (FCAP) and CF slugs `fabric-api`,
  `forge-config-api-port` are unchanged. NeoForge still has no required deps.

### 5.5 `scripts/`
- `lib.sh`: no code change. `setup_fabric` (meta `…/loader/26.2/0.20.1/<installer>/server/jar`, the Fabric API jar,
  `modrinth_jar forge-config-api-port rSd3GiG8`) and `setup_neoforge` (installer `--installServer` of 26.2.0.95, `run.sh`)
  work with the new properties. `java` on PATH must be 25.
- `prod-gametest.sh`:
  - Fabric branch (lines 146-150): still valid. The fabric-api 0.161.0+26.2 pom lists `fabric-gametest-api-v1` `4.0.22+4a7fa0819e`, so the grep finds it.
  - NeoForge branch (line 143): `-Dneoforge.gameTestServer=true` no longer does anything (§3.9). Replace it with:
    `setup_neoforge "$dir" "${jvm_args[@]}" -Dcapsule.gametest.production=true`, then switch the entrypoint. The
    installer's args file ends with `net.neoforged.fml.startup.Server` plus `--fml.*` args (checked in 26.2.0.95's
    `data/unix_args.txt`):
    ```bash
    args="libraries/net/neoforged/neoforge/$NEO_VERSION/unix_args.txt"
    sed 's/^net\.neoforged\.fml\.startup\.Server$/net.neoforged.fml.startup.GameTestServer/' "$dir/$args" > "$dir/gametest_args.txt"
    START=(java @user_jvm_args.txt @gametest_args.txt)
    ```
    `net.neoforged.fml.startup.GameTestServer` (FML 11.0.16) starts vanilla `net.minecraft.gametest.Main`. Combined with
    the `GameTestHooksMixin` (§3.9), tests register and tick in production. **UNVERIFIED** end to end; needs one run.
    Update the header comment (lines 112-115) to match.
  - The 'All [0-9]+ required tests passed' grep (lines 162-168): still valid (vanilla string).
- `prod-smoke.sh`: line 12 example jar name `1.21.1` → `26.2`; line 25 "Needs java 21" → "Needs Java 25".
- `client-smoke.sh`: lines 19, 41-42 drop `emi` if EMI is dropped. Line 23 comment "NeoForge 1.21.1 dev runs …" → 26.2.
- `showcase.sh`: no version strings.
- `validate-all.sh`: line 31 "Needs Java 21 … Gradle runs on a Java 25 it downloads" → "Needs Java 25". Line 146
  `for viewer in jei rei emi` → `jei rei`. Line 137 comment (GOML 1.13.1+1.21, OPAC 0.32.7) → 26.2 versions. Lines
  177-186 (`--incompat`): drop the refined-storage/mekanism/IE/SFM `compat_mods` entries (lines 181-…: sha1 pins
  of 1.21.1 files), keep tombstone with a 26.2 file sha1. Line 18-19 `--modpack` default `forgeulously-optimized:mPRwXMh4`
  is a 1.21.1 Sinytra Connector pack and needs a 26.2 pack or removal (**UNVERIFIED** that a 26.2 Connector pack exists).
- `prod-client.py` line 61: the modpack search hardcodes `game_versions=["1.21.1"]` → read `minecraft_version` from
  `gradle.properties` (or use `26.2`). The rest (installer `--installClient`, vanilla manifest, asset index) is version-independent.
  The launched `java` must be 25.

### 5.6 Docs (not requested, worth a later pass)
`docs/TESTING.md`, `README.md`, `TEST.md` mention 1.21.1, Java 21, EMI and `-Dneoforge.gameTestServer`.

---

## 6. Summary of UNVERIFIED points
1. MDG 2.0.148 on Gradle 9.8.2 (fallback: Gradle 9.7.1 + Loom 1.18.4).
2. Whether the Fabric dev runs still need the explicit night-config and mezz_config dependencies with non-remap Loom.
3. The NeoForge production GameTest path (mixin on `GameTestHooks.isGametestEnabled` + the `GameTestServer` entrypoint swap in `unix_args.txt`).
4. CurseForge's display name for the 26.2 game version.
5. A 26.2 Sinytra Connector modpack for `--modpack`.
6. The exact 26.2 constant names of the vanilla shaped/shapeless recipe serializers to pass to `RecipeSynchronization` (vanilla agent).
