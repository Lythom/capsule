# Backports to 1.20.1, 1.18.2 and 1.16.5

The bug fixes of `dev-1.21.1` (round 1: `git log origin/1.21.1..17b69f5`, round 2: `git log 17b69f5..dev-1.21.1`)
ported to the Forge branches, as decided by the owner: bug fixes only, no enhancements.

| Branch | Created from | Minecraft / Forge | JDK | Head |
|---|---|---|---|---|
| `dev-1.20` | `1.20` | 1.20.1-47.1.3 | Temurin 17 | 945e59b |
| `dev-1.18` | `1.18` | 1.18.2-40.1.16 | Temurin 17 | 2661926 |
| `dev-1.16` | `1.16` | 1.16.5-36.2.31 | Temurin 8 | 6396571 |

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

## Claims on 1.16.5

Owner's decision (round 2b, L4): `dev-1.16` keeps the per-block check. Open Parties and Claims has
no 1.16.5 release, and Flan 1.16.5 (1.7.2, Flemmli97's GitLab maven, not on Modrinth) has no per-chunk claim query:
`ClaimStorage` only offers `getClaimAt(BlockPos)`, `getDimensions()` returns `int[]` and permissions are
`ClaimPermission` objects (javap). Without an adapter, a probe once per chunk column would miss Flan claims not
containing the probe positions, a protection regression. So 1.16.5 keeps the per-block placement event and gets the
protection bypass fixes only: capture bases act for their placer (fake player), a deployed base for its deployer,
offline throwers are checked as fake players (b429eb0).

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
