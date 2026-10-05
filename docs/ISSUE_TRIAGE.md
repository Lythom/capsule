# Issue triage (dev-1.21.1)

All 45 issues open on https://github.com/Lythom/capsule/issues on 2026-10-05, checked against the 1.21.1 code
(NeoForge). Nothing below has been posted on GitHub: the "Comment" sections are drafts ready to post.

Classifications:

- **CONFIRMED-FIXED**: reproduced on 1.21.1 by an automated test that failed before the fix and passes after it.
- **CONFIRMED-HARDENED**: plausible cause found in 1.21.1 code and guarded, but no reliable automated reproduction
  (client rendering with third-party mods).
- **CONFIRMED-BACKLOG**: real on 1.21.1, not fixed in this branch, see `BACKLOG.md`.
- **OBSOLETE**: already fixed, or the code involved no longer exists in 1.21.1.
- **ENHANCEMENT**: feature request, see `BACKLOG.md`.
- **DOUBT**: not reproducible from the code or the information given, see `BACKLOG.md` for what to check.

Test names refer to GameTests in `common/src/gametest/java/capsule/gametest` or JUnit tests in
`common/src/test/java` (see `docs/TESTING.md`).

## Summary

| # | Title | Classification | Commit | Fail-before evidence |
|---|---|---|---|---|
| [126](https://github.com/Lythom/capsule/issues/126) | Birch hut roof too low / crafting table message | DOUBT | | |
| [125](https://github.com/Lythom/capsule/issues/125) | Crash with Flatpak ATLauncher | CONFIRMED-FIXED | 5bd81ee | `CapsuleTemplateManagerTest`: `ResourceLocationException: Invalid resource path` from `writeToFile`; `../escaped` written outside the template folder |
| [124](https://github.com/Lythom/capsule/issues/124) | Prefab blueprints do nothing (1.20.1) | DOUBT | | |
| [123](https://github.com/Lythom/capsule/issues/123) | Use vanilla Loyalty instead of Recall | ENHANCEMENT | | |
| [122](https://github.com/Lythom/capsule/issues/122) | Furnace drops experience on every capture | CONFIRMED-FIXED | f1ddf58 | `capturingAFurnaceDropsNoExperience`: "Did not expect experience_orb to exist" |
| [121](https://github.com/Lythom/capsule/issues/121) | Waystones ghost blocks and broken doors (1.20.1) | OBSOLETE (1.21.1) | 07873fc | `waystoneStaysWhenCapturedWithADoor` with Waystones 21.1.46: waystones are never captured (`c:relocation_not_supported`), the door next to them moves without ghost blocks |
| [120](https://github.com/Lythom/capsule/issues/120) | Vanilla 13³ capsule recipe | ENHANCEMENT (side fix in 55c7f6f) | 55c7f6f | `everyCapsuleRecipeLoadsWithResolvedIngredients`: 12 addon recipes "have an ingredient matching no item" |
| [119](https://github.com/Lythom/capsule/issues/119) | SecurityCraft blocks can be captured | CONFIRMED-FIXED | 91cd01f | `onlyOwnersPassTheSecurityCraftOwnerCheck`: "another player cannot take the block" |
| [118](https://github.com/Lythom/capsule/issues/118) | 1.21.1 please | OBSOLETE | | |
| [117](https://github.com/Lythom/capsule/issues/117) | Crash placing a capsule with Ad Astra pipes | CONFIRMED-HARDENED | 6c5aed7 | none (client rendering) |
| [116](https://github.com/Lythom/capsule/issues/116) | Deploy floats above snow layers and grass | CONFIRMED-FIXED | 372ba8e | `deployReplacesTheAimedSnowLayer`: "got BlockPos{x=4, y=3, z=4}" instead of the snow layer at y=2 |
| [115](https://github.com/Lythom/capsule/issues/115) | Starter chest items missing (Sophisticated Storage) | CONFIRMED-FIXED | 4c18fbc | `templateCopiesDoNotShareBlockEntityData`: "source and copy share the same block entity tag"; with Sophisticated Storage 1.6.1, `rewardBarrelsDoNotShareTheirContent`: "emptying the first barrel emptied the second one: 0 diamonds" |
| [113](https://github.com/Lythom/capsule/issues/113) | Chest boat / minecart dupe | OBSOLETE | | |
| [112](https://github.com/Lythom/capsule/issues/112) | Add `c:relocation_not_supported` to excluded | OBSOLETE | | |
| [109](https://github.com/Lythom/capsule/issues/109) | "Invalid player data" with loot viewers | CONFIRMED-FIXED | a454161 | `capsuleLootEntryRoundTrips`, `lootTablesHoldingCapsulesEncode`: `CapsuleLootEntry cannot be cast to NestedLootTable` |
| [108](https://github.com/Lythom/capsule/issues/108) | Capsule won't deploy (1.19.2) | DOUBT | | |
| [106](https://github.com/Lythom/capsule/issues/106) | "Sucked in" capture effect | ENHANCEMENT | | |
| [101](https://github.com/Lythom/capsule/issues/101) | Blueprint whitelist for 1.18+ blocks | ENHANCEMENT | | |
| [100](https://github.com/Lythom/capsule/issues/100) | Infested blocks in loot capsules | CONFIRMED-FIXED | 0ba3f27 | `noBundledTemplateContainsInfestedBlocks`: "[initialconfig/loot/uncommon/_uncommon_well]" |
| [99](https://github.com/Lythom/capsule/issues/99) | Book says lapis, recipe needs blue dye | OBSOLETE | | |
| [98](https://github.com/Lythom/capsule/issues/98) | Recall prevents deploying | CONFIRMED-FIXED | 53dbca0 | `recallLetsAnEarlyCollidingCapsuleDeploy`: "capsule should deploy its gold block" (timeout) |
| [97](https://github.com/Lythom/capsule/issues/97) | Recall prevents the recovery recipe | OBSOLETE | | |
| [96](https://github.com/Lythom/capsule/issues/96) | Recall tick handler server load | ENHANCEMENT | | |
| [94](https://github.com/Lythom/capsule/issues/94) | Crash with Integrated Dynamics cables | CONFIRMED-HARDENED | 6c5aed7 | none (client rendering) |
| [93](https://github.com/Lythom/capsule/issues/93) | "Invalid resource path" on GDLauncher (1.16.5) | OBSOLETE | | |
| [91](https://github.com/Lythom/capsule/issues/91) | Claim bypass (Flan) | CONFIRMED-FIXED (part 1), part 2 CONFIRMED-BACKLOG | 65036a2 | `instantQueryIsRefusedForNonInstantCapsules`, `instantQueryIsRefusedOutOfRange`: the stone was captured |
| [90](https://github.com/Lythom/capsule/issues/90) | Recall not obtainable / not in JEI | CONFIRMED-FIXED | 95e534b | `recallIsOfferedByEnchantingTables`: "recall should be in #minecraft:in_enchanting_table" |
| [89](https://github.com/Lythom/capsule/issues/89) | Height offset on blind deploy | CONFIRMED-FIXED | fd533d4 | `blindThrowDeploysOnTheGround`: "Expected Block of Gold, got Air at (relative: 4,1,4)" |
| [88](https://github.com/Lythom/capsule/issues/88) | Preview visual glitch (1.18.2) | DOUBT (1.21.1 preview checked visually, `docs/MANUAL_VALIDATION.md`) | | |
| [85](https://github.com/Lythom/capsule/issues/85) | Cannot deploy "traveller's base" (1.12.2) | OBSOLETE (related fix 4f94c86) | | |
| [84](https://github.com/Lythom/capsule/issues/84) | Crafting dupe, book says lapis | DOUBT | | |
| [83](https://github.com/Lythom/capsule/issues/83) | Crash rendering fluids (1.19) | OBSOLETE | | |
| [82](https://github.com/Lythom/capsule/issues/82) | Crash after world creation (1.16.5) | OBSOLETE | | |
| [81](https://github.com/Lythom/capsule/issues/81) | Crash with Mob Grinding Utils dirt | CONFIRMED-HARDENED | 6c5aed7 | none (client rendering) |
| [80](https://github.com/Lythom/capsule/issues/80) | 1.19 won't work on latest Forge | OBSOLETE | | |
| [78](https://github.com/Lythom/capsule/issues/78) | Crash protection when loading block materials | OBSOLETE (related fix 9f3b73b) | | |
| [77](https://github.com/Lythom/capsule/issues/77) | Crash on new world (GDLauncher) | OBSOLETE | | |
| [76](https://github.com/Lythom/capsule/issues/76) | Crash previewing farmland | CONFIRMED-HARDENED | 6c5aed7 | none (client rendering) |
| [75](https://github.com/Lythom/capsule/issues/75) | undeployDelay blocks undeploy after restart | CONFIRMED-FIXED | b65ed27 | `instantCapsuleUndeploysAfterRelog`, `legacyUndeployDelayDoesNotBlockUndeploy`: "capsule should be undeployed"; `activatedCapsuleTimesOutAfterRelog`: "activation should time out" |
| [72](https://github.com/Lythom/capsule/issues/72) | ItemPhysic compatibility | DOUBT | | |
| [71](https://github.com/Lythom/capsule/issues/71) | Invalid resource path on GDLauncher (1.18.2) | OBSOLETE (related fix 5bd81ee) | | |
| [70](https://github.com/Lythom/capsule/issues/70) | Schematics not loading | CONFIRMED-BACKLOG | | |
| [69](https://github.com/Lythom/capsule/issues/69) | Shaders: invisible preview / black screen | DOUBT | | |
| [68](https://github.com/Lythom/capsule/issues/68) | Startup crash with Snow! Real Magic | OBSOLETE | | |
| [56](https://github.com/Lythom/capsule/issues/56) | ProjectRed wires free in blueprints | CONFIRMED-FIXED | 86600be | `pottedPlantsAreNotFree`: "a flower pot should be required, got {}" |

Totals: 13 CONFIRMED-FIXED (#91 counted once), 4 CONFIRMED-HARDENED, 1 CONFIRMED-BACKLOG (+ #91 part 2),
15 OBSOLETE, 5 ENHANCEMENT, 7 DOUBT.

## Bugs found without an issue

Found while writing the tests and the client smoke test; each has its own commit, with a test that failed before the
fix or, for client rendering, before/after screenshots.

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

## Ready-to-post comments

### #126
Thanks for the report. The starter huts are 3×3×3 on purpose, so the roof is low by design. The message
"Unable to open. Break and replace to use." does not come from Capsule or vanilla: it comes from another mod that
needs its own block entity on crafting tables. Which crafting table mod do you have? In the meantime, the axe item
frame hangs on the only reachable face of the table, so right clicks may hit the frame. I am keeping this open to
check the hut against that mod.

### #125
Fixed in the next 1.21.1 build: the path check rejected any install folder named like a reserved Windows device
(`com.atlauncher.ATLauncher` matches `COM`). Only the template part of the path is checked now, and a bad path is
reported instead of crashing the server.

### #124
I could not reproduce this from the code, and the zip is no longer downloadable. On 1.21.1 a prefab blueprint whose
source template cannot be read silently becomes an empty blueprint, which matches "nothing happens, nothing in the
logs". Could you attach the prefab files again (and the Capsule version)? I'll make that case report an error.

### #123
Good idea, tracked in the backlog: accept Loyalty as an equivalent of Recall and make capsules fire/lava proof.
Recall stays registered so existing enchanted capsules keep working.

### #122
Fixed in the next 1.21.1 build. The experience stored in a captured furnace is no longer dropped on capture; it
stays in the furnace and is awarded when you take its output after deploying.

### #121
Tested on 1.21.1 with Waystones 21.1.46: Waystones now tags its blocks `c:relocation_not_supported`, which Capsule
excludes, so capsules (overpowered ones included) leave waystones in place, with their data. A door next to a waystone
is captured, deployed and undeployed with both halves and no ghost block (automated test with Waystones). I could not
test the 1.20.1 build; if it still happens there, a world or a log would help. Closing for 1.21.1.

### #120
Tracked as an enhancement. Note that 13×13×13 is already reachable with vanilla items: an emerald capsule (11) plus
one upgrade (popped chorus fruit). Also in the next build, the addon recipes (platinum, tin...) are only loaded when
a mod provides the ingot, so recipe viewers stop showing capsules you cannot craft.

### #119
For 1.21.1: SecurityCraft tags its blocks `c:relocation_not_supported`, which Capsule excludes from captures, so
nobody can capture them. The next build also restores the owner check for SecurityCraft blocks outside that tag
(only the owner can take them). There will be no new 1.19.2 build; a backport to 1.20.1 is in the backlog.

### #118
The NeoForge 1.21.1 version is released (1.21.1-9.0.117 on CurseForge and Modrinth). Closing.

### #117
I could not test with Ad Astra, but the likely cause is fixed in the next 1.21.1 build: blocks whose code expects a
real level while the preview is built no longer crash the client; the block is skipped, or the wireframe preview is
shown instead. Please report back if it still crashes on 1.21.1, with the crash report.

### #116
Fixed in the next 1.21.1 build: aiming at a snow layer, grass or another replaceable block now deploys the content in
its place, like a placed block, instead of one block above.

### #115
Fixed in the next 1.21.1 build. Starter copies shared their block entity data with the cached template; a barrel
keeping that data as its storage (Sophisticated Storage) emptied every other copy. Each copy now gets its own data.
Checked with Sophisticated Storage 1.6.1: a starter barrel deployed twice keeps its content in both copies.

### #113
Fixed since the 1.20.4 version: container entities are emptied before being removed during a capture. Closing.

### #112
Done: `capsule:excluded` includes `#c:relocation_not_supported` (optional, without replace). Closing.

### #109
Fixed in the next 1.21.1 build: capsule loot entries have their own loot entry type and codec, so mods that
serialize loot tables (Roughly Enough Resources and similar) no longer fail with "Invalid player data".
Also fixed: on 1.21.1 the default loot table list never matched, so capsules were not added to dungeon chests at all.
Configs generated by previous 1.21.1 versions are read correctly, no need to delete them.

### #108
The log only shows an audio thread error, not the deploy error, and 1.19.2 is no longer maintained. 1.21.1 now has an
automated test deploying every bundled template. If this happens on 1.21.1, please attach the full server log (the
line after "Couldn't deploy the capsule").

### #106
Nice idea, tracked in the backlog as a client-side animation with a config option to disable it.

### #101
Tracked in the backlog: refresh the blueprint whitelist for 1.21 block entities (campfire, signs, banners, heads...)
and find a way to update existing config files.

### #100
The castle kit was fixed in 1.20.4; the uncommon well still had 4 infested blocks and is fixed in the next 1.21.1
build. Existing worlds keep the templates copied in `config/capsule/loot`: delete that folder to get the updated
ones.

### #99
Fixed since 1.20.4: the books and chests use blue dye, like the recipe. Existing installs keep the old templates in
`config/capsule/loot` until that folder is deleted. Closing.

### #98
Fixed in the next 1.21.1 build: a capsule with Recall that hits a ceiling or a wall right after being thrown was
brought back before it could deploy. Recall now waits until the capsule can deploy.

### #97
Not reproducible on 1.21.1: enchanted capsules match the recovery recipe and keep their enchantment. Closing.

### #96
Behavior is correct but scanning every item each tick is wasteful; tracked in the backlog (track recall item
entities when they spawn instead).

### #94
I could not test with Integrated Dynamics, but the preview is now protected against blocks whose code expects a real
level (next 1.21.1 build). If it still crashes on 1.21.1, please attach the crash report.

### #93
Fixed since 1.20.x (absolute template paths). 1.16.5 is no longer maintained. Closing.

### #91
Part of this is fixed in the next 1.21.1 build: the server now refuses instant capture/deploy requests for capsules
that are not instant, and positions out of reach. Firing a block break event for every captured block, so claim mods
can veto captures, needs more design (cost on large captures, side effects on other mods, capture bases without a
player) and is tracked in the backlog.

### #90
Fixed in the next 1.21.1 build: Recall can be obtained from enchanting tables (and trades/loot through
`#minecraft:non_treasure`). The `recallEnchantType` and `recallEnchantRarity` options have no effect since 1.21;
use a datapack (`#capsule:enchantable/recall` item tag, `data/capsule/enchantment/recall.json`) instead.

### #89
Fixed in the next 1.21.1 build: a capsule thrown without preview target deployed one block above the ground.

### #88
The preview renderer was rewritten since 1.18.2. On 1.21.1 the full preview draws the textured blocks where the
content will deploy, opaque, so it hides what is behind it; the next build also fixes walls, fences and other multipart
blocks missing from the preview on NeoForge, and wrong previews right after switching capsules. Could you tell whether
the glitch on your screenshot still happens on 1.21.1?

### #85
The 1.12.2 asset copy and this template no longer exist. Related: in the next 1.21.1 build, template files with names
that are not valid ids (uppercase, spaces, apostrophes) are skipped with a warning instead of breaking logins.
Closing.

### #84
The book was fixed (it says blue dye). I could not find a dupe in the 1.21.1 blueprint recipe: button, dye and paper
are consumed and the source capsule is kept. Tracked in the backlog as a crafting test. If you can reproduce it on
1.21.1, please describe the exact steps.

### #83
The 1.21.1 fluid preview uses the current NeoForge API; this was a binary incompatibility of the 1.19 build. Closing.

### #82
Duplicate of #71, fixed since 1.20.x. Closing.

### #81
The cause (block code expecting a real level in the preview) is guarded in the next 1.21.1 build: such blocks are
skipped, or the wireframe preview is shown, instead of crashing the client.

### #80
1.19 is no longer maintained; the current version targets NeoForge 1.21.1 and now declares its supported Minecraft
range, so it cannot be loaded on an incompatible version. Closing.

### #78
The code iterating every block material was removed in 1.20.1. The next 1.21.1 build also stops the config from
crashing on invalid block ids and makes tags work in `excludedBlocks`. Closing.

### #77
Duplicate of #71 (GDLauncher path), fixed since 1.20.x. Closing.

### #76
Vanilla farmland is safe in the 1.21.1 preview; the crash probably came from a modded farmland block. The preview is
protected against that kind of block in the next 1.21.1 build. Please report back with a crash report if it still
happens.

### #75
Fixed in the next 1.21.1 build: the undeploy delay and the activation timer use the world time instead of the player
tick counter, which restarts on relog. Capsules deployed with an older version can be undeployed again.

### #72
Not tested yet: deploying relies on the item entity collision, which ItemPhysic replaces. Tracked in the backlog for
a test with ItemPhysic on 1.21.1.

### #71
Fixed since 1.20.x, and in the next 1.21.1 build the path check also ignores the install path itself (an install
folder named like a reserved Windows name or ending with a dot works). Closing.

### #70
Sponge v2 schematics load, but v3 (WorldEdit 7.3+ `.schem` files) do not: different root, no `PaletteMax`, nested
block entity data, and the `.schem` extension is ignored. Tracked in the backlog.

### #69
OptiFine does not exist for NeoForge 1.21.1; tracked in the backlog to test the preview with Iris shaders.

### #68
The code that triggered this was removed in 1.20.1, and Capsule no longer touches other mods' blocks during
startup. Closing.

### #56
Fixed in the next 1.21.1 build: blocks without an item (potted plants, attached stems...) are no longer free in
blueprints; potted plants cost the pot and the plant, other blocks cost the item they are picked as.
