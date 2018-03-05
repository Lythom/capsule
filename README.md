# Capsule mod by Lythom #

Bring your base! Capsules can capture a region containing any blocks or machines, then deploy and undeploy at will. Inspired by Dragon Ball capsules.

## Mod page ##
[https://minecraft.curseforge.com/projects/capsule](https://minecraft.curseforge.com/projects/capsule)

## Wiki ##
[https://github.com/Lythom/capsule/wiki](https://github.com/Lythom/capsule/wiki)

## Changelog ##

**1.12.2-3.1.63 : Water and loot fine tuning**

* New Water behaviour : Capsules now deploys on surface of water (or liquids), unless the thrower is immerged in the liquid itself.
* Configurable loot tables. A new entry in the config file (lootTablesList) allows to configure where reward capsule will spawn.
* Removed gameplay/fishing/treasure from default loot tables (can be re-added trough config).
* Update forge to 1.12.2-14.23.0.2550
* Update JEI API to 1.12.2-4.8.0.114

**1.12.2-3.1.57 : Chinese and Bug fixes**

* Add chinese translation (Thanks to 0nepeop1e)
* Fix a bug where the content of the capsule would not be saved if modified in another dimension
* Fix logic to load content from older version of structure blocks

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

* Blueprint capsules that can be loaded with material and then spawn several times the same pattern
* Capsule shirts, Capsules banner logo (idea of AlexisMachina)