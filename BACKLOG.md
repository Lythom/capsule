# Backlog

Known gaps and possible work, not scheduled. Effort: S (hours), M (a day or two), L (several days).

## Confirmed gaps

### Claim mods: FTB Chunks and Cadmus on Fabric
Issues: https://github.com/Lythom/capsule/issues/91

- FTB Chunks and Cadmus do not implement Common Protection API, so on Fabric they are not asked at all (on NeoForge the
  placement event probe covers them). An FTB Chunks adapter can be written from the API in `docs/CLAIMS.md`; neither
  mod is on Modrinth, so it needs another way to get them into the GameTest runtime (maven.ftb.dev). Effort: S to M.

### Integrated Dynamics cables and parts invisible in the full preview
Issues: https://github.com/Lythom/capsule/issues/94

No crash, the deploy is fine (client smoke `--modded`), but cables and parts are drawn from block entity model data the
preview does not provide. Pass the model data of the captured block entities to the preview renderer, or draw such
blocks as wireframe. Effort: M.

### Preview hidden by translucent terrain in front of it
Issues: https://github.com/Lythom/capsule/issues/88

The translucent preview is drawn after the translucent terrain: water and stained glass in front of it hide it.
Optional: draw it before the translucent terrain (render stage and depth to
check). Effort: S to M.

### Wireframe shadows with Iris
Issues: https://github.com/Lythom/capsule/issues/69

With Iris and a shader pack, the capture zone wireframe casts a shadow on both loaders (client smoke screenshots
`*-iris/02-*.png`). On 26.2 the full preview no longer does (Capsule's ghost pipelines get no Iris shadow program).
Cosmetic; skip the wireframe during the shadow pass (`IrisApi#isRenderingShadowPass`). Effort: S.

### Creative copy of a deployed capsule shows an empty full preview
Not investigated: a deployed capsule copied in creative (middle click) previews nothing. Effort: S.

### Fabric: blueprint materials from modded storages
Blueprint material sources use Transfer API storages; a storage that is not slotted is read as its list of views,
which a modded storage may reorder between the listing and the extraction. Check with a Fabric storage mod linked to a
blueprint. Also not covered on Fabric: left click in the air (`Minecraft#startAttack` mixin).
Effort: S to M.

### SecurityCraft stops pre-1.13 block entity upgrades
SecurityCraft mixes into the 1.13 data fixer schema: with it loaded, block entity data older than 1.13 (MCEdit
schematics) is no longer upgraded, so chests from 1.12 schematics hold one item per stack. Found by the schematic
GameTests, which skip the item count check when SecurityCraft is loaded. Report it to SecurityCraft or work around it.
Effort: S.

### Mob Grinding Utils dirt in the preview
Issues: https://github.com/Lythom/capsule/issues/81

Blocked: Mob Grinding Utils has no 1.21.1 build. The full preview skips a block whose placement, shape update or
model lookup throws (the reported crash), and falls back to the wireframe.
Check when a build exists (`validate-all.sh --modded` takes its jar). Effort: S.

### Label screen: the Done button is never shown
`LabelGui#init` builds a Done button into a list of its own and never adds it to the screen (`addRenderableWidget`), so
the label screen only has its text field: Enter or Escape close it. Older than the 26.2 port. Effort: S.

### Client smoke inventory overflows with 29 tiers
`ClientSmokeTest#giveDisplayCapsules` puts 46 capsules (27 tiers, 16 dyed iron capsules, an uncharged blueprint, an
upgraded and a Recall capsule) in the 27 main inventory slots from slot 9: the next ones land in the armor and offhand
slots (capsules show on the player in the `06`/`07` inventory tooltip screenshots), the rest is dropped silently. Older
than the 26.2 port. Give fewer dyed capsules, or check fewer tiers in the inventory and leave the others to the
creative search screenshot. Effort: S.

### Starter templates named `_stater_*`
The five starter huts are named `_stater_*`, so labels show "Stater". Renaming changes the ids existing configs refer to.
Effort: S.

## Tests and tooling

### Recovery recipe shift-click guard has no test
The clear recipe does not match a capsule put back by a blueprint, blueprint change or recovery craft; the blueprint
cases have GameTests, the recovery one does not. Effort: S.

### Maven Central rate limits
Maven Central can answer 429 (rate limit) to a build machine. Workaround outside the repository: a
Gradle init script putting a Maven Central mirror first, passed with `GRADLE_ARGS="-I mirror.gradle"` to the scripts. A committed mirror or a
retry would make clean clones more reliable. Effort: S.

### EMI on Minecraft 26.x
EMI has no 26.x build: on 26.2 its plugin (`common/.../plugins/emi/`) and the client smoke `EmiProbe` stay in the
sources, out of the build (a `sourceSets` exclude, decision D4 of `docs/port-26.2/PLAN.md`), and `-PrecipeViewer` and
the scripts only know JEI and REI. When EMI publishes a 26.x version: compile them again, add `emi` back to
`-PrecipeViewer`, `client-smoke.sh` and `validate-all.sh`, and port what EMI's API changed. On 1.21.1, EMI's dev runs
warn about untranslated `#c:ingots/*` tags of the addon recipes (harmless; `tag.item.c.ingots.*` translations would
silence it). Effort: S to M.

### Known incompatibilities without a 26.3 build
Refined Storage, Mekanism, Immersive Engineering and Super Factory Manager have no 26.x build (Refined Storage and Super
Factory Manager exist for 26.1.2), so their `-Pincompat` tests are out of the build (`neoforge/build.gradle`, decision
D5). Corail Tombstone had a 26.2 build (tested on 26.2) but has none for 26.3: `-Pincompat` loads no mod and says so,
and `validate-all.sh --incompat` reports its two steps SKIP. Bring each back with its Modrinth version id in
`gradle.properties` (the `incompatMods` map of `neoforge/build.gradle`, `INCOMPAT_MODS` of `validate-all.sh`) when it
is published for the Minecraft version of the branch. Effort: S each.

### Modded preview and modpack runs on 26.x
`validate-all.sh --modded` (the client smoke with the mods of #81, #94, #117, #76) and `--modpack` are reported SKIP on
26.2 and 26.3: Ad Astra and Farmer's Delight (NeoForge) have no 26.2 build (Integrated Dynamics and Farmer's Delight
Refabricated do), and the 1.21.1 pack, Forgeulously Optimized (Sinytra Connector), has no 26.x version. Pick the 26.2
builds that exist and a 26.2 NeoForge pack. Effort: S to M.

### Wireframe preview not checked by the client smoke test on 26.x
The smoke scenario checks the full (translucent) preview of the deploys and blueprints, not the wireframe preview of a
capsule too large or with blocks the full preview cannot draw, nor the capture zone wireframe of an empty capsule
beyond the screenshots. Check them by eye in `docs/MANUAL_VALIDATION.md`, or add a scenario step with a check. Effort: S.

### REI in a production client
REI is checked in dev clients only; the production client (`MODPACK=`) leaves the recipe viewer to the pack. Run a pack
bringing REI. Effort: S.

### Production NeoForge GameTests depend on NeoForge internals
`scripts/prod-gametest.sh` relies on undocumented NeoForge behavior: it starts FML's GameTest server entrypoint
(`net.neoforged.fml.startup.GameTestServer`) on an installed server, and NeoForge never enables GameTests in production
(`GameTestHooks#isGametestEnabled` gates `RegisterGameTestsEvent` and the GameTest ticker), so the test mod mixin
`GameTestHooksMixin` forces it on with `-Dcapsule.gametest.production=true`. Works on NeoForge 26.2.0.95 and 26.3.0.115. A NeoForge
update can break it. Effort: S per update.

## Platforms and versions

### Publishing 26.2: CurseForge game version and Jenkinsfile
`publish.sh` looks up the CurseForge game version named exactly `26.2`, and that name is not checked (the
`/api/game/versions` endpoint needs a token): check it, with the token, before the first 26.2 publish (the script
stops and lists the `26.*` names when it does not match). `Jenkinsfile` still builds branch `1.12` with the old
single-project layout (`build/libs`, a `sed` of `BUILD_ID`), and CircleCI is the live CI: delete it or rewrite it for
the multiloader build (`docs/port-26.2/inventory/loaders-build.md` §5.2). Effort: S.

### Publishing 26.3
Branch `dev-1.26.3` (Capsule 11.0, phase 10 of `docs/port-26.2/PLAN.md`) is ready to be pushed to a `26.3` branch for
CI once released; `publish.sh` then looks up the CurseForge game version `26.3`, unchecked like `26.2`. Effort: S.

### Recall box edges hidden with Improved Transparency (26.3)
With the Improved Transparency option on, the recall box (alpha below 255, `RenderTypes.linesTranslucentNoDepthWrite()`)
is drawn by vanilla's OIT line pipelines, whose depth test hides the edges lying on a block face, such as the bottom
front edge of a deployed structure; with the option off they show as on 26.2. Cosmetic; an OIT line pipeline set of
Capsule's own with a depth bias would fix it. Effort: S.

### LWJGL loader deadlock at client start (26.3)
One client smoke run out of six hung at start, before the window: the render thread was in LWJGL's
`SharedLibraryUtil#getLibraryPath` (a `dlopen` inside `dl_iterate_phdr`) while the services discovery thread loaded a JVM
native library: a glibc loader lock-order deadlock in LWJGL 3.4.3, not in Capsule. The run times out and fails; rerun
it. Effort: none on Capsule's side (report to LWJGL if it comes back).

### Multi-version builds with Stonecutter
Targets: 1.21.1 (NeoForge + Fabric), 1.20.1 (Forge + Fabric), 26.1.2, 26.2 and 26.3 (NeoForge + Fabric). Older versions
stay on their Forge branches (`1.20`, `1.18`, `1.16`), which take bug fixes only. Effort: L.

Stonecutter 0.9.x on top of the multiloader layout (`common`, one project per loader, platform interfaces loaded with
`ServiceLoader`):

- one build script per loader: ModDevGradle for NeoForge 1.21.1 and 26.x, ModDevGradle `legacyforge` for Forge 1.20.1
  (it supports Forge 1.17 to 1.20.1 only), Loom for Fabric;
- version specific code behind `//? if >=1.21.1 {` style comments, kept small by the platform interfaces;
- the official Stonecutter multiloader template (codeberg.org/stonecutter/template-multiloader) as reference.

Porting notes:

- **1.20.1**: no data components. Capsule stores its state in `minecraft:custom_data` through `NBTHelper`, and the base
  color in `minecraft:dyed_color` (`MinecraftNBT`); on 1.20.1 both become plain item NBT (`getTag()`, `display.color`).
  Networking uses `SimpleChannel` instead of payloads, registries `DeferredRegister` from Forge, enchantments are
  classes instead of data driven (`recall` must become an `Enchantment` subclass again), recipes and loot tables use the
  older JSON formats (`item` + `nbt` results), and Java 17.
- **1.21.5+**: GameTest is data driven `test_instance` registry entries with test environments and a `/test` command;
  `CapsuleGameTests` and the loader registrations must be rewritten (the test bodies mostly survive).
- **1.21.11**: `ResourceLocation` is renamed `Identifier` (a plain replacement with Stonecutter).
- **26.x**: Java 25, unobfuscated game (no intermediary or reobfuscation, Parchment optional), NeoForge versions with
  four parts (`26.1.2.114`), Loom 1.18+ which needs Gradle running on Java 25.
- Fabric client GameTests (`fabric-client-gametest-api-v1`) exist from 1.21.4; on 1.21.1 the client smoke test
  (`scripts/client-smoke.sh`) fills that role.

### ItemPhysic compatibility
Issues: https://github.com/Lythom/capsule/issues/72

Later. Thrown capsules deploy from `Item#onEntityItemUpdate` and the item entity collision flags, which ItemPhysic
replaces. `ThrownCapsules` (`common`) tracks every thrown capsule per level,
so a fallback can deploy on `onGround()` or after a timeout without a new scan. Effort: M.

## Decided, not doing

- **Forge 1.21.1**: dropped. 1.21.1 packs are NeoForge and Fabric.
- **Experience merging into an orb already near a capture** (#122): won't fix, whoever finds it deserves it.
- **Updated default templates never reach existing installs**: fine, users delete the config folder
  (`config/capsule/loot`, `starters`, `blueprint_whitelist.json`).
- **Deploying a reward capsule rewrites its template file**: intended.
- **A capture base powered by redstone shows its activated top**: intended, kept.
- **Backports**: bug fixes only, no enhancements on 1.20.1, 1.18.2 and 1.16.5.
