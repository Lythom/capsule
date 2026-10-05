# Backlog

Work identified on the dev-1.21.1 branch and not done there. Effort: S (hours), M (a day or two), L (several days).
Triage details and ready-to-post issue comments are in `docs/ISSUE_TRIAGE.md`.

## Confirmed bugs not fixed

### Claim mods: veto captures and deploys per block
Issues: https://github.com/Lythom/capsule/issues/91

The server now validates throw requests (part 1, commit 65036a2). Block removal is still only checked with a
simulated `EntityPlaceEvent` (dirt) per position, never a `BlockEvent.BreakEvent`, so protection mods listening to
break events only are bypassed; deploy checks also use a dirt state instead of the placed state.

Analysis: firing a real `BreakEvent` per captured block lets claim mods veto, but also triggers every other
listener: quest and statistics mods, vein miners, XP and drop modifiers, and it costs one event per block on 255³
captures. Capture bases (dispenser path) have no player at all. Design options: an opt-in config, a per-chunk probe
(one event per chunk or claim boundary), or a dedicated Capsule event; for the dispenser path, store the placer UUID on
the capture base block entity and use a fake player with it, or a `captureBaseRespectsProtection` option.
Test sketch: a test-only listener cancelling `BreakEvent` in a box; capture must leave the block. Effort: M.

### Sponge v3 schematics (`.schem`)
Issues: https://github.com/Lythom/capsule/issues/70

`CapsuleTemplate.readSchematic` handles MCEdit and Sponge v2. Sponge v3, the WorldEdit 7.3+ default, fails: the root
is wrapped in `Schematic`, `PaletteMax` no longer exists, block entity and entity payloads are nested under `Data`,
entity ids are `Id` (also wrong for v2), and `.schem` files are ignored by `CapsuleTemplateManager.getTemplate` and
`Files.iterateTemplates`. Format support, not a regression. Needs fixture files exported by WorldEdit 7.3 (v2 and v3,
with a chest and an armor stand) and a test loading and deploying them. Effort: M.

## Needs verification (DOUBT and hardened issues)

### Crafting table blocked in the starter huts
Issues: https://github.com/Lythom/capsule/issues/126

The huts are 3×3×3 by design. The message "Unable to open. Break and replace to use." comes from a third-party
crafting table mod (FastWorkbench-like) needing its own block entity; the axe item frame of the birch hut also hangs
on the only reachable face of the crafting table. Check: deploy `_stater_birch_hut` with that mod and right click the
table. Cheap improvement: move the frame off the table in the 5 huts. Effort: S.

### Prefab blueprints silently empty
Issues: https://github.com/Lythom/capsule/issues/124

`CapsuleTemplateManager.getOrCreateTemplate` never returns null: when a prefab's source template cannot be read
(wrong case, unknown blocks, schematic conversion failure), the crafted blueprint gets an empty template and "does
nothing" without any error. Make `StructureSaver.createBlueprintTemplate` report `blueprintCreationError` and log the
path when the source palette is empty; ask the reporter for the files. Effort: S.

### Full preview with modded blocks (Ad Astra, Integrated Dynamics, farmland mods, Mob Grinding Utils)
Issues: https://github.com/Lythom/capsule/issues/117, https://github.com/Lythom/capsule/issues/94,
https://github.com/Lythom/capsule/issues/76, https://github.com/Lythom/capsule/issues/81

Hardened in commit 6c5aed7 (per-block guards, wireframe fallback) but not verified with the mods. Check in a dev
client with each mod. Longer term: move the preview world building out of `CapsuleTemplateRenderer` (it needs
`Minecraft.getInstance()`) so a GameTest can build a preview containing a test block whose `updateShape` casts to
`Level`. Effort: S per mod, M for the testable refactor.

### Deploy failure reports
Issues: https://github.com/Lythom/capsule/issues/108

No usable log (1.19.2). Every bundled template now deploys in `everyBundledTemplateDeploys`; close as
cannot-reproduce unless a 1.21.1 log shows up. Effort: S.

### Full preview opacity and occlusion
Issues: https://github.com/Lythom/capsule/issues/88

Checked on 1.21.1 with the client smoke test (`docs/MANUAL_VALIDATION.md`): the full preview draws textured blocks with
a solid shader and vertex alpha 1, so it is opaque and hides what is behind it. Consider the translucent shader with a
lower vertex alpha (sorting artifacts to check), or a lighter tint. Also: four starter files are named `_stater_*`
(labels show "Stater"). Effort: S to M.

### Blueprint crafting dupe and prefab refund slots
Issues: https://github.com/Lythom/capsule/issues/84

No dupe found in `BlueprintCapsuleRecipe` on 1.21.1. Latent bug: `PrefabsBlueprintAggregatorRecipe` refunds the
hard-coded grid slots 4, 0 and 2, which only match the default `prefab_blueprint_recipe.json` layout although the file
invites pack makers to move the ingredients. Compute the refund slots from the pattern. Test: craft through a
`CraftingMenu` with a mock player, default and shuffled layouts. Effort: S to M.

### ItemPhysic compatibility
Issues: https://github.com/Lythom/capsule/issues/72

Thrown capsules deploy from `Item#onEntityItemUpdate` and the item entity collision flags, which ItemPhysic replaces.
Check with ItemPhysic on 1.21.1; fallback idea: track thrown capsules per level and deploy on `onGround()` or after a
timeout. Effort: M.

### Preview with shaders (Iris)
Issues: https://github.com/Lythom/capsule/issues/69

OptiFine does not exist on NeoForge 1.21.1. Test the preview with Iris and a common shader pack. Hypotheses: the
custom render type drawn at `AFTER_TRANSLUCENT_BLOCKS` is ignored by shader pipelines (flush it explicitly or render
later, or fall back to wireframe when Iris is loaded). The preview works with Sodium without shaders (client smoke test
with the Create: OneBlock pack). Effort: M.

## Enhancements

### Vanilla Loyalty as Recall, fire-proof capsules
Issues: https://github.com/Lythom/capsule/issues/123

Accept `minecraft:loyalty` wherever `capsule:recall` is checked (`CapsuleEnchantments.hasRecallEnchant`), allow it on
capsules through `supportsEnchantment` rather than the trident tag (which would also allow riptide and channeling),
and make capsules `fireResistant()`. Keep `capsule:recall` registered for existing items. Effort: S to M.

### Vanilla base 13³ capsule
Issues: https://github.com/Lythom/capsule/issues/120

13³ is already reachable in vanilla (emerald 11 + 1 upgrade). A base recipe could use netherite or amethyst, or a
platinum fallback recipe guarded by `neoforge:tag_empty`. Addon recipes are already hidden when their ingot is
missing (commit 55c7f6f). Effort: S (JSON only).

### "Sucked in" capture animation
Issues: https://github.com/Lythom/capsule/issues/106

On `CapsuleUndeployNotifToClient`, render the captured template shrinking toward the capsule for 10-15 ticks with
`CapsuleTemplateRenderer` (wireframe above a block count threshold), plus a particle trail; client config option
`captureAnimation`. Effort: M.

### Blueprint whitelist for 1.21 block entities
Issues: https://github.com/Lythom/capsule/issues/101

Add campfires, signs and hanging signs (`front_text`, `back_text`, `is_waxed`), banners, heads, undyed shulker box,
ender chest, decorated pot, etc.; remove 1.12 ids. Existing configs never get updates (the file is only copied when
missing): merge defaults with a version marker, or use a `capsule:blueprint_whitelist` block tag. Effort: S for the
list, M with migration.

### Recall: track item entities instead of scanning
Issues: https://github.com/Lythom/capsule/issues/96

`RecallEnchant.onWorldTickEvent` scans every item entity of every level each tick and resolves the enchantment holder
per item. Track recall item entities when they join the level and cache the holder. Effort: S.

## Platforms and versions

### Forge 1.21.1 module
Not built on purpose (Modrinth 1.21.1 modpacks: Forge 25, NeoForge 2111, Fabric 3032; see `docs/RESEARCH.md`). Cost if
wanted: one more platform module next to `neoforge` and `fabric`, built with ForgeGradle 6 as in the
MultiLoader-Template `1.21.1` branch (ModDevGradle legacyforge stops at 1.20.1), Forge implementations of the platform
interfaces (registries, networking, events, config, capabilities), and its own GameTest and production-jar smoke run.
Effort: M to L.

### Backport the confirmed fixes to 1.20.1
1.20.1 Forge is Capsule's most downloaded version (369k on Modrinth). Most fixes of this branch apply there: #125 path
check, #122 experience orbs, #115 template copies, #109 loot entry type, #100 template, #98 recall, #90 enchanting
table tag (if 1.20.1 still uses the old enchantment API, through `canApplyAtEnchantingTable`), #89 blind deploy, #75
undeploy delay, #56 item-less blocks, #116 deploy position, #91 request validation, N1/N5 template names, N3 config,
N6 capture base, reload listener, preview hardening. Either cherry-pick on the 1.20.1 branch or, better, build 1.20.1
from the multi-version setup below. Effort: M (cherry-picks) to L (with tests).

### Fabric parity gaps
Everything automated passes on Fabric (10 unit tests, the 41 common GameTests, the client smoke test, the production
server smoke test).
Not verified on Fabric:
- JEI: the plugin is declared through the `jei_mod_plugin` entrypoint but never loaded in a test. JEI for Fabric
  1.21.1 is built with Loom 1.18, which Loom 1.17 refuses as a dependency; Loom 1.18 needs Gradle running on Java 25.
  Check: Java 25 for Gradle, Loom 1.18, `modLocalRuntime` JEI, look at the capsule recipes in a dev client.
- Client side is covered by the client smoke test on Fabric too (previews, rotation by left click on a block, recall
  box, capture base renderer and highlight, item colors and states, creative tab contents from the synced recipes),
  except the label GUI and left click in the air (`Minecraft#startAttack` mixin).
- Protection: claims are probed through Common Protection API (jar-in-jar), only for mods implementing it, and only
  with a player: capture bases (dispenser path) are not checked. On NeoForge the dirt `EntityPlaceEvent` probe is
  posted even without a player.
- Blueprint material sources use Transfer API storages; a storage that is not slotted is read as its list of views,
  which a modded storage may reorder between the listing and the extraction. Check with a modded storage (e.g.
  a Fabric storage mod) linked to a blueprint.
Effort: S to M per point.

### GameTests against the release jars
`scripts/prod-smoke.sh` boots the release jars but cannot run the GameTests there: NeoForge only registers GameTests
outside production (`GameTestHooks.isGametestEnabled`), and the tests are not in the jars. Fabric could: a small
`capsule-gametest` jar (the fabric gametest source set) plus `fabric-gametest-api-v1` in the smoke server's mods
folder, started with `-Dfabric-api.gametest`. Effort: S (Fabric), M (NeoForge, needs a hook to enable GameTests).

### Update checker URL
`neoforge.mods.toml` points `updateJSONURL` to `mc-curse-update-checker.herokuapp.com`, which no longer answers: every
NeoForge start logs "Failed to process update information". Remove it or publish an update JSON. Effort: S.

### REI and EMI on Fabric
JEI is the only recipe viewer plugin. On Fabric, REI and EMI are common; add plugins showing capsule recipes, prefab
blueprints and the recovery/upgrade/dye special recipes. Effort: M.

### Multi-version builds with Stonecutter
Targets from `docs/RESEARCH.md`: 1.20.1 (Forge + Fabric), 1.21.1 (NeoForge + Fabric), latest 26.x (NeoForge +
Fabric; Java 25, unobfuscated, `ResourceLocation` renamed `Identifier` from 1.21.11). Stonecutter 0.9.x on top of the
multiloader layout, one build script per loader (ModDevGradle, ModDevGradle legacyforge for Forge 1.20.1, Loom for
Fabric), version-specific code behind `//? if` comments. Effort: L.

## Other findings

### Capsule tooltips end with "Dyed"
The base color is a `minecraft:dyed_color` component created with `show_in_tooltip` true (`MinecraftNBT.setColor`), so
every capsule made by commands, loot or dyeing lists the vanilla "Dyed" line. Create it with `show_in_tooltip` false;
existing items keep their flag. Effort: S.

### Capture bases show the redstone state
The capture base highlight (activated top while an empty capsule is held) is the dispenser `triggered` property, also
set by redstone: a powered capture base looks activated. A client side flag on `BlockEntityCapture` read by a model
property or by `CaptureBER` would separate them. Effort: S to M.

### Client smoke test: JEI on Fabric, modpacks with Sinytra Connector
The client smoke test skips JEI on Fabric (not loadable with Loom 1.17), and Sinytra Connector cannot start in a dev
client ("Could not determine clean minecraft artifact path"), so Connector packs need a production client (for example
HeadlessMC with the release jar). Effort: M.

### Updated default templates never reach existing installs
`Files.populateFolder` only copies `initialconfig` templates when the target folder does not exist, so fixes to
bundled templates (#100, #99) need players to delete `config/capsule/loot`. Option: store a hash of copied files and
replace unmodified ones. Effort: S to M.

### Deploying a reward capsule rewrites its template file
`StructureSaver.deploy` writes the template (with occupied positions) before deploying, including reward templates in
`config/capsule/rewards` or `config/capsule/starters`. Harmless today but surprising for pack makers; rewards could
skip the write. Effort: S.

### Item frames log "Block-attached entity at invalid position"
Deploying templates with item frames logs this error once per frame: the entity is created from its saved `TileX/Y/Z`
before being moved. Update the tile position in the entity NBT before creating it. Effort: S.
