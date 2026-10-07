# Features missing from the wiki (review notes, not published)

Compared on 2026-10-07: the wiki at commit 2ff5387 of `capsule.wiki.git` against the `dev-1.21.1` code (`Config.java`,
`ClientConfig.java`, `CapsuleCommand.java`, `data/capsule/recipe`, `data/capsule/tags`, `en_us.json`, `CapsuleItem`,
the recipes, the dispenser behavior, `plugins/claims`). "Drafted in" points to the section written on this branch
(`docs/wiki/`), to review in the diff; "Status" says whether it is documented now or left out, and why. The 9.0
features (Loyalty, fire proof, tiers, translucent preview, capture animation, claims, REI/EMI, Fabric, Sponge v3,
whitelist update) are not repeated here: they are new, see the 9.0 changelog page.

## Player (Home)

| Feature | What / where in the code | Drafted in | Status |
|---|---|---|---|
| Tier table | Every material and its size, vanilla and modded (`data/capsule/recipe/capsule_*.json`, `addons_capsule_*.json`, `docs/RECIPES.md`); the wiki only listed iron/gold/diamond with wrong sizes (1/3/5 instead of 3/5/7) | Home#capsule-tiers | documented, with the netherite recipe image |
| Capture Base is directional | Since 1.16.5-5.0.70 it is a dispenser: captures in front of its face, placed facing up when placed looking down (`BlockCapsuleMarker extends DispenserBlock`, `Spacial.getAnchor`); highlighted with a wireframe when an empty capsule is held | Home#getting-bigger | documented |
| Current Capture Base recipe | Cobblestone, compass, torch, dispenser (`captureblock.json`); the wiki image shows the pre-1.16.5 recipe | Home#getting-bigger | documented: JEI image regenerated (`images/recipes/capture-base.png`) |
| Activation duration | A right click activates the capsule for 6 s (`previewDisplayDuration`, 120 ticks) | Home#empty-capsule | documented |
| Preview range and blind throws | Preview up to 18 blocks + capsule size (`Spacial.PREVIEW_REACH`); a capsule not aimed at a block deploys where it lands | Home#linked-capsule | documented |
| Main hand only | Off-hand use is refused (`CapsuleItem.use`) | Home#getting-started | documented |
| Deployed capsule shows its content's location | A box around the deployed content while held (`tryPreviewRecall`) | Home#linked-capsule | documented |
| Deploy rules | Blocks in the way shown in red and block the deploy; mobs block it ("in the way"), players are pushed up (`placePlayerOnTop`); deploy on liquid surfaces unless underwater (`Spacial`); pre-existing blocks are not taken back | Home#linked-capsule | documented |
| What is captured | Blocks with content, non-living entities and armor stands; never mobs, players or dropped items (`CapsuleTemplate.takeEntitiesFromWorld`) | Home#what-goes-into-a-capsule | documented |
| Labels only on non-empty capsules | Sneak + right click on linked, deployed, one-use and blueprint capsules (`CapsuleItem.use`); the wiki said "all capsules" | Home#empty-capsule | documented |
| Upgrade math | +2 per popped chorus fruit, several at once, empty capsules only, limit `capsuleUpgradesLimit` (`UpgradeCapsuleRecipe`) | Home#upgrading | documented |
| Emptying recipe | A capsule alone in the grid: linked → empty + one-use copy of the content; deployed → empty, content stays (`ClearCapsuleRecipe`) | Home#emptying-a-capsule | documented |
| Recovery recipe details | Capsule + glass bottle, original given back; deploying the recovery capsule empties the original | Home#backup | documented |
| Blueprint crafting recipe | Capsule with content, blue dye ×2, stone button, paper; source capsule given back (`blueprint.json`) | Home#blueprints | documented |
| Blueprint change recipe | Blueprint + another capsule with content replaces its structure (`BlueprintChangeRecipe`, JEI text "Craft with any capsule to update structure") | Home#blueprints | documented |
| Unlinking an inventory | Sneak + right click the linked inventory again (`CapsuleItem.useOn`) | Home#blueprints | documented |
| Blueprint undo conditions | Area must match the blueprint, no items in inventories (`capsule.error.blueprintDontMatch`) | Home#blueprints | documented |
| Prefab blueprints for players | Ready-made blueprint recipes (castle parts, chicken cooker) in the recipe viewers | Home#blueprints | documented |
| Loot, starter and reward capsules (player view) | Where they come from, one-use behavior, loot as pre-charged blueprints | Home#reward-loot-and-starter-capsules | documented |
| Dispenser and Capture Base automation | A redstone signal deploys a linked/one-use capsule in front, the next one undeploys it (`DispenseCapsuleBehavior`, since 1.16.5) | Home#automation-with-dispensers | documented |
| Recipe viewer info pages | Capsule information pages in JEI (REI/EMI since 9.0) | Home#recipe-viewers | documented |
| Overpowered capsule recipe and size | Iron ingots + nether star, size 1 (`capsule_op.json`); bedrock is excluded for OP capsules too now | Home#overpowered-capsules | documented |
| Where templates are stored | `<world>/capsules`, `C-`/`B-` prefixes (`StructureSaver`) | Home#faq | documented |
| Beds and respawn | From the 1.12.2 showcase captions | Home#faq | documented |

## Modpack maker (Modpack-making)

| Feature | What / where in the code | Drafted in | Status |
|---|---|---|---|
| Configuration reference | Every key of `capsule-common.toml` with section and default (`Config.java`): `lootTablesList`, `lootTemplatesPaths`, `starterMode`, `starterTemplatesPath`, `prefabsTemplatesPath`, `rewardTemplatesPath`, `allowBlueprintReward`, `allowMirror`, `previewDisplayDuration`, `capsuleUpgradesLimit`, `excludedBlocks`, `opExcludedBlocks`, the unused `recall*` keys | Modpack-making#configuration | documented |
| Client config | `capsule-client.toml` `captureAnimation` (`ClientConfig.java`) | Home#client-options, Modpack-making#configuration | documented |
| Default loot tables and weights | 24 vanilla chest loot tables, folders common/uncommon/rare 10/6/2, any loot table id works | Modpack-making#loots | documented |
| `allowBlueprintReward` | Loot without entities given as pre-charged blueprints (`CapsuleLootEntry`) | Modpack-making#loots | documented |
| `starterMode` and disabling starters | `all` / `random` / `none`, empty path disables (`StarterLoot`) | Modpack-making#starters | documented |
| `allowMirror` | Disable mirroring for multiblocks | Modpack-making#about-rotation | documented |
| Excluded blocks: current defaults, mod prefixes, tags | `Config.configureCapture`, `Serialization` (`ic2:` prefix form, `#tag` or bare tag, invalid ids ignored); the wiki showed the 1.12 `capsule.cfg` values | Modpack-making#excluded-blocks | documented |
| Tags | `capsule:excluded` (incl. `c:relocation_not_supported`, `c:immovable` since 1.20.4, `forge:relocation_not_supported` in the next 1.20.1 / 1.18.2 / 1.16.5 builds), `capsule:overridable`, item tag `capsule:enchantable/recall`; singular folders since 1.21 | Modpack-making#tags, Getting-compatible-with-capsule | documented |
| Datapack recipes | Every recipe in `data/capsule/recipe/` can be overridden; size in the result's custom data; upgrade ingredient in `upgrade.json`; modded tiers load only when their `c:ingots/*` tag is filled | Modpack-making#recipes | documented |
| Disabling Loyalty on capsules | Datapack emptying `capsule:enchantable/recall` | Modpack-making#loyalty | documented |
| Template file rules | Formats (`.nbt`, `.schematic`, `.schem`), allowed characters, subfolders, `/reload`, defaults never updated (delete folders), reward deploy rewrites the template (`Files`, `CapsuleTemplateManager.EXTENSIONS`) | Modpack-making#template-files | documented |
| Prefab recipe pattern and mod subfolders | Default pattern `2b3/l1l/ p `, subfolder = required mod id (`Blueprint.getModEnabledTemplates`), default prefab list | Modpack-making#add-a-preconfigured-blueprint | documented |
| `setYOffset` usage | Deployment offset, also used by dispensers (`DispenseCapsuleBehavior`) | Modpack-making#setting-a-deployment-offset, Commands#setyoffset | documented |
| `/capsule help` | Lists usable commands with a link to the wiki | Commands#help | documented |
| `/capsule reloadWhitelist` | Reloads the whitelist, starters and loot list | Commands#reloadwhitelist | documented |
| Command permissions | Level 2 except `help` and `downloadTemplate` (level 0); given capsules are dropped at the player's feet | Commands | documented |
| Data components (1.20.5+) | `minecraft:custom_data` and `minecraft:dyed_color`; give syntax; enchantments outside custom data | Modpack-making#exporting-the-item-nbt, #capsule-nbt-data-reference | documented; export commands now print the 1.21 component syntax |
| NBT keys | `state` values, `undeployAt`, `yOffset`; `oneUse` (the wiki said `onUse`) | Modpack-making#capsule-nbt-data-reference | documented |
| Claims for server owners | Per block up to 31, per chunk column above, Capture Base `placer`, nobody refused, fail closed, FTB Chunks/Cadmus not on Fabric | Modpack-making#claims-and-protection | documented; per-block limit now the largest survival capsule from the config (33 by default on 1.21.1, 31 on the Forge backports) |
| Claim API for mod developers | `ClaimAdapter`, `Claims.register`, `Claims.load`, the `[Capsule]` anonymous fake player | Getting-compatible-with-capsule#3-if-your-mod-protects-areas-claims | left out: the claim API stays internal (owner decision); only the supported mods, the placement event / Common Protection API hooks and the `[Capsule]` fake player are documented |

## Found while comparing (bugs or doubts for the owner, not documented as features)

| Finding | Status |
|---|---|
| `exportHeldItem` printed `/give @p capsule:capsule{...}` (pre-1.20.5 syntax) on 1.21.1 without the other components; `exportSeenBlock` printed `{BlockEntityTag:…}` | fixed in 9.0 (both print the 1.21 component syntax); documented in Commands and Modpack-making, TODO comments removed |
| `fromHeldCapsule` had no `executes()` without its argument on 1.21.1 | fixed in 9.0 on 1.21.1 (falls back to the capsule label again; the Forge backports always required the name); documented in Commands |
| `setAuthor` needs an argument (`""` removes the author); the old wiki said "omit it" | documented in Commands |
| With the default `capsuleUpgradesLimit` (10) a netherite capsule reaches 33, above the claim check's per-block limit of 31 | fixed in 9.0 (limit computed from the config: largest tier + 2 × upgrades limit, 33 by default; 31 on the Forge backports, emerald 11); bigger boxes, checked per chunk column, are operator-only (owner decision); documented in Home, Modpack-making and Getting-compatible |
| `blueprint_whitelist.json` has `"seed": "LONG"` under `minecraft:structure_block` (a non-null value means "the item must carry a matching `seed`") | it asked for a structure block item holding the same seed: fixed in 9.0 and the Forge backports (`"seed": "LONG"`, new installs only), listed in the 9.0 changelog page |
| The starter templates are named `_stater_*`: labels show "Stater" | left out: renaming would break existing configs (see the #71 reply) |
| Default `excludedBlocks` entries from 1.12 that match nothing today (`superfactorymanager:`, `gregtech:machine`, `gtadditions:`, `bloodmagic:alchemy_table`, `mekanism:machineblock`, `mekanism:boundingblock`), from the incompatibility tests | documented in Known-incompatibilities and Modpack-making#excluded-blocks; removed from the 1.20.1 / 1.18.2 / 1.16.5 defaults (new installs get `bloodmagic:alchemytable`, the Immersive Engineering connectors and `waystones:`; `#forge:relocation_not_supported` and the optional entries of `capsule:excluded` now apply there); the 1.21.1 defaults still list them, cleaning them is left to the owner |
