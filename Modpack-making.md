# Modpack making tools

- [Modpack making tools](#modpack-making-tools)
  * [Types of Capsules](#types-of-capsules)
    + [Standard](#standard)
    + [Rewards](#rewards)
    + [Loots](#loots)
    + [Blueprints](#blueprints)
        - [Whitelist](#whitelist)
  * [How to](#how-to)
    + [Create an Empty Capsule](#create-an-empty-capsule)
    + [Create a Reward Capsule](#create-a-reward-capsule)
    + [Add a preconfigured blueprint](#add-a-preconfigured-blueprint)
    + [Relabeling a Capsule](#relabeling-a-capsule)
    + [Setting the author of a capsule](#setting-the-author-of-a-capsule)
    + [Dying a capsule](#dying-a-capsule)
    + [Create a Template to be used as Loot](#create-a-template-to-be-used-as-loot)
    + [Test looting system](#test-looting-system)
  * [Submit your templates !](#submit-your-templates--)
  * [Other tools](#other-tools)
    + [Exporting the item NBT](#exporting-the-item-nbt)
    + [Exporting a block + TileEntity](#exporting-a-block---tileentity)
  * [Capsule NBT data reference](#capsule-nbt-data-reference)

<small><i><a href='http://ecotrust-canada.github.io/markdown-toc/'>Table of contents generated with markdown-toc</a></i></small>


## Types of Capsules

There are 3 types of capsules to know in order to use Capsule as a modpack making tool.

### Standard

Standard capsules are live created by players while playing. They are stored in <worldsave>/structures/capsules for each world.

When the player capture some new content with an Empty Capsule, a new Template file is created there. Through the game, only this crafted Capsule and the Recovery Capsules linked to it can modify the Template file. In-game commands for modpackmakers are non-destructive on live created capsules, so you can't mess with players contents if using commands on a "live" server. The Template file is empty if the capsule is deployed, and contains the captured region data if the capsule is undeployed, so you want to create rewards from undeployed capsules.

#### How to give one

Apart from the player crafting it's own capsule, there are 2 ways to give players preloaded standard (reusable) capsules :
- [since 3.2.95] Using the command [`/capsule giveLinked <reward_name> [playerName]`](https://github.com/Lythom/capsule/wiki/Commands#givelinked).
- [since 3.2.95] As starter capsule, given when the player login for the first time. Starters can be added or removed in the `config/capsule/starters` folder. To make one, first [create a Reward Capsule](#create-a-reward-capsule), then copy the created structure from `config/capsule/rewards/<structure_name>` to `config/capsule/starters/<label_name>`.

### Rewards

Reward capsules are what you would be doing most. They are prepared by modpack maker, and stored in the config/capsule/rewards folder by default.

Those capsule are always oneUse (item is destroyed when successfully deployed), but the Template file is never emptied. That means a Reward Capsule can be given any number of time to any number of players.

#### How to give one

The players won't be able to get those capsules by themselves. They can be given:

- [since 3.2.95] Using the command [`/capsule fromExistingReward <reward_name> [playerName]`](https://github.com/Lythom/capsule/wiki/Commands#fromexistingreward)
- in another reward or loot capsule (#capsuleception :D)
- as a quest reward (choose your favorite quest mod)
- using any mod that can reward an ItemStack with custom nbt data
- if using a map template, placed anywhere in the world (chest, item frame, be creative !)


### Loots

Loot capsules are reward capsule that appears in the loot table of dungeon chests. They are taken from specifics folders defined in the config file.

The Template files located under any of the `lootTemplatesPaths` entry in the config file will be eligible to spawn as loot in dungeon chests. They work the same way as Reward Capsule except they have this additional way to be obtain : the player can find them in a loot chest.

By default, 3 folders are defined in the config file (in "/config/capsule/loot/…") that can be filled as you wish.

Each folder will get an entry in the capsule config file on server restart, to be able to configure weight (chance for the folder to be picked rather than another when selecting a loot).


### Blueprints

[since 3.2.95]

Blueprints capsules allow player to build structures multiple times by taking materials from player inventory and any linked inventory. The blueprint make it easy to build the structure (ie. multiblock or wall pattern) but still requires the player to gather the materials. Rotation, mirror and undeploy are possible so it makes it very easy to experiment when placing a structure. See [blueprint player documentation](https://github.com/Lythom/capsule/wiki#blueprints) and [https://imgur.com/gallery/vN1sJrf](https://imgur.com/gallery/vN1sJrf) for more information on how to use as a player).

Blueprints crafted by player have a dedicate template created when crafted that is located in the same folder than standard capsules: <worldsave>/structures/capsules. They can be identified by prefix, blueprints are prefixed "B-" and standard capsules "C-".

#### How to give one

Apart from the default player recipe, Blueprints capsules can be accessed in 2 ways :
- Using a craft recipe of [preconfigured blueprints](#add-a-preconfigured-blueprint) (the modpack maker can provide specific structures to be craftable).
- using command [`/capsule giveBlueprint <reward_name> [playerName]`](https://github.com/Lythom/capsule/wiki/Commands#giveblueprint).

#### Whitelist

By default tile entities are not supported by blueprints. Still, it is possible to allow specific tile entities to the `config/capsule/blueprint_whitelist.json` file to enable them. An entry can consist of the block id (ie. "minecraft:chest") or a json object with properties "block" and "keepNBT". Exemple of the immersive engineering conveyor belt that is whitelisted by default:
```json
{
  "block": "immersiveengineering:conveyor",
  "keepNBT": {
    "conveyorType": null,
    "conveyorBeltSubtype": "conveyorType",
    "conveyorBeltSubtypeNBT": null,
    "facing": null
  }
},
```
In this example, the conveyor block will be allowed in blueprints,
- the NBT properties of the tile entity listed under `keepNBT` will be preserved in the blueprints,
- all unlisted properties will be removed. Typically, only configuration should be kept, "items" or inventory properties if kept would lead to dupe issues.

`keepNBT` is a key/value object where almost every property value is `null` except `"conveyorBeltSubtype": "conveyorType"`, it means than the item required to charge this blueprint MUST have the `conveyorType` nbt data in the item, and the value of the item `conveyorType` must match the value of the block nbt `conveyorBeltSubtype` to be a valid material. `null` value means the material item don't requires a specific NBT to be a valid material. In this example it forces the type of conveyor belt in the inventory to match the type of conveyor belt built in the blueprint, not any conveyor belt could be used as item input.

Tips : 
- You can use the [`/capsule exportSeenBlock`](https://github.com/Lythom/capsule/wiki/Commands#exportseenblock) when looking at a block to display its nbt data and know what values should be added
- x, y, z, Inventory and id properties should not be listed under "keepNBT", indeed you don't want to keep location information (you want them to be forgotten and reset when blueprint is placed) neither inventory information (because it could lead to dupe bugs), and finally id is not actually nbt data.

## How to

Manipulating nbt data can be tricky. Capsule mod include some commands and tools to help modpack makers achieve the creation of the Capsules they need.

### Create an Empty Capsule

* Use JEI in cheat mod or the creative mode panel
* Use the command [`/capsule giveEmpty <size>`](https://github.com/Lythom/capsule/wiki/Commands#giveempty)
* Use the crafting recipe in a workbench.

### Create a Reward Capsule

This procedure will have you create a template file located under `config/capsule/rewards`. This config subfolder must be distributed with your pack to have the given capsule item (or any exact copy) work on the player game.

1. Get an empty capsule,
2. capture the content you want to reward on a Capture Base,
3. finally use the command [`/capsule fromHeldCapsule <structure_name>`](https://github.com/Lythom/capsule/wiki/Commands#fromheldcapsule) while having the capsule in main hand. The structure is now at `config/capsule/rewards/<structure_name>.nbt`.

To allow more advanced captures :

1. Setup a structure block in save mode
2. Configure the way you want (you can even capture entities and blocks a capsule wouldn't capture like mobs or monsters or EntityItems on the ground),
3. Save using a unique name (lowercase only),
4. Finally use the command [`/capsule fromStructure <structure_name>`](https://github.com/Lythom/capsule/wiki/Commands#fromstructure) where "<structure_name>" is the unique name used previously. The size of the capsule will be calculated to include the whole structure block content. If the name contains uppercase characters, the capsule won't deploy on linux server (which most of server providers uses).

[Click to see Demo of using `/capsule fromStructure <structure_name>`
![](https://imgur.com/msB7g5I.png)](https://imgur.com/aG5Dt4c.gif)


### Add a preconfigured blueprint

To add a new craftable preconfigured blueprint, first [create a Reward Capsule](#create-a-reward-capsule), then copy the created structure from `config/capsule/rewards/<structure_name>` to `config/capsule/prefabs/<label_name>`. A recipe for this preconfigured blueprint will be dynamically created and added to JEI and creative tabs.

Additional notes:
- The recipe will be created using the `config/prefabs/prefab_blueprint_recipe.json` configuration. Capsule will dynamically replace "1", "2" and/or "3" with most used blocks in the structure the create the final recipe. The blocks from the structure used to craft the recipe are not consumed, so they can be reused to charge the blueprint. You can change ingredients and move around 1, 2 and 3 in the json.
- Blueprints are limited to plain blocks. Tile entities are ignored during blueprint structure copy unless they are listed in the [whitelist](#whitelist).
- If inside a sub-folder, a mod with the same name as the folder must me loaded to enable the recipe.
- Ensure the file name is lowercase only, _ will be replaced by spaces and each word is capitalize for the capsule label.


### Relabeling a Capsule

* Non-empty capsules can be labeled using sneak + right click to show the GUI.
* Renaming a capsule on an anvil is possible, but it will completely override the item naming mechanics and the label will be ignored.

Note : A Loot capsule will always be labelled using the name of the file (without .nbt), The Name Will Be Capitalized.

### Setting the author of a capsule

You may want to feature any creator content (may it be yourself !), so an author can be set to give credit to the person who designed the content by using the command [`/capsule setAuthor <name>`](https://github.com/Lythom/capsule/wiki/Commands#setauthor). The <name> will appear in the Capsule description as "Designed by <name>".

### Dying a capsule

* Base color can be dyed on a workbench by combining the capsule + dyes
* Base color can be set using the command [`/capsule setBaseColor 0xCCCCCC`](https://github.com/Lythom/capsule/wiki/Commands#setbasecolor) where "CCCCCC" is the hexadecimal code for the color.
* Material color can be changed using the command [`/capsule materialColor 0xCCCCCC`](https://github.com/Lythom/capsule/wiki/Commands#setmaterialcolor), where "CCCCCC" is the hexadecimal code for the color
* At the moment, capsule generated as Loot get a random color that cannot be specified.

### Create a Template to be used as Loot

The template files does NOT include the capsule NBT data, so the options for the Capsule created in the dungeon chest are limited. The Capsule will take the Template file "name" capitalized (Each First Letter Is Uppercase) as its label (without .nbt), the Template file value "author" as its author, and the capsule size will be calculated from the structure size. the template file name itself must be lowercase.

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

If you followed "Create a Template to be used as Loot" and came up with great Loot templates, you can ask me to include them as a default reward in the mod ! If I believe the structure is not breaking the game and have a place into the mod, it'll be included in the next version of capsule. If the author is set, he/she will be credited in the capsule description when looted by the player.

2 ways to submit your template .nbt file :
- on the Discord (https://discord.com/invite/wZpBVdr), please provide a textual description of the content with the file,
- at https://github.com/Lythom/capsule/issues/new?title=[Submission] with a description of the content. 

The see you on the next version of capsule ;)

## Other tools

### Exporting the item NBT

The Capsule item is ready but you may need the give command to setup a command block, or the NBT data the configure a mod. Use the command  [`/capsule exportHeldItem`](https://github.com/Lythom/capsule/wiki/Commands#exporthelditem) to generate a the /give command in the chat. Click the message to open the log file and be able to copy/paste it. The last parameter is the NBT data.

Note : this command will work for any item, not only capsules.

### Exporting a block + TileEntity

Mostly usefull for modders. The command [`/capsule exportSeenBlock`](https://github.com/Lythom/capsule/wiki/Commands#exportseenblock) will create a give command to get an item that would spawn the exact block + TileEntity you are looking at.

## Capsule NBT data reference

If you want to create your own capsules or give them using commandblocks, you'll need to properly fill their nbt data. The easiest choice is to get the capsule in-game ("Create a Reward Capsule"), then to use the [`/capsule exportHeldItem`](https://github.com/Lythom/capsule/wiki/Commands#exporthelditem) command while holding the capsule. You can eventually modify the nbt data :

```
* int color                                                  // material color
* tag display : {int color}                                  // base color
* int size                                                   // odd number, size of the square side the capsule can hold
* string label                                               // User customizable label
* byte overpowered                                           // If the capsule can capture powerfull blocks
* bool onUse                                                 // if the content of the template must be kept when capsule is deployed.
* bool isReward                                              // if the template is located in the configured reward folder
* string author                                              // Name of the player who created the structure. Set using commands.
* string structureName                                       // name of the template file name without the .nbt extension.
// Lookup paths are /<worldsave>/structures/capsule for non-rewards, and structureName must contains the full path for rewards and loots
* string prevStructureName                                   // Used to remove older unused blueprint templates
* tag activetimer : {int starttime}                          // used to time the moment when the capsule must deactivate
* tag spawnPosition : {int x, int y, int z, int dim    }     // location where the capsule is currently deployed
* tag occupiedSpawnPositions : [{int blockId, long pos},…]   // remember what position not the recapture is block didn't change
* long deployAt                                              // when thrown with preview, position to deploy the capsule to match preview
* int upgraded                                               // How many upgrades the capsule has
* tag sourceInventory : {int x, int y, int z, int dim    }   // [Blueprints] location of the linked inventory
* string mirror                                              // [Blueprints] current mirror mode
* string rotation                                            // [Blueprints] current rotation mode
* arr ench:[0:{lvl:1s,id:101s}]
```
NBTData reference is also up to date at https://github.com/Lythom/capsule/blob/master/src/main/java/capsule/items/CapsuleItem.java#L79.