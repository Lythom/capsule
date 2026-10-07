# Backports to 1.20.1, 1.18.2 and 1.16.5

The bug fixes of `dev-1.21.1` (round 1: `git log origin/1.21.1..17b69f5`, round 2: `git log 17b69f5..fe6ef90`,
round 3: `git log fe6ef90..dev-1.21.1`) ported to the Forge branches, as decided by the owner: bug fixes only, no enhancements.

| Branch | Created from | Minecraft / Forge | JDK | Head |
|---|---|---|---|---|
| `dev-1.20` | `1.20` | 1.20.1-47.1.3 | Temurin 17 | ec9508f |
| `dev-1.18` | `1.18` | 1.18.2-40.1.16 | Temurin 17 | a6755d2 |
| `dev-1.16` | `1.16` | 1.16.5-36.2.31 | Temurin 8 | 05e11ec |

One commit per fix (or tight group), each naming its `dev-1.21.1` source ("Backport of ..."). Nothing is pushed to
`1.20`, `1.18` or `1.16`. Each branch's `CHANGELOG.md` has an entry `1.20.1-8.0.BUILD_ID` / `1.18.2-6.0.BUILD_ID` /
`1.16.5-5.0.BUILD_ID` "Bug fixes backported from 1.21.1" listing the ported fixes (round 1: d4f95dc, aedb2d6, e6fcdb2;
round 2: 945e59b, 2661926, 6396571).

## Evidence

The old branches have no GameTest setup. A fix that is pure logic gets a JUnit test; the others are covered by the
build, the boot test and the reasoning in the matrix (documented exception to the test-first rule). The 1.21.1
GameTests that found the bugs describe the scenarios.

- **Baseline**: the untouched branches build (`./gradlew build`, ForgeGradle 6 / 5 / 5, with
  `-Dorg.gradle.java.home` on the JDK above) and their jars boot on a real Forge server. No build fix was needed.
  Maven Central answered 429 during the work; a Gradle init script putting Google's Maven Central mirror first worked
  around it (not committed).
- **Build**: `./gradlew build` passes on each `dev-*` branch, JUnit included: 9 tests, 0 failures.
- **JUnit** (added to `build.gradle`):
  - `CapsuleTemplateManagerTest` (#125), `FilesTest` (N1/N5), `SpacialTest` (#89). Fail before: the same tests run
    against the original sources of the fixed methods fail on all three branches (`Invalid resource path:
    .../com.atlauncher.ATLauncher/aux/capsules/c-player-1.nbt`, `../escaped` written outside the template folder,
    `[My House, cab, ok_house, sub/tower]`, `Mutable{x=5, y=3, z=5}` instead of the block under the capsule).
  - `CapsuleTemplateManagerTest.missingTemplatesAreEmptyAndNameTheSearchedLocations` (#124): a missing template is
    empty and `searchedLocations` names the two files and two resources searched.
  - `PrefabsBlueprintAggregatorRecipeTest` (#84): the given back slots are the positions of 1, 2 and 3 in the shrunk
    pattern (default pattern `[0, 2, 4]`, a moved pattern with a blank column `[0, 3, 5]`, a blank row `[1, 3]`).
  - The last two test new helpers, so there is no fail-before run: before, the refund slots were the constants 4, 0
    and 2.
- **Boot test**: `scripts/prod-smoke-forge.sh` (d5bd2e1, 9cafd7a, 32d104d) installs the branch's Forge server with the
  official installer, adds the release jar, waits for "Done", checks the `/capsule` command, the config files and that
  the log has no capsule error, then stops the server. Since round 2 it keeps the Forge version check on and fails
  when capsule asks an update checker: the round 1 jars fail it on the three branches ("FAIL: capsule asks an update
  checker"), the round 2 jars pass. It also reports the recipes skipped for unmet conditions: 1.20.1 and 1.18.2 skip
  the 12 addon capsules except copper (vanilla copper fills `forge:ingots/copper`), 1.16.5 skips all 13.

| Branch | `./gradlew build` | JUnit | Boot test on the built jar |
|---|---|---|---|
| `dev-1.20` | passes | 9 tests, 0 failures | OK: Forge 1.20.1-47.1.3 booted, `/capsule` registered, no capsule error, no update check, stopped cleanly |
| `dev-1.18` | passes | 9 tests, 0 failures | OK: Forge 1.18.2-40.1.16, same checks |
| `dev-1.16` | passes | 9 tests, 0 failures | OK: Forge 1.16.5-36.2.31, same checks |

## Round 1 fixes

| Fix (dev-1.21.1 commit) | 1.20.1 (`dev-1.20`) | 1.18.2 (`dev-1.18`) | 1.16.5 (`dev-1.16`) |
|---|---|---|---|
| N6 capture base ignores its first redstone signal (077c784) + N13 highlight with `triggered=true` (4c1e887) | 0f00e90 | f2685fd | b0452a7 |
| #125 template path check on the install path (5bd81ee) | 7432cf4 (JUnit) | 33ae19d (JUnit) | 4e1adbf (JUnit) |
| N1/N5 invalid template file names, `.nbt` regex (4f94c86) | f9e3ff6 (JUnit) | 4c310e8 (JUnit; `validPathChar`, `isValidPath` is private) | 69d5a9d (JUnit; same) |
| N3a/N3b `excludedBlocks` invalid ids crash, tags become air (9f3b73b) | 54570f5 | 57738f2 | e5009d6 (tags kept as ids, expanded with `BlockTags.getAllTags()`) |
| #122 furnace experience on capture (f1ddf58) | ddcec2b | 269ef4a | 31f8840 (`AbstractFurnaceBlock.onRemove` pops experience, checked with javap) |
| #115 template copies share block entity data (4c18fbc) | cd76910 | 85e122e (deploy also mutated the template tag itself) | 49d2995 (same) |
| #109 loot entry reports the `loot_table` type (a454161) | f58f5b3 (serializer, DeferredRegister) | 7df2dda (same, `Registry.LOOT_ENTRY_REGISTRY`) | 89863f1 (`Registry.register` in common setup) |
| #100 infested blocks in loot templates (0ba3f27) | 232bf2a (well + castle kit, see notes) | 01919b4 | f20984e |
| #98 recall before the capsule could deploy (53dbca0) | 3f1312d | 1186765 | 4f18f09 |
| #89 blind throw deploys one block up (fd533d4) | bf9de3c (JUnit) | 918a09c (JUnit) | e15ec20 (JUnit: there the stream's shared mutable position made the anchor the last block, `(5,3,5)`) |
| #75 undeploy delay / activation timer in `player.tickCount` (b65ed27) | e897c7a (both parts) | 81e3fae (activation timer only: no undeploy delay on 1.18) | 7c94446 (same) |
| #56 item-less blocks free in blueprints (86600be) + liquid sources keep their bucket cost (35e89de) | a64760f | d94ccd1 | 1789dcd |
| #116 deploy above aimed snow layers and grass (372ba8e) | c0a567f (`BlockTags.REPLACEABLE`) | 6676444 (`Material.isReplaceable`) | 12a2f84 (same) |
| #91 part 1 server-side throw validation (65036a2) + out-of-range throw lands where it falls (42127c2) | 966d37d | 0c1675f | f6207dd (no yOffset on 1.16) |
| N8 `/reload` on dedicated servers (4a47699) | fc1c316 | 242efe6 | 3c2492b (cleared the caches on the event from a client-only subscriber; now a reload listener) |
| N9 addon recipes without ingot (55c7f6f) | 17da766 (`forge:not` + `forge:tag_empty`, boot log) | 49111bc (same) | a3612b0 (same; boot log shows the 13 recipes skipped) |
| #119 SecurityCraft owner check (91cd01f) | ba7484c (stub since the dependency was dropped; reflection, `isOwnedBy(Entity)` or `isOwnedBy(Player)` before SecurityCraft v1.9.9, checked with javap on v1.9.7, 1.9.8, 1.9.9, 1.9.10, 1.10.2.1) | not applicable: the check calls SecurityCraft's `IOwnable` API (compile dependency) | not applicable: same |
| #81 #117 #76 #94 full preview hardening (6c5aed7) | c96b240 | 5c4ea11 | dbf85c5 (also guards the block color lookup) |
| N12 multipart blocks missing from the full preview (dbc35e5) | 687015c (Forge 1.20.1 `MultiPartBakedModel` filters parts by render type, checked in the Forge patch) | not applicable: `tesselateWithAO` without render type | not applicable: quads read directly with `EmptyModelData` |
| N10 preview answered with the held item, not the asked capsule (b7b10b5) | a3420b3 | 0cd6bcd | c1cc259 |
| N7 capsules never in dungeon loot (5517bd6) | not applicable: `BuiltInLootTables` are `ResourceLocation`s before 1.20.5, the default list matches | not applicable: same | not applicable: same (`LootTables`) |
| #90 Recall not obtainable (95e534b) | not applicable: enchantments are not data driven before 1.21; the table uses `canApplyAtEnchantingTable`, which accepts capsules | not applicable: same | not applicable: same |
| N11 hand transforms (0c63f8a) | not applicable: the models use the 1.8 `firstperson`/`thirdperson` keys, ignored; the edge-on transforms came with the 1.21.1 port's `_righthand` keys | not applicable: same | not applicable: same |
| Fabric activated model (8fe831e) | not applicable: Fabric only | not applicable | not applicable |

## Round 2 fixes

| Fix (dev-1.21.1 commit) | 1.20.1 (`dev-1.20`) | 1.18.2 (`dev-1.18`) | 1.16.5 (`dev-1.16`) |
|---|---|---|---|
| Dead update checker URL (760456c) | cba464f (mods.toml + boot test check, fail-before above) | 690ce89 (same) | 1abe069 (same) |
| #124 blueprint of a missing or empty template reported (142ddcf) | 320c8f6 (JUnit) | c20494b (JUnit) | 57bc98b (JUnit; the branch reads `.schematics`, kept) |
| Item frames "invalid position" on deploy (e439d3a) | 0ef3983 (1.20.1 `HangingEntity.readAdditionalSaveData` logs "Hanging entity at invalid position" past 16 blocks, checked with javap) | not applicable: 1.18.2 `HangingEntity` reads TileX/Y/Z without distance check, and `moveTo`/`setPos` then resets it (javap) | not applicable: same (javap) |
| #126 starter hut item frame (2bcfd5f) | 098d692 | f3eeadb | 576b68c |
| #84 prefab refunds + shift-click chain into the clear recipe (fa7b2c2) | 52fb06a (JUnit) | 5ea3802 (JUnit) | 768ec44 (JUnit; refund indexes sent as a VarInt list) |
| Blueprint content-showing states reset (25f9268) | a142655 (chiseled bookshelf slots, lectern, jukebox, brewing stand) | bf4d343 (no chiseled bookshelf before 1.20) | 0cd6f68 (same) |
| #91 part 2 claims per chunk or claim (82a85aa) | f65f8e9 (generic probe per chunk column, Open Parties and Claims and Flan adapters, capture base placer, offline throwers) | 6623540 (same) | b429eb0 (capture base placer and offline throwers only, see below) |
| Escaping template path test in its temporary folder (6f58695) | not applicable: K1a wrote the test in the fixed form | not applicable: same | not applicable: same |

Not backported (enhancements): Loyalty instead of Recall (0640895), fire and lava proof capsules (d88bbef), new tiers
(342b153), blueprint whitelist update (551986d), Sponge v3 and `.schem` (9c99a3b), translucent preview (0c1d3aa),
capture animation (0a51a38), REI and EMI (48dbce0). The "Dyed" tooltip line (f397203) does not apply: it comes from
the `dyed_color` data component, which exists from 1.20.5. Round 1 infrastructure is not backported either:
ModDevGradle, unit test and GameTest infrastructure, GameTests, common/neoforge split, Fabric, smoke tests, CircleCI
and publish scripts, documentation.

## Round 2b (owner decisions L2 to L4)

| Change (dev-1.21.1 commit) | 1.20.1 (`dev-1.20`) | 1.18.2 (`dev-1.18`) | 1.16.5 (`dev-1.16`) |
|---|---|---|---|
| L2 captures and deploys of nobody refused inside claims (d303680) | 75d1eb9 | bdd5efe | 8361759 (placement event fired per block for an anonymous fake player) |
| L3 claim probe measured (2d5b468) | not applicable: the 1.21.1 numbers apply | not applicable: same | 6d54239 (per block 31³ 4 to 17 ms, 255³ about 2 s; Flan 1.16.5 does not listen to `EntityPlaceEvent`, so the probe does not see its claims) |
| L3 generic probe per block up to size 31, per chunk column above (efa286a) | b9acfec | a2ae24b | a13e209 |
| Flan 1.16.5 adapter (1.16.5 only) | not applicable: Flan adapter of 82a85aa | not applicable: same | eba5fed |
| Failed deploys rolled back inside claims, a dupe since L2 (780c7f6) | e71d26c | fd477f4 | 62955f8 |
| Claim checks that fail refuse captures and deploys, adapter kept, player told (3c399cc) | cd73eb5 | 6f9252c | 24de839 (Flan asked for the whole operation before the world changes) |
| CircleCI on version branches only (1.21.1: fbf85d5) | 57c98fd | 0f0eb49 | f20202d |
| L4 publish script (2e8f4b0) | 901f440 | d3488cf | ad83dfd |
| Refused blueprint undeploys only give the claim message, refused before any block is removed (8253687) | 19836cb | 855bd47 | 5cb9955 |
| Claim and SecurityCraft APIs compiled against instead of reflection, Flan per block up to 31 and per chunk column above (dd99a27) | cdb7bec (Open Parties and Claims `forge-1.20.1-0.32.7`, Flan `1.20.1-1.11.16-forge`, SecurityCraft v1.10.2.1, `compileOnly fg.deobf` from the Modrinth maven; SecurityCraft before v1.9.9, whose `isOwnedBy` took a `Player`, kept its blocks until 1a8859d) | a0fffe9 (Open Parties and Claims `forge-1.18.2-0.32.7`, Flan `1.18.2-1.11.9-forge`; SecurityCraft was compiled against already) | 8a7e5c5 (Flan 1.7.2 has the public API too, see below) |
| Flan's world outside claims asked 1024 blocks below the world, at opposite corners of it: claims reach 10 blocks below the world with `defaultClaimDepth` -1 (fb9570f) | 0fdd7bc | 56d391b | not applicable: Flan 1.16.5-1.7.2 claims start at y 0 or above (`Math.max(0, …)` in the constructors, `extendDownwards` to a block position; javap), so its reference at y -1 is never claimed |
| A failing SecurityCraft owner check logged once (616c761) | 1a8859d (and SecurityCraft before v1.9.9, without `isOwnedBy(Entity)`, asked `isOwnedBy(new Owner(player))`: v1.9.8 and v1.10.2.1 have both, javap) | 4f1d101 (the check had no try/catch: a block whose owner cannot be checked is refused) | c4e3042 (same as 4f1d101) |

## Claims on 1.16.5

First decision (round 2b): `dev-1.16` kept the per-block check. Open Parties and Claims has
no 1.16.5 release, and Flan 1.16.5 (1.7.2, Flemmli97's GitLab maven, not on Modrinth) has no per-chunk claim query:
`ClaimStorage` only offers `getClaimAt(BlockPos)`, `getDimensions()` returns `int[]` and permissions are
`ClaimPermission` objects (javap). Without an adapter, a probe once per chunk column would miss Flan claims not
containing the probe positions, a protection regression. So 1.16.5 first kept the per-block placement event and got the
protection bypass fixes only: capture bases act for their placer (fake player), a deployed base for its deployer,
offline throwers are checked as fake players (b429eb0).

The 1.16.5 measurement (6d54239) showed that Flan 1.16.5-1.7.2 on Forge listens to `BreakEvent`, `LeftClickBlock` and
`RightClickBlock`, not to `EntityPlaceEvent`: no probe was denied inside a Flan claim, so Flan claims are not protected
from captures and deploys by the placement event, per block or not.

The owner then chose the 1.21.1 rule for every version (per block up to size 31, per chunk column above; a13e209), and
1.16.5 got a Flan adapter (eba5fed): reflection only, loaded when Flan is present, one query per tested position
(`ClaimStorage.get(ServerWorld).getClaimAt(BlockPos)`, `Claim.canInteract(ServerPlayerEntity, ClaimPermission,
BlockPos, boolean)` with `PermissionRegistry.BREAK`, checked with javap on `flan-1.16.5-1.7.2-forge.jar`). Strangers and
nobody are refused inside Flan claims. Cost of `Claims.denied` for a stranger and a whole box, ms (median of 3 server
launches of 7 runs each, 3 for 255):

| Case | 3 | 11 | 31 | 255 |
|---|---|---|---|---|
| no claim mod | 0.02 | 0.66 | 11.5 | 418 |
| Flan loaded, no claim | 0.02 | 0.56 | 9.5 | 513 |
| inside a Flan admin claim | 0.16 | 1.69 | 17.0 | 5 033 |

The adapter is exercised on the dev server only (no GameTests on this branch); the production boot test does not
capture.

Since 24de839 Flan is asked for every position (and the chunk columns above size 31 for their probe positions) before
the capture or deploy changes the world, so a failing query refuses the whole operation instead of leaving it partial.
Same benchmark, one launch: 13.5 ms for 31 and 594 ms for 255 without claim, 14.0 ms and 4 353 ms inside a Flan admin
claim.

Since 8a7e5c5 the adapter compiles against Flan 1.7.2's public API (`io.github.flemmli97.flan.api`:
`ClaimHandler.getPermissionStorage(ServerWorld)`, `IPermissionStorage.getForPermissionCheck(BlockPos)`,
`IPermissionContainer.canInteract(ServerPlayerEntity, ClaimPermission, BlockPos)`, `PermissionRegistry.BREAK`; javap on
`flan-1.16.5-1.7.2-forge.jar`) instead of its internal `ClaimStorage` and `Claim` by reflection, and follows the 1.21.1
rule: each position up to size 31, the center of each chunk column above (the placement event probes the centers Flan
does not claim). Same benchmark, one launch: 5.9 ms for 31 and 322 ms for 255 without claim, 14.0 ms and 332 ms inside
a Flan admin claim.

Production check of the rc5 jars: each booted on a real Forge server with the claim mods (1.20.1-47.4.26 with Open
Parties and Claims, Flan and SecurityCraft, 1.18.2-40.3.12 with Open Parties and Claims and Flan, the mods needing a
newer Forge than the branches; 1.16.5-36.2.31 with Flan), and a dispenser deployed a reward capsule from the console:
the adapters were asked through the APIs in the reobfuscated jar, without error.

The rc6 jars (1.20.1 with 0fdd7bc and 1a8859d, 1.18.2 with 56d391b and 4f1d101, 1.16.5 unchanged since rc5) were
built the same way: `./gradlew build` passes with 9 JUnit tests, and the boot test is OK on the three branches.
The 1.16.5 rc7 jar (with c4e3042) was built and boot tested the same way.

The rc8 jars (round 3, below) were built the same way: `./gradlew build` passes on the three branches with 9 JUnit
tests, 0 failures, the boot test is OK on each, and the 1.20.1 known incompatibility scenario (`INCOMPAT=1`, Forge
1.20.1-47.4.10) passes its 33 checks.

## Round 3 (pre-release fixes)

| Fix (dev-1.21.1 commit) | 1.20.1 (`dev-1.20`) | 1.18.2 (`dev-1.18`) | 1.16.5 (`dev-1.16`) |
|---|---|---|---|
| #113 container entities emptied on capture (1.20.4 b8fdf98) and by a failed deploy's rollback (403e6c0) | 03e26be, 57ce4ab (chest boats duped: `ChestBoat.remove` drops its content for a `DISCARDED` removal) | 7d98af3 (no chest boat before 1.19 and minecarts were emptied already; any `Clearable` entity now, for modded ones) | c7c705b (same, `IClearable`) |
| #78 #68 startup crash when a block's material cannot be read (Snow! Real Magic) | not applicable: no material check, overridable blocks are the `capsule:overridable` tag | ae5c8f9 (try/catch per block, first failure logged with its block, then the count) | fce3523 (same) |
| Default excluded blocks (known incompatibilities, #121) | 8b2d109, da6fb59 (see below) | e52201e, e27b495 | cb77a28 (Forge 1.16.5 reads the tag's `optional` key, `ForgeHooks.deserializeTagAdditions`) |
| #99 #84 lapis in the castle kit and blueprint discovery | 137fd51 | 159f983 | 7c68b54 |
| Structure block `"seed": "LONG"` in the blueprint whitelist (5d20fd7) | fe43e6a | 1af99ff | 0e85def |
| Claims per block up to the largest survival capsule from the config (b79dbaa) | b6a357c | faa9b01 | 5097f58 |
| Rotation message glyph (8076892) | d53ef71 (same fonts as 1.21.1: bitmap fonts, then Unifont) | not applicable: the legacy unicode font draws it | not applicable: same |
| `/capsule exportHeldItem` and `exportSeenBlock` in the 1.20.5 item syntax (5559df4) | not applicable: the NBT syntax is right before 1.20.5 | not applicable | not applicable |
| `/capsule fromHeldCapsule` without a name (4af3d05) | not done: enhancement on these branches (the name was required since 1.13) | not done | not done |

CHANGELOG commits: ec9508f, a6755d2, 05e11ec.

- **Container entities**: the 1.20.4 fix b8fdf98 never reached `dev-1.20`. On 1.18.2 and 1.16.5 the vanilla
  container entities are minecarts, which both paths emptied already.
- **Default excluded blocks**, from the known incompatibilities played on a real 1.20.1 server (`INCOMPAT=1
  scripts/prod-smoke-forge.sh`, see its header) and the jars of the mods for each version (Modrinth):
  - `capsule:excluded` includes the optional `#forge:relocation_not_supported`, the tag of Mekanism (Digital Miner,
    bounding blocks, tubes and on 1.20.1 cables and pipes; 10.4.16.80, 10.2.5.465, 10.1.2.457) and Refined Storage
    (1.12.4 and 1.10.6; 1.9.18 tags nothing). Forge 1.20.1 and 1.18.2 do not read the tag's Forge `optional` key
    (1.18.2 `Tag$Builder` reads `values`, `required` and `replace`, javap), so its entries never applied there,
    `#tombstone:player_graves` included: they now use the vanilla `{"id": ..., "required": false}` form (da6fb59,
    e27b495), as 1.20.4 does (aa96d01).
  - `excludedBlocks` and `opExcludedBlocks` defaults: `bloodmagic:alchemytable` instead of `bloodmagic:alchemy_table`
    (Blood Magic 3.3.8, 3.2.6, 3.1.13); the 20 wire connector blocks of Immersive Engineering, which tags only their
    block entity types (connectors, relays, transformers, breakers, current transformer, feedthrough, electric
    lantern, floodlight, razor wire: the `ImmersiveConnectableBlockEntity` subclasses of 10.2.0, javap; same ids in
    8.4.0 and 5.1.0); `waystones:` (#121), Waystones 14.1.21, 10.2.2 and 7.6.4 tag nothing. Removed the 1.12 ids that
    match nothing: `superfactorymanager:` (now `sfm:`, the manager moves with its program), `gregtech:machine` and
    `gtadditions:` (now `gtceu:`, machines move with their content), `mekanism:machineblock` and
    `mekanism:boundingblock` (now tagged). `ic2:` stays: the namespace of IndustrialCraft ports. Only new installs get
    config defaults.
  - 1.20.1 scenario of the rc8 jar on Forge 1.20.1-47.4.10 (Waystones and Balm added): the Digital Miner, the wired
    connectors (still wired), the Blood Magic table and a waystone stay in place for standard and overpowered
    capsules; without `refinedstorage:` in the config, the tag keeps the disk drive and controller in place.
- **Lapis**: the blueprint recipe of the three branches takes `forge:dyes/blue` (blue dye). 1.20.4 replaced the two
  templates (8ec9143, DataVersion 3700); here the 2 lapis items of the castle kit chest and the two book pages are
  rewritten in place with a typed NBT round trip, DataVersion 2586 unchanged.
- **Claims**: emerald 11 + 10 upgrades = 31 by default on these branches, 33 when a mod fills `forge:ingots/platinum`.

## Notes

- **#100**: the old branches still had the pre-1.20.4 `_rare_castle_kit.nbt`, whose chests held infested cobblestone
  (replaced by a new kit in 8ec9143 on 1.20.4). Its 3 item ids were rewritten in place instead of copying the newer
  template; the DataVersion (2586) of both templates is unchanged.
- **760456c**: the three `mods.toml` had the same dead `updateJSONURL`. `VersionChecker` logs
  `[{}] Starting version check at {}` on Forge 1.16.5, 1.18.2 and 1.20.1 (javap on fmlcore / forge universal), which
  the boot test looks for.
- **2bcfd5f**: the five hut templates of the three branches were byte-identical to the 1.21.1 ones before the fix, so
  the fixed files are copied as is. The only change is the axe frame entity (Facing 3 to 0, TileY 57 to 58, position
  and rotation); DataVersion stays 2586. The frame hangs below leaves, which are solid for `ItemFrame.survives` on
  1.16.5 and 1.18.2 (`Material.LEAVES` is not `nonSolid`, javap).
- **fa7b2c2**: `ItemStack` has identity equality on all three versions and the crafting result slot puts the given
  back stack itself in the grid, as on 1.21.1. 1.16.5 set its refund indexes from the raw pattern string and compared
  them with grid slots; it now uses the shrunk pattern positions under the matched (possibly mirrored) placement.
- **25f9268**: the properties are in a lazily loaded holder class (`ContentProperties`): as a plain static field they
  loaded `Block` classes when `CapsuleTemplate` loaded, which broke the unit tests (no registry bootstrap there).
- **82a85aa on 1.20.1 and 1.18.2**: `Claims`, `ClaimAdapter`, `OpenPartiesAndClaimsAdapter`, `FlanAdapter` as on
  1.21.1; the generic probe is the dirt `EntityPlaceEvent` the branches already fired per block, now once per chunk
  column outside adapter claims; fake players from `FakePlayerFactory.get`. Adapters checked with javap on the real jars
  from Modrinth: Open Parties and Claims `forge-1.20.1-0.32.7` and `forge-1.18.2-0.32.7`
  (`OpenPACServerAPI.get(MinecraftServer)`, `getServerClaimsManager().get(ResourceLocation, int, int)`,
  `getChunkProtection().hasChunkAccess(UUID, ResourceLocation, int, int)`, mod id `openpartiesandclaims`); Flan
  `1.20.1-1.11.16-forge` and `1.18.2-1.11.9-forge` (`ClaimStorage.get(ServerLevel)`, `getClaimsAt(int, int)`,
  `Claim.getDimensions()` → `ClaimBox.minX()...maxZ()`, `getAllSubclaims()`,
  `canInteract(ServerPlayer, ResourceLocation, BlockPos, boolean)`, `BuiltinPermission.BREAK`, mod id `flan`). Get Off
  My Lawn: not applicable (Fabric only). The adapters are not exercised at runtime there (no GameTests); a missing or
  changed API disables the adapter with one log line, and the generic probe still covers the mod.
