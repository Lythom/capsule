# Capsule mod by Lythom #

[![CircleCI](https://circleci.com/gh/Lythom/capsule.svg?style=svg)](https://circleci.com/gh/Lythom/capsule)

Bring your base! Capsules can capture a region containing any blocks or machines, then deploy and undeploy at will. Inspired by Dragon Ball capsules.

## Mod page and downloads ##
[https://minecraft.curseforge.com/projects/capsule](https://minecraft.curseforge.com/projects/capsule)

## Wiki ##
[https://github.com/Lythom/capsule/wiki](https://github.com/Lythom/capsule/wiki)

## Changelog ##

**1.15.2-4.0.40 : The 1.15 Update**

Template from previous versions might (or might not) work ! There are some holes in the minecraft datafixer when upgrading structures 1.12 → 1.15 so rememeber to do backups before you try =) 

- Port capsule from 1.12 to 1.15, it was awfully long and painfull and I hope minecraft team calm down with breaking changes xD
- New FX for capture zone display
- Thank you [Zerus (on Discord)](https://discord.gg/pgfCwW9) for the help with testing!
- 1.15 Update details :
    - Update the TemplateManager storing logic to ensure vanilla structure blocks compatibility
    - Update config to match new system (from cfg to toml), the new `capsule-common.toml` config file should be even better and documented enough to be understood.
    - Update recipe registration, recipes should now be overridable using datapacks
    - Udpate recipes logic to match new interfaces and Serializer system
    - Update capsule item registration, rendering and logic for empty cpasules, linked capsule, blueprints, one-use and reward capsules
    - Update the prefab dynamic recipes system, it remains the same: put nbts in `config/capsule/prefabs` and enjoy the blueprint recipes being dynamically generated for you with matching template associated. Recipe template configurable in `config/capsule/prefab_blueprint_recipe.json`.
    - Update block registration, rendering and block-properties logic.
    - Update networking code
    - Update the command system to match reworked vanilla system
    - Update client/server logic
    - Update GUI code for relabelling capsules
    - Update rendering code to work with vanilla refactors
    - Update lang files to match new format
    - Update projet structure to match forge new expectations
    - Rename all variables that were renamed from forge and minecraft
- Some starters and reward templates broke so I updated them, tell me if some of them are still to be fixed! ([→ Discord](https://discord.gg/pgfCwW9))
    - Remove 3rd party blueprints (immersive ingeneering and addons) because they are not compatible anymore and most be redone
- Add licence.md, stating from the repository the project in licenced under MIT License terms (it was already stated in curseforge, no change here).
- Wrote (and went through) a full test campaign to ensure mod quality : https://github.com/Lythom/capsule/issues/33

**1.12.2-3.3.7 : Fix deployment can create network overflow**

- [Community report] Fix: deploying was flooding item nbt with occupiedPositions informations if a big capsule was deployed in dense non-air environment (ie. under water). This fix saves occupiedPositions in the template files instead of nbt data to prevent any network transmission of this data. [Thanks to Kazahm on Discord](https://discord.gg/pgfCwW9).
- [Community report] Allow whitelisted tile entities to be rotated in standard capsules. It uses the blueprint whitelist: `config/capsule/blueprint_whitelist.json`. See docmentation at https://github.com/Lythom/capsule/wiki/Modpack-making#whitelist). [Thanks to MFWalter on Discord](https://discord.gg/pgfCwW9).


**1.12.2-3.3.5 : Disable starters config**

- [Submissions] Add some player submissions as default loots! [Thanks Kam](https://github.com/Lythom/capsule/issues/18) for new loots, [Thanks Grade](https://github.com/Lythom/capsule/issues/16) for new starters, Thanks heaven_k for new loot.
- [Community suggestion] Add randomized starters. Default configuration is now to give a random starter instead of all starters in the folder. A new config entry as been added: starterMode which can take values "all", "random" or "none". Starters remains disabled if "starterTemplatesPath" configuration is empty. [Thanks to happybandit360](https://github.com/Lythom/capsule/issues/19)
- [Community report] Fix server crash when starter fail to be delivered to the player. [Thanks to LennartCockx](https://github.com/Lythom/capsule/issues/20)
- Fix blueprint undeploy sometimes not not matching properly when nbt is empty

**1.12.2-3.2.103 : Disable starters config**
- [Community report] Emptying "starterTemplatesPath" config value now disables starters. [Thanks to Boolyman](https://github.com/Lythom/capsule/issues/14) and [Thanks to TheQKnight](https://www.curseforge.com/minecraft/mc-mods/capsule?comment=157).


**1.12.2-3.2.102 : loot reward**

- [Community suggestion] Add configuration to force one-use capsules over precharged blueprint as dungeon chests loots. [Thanks to TheQKnight](https://www.curseforge.com/minecraft/mc-mods/capsule#c158).


**1.12.2-3.2.101 : recipe fix**

- [Community report] Fix "clear capsule" recipe that can make strange conversions. [Thanks to Ghoul-Generator](https://github.com/Lythom/capsule/issues/13).


**1.12.2-3.2.100 : Modpacker's daydream update**

First an update notice: 
- some incompatible mods have been identified and added to configuration, please remove the `excludedBlocks` and `opExcludedBlocks` entries in your `config/capsule.cfg` so that they regenerates. Sorry gregtech machines, superfactorymanager and refined storage, you can't go into capsules.
- New loot included! Remove the `config/capsule/loot` folder to generate the new loots !

Now the cool stuff!    
New options for modpack makers!
- You can give the player recipes for pre-configured blueprint.
    - Once the blueprint is crafted (ie. Immersive Engineering Arc Furnace blueprint), the player only have to gather the materials to be able to deploy the structure. 
    - Rotation, mirror and undo are possibles so it makes it very easy to try and place correctly a multiblock. 
    - Any structure can easily be added as pre-configured blueprint, recipes are generated automatically! See [https://github.com/Lythom/capsule/wiki/Modpack-making#add-a-preconfigured-blueprint](https://github.com/Lythom/capsule/wiki/Modpack-making#add-a-preconfigured-blueprint) for more information.
- You can give the player starter(s) preloaded caspule.
    - Default starter is a small house designed to serve as mobile base but it can be removed or changed. See [https://github.com/Lythom/capsule/wiki/Modpack-making#how-to-give-one](https://github.com/Lythom/capsule/wiki/Modpack-making#how-to-give-one) for more information.
    
Full changelist and fixes :
- Add rotation and mirror to standard capsule if their content support it (ie. basic block or whitelisted tile entity).
- Add starter capsule mechanic and a default starter.
- Add new loots ! Remove the `config/capsule/loot` folder to get the new ones !
- Add Loot now can be charged (pre-filled) blueprints instead of one use capsules.
- Add preconfigured blueprint recipes. Check JEI for blueprints ! Blocks used in the structure are given back.
- Add Immersive engineering, Immersive Tech and Immersive Petroleum multiblocks as preconfigured blueprint (only when the mod is loaded).
- Add a few vanilla based preconfigured blueprints.
- Add a [whitelist](https://github.com/Lythom/capsule/wiki/Modpack-making#whitelist) mechanic to allow more blocks to be used in blueprints.
- Add fluid support in blueprints (only through buckets for now).
- Add [giveBlueprint](https://github.com/Lythom/capsule/wiki/Commands#giveblueprint) command.
- Add [giveLinked](https://github.com/Lythom/capsule/wiki/Commands#givelinked) command.
- Add optional "playerName" argument to all relevant commands to give any kind of capsule to any player.
- Improve tooltip display for all capsules..
- Revert forge version dependency to latest recommended (14.23.5.2768).
- Blueprints capsule can now be crafted not only from standard capsule but also from reward, recovery and other blueprint capsules.
- Recovery capsule recipe is shapeless again.
- Improve JEI recipes and descriptions.
- Update excludedBlocks for newly discovered incompatibilities. please remove the `excludedBlocks` and `opExcludedBlocks` entries in your `config/capsule.cfg` so that they regenerates.
- Fix a bug where ":" in the label of a reward would mess the structure file location.
- Fix entity rotation (ie. item frames or armor stands)

**1.12.2-3.2.91 : Builder's daydream update**

New options for builders ! Blueprints makes it fast to build patterned constructions like bridges, walls, or dungeons with the ability to deploy a template using materials from chest or player inventory. The template can be rotated and mirrored to fit any situation !

* New type of capsule : the blueprint ! Allow to deloy several times the same template using materials from any kind of chest or from player inventory.
    * Blueprint copies the template of the capsule they are crafted with. Original capsule is not consumed.
    * Blueprint allows rotation and mirror of the structure using left click and sneak + left click.
    * Blueprint are recharged using left click in the air.
    * Last blueprint deployment can be undeployed as long as the blueprint is not recharged and the deployed blocks has not been modified.
    * Unlike standard capsules, blueprints are limited to non-entities blocks. Ie. torches, doors and redstone wires are allowed but chests, furnaces or paintings will be ignored in the blueprint template.
* [Community suggestion] Add particles on deploy and undeploy. https://github.com/Lythom/capsule/issues/9.
* [Community suggestion] Add optional "player" argument to fromExistingReward and fromStructure commands. It makes it possible to configure vending machines or structure blocks to give a specific template to a secific player. https://github.com/Lythom/capsule/issues/11.
* [Community suggestion] Players don't prevent deployment anymore, instead the player is teleported to the deployed structure nearest floor.
* Add a new starter capsule of size 1 that only requires wood to be used.
* Add new capsules sizes: 3 (iron), 5 (gold), 7 (diamond), 9 (obsidian) and 11 (emerald).
* Add instant mode for blueprints and capsules of size 1 : they continuously display content preview and deploy instantly on right click.
* Add recipe to clear a deployed capsule: deployed content stays in the world and the capsule become empty again.
* Improve preview of capsule deployment: deployment can be previewed from further away and similar blocks are displayed together.
* Fix capsule size configuration ignored. 0 or negative value will not correctly disable the capsule, and any positive integer will be used by the recipe.
* Migrate recipe system to 1.12 json recipes. It allows via resource pack to override or add recipes for empty capsules, upgrade ingrendients, recovery capsule, blueprint capsule. 
* [Experimental] Add schematic support for reward capsules. You can creat reward from .schematic files in the reward folder.
* [Experimental] Automatically infer configuration for overridable blocks of other mods. Blocks whose name include "leave", "sapling", "mushroom", or "vine" will be automatically added to overrideable list.
* Raised the max capsule size hard limit to 255 (from 31). Default configuration allows capsules to be upgraded up to 31. It is not advised to go bigger since the performance hit for the server can be important, but it can be used to offer the player bigger structures as reward for example.
* Fix trying to undeploy near max height limit will correctly prevent deploy.
* Fix loot files name (lowercase only).
* Update JEI recipes and "information" tab for each kind of capsule.
* Update forge to 1.12.2-14.23.5.2781
* Update JEI API to 1.12.2-4.15.0.268

**1.12.2-3.1.68 : Important bug fix**

* Fix error when capsule thrown by non player entity (ex: dropper)
* Update forge to 1.12.2-14.23.3.2655
* Update 1.12.2-3.1.68 README

**1.12.2-3.1.63 : Water and loot fine tuning**

* New Water behaviour : Capsules now deploys on surface of water (or liquids), unless the thrower is immerged in the liquid itself.
* Add configurable loot tables. A new entry in the config file (lootTablesList) allows to configure where reward capsule will spawn.
* Remove gameplay/fishing/treasure from default loot tables (can be re-added trough config).
* Update forge to 1.12.2-14.23.0.2550
* Update JEI API to 1.12.2-4.8.0.114

**1.12.2-3.1.57 : Chinese and Bug fixes**

* Add chinese translation (Thanks to 0nepeop1e)
* Fix a bug where the content of the capsule would not be saved if modified in another dimension
* Fix logic to load content from older version of structure blocks

**1.12.2-3.1.66 : template naming on linux servers**

It is required that template names are lowercase to work in a linux environment, this applies to all version of capsule.

* Change default templates names to be lowercased.
* Change Reward capsule to use capitalized file name as label. Ex: "small villager house.nbt" will give a "Small Villager House" capsule.

**1.12.2-3.1.48 : sound and Bug fixes**

Upgrading:
If you upgrade from an older version, you will not have loot in dungeon chest by default.
To get them back: remove `config/capsule.cfg` (to generate a new default config) and remove folder `/config/capsule/loot`.

* Add sounds for activation, deactivation, throw, deploy and undeploy actions
* Looting system change ! All loots from dungeon chests are now taken from /config/capsule/loot (and not more from the jar assets). The default loots or copied there the first time the folder are created.
* Change versionning number to follow minecraftforge guidelines (without API)
* Fix excluded blocks (modded) that would not be actually excluded during capture

**1.12.2-1.5.39 : 1.12.2 Update**

* Update forge to 1.12.2-14.23.0.2550
* Update JEI API to 1.12.2-4.8.0.114
* new default path to capsule files in save folder : <worldsave>/structures/capsule (previously "/capsules")
* new default path to capsule files for loots : <jar>/assets/capsule/loot/ and <instance>/config/capsule/loot/ (previously "/capsules")
* new default path to capsule rewards <instance>/config/capsule/rewards (previously "/capsules")
* Recipes updates to use the new json system when possible. Warning: Size of capsules from crafting is now defined in the json recipe, not in the config.

**1.11.2-1.4.0 : 1.11.2 Update**

* Update forge to 1.11.2-13.20.1.2530
* Update JEI API to 1.11.2-4.5.0.294

**1.10.2-1.3.0 : The multiplayer friendly update**

* Add grieffing protection for bother capturing and deploying (check if the player could place / harvest block manually)
* Fix activated capsule being thrown by non-player (dispenser and dropper can now deploy an activated capsule)
* Improve previewed capsule throw trajectory
* Improve resilience: blocks crashing when manipulated will be ignore during the capture + error log but no crash or interruption of the capture
* Update forge to 1.10.2-12.18.3.2511
* Update JEI API to 1.10.2-3.14.7.420

**1.10.2-1.2.0 : The Big 1.10.2 update : the modpack maker update : the Structure blocks update**

* Now uses structure blocks mechanics and file format to store capsule content.
* Compatible with structure blocks (a structure template can be converted into capsule and a capsule can be load from structure block).
* Add a preview of the future content deployment when the capsule is activated. Red wireframes indicates the deploy will fail.
* The capsule will always deploy exactly where is was previewed.
* Recipes tweaks, see NEI or wiki for new recipes (https://bitbucket.org/Lythom/mccapsule/wiki/Home).
* Visual changes on blocks and items.
* One use Loot capsules now appears in dungeon chests.
* Add tools for modpack makers. See https://bitbucket.org/Lythom/mccapsule/wiki/Modpack%20maker%20How%20To's.
* Fixes :
* Fix enchantment registring to match latest forge pratices.
* Fix preview rendering for capturing and undeploying
* Fix undeploying from another dimension

NOT BACKWARD COMPATIBLE with any previous version of the mod : backup, deploy all your capsule contents, and destroy all your capsules items before updating from a previous version.

**1.9.4-1.1.8 : BugFix fix**
 
* Fix #9 - Enchantment "Recall" not anymore present on every item whatever the config
* Default config now allow Recall only on capsules (see config file for more options)

**1.9.4-1.1.7 : Compatibility fixes**

* Fix #5 - Mod crashing when CapsuleItem.getItemStackDisplayName is called server-side

**1.9.4-1.1.6 : Update for 1.9.4 version of forge + JEI integration + new recipe**

* Update for forge 1.9.4 (thank you @Walter Daniel for the help =) )
* Add JEI integration with descriptions and special recipes (recovery, upgrade, clear)
* Add a new recipe to clear the content of a capsule (to allow upgrades and new recaptures of existing capsules)

**1.9-1.1.5 : Update for forge 12.16.1.1887**

* Update Capsule mod for forge 1.9 - 12.16.1.1887 (recommanded)

**1.9-1.1.4 : Transfer logic rework**

* Rework transfer algorithm to force transfer without block update logic being executed during the move
* Fixes bug with dependants blocks (torches on top of wall, doors powered by redstone) that would not be kept correctly during the transfer

**1.9-1.1.3 : Fix recovery capsule recipe**

* Fix recovery capsule recipe

**1.9-1.1.2 : Minor fixes**

* Transfert logic fix

**1.9-1.1.1 : Migrate to 1.9 + more configuration options**

* Update to minecraft forge 1.9
* Offhand currently not able to throw capsules
* Fix weird behaviour when teleporting to capsule dimensions with creativePlayer2CapsuleTP
* Visually lighten capsule dimension

**1.8-1.1.0 : Migrate to 1.8.9 + more configuration options**

* Add item "Capsule : overpowered" crafted with a nether star instead of ender pearl. Overpowered capsule as a different "excluded blocks" config (to allow more blocks to be captured).
* Add a recipe to upgrades empty capsules capacity (surround with 8 ender pearls). Number of upgrades can be configured, default 5.
* Add Configuration options for default capsules sizes


**1.8-1.0.3 : Bugfixes : capsule content messing up after server restart + network error with large payload**

* Fix : capsule content messing up after server restart (last available storage space was not retrieved correctly after server restart)
* Fix : network payload error with big custome capsules

**1.8-1.0.2 : Capsule labeling fix + configurable overridable and excluded blocks**

* Add overridable and excluded blocks in config files. You can now choose is you want this spawner to get captured =)
* Improve feedback when an entity prevent the capsule from deploying. Ex: "Can't deploy : <EntityName> in the way !"
* Fix label taking only the first letters when edited.
* Fix some block states (ie. some flowers) that would not get overriden on deploy and prevent the capsule from deploying.

**1.8-1.0.1 : Dedicated server fix + various bug fixes**

* Downgrade minecraftforge dependency to recommanded version (11.14.3.1450). Still compatible with latest version.
* Allow the mod to run on dedicated server
* Fix encoding for the capsule label quotes
* Fix mobs not preventing capsule to deploy

**1.8-1.0.0 : Initial release**

Items :

* capsule item (iron, gold and diamond)
* creative player2Capsule teleporter

Blocks :

* captures base

## Planned ##

* Capsule shirts, Capsules banner logo (idea of AlexisMachina)