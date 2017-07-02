# Capsule mod by Lythom #
## Mod page ##
[http://minecraft.curseforge.com/mc-mods/235338-capsule](http://minecraft.curseforge.com/mc-mods/235338-capsule)

## Changelog ##

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

**1.9.4-1.1.8 : BugFix fix **
 
* Fix #9 - Enchantment "Recall" not anymore present on every item whatever the config
* Default config now allow Recall only on capsules (see config file for more options)

**1.9.4-1.1.7 : Compatibility fixes **

* Fix #5 - Mod crashing when CapsuleItem.getItemStackDisplayName is called server-side

**1.9.4-1.1.6 : Update for 1.9.4 version of forge + JEI integration + new recipe **

* Update for forge 1.9.4 (thank you @Walter Daniel for the help =) )
* Add JEI integration with descriptions and special recipes (recovery, upgrade, clear)
* Add a new recipe to clear the content of a capsule (to allow upgrades and new recaptures of existing capsules)

**1.9-1.1.5 : Update for forge 12.16.1.1887 **

* Update Capsule mod for forge 1.9 - 12.16.1.1887 (recommanded)

**1.9-1.1.4 : Transfer logic rework **

* Rework transfer algorithm to force transfer without block update logic being executed during the move
* Fixes bug with dependants blocks (torches on top of wall, doors powered by redstone) that would not be kept correctly during the transfer

**1.9-1.1.3 : Fix recovery capsule recipe **

* Fix recovery capsule recipe

**1.9-1.1.2 : Minor fixes **

* Transfert logic fix

**1.9-1.1.1 : Migrate to 1.9 + more configuration options **

* Update to minecraft forge 1.9
* Offhand currently not able to throw capsules
* Fix weird behaviour when teleporting to capsule dimensions with creativePlayer2CapsuleTP
* Visually lighten capsule dimension

**1.8-1.1.0 : Migrate to 1.8.9 + more configuration options **

* Add item "Capsule : overpowered" crafted with a nether star instead of ender pearl. Overpowered capsule as a different "excluded blocks" config (to allow more blocks to be captured).
* Add a recipe to upgrades empty capsules capacity (surround with 8 ender pearls). Number of upgrades can be configured, default 5.
* Add Configuration options for default capsules sizes


**1.8-1.0.3 : Bugfixes : capsule content messing up after server restart + network error with large payload **

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

* TODO : fix case where world or thrower is not available (thrown by a dropper ?)
* TODO : Fix case where capture / resent fails./by Instead of brutally aborting, :
			reverse to initial situation ?
		    continue and ignore crashing block ?
* Grieffing protection : plug-in to the claim chunk system | config force block @corners (so if all 4 blocks can't be placed the content can't be captured) | protective block.
* Blueprint capsules that can be loaded with material and then spawn several times the same pattern
* Capsule shirts, Capsules banner logo (idea of AlexisMachina)