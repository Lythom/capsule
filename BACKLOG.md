# Backlog

Work identified on the dev-1.21.1 branch and not done there, after round 2 (`docs/PLAN-ROUND2.md`). Effort: S (hours),
M (a day or two), L (several days). Triage details and ready-to-post issue comments are in `docs/ISSUE_TRIAGE.md`,
backports to 1.20.1, 1.18.2 and 1.16.5 in `docs/BACKPORTS.md`.

## Waiting for the owner

### Capsule tier table
Issues: https://github.com/Lythom/capsule/issues/120

`docs/RECIPES.md` (342b153) is a proposal: sizes by material rarity, material color from the material's dominant
color, base (body) left white for every metal and gem tier so it stays free for dyeing. Alternative: tier-tinted bases.
Changing sizes or colors is JSON only. Effort: S.

### Claims and actors without a player
Issues: https://github.com/Lythom/capsule/issues/91

Vanilla dispensers throwing a capsule and capture bases placed before 9.0 have no player: claims do not apply to them
(`docs/CLAIMS.md`, "Who is checked"). Proposal: treat them as "nobody", refused inside any claim (adapters answer for a
stranger, the generic probe with a fake player without rights). Changes the behavior of existing capture bases inside
claims, hence the decision. Effort: S.

### Claims on 1.16.5 stay per block
Agent's decision pending confirmation (`docs/BACKPORTS.md`): Flan 1.16.5 can only answer per position and Open Parties
and Claims has no 1.16.5 release, so `dev-1.16` keeps the per-block placement event and only got the capture base
placer and offline thrower checks (b429eb0). Per-chunk probing there would miss Flan claims not containing the probe
positions.

## Confirmed gaps

### Claim mods: FTB Chunks and Cadmus on Fabric, Get Off My Lawn runtime test
Issues: https://github.com/Lythom/capsule/issues/91

- FTB Chunks and Cadmus do not implement Common Protection API, so on Fabric they are not asked at all (on NeoForge the
  per-chunk placement event covers them). An FTB Chunks adapter can be written from the API in `docs/CLAIMS.md`; neither
  mod is on Modrinth, so it needs another way to get them into the GameTest runtime (maven.ftb.dev). Effort: S to M.
- Get Off My Lawn: the adapter was only checked with `javap`; the Loom dev runs do not load the mods nested in its jar.
  Test it on a production Fabric server (`scripts/prod-gametest.sh` with `EXTRA_MODS`). Effort: S to M.

### Integrated Dynamics cables and parts invisible in the full preview
Issues: https://github.com/Lythom/capsule/issues/94

No crash, the deploy is fine (client smoke `--modded`), but cables and parts are drawn from block entity model data the
preview does not provide. Pass the model data of the captured block entities to the preview renderer, or draw such
blocks as wireframe. Effort: M.

### Preview hidden by translucent terrain in front of it
Issues: https://github.com/Lythom/capsule/issues/88

The translucent preview (0c1d3aa) is drawn after the translucent terrain: water and stained glass in front of it hide
it, as they hid the opaque preview. Optional: draw it before the translucent terrain (render stage and depth to
check). Effort: S to M.

### Preview shadow with Iris on NeoForge
Issues: https://github.com/Lythom/capsule/issues/69

With Iris and a shader pack, the preview casts a shadow on NeoForge only: the render stage event Capsule draws in is
also fired in Iris' shadow pass. Cosmetic; skip drawing during the shadow pass. Effort: S.

### Creative copy of a deployed capsule shows an empty full preview
Seen during round 2, not investigated: a deployed capsule copied in creative (middle click) previews nothing. Effort: S.

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

Blocked: Mob Grinding Utils has no 1.21.1 build. The preview hardening (6c5aed7) covers the reported crash pattern.
Check when a build exists (`validate-all.sh --modded` takes its jar). Effort: S.

### Starter templates named `_stater_*`
The five starter huts are named `_stater_*`, so labels show "Stater". Renaming changes the ids existing configs refer to.
Effort: S.

## Tests and tooling

### Recovery recipe shift-click guard has no test
fa7b2c2 stops the clear recipe from matching a capsule put back by a blueprint, blueprint change or recovery craft; the
blueprint cases have GameTests, the recovery one does not. Effort: S.

### Maven Central rate limits
Maven Central answered 429 (rate limit) to the build machine during round 2. Workaround outside the repository: a
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
Targets from `docs/VERSIONS.md`: 1.21.1 (NeoForge + Fabric), 1.20.1 (Forge + Fabric), 26.1.2 and 26.2 (NeoForge +
Fabric; Java 25, unobfuscated, `ResourceLocation` renamed `Identifier` from 1.21.11; 26.2 asked by a user, expected
small once 26.1 builds). Stonecutter 0.9.x on top of the multiloader layout, one build script per loader (ModDevGradle,
ModDevGradle legacyforge for Forge 1.20.1, Loom for Fabric), version-specific code behind `//? if` comments. The fixes
are already backported to the Forge branches `dev-1.20`, `dev-1.18`, `dev-1.16` (`docs/BACKPORTS.md`). Effort: L.

### ItemPhysic compatibility
Issues: https://github.com/Lythom/capsule/issues/72

Later. Thrown capsules deploy from `Item#onEntityItemUpdate` and the item entity collision flags, which ItemPhysic
replaces. Keep compatibility easy: `ThrownCapsules` (0640895, `common`) already tracks every thrown capsule per level,
so a fallback can deploy on `onGround()` or after a timeout without a new scan. Effort: M.

## Decided, not doing

Owner's review of the backlog, 2026-10-06:

- **Forge 1.21.1**: dropped. 1.21.1 packs are NeoForge and Fabric (`docs/VERSIONS.md`).
- **Experience merging into an orb already near a capture** (#122): won't fix, whoever finds it deserves it.
- **Updated default templates never reach existing installs**: fine, users delete the config folder
  (`config/capsule/loot`, `starters`, `blueprint_whitelist.json`).
- **Deploying a reward capsule rewrites its template file**: intended.
- **A capture base powered by redstone shows its activated top**: intended, kept.
- **Backports**: bug fixes only, no enhancements on 1.20.1, 1.18.2 and 1.16.5.
