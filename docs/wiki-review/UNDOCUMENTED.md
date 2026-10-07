# Features missing from the wiki (review notes, not published)

Compared on 2026-10-07: the wiki at commit 2ff5387 of `capsule.wiki.git` against the `dev-1.21.1` code (`Config.java`,
`ClientConfig.java`, `CapsuleCommand.java`, `data/capsule/recipe`, `data/capsule/tags`, `en_us.json`, `CapsuleItem`,
the recipes, the dispenser behavior, `plugins/claims`). "Drafted in" points to the section written on this branch
(`docs/wiki/`), to review in the diff. The 9.0 features (Loyalty, fire proof, tiers, translucent preview, capture
animation, claims, REI/EMI, Fabric, Sponge v3, whitelist update) are not repeated here: they are new, see the 9.0
changelog page.

## Player (Home)

| Feature | What / where in the code | Drafted in |
|---|---|---|
| Tier table | Every material and its size, vanilla and modded (`data/capsule/recipe/capsule_*.json`, `addons_capsule_*.json`, `docs/RECIPES.md`); the wiki only listed iron/gold/diamond with wrong sizes (1/3/5 instead of 3/5/7) | Home#capsule-tiers |
| Capture Base is directional | Since 1.16.5-5.0.70 it is a dispenser: captures in front of its face, placed facing up when placed looking down (`BlockCapsuleMarker extends DispenserBlock`, `Spacial.getAnchor`); highlighted with a wireframe when an empty capsule is held | Home#getting-bigger |
| Current Capture Base recipe | Cobblestone, compass, torch, dispenser (`captureblock.json`); the wiki image shows the pre-1.16.5 recipe | Home#getting-bigger (text + media placeholder) |
| Activation duration | A right click activates the capsule for 6 s (`previewDisplayDuration`, 120 ticks) | Home#empty-capsule |
| Preview range and blind throws | Preview up to 18 blocks + capsule size (`Spacial.PREVIEW_REACH`); a capsule not aimed at a block deploys where it lands | Home#linked-capsule |
| Main hand only | Off-hand use is refused (`CapsuleItem.use`) | Home#getting-started |
| Deployed capsule shows its content's location | A box around the deployed content while held (`tryPreviewRecall`) | Home#linked-capsule |
| Deploy rules | Blocks in the way shown in red and block the deploy; mobs block it ("in the way"), players are pushed up (`placePlayerOnTop`); deploy on liquid surfaces unless underwater (`Spacial`); pre-existing blocks are not taken back | Home#linked-capsule |
| What is captured | Blocks with content, non-living entities and armor stands; never mobs, players or dropped items (`CapsuleTemplate.takeEntitiesFromWorld`) | Home#what-goes-into-a-capsule |
| Labels only on non-empty capsules | Sneak + right click on linked, deployed, one-use and blueprint capsules (`CapsuleItem.use`); the wiki said "all capsules" | Home#empty-capsule |
| Upgrade math | +2 per popped chorus fruit, several at once, empty capsules only, limit `capsuleUpgradesLimit` (`UpgradeCapsuleRecipe`) | Home#upgrading |
| Emptying recipe | A capsule alone in the grid: linked → empty + one-use copy of the content; deployed → empty, content stays (`ClearCapsuleRecipe`) | Home#emptying-a-capsule |
| Recovery recipe details | Capsule + glass bottle, original given back; deploying the recovery capsule empties the original | Home#backup |
| Blueprint crafting recipe | Capsule with content, blue dye ×2, stone button, paper; source capsule given back (`blueprint.json`) | Home#blueprints |
| Blueprint change recipe | Blueprint + another capsule with content replaces its structure (`BlueprintChangeRecipe`, JEI text "Craft with any capsule to update structure") | Home#blueprints |
| Unlinking an inventory | Sneak + right click the linked inventory again (`CapsuleItem.useOn`) | Home#blueprints |
| Blueprint undo conditions | Area must match the blueprint, no items in inventories (`capsule.error.blueprintDontMatch`) | Home#blueprints |
| Prefab blueprints for players | Ready-made blueprint recipes (castle parts, chicken cooker) in the recipe viewers | Home#blueprints |
| Loot, starter and reward capsules (player view) | Where they come from, one-use behavior, loot as pre-charged blueprints | Home#reward-loot-and-starter-capsules |
| Dispenser and Capture Base automation | A redstone signal deploys a linked/one-use capsule in front, the next one undeploys it (`DispenseCapsuleBehavior`, since 1.16.5) | Home#automation-with-dispensers |
| Recipe viewer info pages | Capsule information pages in JEI (REI/EMI since 9.0) | Home#recipe-viewers |
| Overpowered capsule recipe and size | Iron ingots + nether star, size 1 (`capsule_op.json`); bedrock is excluded for OP capsules too now | Home#overpowered-capsules |
| Where templates are stored | `<world>/capsules`, `C-`/`B-` prefixes (`StructureSaver`) | Home#faq |
| Beds and respawn | From the 1.12.2 showcase captions | Home#faq |

## Modpack maker (Modpack-making)

| Feature | What / where in the code | Drafted in |
|---|---|---|
| Configuration reference | Every key of `capsule-common.toml` with section and default (`Config.java`): `lootTablesList`, `lootTemplatesPaths`, `starterMode`, `starterTemplatesPath`, `prefabsTemplatesPath`, `rewardTemplatesPath`, `allowBlueprintReward`, `allowMirror`, `previewDisplayDuration`, `capsuleUpgradesLimit`, `excludedBlocks`, `opExcludedBlocks`, the unused `recall*` keys | Modpack-making#configuration |
| Client config | `capsule-client.toml` `captureAnimation` (`ClientConfig.java`) | Home#client-options, Modpack-making#configuration |
| Default loot tables and weights | 24 vanilla chest loot tables, folders common/uncommon/rare 10/6/2, any loot table id works | Modpack-making#loots |
| `allowBlueprintReward` | Loot without entities given as pre-charged blueprints (`CapsuleLootEntry`) | Modpack-making#loots |
| `starterMode` and disabling starters | `all` / `random` / `none`, empty path disables (`StarterLoot`) | Modpack-making#starters |
| `allowMirror` | Disable mirroring for multiblocks | Modpack-making#about-rotation |
| Excluded blocks: current defaults, mod prefixes, tags | `Config.configureCapture`, `Serialization` (`ic2:` prefix form, `#tag` or bare tag, invalid ids ignored); the wiki showed the 1.12 `capsule.cfg` values | Modpack-making#excluded-blocks |
| Tags | `capsule:excluded` (incl. `c:relocation_not_supported`, `c:immovable` since 1.20.4), `capsule:overridable`, item tag `capsule:enchantable/recall`; singular folders since 1.21 | Modpack-making#tags, Getting-compatible-with-capsule |
| Datapack recipes | Every recipe in `data/capsule/recipe/` can be overridden; size in the result's custom data; upgrade ingredient in `upgrade.json`; modded tiers load only when their `c:ingots/*` tag is filled | Modpack-making#recipes |
| Disabling Loyalty on capsules | Datapack emptying `capsule:enchantable/recall` | Modpack-making#loyalty |
| Template file rules | Formats (`.nbt`, `.schematic`, `.schem`), allowed characters, subfolders, `/reload`, defaults never updated (delete folders), reward deploy rewrites the template (`Files`, `CapsuleTemplateManager.EXTENSIONS`) | Modpack-making#template-files |
| Prefab recipe pattern and mod subfolders | Default pattern `2b3/l1l/ p `, subfolder = required mod id (`Blueprint.getModEnabledTemplates`), default prefab list | Modpack-making#add-a-preconfigured-blueprint |
| `setYOffset` usage | Deployment offset, also used by dispensers (`DispenseCapsuleBehavior`) | Modpack-making#setting-a-deployment-offset, Commands#setyoffset |
| `/capsule help` | Lists usable commands with a link to the wiki | Commands#help |
| `/capsule reloadWhitelist` | Reloads the whitelist, starters and loot list | Commands#reloadwhitelist |
| Command permissions | Level 2 except `help` and `downloadTemplate` (level 0); given capsules are dropped at the player's feet | Commands |
| Data components (1.20.5+) | `minecraft:custom_data` and `minecraft:dyed_color`; give syntax; enchantments outside custom data | Modpack-making#exporting-the-item-nbt, #capsule-nbt-data-reference |
| NBT keys | `state` values, `undeployAt`, `yOffset`; `oneUse` (the wiki said `onUse`) | Modpack-making#capsule-nbt-data-reference |
| Claims for server owners | Per block up to 31, per chunk column above, Capture Base `placer`, nobody refused, fail closed, FTB Chunks/Cadmus not on Fabric | Modpack-making#claims-and-protection |
| Claim API for mod developers | `ClaimAdapter`, `Claims.register`, `Claims.load`, the `[Capsule]` anonymous fake player | Getting-compatible-with-capsule#3-if-your-mod-protects-areas-claims |

## Found while comparing (bugs or doubts for the owner, not documented as features)

- `exportHeldItem` prints `/give @p capsule:capsule{...}` (pre-1.20.5 syntax) on 1.21.1 and leaves out the other
  components (dyed color, enchantments): the printed command does not work. `exportSeenBlock` prints `{BlockEntityTag:…}`,
  also obsolete. TODO comments left in Commands and Modpack-making.
- `fromHeldCapsule` has no `executes()` without its argument on 1.21.1: the output name is required, while the wiki
  said it defaults to the label.
- `setAuthor` needs an argument (`""` removes the author); the old wiki said "omit it".
- With the default `capsuleUpgradesLimit` (10), a netherite capsule reaches 33 (13 + 20) in survival, above the
  "largest survival capsule is 31" assumption of the claim check (`Claims.PER_BLOCK_MAX_SIZE`): an upgraded netherite
  or platinum capsule is probed per chunk column instead of per block.
- `blueprint_whitelist.json` has `"seed": "LONG"` under `minecraft:structure_block`: a non-null value means "the
  item must carry a matching `seed`", probably not intended.
- The starter templates are named `_stater_*` (BACKLOG): labels show "Stater".
