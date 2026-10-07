# Capsule in-game Commands #

These commands are only available with operator permissions (level 2), except `help` and `downloadTemplate`. They are designed to help content creators play around with Capsule and create whatever capsule they want.

**Parameters marked with a * have autocompletion available using the Tab key.**

Most commands act on the capsule held in the main hand. Commands that give a capsule drop it at the player's feet, ready to be picked up.

- [Capsule in-game Commands](#capsule-in-game-commands)
  * [help](#help)
  * [giveEmpty](#giveempty)
  * [giveLinked](#givelinked)
  * [giveBlueprint](#giveblueprint)
  * [exportHeldItem](#exporthelditem)
  * [exportSeenBlock](#exportseenblock)
  * [fromHeldCapsule](#fromheldcapsule)
  * [fromStructure](#fromstructure)
  * [fromExistingReward](#fromexistingreward)
  * [giveRandomLoot](#giverandomloot)
  * [reloadLootList](#reloadlootlist)
  * [reloadWhitelist](#reloadwhitelist)
  * [setBaseColor](#setbasecolor)
  * [setMaterialColor](#setmaterialcolor)
  * [setAuthor](#setauthor)
  * [setYOffset](#setyoffset)
  * [downloadTemplate](#downloadtemplate)

---

## help ##

List the Capsule commands you can use, with a link to this page.

```
/capsule help
```

---

## giveEmpty ##

Give an empty capsule.

```
/capsule giveEmpty [size] [overpowered]
```
* **size** Optional, default 3. Value must be an integer between 1 and 255. Even values will be rounded down.
* **overpowered** Optional, default false. Value must be true or false.

---

## giveLinked ##

[since 3.2.91]

Give a standard capsule with preloaded content.

```
/capsule giveLinked <reward_name> [playerName] [withLoyalty]
```
* **reward_name*** Required. Name of the structure reward to load from. The template is expected to be in `config/capsule/rewards`, use Tab for autocompletion.
* **playerName** Optional. Name of the player who will receive the reward.
* [since 9.0] **withLoyalty** Optional. Default: false. Add the Loyalty enchantment to the capsule if set to true. Example: `/capsule giveLinked my_house Steve true`.
* [since 7.0.91, also in the 1.16.5 and 1.18.2 builds, until 9.0] **withRecall** Optional. Default: false. Add the Recall enchantment to the capsule if set to true.

---

## giveBlueprint ##

[since 3.2.91]

Give an uncharged blueprint capsule with a preconfigured structure.

```
/capsule giveBlueprint <reward_name> [playerName]
```
* **reward_name*** Required. Name of the structure reward to load from. The template is expected to be in `config/capsule/rewards`, use Tab for autocompletion.
* **playerName*** Optional. Name of the player who will receive the reward.

---
## exportHeldItem ##

Print the /give command that would give the exact item in hand. The item can be any item (not limited to capsules).  
Tip: click on the result text in the chat to open the client log file and allow copy/paste of the command.

```
/capsule exportHeldItem
```

[since 9.0, Minecraft 1.21.1] The printed command uses the 1.21 data component syntax (`capsule:capsule[minecraft:custom_data={...},...]`) with the item's data components, ready for `/give`. See [Exporting the item NBT](Modpack-making#exporting-the-item-nbt).

---
## exportSeenBlock ##

```
/capsule exportSeenBlock 
```

Print the /give command that would give an item that would spawn the exact block + block entity you are looking at. The block can be any block (not limited to capsules). Only works in single player (integrated server).  
Tip: click on the result text in the chat to copy the command to the clipboard.

[since 9.0, Minecraft 1.21.1] The block entity data is printed in the 1.21 data component syntax (`minecraft:block_entity_data`) instead of the old `BlockEntityTag`.

---
## fromHeldCapsule ##

Create a reward template by copying the template from the held capsule into the configured reward folder (default `config/capsule/rewards`).
This command can be used to create a new Reward or to update an existing one.

A One Use Capsule linked to the created/updated reward template is given to the player.

**Warning**: If a template already exists with that name in the reward folder, it will be overwritten without confirmation.

```
/capsule fromHeldCapsule [outputName]
```
* **outputName** [since 9.0, 1.21.1] Optional, defaults to the capsule label. Required in 1.21.1-9.0.117 and in the 1.20.1, 1.18.2 and 1.16.5 builds. Name of the structure to create in the reward folder. Uppercase letters are lowercased, spaces become `_` and `:` becomes `-`.

---
## fromStructure ##

Create a new Reward Capsule by copying the template from a named structure block template (1.12 and older: `<worldsave>/structures/<structureName>.nbt`, 1.15 and newer: `<worldsave>/generated/minecraft/structures`) into the configured reward folder (default `config/capsule/rewards`).

This command can be used to create a new Reward or to update an existing one.

A One Use Capsule linked to the created/updated reward template is given to the player.

**Warning**: If a template already exists with that name in the reward folder, it will be overwritten without confirmation.

```
/capsule fromStructure <structure_name> [playerName]
```
* **structure_name*** Required. Name of the structure template to load from. The template is expected to be in the Minecraft default structure block saves folder, use Tab for autocompletion.
* [since 3.2.91] **playerName*** Optional. Name of the player who will receive the reward.

---
## fromExistingReward ##

Give a One Use Capsule linked to the reward template named <reward_name> from the configured reward folder (default `config/capsule/rewards`).

```
/capsule fromExistingReward <reward_name> [playerName]
```
* **reward_name*** Required. Name of the reward template to load from. The template is expected to be in `config/capsule/rewards`, use Tab for autocompletion.
* [since 3.2.91] **playerName*** Optional. Name of the player who will receive the reward.

---
## giveRandomLoot ##

Give the player a random Loot Capsule using the loot generation configuration.

```
/capsule giveRandomLoot [player]
```
* **player*** Optional, default is the current player. Name of the player to give the reward to.

---
## reloadLootList ##

Refresh the lootable capsules by reading the configured directories. If directories are added or removed, a server restart is required (or just a world restart client side).

```
/capsule reloadLootList
```

---
## reloadWhitelist ##

Reload `config/capsule/blueprint_whitelist.json`, the starter templates and the lootable capsules, without a restart.

```
/capsule reloadWhitelist
```

---
## setBaseColor ##

Specify the capsule base color.

```
/capsule setBaseColor <color>
```
* **color*** Required. Integer of the color, i.e. 0x5e8eb7. Use autocompletion for more example values.

---
## setMaterialColor ##

Specify the capsule material color (around the label).

```
/capsule setMaterialColor <color>
```
* **color*** Required. Integer of the color, i.e. 0x5e8eb7. Use autocompletion for more example values.

---
## setAuthor ##

Save an author for both the capsule and the associated template. The author will then be credited in the capsule description as "Designed by <author>".

```
/capsule setAuthor <author>
```
* **author** Author name, or `""` (empty quotes) to remove any author reference.

---
## setYOffset ##

[since 7.0.91, also in the 1.18.2 builds]

Configure a deployment Y offset on a capsule. Using the aimed position as reference, negative values will offset the deployment toward the ground and positive values toward the sky. The offset also applies when a dispenser or a Capture Base deploys the capsule.

```
/capsule setYOffset <yOffset>
```
* **yOffset** Integer. Offset to apply to the deployed position.


---

## downloadTemplate ##

[since 4.0 (1.15+)]

Copy the .nbt file of the currently held capsule into a "capsule_exports" folder inside the Minecraft instance directory. No permission is needed.
This might not work if the content is too big or if the content is not fetched yet (use right click to preview and force the download).

```
/capsule downloadTemplate
```
