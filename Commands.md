# Capsule in-game Commands #

Those commands are accessible only with the op permissions. They are designed to help content creators to play around with capsule and create whatever capsule they want.

**Parameters marked with a * have autocompletion available using the tabulation key.**

- [Capsule in-game Commands](#capsule-in-game-commands)
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
  * [setBaseColor](#setbasecolor)
  * [setMaterialColor](#setmaterialcolor)
  * [setAuthor](#setauthor)
  * [downloadTemplate](#downloadTemplate)

<small><i><a href='http://ecotrust-canada.github.io/markdown-toc/'>Table of contents generated with markdown-toc</a></i></small>

---

## giveEmpty ##

Give an empty capsule.

```
/capsule giveEmpty [size] [overpowered]
```
* **size** Optional, default 3. Value must be an odd integer between 1 and 31. Even values will be rounded down.
* **overpowered** Optional, default false. Value must be true or false

---

## giveLinked ##

[since 3.2.91]

Give an standard capsule with preloaded content.

```
/capsule giveLinked <reward_name> [playerName]
```
* **reward_name*** Required. Name of the structure reward to load from.
* **playerName*** Optional. Name of the player that will receive the reward.

---

## giveBlueprint ##

[since 3.2.91]

Give an uncharged blueprint capsule with preconfigured structure.

```
/capsule giveBlueprint <reward_name> [playerName]
```
* **reward_name*** Required. Name of the structure reward to load from. Use tab for auto-completion from the reward folder.
* **playerName*** Optional. Name of the player that will receive the reward.

---
## exportHeldItem ##

Print the /give command that would give the exact item in hand. The item can be any item (not limited to capsules).  
Tip : click on the result text in the chat to open the client log file and allow copy/paste of the command.

```
/capsule exportHeldItem
```

---
## exportSeenBlock ##

```
/capsule exportSeenBlock 
```

Print the /give command that would give an item that would spawn the exact block + TileEntity you are looking at. The block can be any block (not limited to capsules).  
Tip : click on the result text in the chat to open the client log file and allow copy/paste of the command.

---
## fromHeldCapsule ##

Create a reward template by copying the template from held capsule into configured reward folder (default config/capsule/rewards).
This command can be used to create a new Reward or to update an existing one.

A One Use Capsule linked to the created/updated reward template is given to the player.

**Warning** : If a template already exists with the name in the reward folder, it will be overwritten without confirmation.


```
/capsule fromHeldCapsule [outputName]
```
* **outputName** Optional, default value is the capsule label. Name of the structure to create in the reward folder

---
## fromStructure ##

Create a new Reward Capsule by copying the template from named structure block template ((1.12-)<worldsave>/structures/<structureName>.nbt or (1.15+)<worldsave>/generated/minecraft/structures) into configured reward folder (default config/capsule/rewards).

This command can be used to create a new Reward or to update an existing one.

A One Use Capsule linked to the created/updated reward template is given to the player.

**Warning** : If a template already exists with the name in the reward folder, it will be overwritten without confirmation.

```
/capsule fromStructure <structure_name> [playerName]
```
* **structure_name*** Required. Name of the structure Template to load from.
* [since 3.2.91] **playerName*** Optional. Name of the player that will receive the reward.

---
## fromExistingReward ##

Give a One Use Capsule linked to the reward template named <templateName> from the configured reward folder (default config/capsule/rewards).

```
/capsule fromExistingReward <reward_name> [playerName]
```
* **reward_name*** Required. Name of the reward Template to load from.
* [since 3.2.91] **playerName*** Optional. Name of the player that will receive the reward.

---
## giveRandomLoot ##

Give the player a random Loot Capsule using the loot generation configuration.

```
/capsule giveRandomLoot [player]
```
* **player*** Optional, default is current player. Name of the player to give the reward to.

---
## reloadLootList ##

Refresh the lootable capsules from reading configured directories. If directories are added or removed, a server restart is required (or just a world restart client side).

```
/capsule reloadLootList
```

---
## setBaseColor ##

Specify the capsule base color.

```
/capsule setBaseColor <color>
```
* **color*** Required. Integer of the color, ie. 0x5e8eb7. Use autocompletion for more exemple values.

---
## setMaterialColor ##

Specify the capsule material color (around the label).

```
/capsule setMaterialColor <color>
```
* **color*** Required. Integer of the color, ie. 0x5e8eb7. Use autocompletion for more exemple values.

---
## setAuthor ##

Save an author for both the capsule and the associated template. If the [author] parameter is omitted, any author reference is removed. The author will then be credited in the capsule decription as "Designed by <author>".

```
/capsule setAuthor [author]
```
* **author*** Author name or leave empty to remove.


---

## downloadTemplate ##

[since 4.0 (1.15+)]

Copy the .nbt file of the currently held capsule in a "capsule_exports" folder inside minecraft instance directory.
This might not work if the content is too big or if the content is the fetched yet (use right click to preview and force the download).

```
/capsule downloadTemplate
```
