# Modpack making tools

- [Modpack making tools](#modpack-making-tools)
  * [Types of Capsules](#types-of-capsules)
    + [Standard](#standard)
    + [Rewards](#rewards)
    + [Loots](#loots)
  * [How to](#how-to)
    + [Create an Empty Capsule](#create-an-empty-capsule)
    + [Create a Reward Capsule](#create-a-reward-capsule)
    + [Relabeling a Capsule](#relabeling-a-capsule)
    + [Setting the author of a capsule](#setting-the-author-of-a-capsule)
    + [Dying a capsule](#dying-a-capsule)
    + [Create a Template to be used as Loot](#create-a-template-to-be-used-as-loot)
    + [Test looting system](#test-looting-system)
  * [Submit your templates !](#submit-your-templates-)
  * [Other tools](#other-tools)
    + [Exporting the item NBT](#exporting-the-item-nbt)
    + [Exporting a block + TileEntity](#exporting-a-block--tileentity)
  * [Capsule NBT data reference](#capsule-nbt-data-reference)

<small><i><a href='http://ecotrust-canada.github.io/markdown-toc/'>Table of contents generated with markdown-toc</a></i></small>

## Types of Capsules

There are 3 types of capsules to know in order to use Capsule as a modpack making tool.

### Standard 

Standard capsules are live created by players while playing. They are stored in <worldsave>/structures/capsules for each world.

When the player capture some new content with an Empty Capsule, a new Template file is created there. Through the game, only this crafted Capsule and the Recovery Capsules linked to it can modify the Template file. In-game commands for modpackmakers are non-destructive on live created capsules, so you can't mess with players contents if using commands on a "live" server. The Template file is empty if the capsule is deployed, and contains the captured region data if the capsule is undeployed, so you want to create rewards from undeployed capsules.

### Rewards

Reward capsules are what you would be doing most. They are prepared by modpack maker, and stored in the config/capsule/rewards folder by default.

Those capsule are always oneUse (item is destroyed when successfully deployed), but the Template file is never emptied. That means a Reward Capsule can be given any number of time to any number of players.

The players won't be able to get thoses capsules by themselves. They must be given, ie. :

* if using a map template, placed anywhere in the world (chest, item frame, be creative !)
* in another reward or loot capsule (#capsuleception :D)
* as a quest reward (choose your favorite quest mod)
* using any mod that can reward an ItemStack with custom nbt data
* using the /give command with the right nbt data

### Loots 

Loot capsules are reward capsule that appears in the loot table of dungeon chests. They are taken from specifics folders defined in the config file.

The Template files located under any of the "lootTemplatesPaths" entry in the config file will be eligible to spawn as loot in dungeon chests. They work the same way as Reward Capsule except they have this additional way to be obtain : the player can find them in a loot chest.

By default, 3 folders are defined in the config file (in "/config/capsule/loot/…") that can be filled as you wish.

Each folder will get an entry in the capsule config file on server restart, to be able to configure weight (chance for the folder to be picked rather than another when selecting a loot).

## How to

Manipulating nbt data can be tricky. Capsule mod include some commands and tools to help modpack makers achieve the creation of the Capsules they need.

### Create an Empty Capsule

* Use JEI in cheat mod or the creative mode panel
* Use the command [`/capsule giveEmpty <size>`](https://github.com/Lythom/capsule/wiki/Commands#giveempty)
* Use the crafting recipe in a workbench.

### Create a Reward Capsule

This procedure will have you create a template file located under config/capsule/reward/. This config subfolder must be distributed with your pack to have the given capsule item (or any exact copy) work on the player game.

1. Get an empty capsule, 
2. capture the content you want to reward on a Capture Base,
3. finally use the command [`/capsule fromHeldCapsule`](https://github.com/Lythom/capsule/wiki/Commands#fromheldcapsule) while having the capsule in main hand

To allow more advanced captures :

1. Setup a structure block in save mode
2. Configure the way you want (you can even capture entities and blocks a capsule wouldn't capture like mobs or monsters or EntityItems on the ground), 
3. Save using a unique name, 
4. Finally use the command [`/capsule fromStructure <structureName>`](https://github.com/Lythom/capsule/wiki/Commands#fromstructure) where "<structureName>" is the unique name used previously. The size of the capsule will be calculated to include the whole structure block content.

[Click to see Demo of using `/capsule fromStructure <structureName>`     
![](https://imgur.com/msB7g5I.png)](https://imgur.com/aG5Dt4c.gif)

### Relabeling a Capsule

* Non-empty capsules can be labeled using sneak + right click to show the GUI.
* Renaming a capsule on an anvil is possible, but it will completely override the item naming mechanics and the label will be ignored.

Note : A Loot capsule will always be labelled using the name of the file (without .nbt).

### Setting the author of a capsule

You may want to feature any creator content (may it be yourself !), so an author can be set to give credit to the person who designed the content by using the command [`/capsule setAuthor <name>`](https://github.com/Lythom/capsule/wiki/Commands#setauthor). The <name> will appear in the Capsule description as "Designed by <name>".

### Dying a capsule

* Base color can be dyed on a workbench by combining the capsule + dyes
* Base color can be set using the command [`/capsule setBaseColor 0xCCCCCC`](https://github.com/Lythom/capsule/wiki/Commands#setbasecolor) where "CCCCCC" is the hexadecimal code for the color.
* Material color can be changed using the command [`/capsule materialColor 0xCCCCCC`](https://github.com/Lythom/capsule/wiki/Commands#setmaterialcolor), where "CCCCCC" is the hexadecimal code for the color
* At the moment, capsule generated as Loot get a random color that cannot be specified.

### Create a Template to be used as Loot

The template files does NOT include the capsule NBT data, so the options for the Capsule created in the dungeon chest are limited. The Capsule will take the Template file "name" as its label (without .nbt), the Template file value "author" as its author, and the capsule size will be calculated from the structure size. 

Take care of setting the structure name and the author correctly when capturing content using either a structureblock in save mode, or an empty capsule.

Steps :

1. [Create a Reward Capsule](#create-a-reward-capsule)
2. [Set the label](#relabeling-a-capsule) and [Set the author](#setting-the-author-of-a-capsule),
3. Either :
    * a. [Submit your creation](#submit-your-templates-) to be distributed with the capsule mod !  
    * b. Copy/paste the Template file from "config/capsule/rewards/<CapsuleName.nbt>" to a valid capsule loot folder (ie. "config/capsule/loots/common/<CapsuleName.nbt>").

Your template now have a chance to spawn in a loot chest !

### Test looting system

You may want to check if the template are correctly added to the loot table are have a preview of what a random set of capsule loots would look like.

1. Use command `/capsule reloadLootList` to read new files added in the folders. If a new folder was added to the config file, a restart is required.
2. Use command `/capsule giveRandomLoot` to roll among the folders a get a Loot capsule (or not if an empty folder is rolled !)

## Submit your templates !

If you followed "Create a Template to be used as Loot" and came up with great Loot templates, you can ask me to include them as a default reward in the mod ! If I believe the structure is not breaking the game and have a place into the mod, it'll be included in the next version of capsule ;). If the author is set, he/she will be credited in the capsule description when looted by the player.

Submit your template .nbt file at https://github.com/Lythom/capsule/issues/new?title=[Submission] with a description of the content. It might be included with the next version of capsule ;)

## Other tools

### Exporting the item NBT

The Capsule item is ready but you may need the give command to setup a command block, or the NBT data the configure a mod. Use the command  [`/capsule exportHeldItem`](https://github.com/Lythom/capsule/wiki/Commands#exporthelditem) to generate a the /give command in the chat. Click the message to open the log file and be able to copy/paste it. The last parameter is the NBT data. 

Note : this command will work for any item, not only capsules.

### Exporting a block + TileEntity

Mostly usefull for modders. The command [`/capsule exportSeenBlock`](https://github.com/Lythom/capsule/wiki/Commands#exportseenblock) will create a give command to get an item that would spawn the exact block + TileEntity you are looking at.

## Capsule NBT data reference

If you want to create your own capsules or give them using commandblocks, you'll need to properly fill their nbt data. The easiest choice is to get the capsule in-game ("Create a Reward Capsule"), then to use the [`/capsule exportHeldItem`](https://github.com/Lythom/capsule/wiki/Commands#exporthelditem) command while holding the capsule. You can eventually modify the nbt data :

```
{
	(int) color, 							// material color
	(tag) display : {int color} 			// base color (vanilla dying nbt)
	(int) size,								// odd number, size of the square side the capsule can hold
	(string) label,							// User customizable label
	(byte) overpowered,						// If the capsule can capture powerfull blocks
	(bool) isReward,						// if the content of the template must be kept when capsule is deployed.
	(string) structureName,					// name of the template file name without the .nbt extension. Lookup paths are /<worldsave>/structures/capsule for non-rewards, and structureName must contains the full path for rewards and loots
	(tag) activetimer : {int starttime}, 	// Time the moment when the capsule must deactivate
	(tag) occupiedSpawnPositions : […],   	// remember what positions not the recapture if block didn't change
	(long) deployAt,						// set when capsule is thrown with preview, position to deploy the capsule that match the preview
	ench:[0:{lvl:1s,id:101s}]               // Vanilla way of storing enchantments. id:101s is the recall id in my case but it might be different depending on your pack.
}
```