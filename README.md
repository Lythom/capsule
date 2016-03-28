# Capsule mod by Lythom #
## Mod page ##
[http://minecraft.curseforge.com/mc-mods/235338-capsule](http://minecraft.curseforge.com/mc-mods/235338-capsule)

## Changelog ##

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

* capsule item (iron, gold and diamon)
* creative player2Capsule teleporter

Blocks :

* captures base

## Planned ##
* Reward capsules holding the content as NBT data rather than in the dimension