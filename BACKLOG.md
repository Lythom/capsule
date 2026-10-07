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

### Preview and wireframe shadows with Iris
Issues: https://github.com/Lythom/capsule/issues/69

With Iris and a shader pack, the preview casts a shadow on NeoForge (the render stage event Capsule draws in is also
fired in Iris' shadow pass), and the capture zone wireframe on both loaders (client smoke screenshots
`*-iris/02-*.png`). Cosmetic; skip drawing during the shadow pass. Effort: S.

### Creative copy of a deployed capsule shows an empty full preview
Not investigated: a deployed capsule copied in creative (middle click) previews nothing. Effort: S.

### Fabric: blueprint materials from modded storages
Blueprint material sources use Transfer API storages; a storage that is not slotted is read as its list of views,
which a modded storage may reorder between the listing and the extraction. Check with a Fabric storage mod linked to a
blueprint. Also not covered on Fabric: the label GUI and left click in the air (`Minecraft#startAttack` mixin).
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

### EMI "Untranslated tag" warning
EMI's dev runs warn about untranslated `#c:ingots/*` tags of the addon recipes. Harmless; tag translations
(`tag.item.c.ingots.*`) would silence it. Effort: S.

### REI and EMI in a production client
REI and EMI are checked in dev clients only; the production client (`MODPACK=`) leaves the recipe viewer to the pack.
Run a pack bringing REI or EMI. Effort: S.

### Production NeoForge GameTests depend on NeoForge internals
`scripts/prod-gametest.sh` relies on undocumented NeoForge behavior: `-Dneoforge.gameTestServer=true` starts a GameTest
server in production while NeoForge registers and ticks GameTests outside production only, so `ProductionGameTests`
does both itself (vanilla `GameTestRegistry`, `GameTestTicker`). A NeoForge update can break it. Effort: S per update.

## Platforms and versions

### Multi-version builds with Stonecutter
Targets: 1.21.1 (NeoForge + Fabric), 1.20.1 (Forge + Fabric), 26.1.2 and 26.2 (NeoForge + Fabric). Older versions
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
