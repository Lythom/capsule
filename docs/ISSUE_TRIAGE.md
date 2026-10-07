# Issue triage (dev-1.21.1)

All 45 issues open on https://github.com/Lythom/capsule/issues on 2026-10-05, checked against the 1.21.1 code
(NeoForge), updated after round 2 (2026-10-06, `docs/PLAN-ROUND2.md`). Nothing below has been posted on GitHub: the
"Comment" sections are drafts ready to post. Fixes ported to 1.20.1, 1.18.2 and 1.16.5 are listed in
`docs/BACKPORTS.md`.

Classifications:

- **CONFIRMED-FIXED**: reproduced on 1.21.1 by an automated test that failed before the fix and passes after it.
- **CONFIRMED-HARDENED**: plausible cause found in 1.21.1 code and guarded, but no reliable automated reproduction
  (client rendering with third-party mods).
- **CONFIRMED-BACKLOG**: real on 1.21.1, not fixed in this branch, see `BACKLOG.md`.
- **OBSOLETE**: already fixed, or the code involved no longer exists in 1.21.1.
- **VERIFIED**: a hardened issue checked with the mods it names in the client smoke test (`scripts/validate-all.sh
  --modded` or `--iris`): no crash and no capsule error in the log, screenshots reviewed.
- **DONE**: feature request implemented, with tests.
- **ENHANCEMENT**: feature request, see `BACKLOG.md`.
- **CANNOT-REPRODUCE**: no usable information and no failure on 1.21.1; to close.
- **DOUBT**: not reproducible from the code or the information given, see `BACKLOG.md` for what to check.

Test names refer to GameTests in `common/src/gametest/java/capsule/gametest` or JUnit tests in
`common/src/test/java` (see `docs/TESTING.md`).

## Summary

| # | Title | Classification | Commit | Fail-before evidence |
|---|---|---|---|---|
| [126](https://github.com/Lythom/capsule/issues/126) | Birch hut roof too low / crafting table message | CONFIRMED-FIXED (item frame; the message comes from another mod) | 2bcfd5f | `starterCraftingTablesHaveAFreeFace`: "crafting tables without a free face inside the starter": the 5 huts |
| [125](https://github.com/Lythom/capsule/issues/125) | Crash with Flatpak ATLauncher | CONFIRMED-FIXED | 5bd81ee | `CapsuleTemplateManagerTest`: `ResourceLocationException: Invalid resource path` from `writeToFile`; `../escaped` written outside the template folder |
| [124](https://github.com/Lythom/capsule/issues/124) | Prefab blueprints do nothing (1.20.1) | CONFIRMED-FIXED (silent empty blueprint; the reporter's files are unknown) | 142ddcf | `missingPrefabTemplateIsReported`: "the error should name the template and the searched paths, got []" |
| [123](https://github.com/Lythom/capsule/issues/123) | Use vanilla Loyalty instead of Recall | DONE | 0640895, d88bbef | `RecallTests` (Loyalty from table and anvil, other trident enchantments refused, Loyalty and legacy Recall capsules come back, a Loyalty trident does not); `capsuleSurvivesLava`: "the capsule should not burn in lava" |
| [122](https://github.com/Lythom/capsule/issues/122) | Furnace drops experience on every capture | CONFIRMED-FIXED | f1ddf58 | `capturingAFurnaceDropsNoExperience`: "Did not expect experience_orb to exist" |
| [121](https://github.com/Lythom/capsule/issues/121) | Waystones ghost blocks and broken doors (1.20.1) | OBSOLETE (1.21.1) | 07873fc | `waystoneStaysWhenCapturedWithADoor` with Waystones 21.1.46: waystones are never captured (`c:relocation_not_supported`), the door next to them moves without ghost blocks |
| [120](https://github.com/Lythom/capsule/issues/120) | Vanilla 13³ capsule recipe | DONE (side fix in 55c7f6f) | 342b153 | `everyCapsuleRecipeLoadsWithResolvedIngredients` loads every tier, client smoke checks a viewer recipe for each of the 29 tiers; tier table `docs/RECIPES.md` |
| [119](https://github.com/Lythom/capsule/issues/119) | SecurityCraft blocks can be captured | CONFIRMED-FIXED | 91cd01f | `onlyOwnersPassTheSecurityCraftOwnerCheck`: "another player cannot take the block" |
| [118](https://github.com/Lythom/capsule/issues/118) | 1.21.1 please | OBSOLETE | | |
| [117](https://github.com/Lythom/capsule/issues/117) | Crash placing a capsule with Ad Astra pipes | VERIFIED | 6c5aed7 | client smoke `--modded`, Ad Astra 1.16.26 on both loaders: preview and deploy without crash |
| [116](https://github.com/Lythom/capsule/issues/116) | Deploy floats above snow layers and grass | CONFIRMED-FIXED | 372ba8e | `deployReplacesTheAimedSnowLayer`: "got BlockPos{x=4, y=3, z=4}" instead of the snow layer at y=2 |
| [115](https://github.com/Lythom/capsule/issues/115) | Starter chest items missing (Sophisticated Storage) | CONFIRMED-FIXED | 4c18fbc | `templateCopiesDoNotShareBlockEntityData`: "source and copy share the same block entity tag"; with Sophisticated Storage 1.6.1, `rewardBarrelsDoNotShareTheirContent`: "emptying the first barrel emptied the second one: 0 diamonds" |
| [113](https://github.com/Lythom/capsule/issues/113) | Chest boat / minecart dupe | OBSOLETE | | |
| [112](https://github.com/Lythom/capsule/issues/112) | Add `c:relocation_not_supported` to excluded | OBSOLETE | | |
| [109](https://github.com/Lythom/capsule/issues/109) | "Invalid player data" with loot viewers | CONFIRMED-FIXED | a454161 | `capsuleLootEntryRoundTrips`, `lootTablesHoldingCapsulesEncode`: `CapsuleLootEntry cannot be cast to NestedLootTable` |
| [108](https://github.com/Lythom/capsule/issues/108) | Capsule won't deploy (1.19.2) | CANNOT-REPRODUCE | | `everyBundledTemplateDeploys` passes on both loaders |
| [106](https://github.com/Lythom/capsule/issues/106) | "Sucked in" capture effect | DONE | 0a51a38 | client smoke: frames drawn with `captureAnimation` on (23 of 24), none with it off |
| [101](https://github.com/Lythom/capsule/issues/101) | Blueprint whitelist for 1.18+ blocks | DONE | 551986d, 25f9268 | `everyVanillaBlockEntityIsWhitelistedOrExcluded`; `blueprintsNeverKeepInventories`: chiseled bookshelf and lectern "show a content it does not have" before 25f9268 |
| [100](https://github.com/Lythom/capsule/issues/100) | Infested blocks in loot capsules | CONFIRMED-FIXED | 0ba3f27 | `noBundledTemplateContainsInfestedBlocks`: "[initialconfig/loot/uncommon/_uncommon_well]" |
| [99](https://github.com/Lythom/capsule/issues/99) | Book says lapis, recipe needs blue dye | OBSOLETE | | |
| [98](https://github.com/Lythom/capsule/issues/98) | Recall prevents deploying | CONFIRMED-FIXED | 53dbca0 | `recallLetsAnEarlyCollidingCapsuleDeploy`: "capsule should deploy its gold block" (timeout) |
| [97](https://github.com/Lythom/capsule/issues/97) | Recall prevents the recovery recipe | OBSOLETE | | |
| [96](https://github.com/Lythom/capsule/issues/96) | Recall tick handler server load | DONE | 0640895 | `onlyCapsuleEntitiesAreTracked`: tracked thrown capsules instead of a scan of every item entity |
| [94](https://github.com/Lythom/capsule/issues/94) | Crash with Integrated Dynamics cables | VERIFIED (no crash; cables and parts invisible in the preview, BACKLOG) | 6c5aed7 | client smoke `--modded`, Integrated Dynamics 1.38.0 + Integrated Tunnels 1.13.0 (NeoForge) |
| [93](https://github.com/Lythom/capsule/issues/93) | "Invalid resource path" on GDLauncher (1.16.5) | OBSOLETE | | |
| [91](https://github.com/Lythom/capsule/issues/91) | Claim bypass (Flan) | CONFIRMED-FIXED (part 1), part 2 DONE | 65036a2, 82a85aa, fb9570f | `instantQueryIsRefusedForNonInstantCapsules`, `instantQueryIsRefusedOutOfRange`: the stone was captured; part 2: `ClaimTests`, and `OpenPartiesAndClaimsTests`, `FlanTests` with the real mods on both loaders, `GetOffMyLawnTests` on the Fabric release jar; no identity (round 2b): `capsulesUsedByNobodyAreRefusedInClaims`: "Expected Stone, got Air at (relative: 7,2,2)" (a capture without player in a claim), `flanVetoesStrangers`: "Did not expect Stone at (relative: 3,2,10)" (a capture base placed before 9.1 deployed in the claim); Flan claims reaching below the world (`defaultClaimDepth` -1): `flanVetoesStrangersInClaimsReachingBelowTheWorld`: "Did not expect Stone at (relative: 5,1,3)" (a claim member refused) |
| [90](https://github.com/Lythom/capsule/issues/90) | Recall not obtainable / not in JEI | CONFIRMED-FIXED, superseded by Loyalty (#123) | 95e534b, 0640895 | `recallIsOfferedByEnchantingTables`: "recall should be in #minecraft:in_enchanting_table"; since 0640895 `enchantingTablesOfferLoyaltyForCapsules`, `recallIsNoLongerObtainable` |
| [89](https://github.com/Lythom/capsule/issues/89) | Height offset on blind deploy | CONFIRMED-FIXED | fd533d4 | `blindThrowDeploysOnTheGround`: "Expected Block of Gold, got Air at (relative: 4,1,4)" |
| [88](https://github.com/Lythom/capsule/issues/88) | Preview visual glitch (1.18.2) | DONE (translucent preview) | 0c1d3aa | client smoke screenshots `14b`, `19`, `20` before/after (`docs/MANUAL_VALIDATION.md`) |
| [85](https://github.com/Lythom/capsule/issues/85) | Cannot deploy "traveller's base" (1.12.2) | OBSOLETE (related fix 4f94c86) | | |
| [84](https://github.com/Lythom/capsule/issues/84) | Crafting dupe, book says lapis | CONFIRMED-FIXED (shift-click chain and prefab refunds; no dupe in the blueprint recipe) | fa7b2c2 | `prefabCraftWithAMovedPatternGivesItsTemplateIngredientsBack`: grid counts `[2, 1, 2, 1, 2, 1, 0, 1, 0]` instead of `[1, 2, 2, 1, 2, 1, 0, 1, 0]`; `blueprintShiftCraftConsumesItsIngredients`, `blueprintChangeShiftCraftConsumesTheBlueprint`: "only blueprints should be crafted, got [EMPTY null, DEPLOYED ...]" |
| [83](https://github.com/Lythom/capsule/issues/83) | Crash rendering fluids (1.19) | OBSOLETE | | |
| [82](https://github.com/Lythom/capsule/issues/82) | Crash after world creation (1.16.5) | OBSOLETE | | |
| [81](https://github.com/Lythom/capsule/issues/81) | Crash with Mob Grinding Utils dirt | CONFIRMED-HARDENED (verification blocked: no 1.21.1 build of the mod) | 6c5aed7 | none (client rendering) |
| [80](https://github.com/Lythom/capsule/issues/80) | 1.19 won't work on latest Forge | OBSOLETE | | |
| [78](https://github.com/Lythom/capsule/issues/78) | Crash protection when loading block materials | OBSOLETE on 1.20.1+ (related fix 9f3b73b); 1.18.2 and 1.16.5 catch the failing materials, untested with Snow! Real Magic: keep open | | |
| [77](https://github.com/Lythom/capsule/issues/77) | Crash on new world (GDLauncher) | OBSOLETE | | |
| [76](https://github.com/Lythom/capsule/issues/76) | Crash previewing farmland | VERIFIED | 6c5aed7 | client smoke `--modded`: vanilla farmland and crops, Farmer's Delight 1.3.4 (NeoForge) and Refabricated 3.2.8 (Fabric) |
| [75](https://github.com/Lythom/capsule/issues/75) | undeployDelay blocks undeploy after restart | CONFIRMED-FIXED | b65ed27 | `instantCapsuleUndeploysAfterRelog`, `legacyUndeployDelayDoesNotBlockUndeploy`: "capsule should be undeployed"; `activatedCapsuleTimesOutAfterRelog`: "activation should time out" |
| [72](https://github.com/Lythom/capsule/issues/72) | ItemPhysic compatibility | DOUBT (later, BACKLOG) | | |
| [71](https://github.com/Lythom/capsule/issues/71) | Invalid resource path on GDLauncher (1.18.2) | OBSOLETE (related fix 5bd81ee) | | |
| [70](https://github.com/Lythom/capsule/issues/70) | Schematics not loading | CONFIRMED-FIXED | 9c99a3b | `spongeV1SchemDeploys`, `spongeV2SchemDeploys`, `spongeV3SchemDeploys`, `spongeV3SchematicDeploys`: "should be read"; `spongeV2SchematicDeploys`: 0 armor stands instead of 1; `mceditSchematicDeploys`: the chest holds 1 diamonds instead of 5 |
| [69](https://github.com/Lythom/capsule/issues/69) | Shaders: invisible preview / black screen | VERIFIED (Iris + MakeUp Ultra Fast; a ghost shadow on NeoForge only) | 3d33156 | client smoke `--iris` on both loaders, screenshots reviewed |
| [68](https://github.com/Lythom/capsule/issues/68) | Startup crash with Snow! Real Magic | OBSOLETE on 1.20.1+; 1.18.2 and 1.16.5 catch the failing materials, untested with the mod: keep open | | |
| [56](https://github.com/Lythom/capsule/issues/56) | ProjectRed wires free in blueprints | CONFIRMED-FIXED | 86600be | `pottedPlantsAreNotFree`: "a flower pot should be required, got {}" |

Totals after round 2: 17 CONFIRMED-FIXED (#91 counted once), 6 DONE (+ #91 part 2), 4 VERIFIED, 1 CONFIRMED-HARDENED,
15 OBSOLETE, 1 CANNOT-REPRODUCE, 1 DOUBT. Round 1 totals were 13 CONFIRMED-FIXED, 4 CONFIRMED-HARDENED,
1 CONFIRMED-BACKLOG (+ #91 part 2), 15 OBSOLETE, 5 ENHANCEMENT, 7 DOUBT.

## Bugs found without an issue

Found while writing the tests and the client smoke test (N1–N13 in round 1, N14–N18 in round 2, N19 in the last
review); each has its own
commit, with a test that failed before the fix or, for client rendering, before/after screenshots.

| Id | Bug | Commit | Fail-before evidence |
|---|---|---|---|
| N1 | A starter/loot/prefab file name with uppercase letters or spaces disconnected the player on every login (`ResourceLocationException` in `PlayerLoggedInEvent`) | 4f94c86 | `FilesTest.templateNamesAreExtensionlessValidResourcePaths`: `My House` listed |
| N5 | `replaceAll(".nbt", "")` is a regex: `cabinbt.nbt` became `ca` | 4f94c86 | same test |
| N3a | An invalid `excludedBlocks` id crashed config loading | 9f3b73b | `SerializationTest.invalidIdsAreIgnoredInsteadOfCrashing`: `ResourceLocationException` |
| N3b | Tags in `excludedBlocks` (documented as "Ex tag: minecraft:beds") silently became air | 9f3b73b | `SerializationTest.tagsAreNotResolvedToAir` |
| N6 | A newly placed capture base ignored its first redstone signal (default state `triggered=true`) | 077c784 | `freshCaptureBaseDispensesOnFirstSignal`: "Expected Stone, got Air at (relative: 5,2,5) (t=201)" |
| N7 | Capsules were never added to dungeon loot: the default `lootTablesList` held `ResourceKey[minecraft:loot_table / ...]` strings | 5517bd6 | `defaultLootTablesHoldTheCapsulePool`: "simple dungeon should hold the capsule pool" |
| N8 | `/reload` did not refresh cached templates on dedicated servers (reload listener registered on the physical client only) | 4a47699 | `reloadRefreshesRewardTemplates`: "/reload should refresh reward templates" |
| N9 | Addon capsule recipes (tin, lead...) loaded with empty ingot tags; dead `data/forge/tags/items/ingots` files | 55c7f6f | `everyCapsuleRecipeLoadsWithResolvedIngredients`: 12 recipes with an ingredient matching no item |
| N10 | The server answered a preview query with the template of the item it saw in hand: after switching capsules the client cached the wrong preview (blueprint showing the linked capsule's house on Fabric) | b7b10b5 | `previewAnswersTheAskedCapsule`: "the preview of the gold capsule should use the gold capsule, got 1 capsule:capsule" |
| N11 | Capsules held edge-on in first and third person (Minecraft 1.8 hand transforms in the item models) | 0c63f8a | client smoke screenshots (rendering, no automated test) |
| N12 | NeoForge full preview without multipart blocks (walls, fences, panes, modded multipart blocks): `RenderType.LINES` passed to `tesselateWithAO` | dbc35e5 | client smoke screenshots `17-blueprint-preview` vs `18-blueprint-deployed` |
| N13 | Every capture base looked activated until an empty capsule had been held (regression of the N6 fix) | 4c1e887 | client smoke screenshots `01-capture-base` vs `02-capture-base-with-empty-capsule` |
| N14 | Every NeoForge start logged "Failed to process update information" (dead `updateJSONURL`) | 760456c | `scripts/prod-smoke.sh`: "capsule asks an update checker" |
| N15 | Every capsule tooltip ended with the vanilla "Dyed" line | f397203 | `capsuleColorsHaveNoDyedTooltip`: `DyedItemColor[rgb=13421772, showInTooltip=true]` |
| N16 | Deploying item frames or paintings more than 16 blocks from the capture logged "Block-attached entity at invalid position" once per entity | e439d3a | `deployedItemFramesHangOnTheirBlocks`: "invalid position logged" |
| N17 | Blueprint blocks showed the content they lost (chiseled bookshelf books, lectern book, jukebox record, brewing stand bottles) | 25f9268 | `blueprintsNeverKeepInventories`: "shows a content it does not have" |
| N18 | JEI on Fabric showed no capsule recipes (lists filled after JEI read them); no viewer showed the recovery and blueprint recipes (special recipes) | 48dbce0 | client smoke: "1 crafting recipes for Empty Capsule, 0 capsule information pages"; "without recipe: [/recovery/240, /blueprint/241]" |
| N19 | `Claims.perBlockMaxSize` resized the shared result of a capsule recipe of even or too large size (`getSize` fixes the stack it reads), and scanned every recipe on each capture and deploy | f1aead2 | `theLargestSurvivalCapsuleLeavesTheRecipesUnchanged`: "the recipe result keeps its size 4, not 5" |

## Ready-to-post comments

### #126
Thanks for the report. The starter huts are 3×3×3 on purpose, so the roof is low by design. The axe item frame hung on
the only face of the crafting table reachable from inside, so right clicks hit the frame: fixed in the next 1.21.1
build, the frame now hangs on the ceiling. Existing installs keep their copy of the huts in `config/capsule/starters`
until that folder is deleted. The message "Unable to open. Break and replace to use." does not come from Capsule or
vanilla but from a crafting table mod that needs its own block entity; break and replace the table as it says. Also
fixed in the next 1.20.1, 1.18.2 and 1.16.5 builds. Closing.

### #125
Fixed in the next 1.21.1 build: the path check rejected any install folder named like a reserved Windows device
(`com.atlauncher.ATLauncher` matches `COM`). Only the template part of the path is checked now, and a bad path is
reported instead of crashing the server. Also fixed in the next 1.20.1, 1.18.2 and 1.16.5 builds.

### #124
The zip is no longer downloadable, so I could not check your files. On 1.21.1 a prefab blueprint whose source template
could not be read (wrong name, unknown blocks, failed schematic conversion) silently became an empty blueprint, which
matches "nothing happens, nothing in the logs". In the next build this is reported: the server log names the template
and every path searched, and the player gets an error message. Also fixed in the next 1.20.1, 1.18.2 and 1.16.5
builds. If it still happens, the log line now tells which file is missing; please post it with the prefab files.

### #123
Done in the next 1.21.1 build. Capsules take vanilla Loyalty (any level) from the enchanting table or from a book on
the anvil, and a thrown capsule with Loyalty comes back like with Recall. Capsules already enchanted with Recall keep
working, but Recall can no longer be obtained. Capsules no longer burn in fire or lava, existing ones included. For
commands: `/capsule giveLinked <template> <player> withLoyalty`.

### #122
Fixed in the next 1.21.1 build. The experience stored in a captured furnace is no longer dropped on capture; it stays
in the furnace and is awarded when you take its output after deploying. Also fixed in the next 1.20.1, 1.18.2 and
1.16.5 builds.

### #121
Tested on 1.21.1 with Waystones 21.1.46: Waystones now tags its blocks `c:relocation_not_supported`, which Capsule
excludes, so capsules (overpowered ones included) leave waystones in place, with their data. A door next to a waystone
is captured, deployed and undeployed with both halves and no ghost block (automated test with Waystones). I could not
test the 1.20.1 build; if it still happens there, a world or a log would help. Closing for 1.21.1.

### #120
Done in the next 1.21.1 build: a netherite capsule gives 13×13×13 with vanilla items. Also new: amethyst and
quartz (5) and prismarine crystals (9), and with mods zinc, aluminum, osmium, brass, steel and uranium. Modded recipes
only load when a mod provides the ingot, so recipe viewers no longer show capsules you cannot craft.

### #119
For 1.21.1: SecurityCraft tags its blocks `c:relocation_not_supported`, which Capsule excludes from captures, so
nobody can capture them. The next build also restores the owner check for SecurityCraft blocks outside that tag (only
the owner can take them). The owner check is also back in the next 1.20.1 build; there will be no new 1.19.2 build.

### #118
The NeoForge 1.21.1 version is released (1.21.1-9.0.117 on CurseForge and Modrinth). Closing.

### #117
Tested on 1.21.1 with Ad Astra 1.16.26 (NeoForge and Fabric): capturing, previewing and deploying its cables and pipes
works without crash in the next build, which protects the preview against blocks whose code expects a real level.
Please report back with the crash report if it still happens on 1.21.1. Closing.

### #116
Fixed in the next 1.21.1 build: aiming at a snow layer, grass or another replaceable block now deploys the content in
its place, like a placed block, instead of one block above. Also fixed in the next 1.20.1, 1.18.2 and 1.16.5 builds.

### #115
Fixed in the next 1.21.1 build. Starter copies shared their block entity data with the cached template; a barrel
keeping that data as its storage (Sophisticated Storage) emptied every other copy. Each copy now gets its own data.
Checked with Sophisticated Storage 1.6.1: a starter barrel deployed twice keeps its content in both copies. Also fixed
in the next 1.20.1, 1.18.2 and 1.16.5 builds.

### #113
Fixed since the 1.20.4 version: container entities are emptied before being removed during a capture. Closing.

### #112
Done: `capsule:excluded` includes `#c:relocation_not_supported` (optional, without replace). Closing.

### #109
Fixed in the next 1.21.1 build: capsule loot entries have their own loot entry type and codec, so mods that serialize
loot tables (Roughly Enough Resources and similar) no longer fail with "Invalid player data". Also fixed: on 1.21.1
the default loot table list never matched, so capsules were not added to dungeon chests at all. Configs generated by
previous 1.21.1 versions are read correctly, no need to delete them. Also fixed in the next 1.20.1, 1.18.2 and 1.16.5
builds.

### #108
Closing as cannot-reproduce: the attached log only shows an audio error from 1.19.2, which is no longer maintained. On
1.21.1 an automated test deploys every bundled template on NeoForge and Fabric. If a capsule still won't deploy on
1.21.1, please open a new issue with the full server log (lines after 'Couldn't deploy the capsule') and the template
file.

### #106
Done in the next 1.21.1 build: captured blocks shrink into the capsule with a particle trail. It is client side only
and can be turned off with `captureAnimation = false` in `config/capsule-client.toml`.

### #101
Done in the next 1.21.1 build: the default whitelist lists every vanilla 1.21.1 block entity block its item can
recreate (signs and hanging signs keep their text, banners, heads, campfires, shulker boxes, ender chests, decorated
pots, chiseled bookshelves, crafters...) and drops the old 1.12 ids. Inventories are never kept. The file is only
copied when missing: delete `config/capsule/blueprint_whitelist.json` to get the new list.

### #100
The castle kit was fixed in 1.20.4; the uncommon well still had 4 infested blocks and is fixed in the next 1.21.1
build. Existing worlds keep the templates copied in `config/capsule/loot`: delete that folder to get the updated ones.
Also fixed in the next 1.20.1, 1.18.2 and 1.16.5 builds.

### #99
Fixed since 1.20.4: the books and chests use blue dye, like the recipe. Existing installs keep the old templates in
`config/capsule/loot` until that folder is deleted. Closing.

### #98
Fixed in the next 1.21.1 build: a capsule with Recall that hits a ceiling or a wall right after being thrown was
brought back before it could deploy. It now waits until the capsule can deploy (Recall and Loyalty). Also fixed in the
next 1.20.1, 1.18.2 and 1.16.5 builds.

### #97
Not reproducible on 1.21.1: enchanted capsules match the recovery recipe and keep their enchantment. Closing.

### #96
Done in the next 1.21.1 build: thrown capsules are tracked when they enter a level instead of scanning every item each
tick. Capsules now come back with vanilla Loyalty (#123).

### #94
Tested on 1.21.1 with Integrated Dynamics 1.38.0 and Integrated Tunnels 1.13.0: capturing, previewing and deploying
cables and parts no longer crashes in the next build, and the deploy is correct. Remaining: cables and parts are not
drawn in the preview (their model needs data the preview does not provide yet); kept open for that.

### #93
Fixed since 1.20.x (absolute template paths). 1.16.5 is no longer maintained. Closing.

### #91
Fixed in the next 1.21.1 build. The server refuses instant capture/deploy requests for capsules that are not instant,
and positions out of reach. Claim mods can now veto captures and deploys: Open Parties and Claims and Flan (NeoForge
and Fabric) and Get Off My Lawn (Fabric) are asked once per chunk or claim, other mods through a placement check per
block (per chunk column above the largest capsule obtainable in survival: the largest crafted tier plus
`capsuleUpgradesLimit` × 2, 33 by default). Protected blocks stay in place on capture, and a deploy touching a claim you have no rights in is refused.
Capture bases act as the player who placed them. Capture bases placed before this version and dispensers act for
nobody: they can no longer capture or deploy inside claims (re-place the base to give it an owner). Also fixed in the
next 1.20.1, 1.18.2 and 1.16.5 builds. On 1.16.5 the checks are also per block up to that size (31 by default there) and per chunk column
above, and Flan is asked through its API for every position, as it does not answer the placement check there.
If a claim mod is loaded but Capsule cannot check its claims (an API changed by a mod update), captures and deploys
are refused with a chat message instead of ignoring them.

### #90
Superseded in the next 1.21.1 build: capsules now come back with vanilla Loyalty, from enchanting tables and books
(#123), and Recall can no longer be obtained; capsules already enchanted with Recall keep working. The
`recallEnchantType` and `recallEnchantRarity` options have no effect since 1.21. Closing.

### #89
Fixed in the next 1.21.1 build: a capsule thrown without preview target deployed one block above the ground. Also
fixed in the next 1.20.1, 1.18.2 and 1.16.5 builds.

### #88
The preview renderer was rewritten since 1.18.2. In the next 1.21.1 build the full preview is translucent, so you see
the terrain and blocks behind it, without sorting artifacts; walls, fences and other multipart blocks no longer go
missing on NeoForge, and switching capsules no longer shows the previous capsule's preview. Water and stained glass in
front of the preview still hide it. Closing; please open a new issue with a 1.21.1 screenshot if a glitch remains.

### #85
The 1.12.2 asset copy and this template no longer exist. Related: in the next 1.21.1 build, template files with names
that are not valid ids (uppercase, spaces, apostrophes) are skipped with a warning instead of breaking logins.
Closing.

### #84
The book was fixed (it says blue dye). On 1.21.1 the blueprint recipe consumes exactly its button, dyes and paper,
also from a one-use capsule. Two related bugs are fixed in the next build: shift-clicking a blueprint, blueprint
change or recovery craft went on clearing the source capsule (an extra empty capsule, and the linked capsule became a
one-use capsule), and prefab blueprint recipes refunded the wrong items when `prefab_blueprint_recipe.json` moves the
ingredients. Also fixed in the next 1.20.1, 1.18.2 and 1.16.5 builds. If you still get a dupe on 1.21.1, please
describe the exact steps.

### #83
The 1.21.1 fluid preview uses the current NeoForge API; this was a binary incompatibility of the 1.19 build. Closing.

### #82
Duplicate of #71, fixed since 1.20.x. Closing.

### #81
The cause (block code expecting a real level in the preview) is guarded in the next 1.21.1 build: such blocks are
skipped, or the wireframe preview is shown, instead of crashing the client. Also fixed in the next 1.20.1, 1.18.2 and
1.16.5 builds. I could not test with Mob Grinding Utils: it has no 1.21.1 build.

### #80
1.19 is no longer maintained; the current version targets NeoForge 1.21.1 and now declares its supported Minecraft
range, so it cannot be loaded on an incompatible version. Closing.

### #78
The code iterating every block material was removed in 1.20.1. The next 1.21.1 build also stops the config from
crashing on invalid block ids and makes tags work in `excludedBlocks`. The next 1.18.2 and 1.16.5 builds skip and log
the blocks whose material cannot be read, so they should no longer crash at Capsule's startup check; not tested with
Snow! Real Magic (#68), whose mixin may make later material reads fail: keeping this open, please report.

### #77
Duplicate of #71 (GDLauncher path), fixed since 1.20.x. Closing.

### #76
Tested on 1.21.1 with vanilla farmland and crops and Farmer's Delight (NeoForge and Fabric): capturing, previewing and
deploying them works without crash in the next build, which protects the preview against blocks whose code expects a
real level. Also fixed in the next 1.20.1, 1.18.2 and 1.16.5 builds. Please report back with a crash report and the
farmland mod if it still happens. Closing.

### #75
Fixed in the next 1.21.1 build: the undeploy delay and the activation timer use the world time instead of the player
tick counter, which restarts on relog. Capsules deployed with an older version can be undeployed again. Also fixed in
the next 1.20.1, 1.18.2 and 1.16.5 builds.

### #72
Not tested yet: deploying relies on the item entity collision, which ItemPhysic replaces. Planned for later: the next
build already tracks thrown capsules, so a fallback that deploys them when they land can be added without cost.

### #71
Fixed since 1.20.x, and in the next 1.21.1 build the path check also ignores the install path itself (an install
folder named like a reserved Windows name or ending with a dot works). Closing.

### #70
Fixed in the next 1.21.1 build: `.schem` files (the WorldEdit 7.3 default) are found in the template folders, and
Sponge v1, v2 and v3 and MCEdit schematics load, with their block entities and entities. Checked with files written by
WorldEdit 7.3.8.

### #69
Tested on 1.21.1 with Iris and the MakeUp Ultra Fast shader pack on NeoForge (Iris 1.8.12) and Fabric (Iris 1.8.8),
with Sodium 0.6.13: the preview, the capture zone wireframe and the capture animation show. The preview is tinted by
the pack, and on NeoForge it also casts a shadow. OptiFine does not exist for 1.21.1. Closing; please open a new issue
with the shader pack name if the preview is invisible with another pack.

### #68
The code that triggered this was removed in 1.20.1. On 1.18.2 and 1.16.5 the next builds skip and log the blocks whose
material cannot be read, so they should no longer crash at Capsule's startup check. Not tested with Snow! Real Magic
(no Forge 1.18.2 or 1.16.5 build to test with): keeping this open, please report whether it starts and plays.

### #56
Fixed in the next 1.21.1 build: blocks without an item (potted plants, attached stems...) are no longer free in
blueprints; potted plants cost the pot and the plant, other blocks cost the item they are picked as. Also fixed in the
next 1.20.1, 1.18.2 and 1.16.5 builds.
