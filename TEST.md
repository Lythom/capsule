# Test campaigns to fully qualify a build of capsule

This test campains describe what should be tested in the capsule mod to ensure all the features are working as expected.

It can sometime use internal or technical naming of capsules, to understand the general vocabulary, 
please check the first paragraphs of https://github.com/Lythom/capsule/wiki/Modpack-making
Some additional information:
- linked or undeployed capsule (depending on it's current state) refers to standard capsules
- Preconfigured blueprint = Prefab
- One-use Capsule == Recovery Capsule
- One-use Capsule and Reward Capsule are almost the same (destroyed at deploy), they differ by where they take the template: 
    - a reward will always work taking the structure from a config/capsule/reward template, and keep the nbt file intact (can be rewarded several times);
    - a one-use is linked to an standard capsule content and will empty the template on deploy (like if the standard capsule were deployed).

## Items 
### Empty capsule
☐ Empty Wood, Iron, Gold, Obsidian, Diamond, Emerald and OP capsules recipes are visible in creative tab and in JEI
☐ Empty Capsules have an information tab in JEI explaining how they work
☐ Empty Capsules have informations in the item description (mouse hover in inventory) explaining how they work
☐ Empty Capsules can be died by crafting it with one or severals dyies
☐ Empty Capsules can be upgraded (https://github.com/Lythom/capsule/wiki#upgrading) several times with chorus fruits (up to 10 times with default config)
☐ Empty Capsules can be enchanted with "Recall" enchant (https://github.com/Lythom/capsule/wiki#recall)
☐ Upgrade recipe can be changed using an asset pack to use another item. JEI recipe is updated accordingly
☐ Upgrade recipe is disabled if config is capsuleUpgradesLimit = 0
☐ Upgrade recipe works once if config is capsuleUpgradesLimit = 1
☐ Upgrade recipe works 24 times if config is capsuleUpgradesLimit = 24
☐ Empty Capsules cannot be re-labelled using shift+right click

### Linked capsule
Linked capsules are capsule that have content but that are undeployed: their content is inside.

☐ Linked Capsules have an information tab in JEI explaining how they work
☐ Linked Capsules have informations in the item description (mouse hover in inventory) explaining how they work
☐ Linked Capsules can be died by crafting it with one or severals dyies
☐ Linked Capsules can be cleared by crafting it alone in the inventory crafting grid, a one-use capsule is given back
☐ Linked Capsules cannot be upgraded (https://github.com/Lythom/capsule/wiki#upgrading)
☐ Linked Capsules can be enchanted with "Recall" enchant (https://github.com/Lythom/capsule/wiki#recall)
☐ Linked Capsules can be re-labelled using shift+right click
- Right click to preview deployment, the left click to rotate while the preview is visible
☐ The preview rotates (90, 180, 270, then 0)
- Right click to preview deployment, the shift + left click to rotate while the preview is visible
☐ The preview mirrors and all of the mode are available (FRONT_BACK, LEFT_RIGHT, no mirror)

### Deployed capsule
Deployed capsules are capsule that have a linked content but that content is deployed in the world.

1. Create a linked capsule with a few blocks and deploy it.
☐ Deployed Capsules have an information tab in JEI explaining how they work
☐ Deployed Capsules have informations in the item description (mouse hover in inventory) explaining how they work
☐ Deployed Capsules can be died by crafting it with one or severals dyies
☐ Deployed Capsules can be cleared by crafting it alone in the inventory crafting grid, only the empty capsule is given back
☐ Deployed Capsules cannot be upgraded (https://github.com/Lythom/capsule/wiki#upgrading)
☐ Deployed Capsules can be enchanted with "Recall" enchant (https://github.com/Lythom/capsule/wiki#recall)
☐ Deployed Capsules can be re-labelled using shift+right click
☐ Right click → The content is put back in the capsule and removed from the world.

### Recovery capsule
☐ Recovery Capsules can be crafted from a standard linked capsule (https://github.com/Lythom/capsule/wiki#backup)
☐ Recovery Capsules recipes are visible in creative tab and in JEI
☐ Recovery Capsules have an information tab in JEI explaining how they work
☐ Recovery Capsules have informations in the item description (mouse hover in inventory) explaining how they work
☐ Recovery Capsules can be died by crafting it with one or severals dyies
☐ Craft a recovery, throw the recovery, check the original capsule content as been emptied
☐ Recovery Capsules cannot be upgraded (https://github.com/Lythom/capsule/wiki#upgrading)
☐ Recovery Capsules cannot be cleared by crafting it alone in the inventory crafting grid
☐ Recovery Capsules cannot be enchanted with "Recall" enchant (https://github.com/Lythom/capsule/wiki#recall)
☐ Recovery Capsules can be re-labelled using shift+right click
☐ The preview mirrors and all of the mode are available (FRONT_BACK, LEFT_RIGHT, no mirror)
☐ Right click → The content is deployed and the capsule is destroyed

### Blueprint capsule
☐ Blueprint Capsules recipes are visible in creative tab and in JEI
☐ Blueprint Capsules have an information tab in JEI explaining how they work
☐ Blueprint Capsules have information in the item description (mouse hover in inventory) explaining how they work
☐ Blueprint Capsule can be crafted from a linked capsule
☐ Blueprint Capsule can be crafted from an other blueprint capsule
☐ Blueprint Capsule can be crafted from a one-use capsule
☐ Blueprint Capsule can be crafted from a reward capsule
☐ Blueprint Capsules can be died by crafting it with one or severals dyies
☐ Blueprint Capsules can be changed using a linked capsule
☐ Blueprint Capsules can be changed using an other blueprint capsule
☐ Blueprint Capsules can be changed using a one-use capsule
☐ Blueprint Capsules can be changed using a reward capsule
☐ Blueprint Capsules cannot be upgraded (https://github.com/Lythom/capsule/wiki#upgrading)
☐ Blueprint Capsules cannot be enchanted with "Recall" enchant (https://github.com/Lythom/capsule/wiki#recall)
☐ Blueprint Capsules can be re-labelled using shift+right click
☐ Blueprint Capsules cannot be cleared by crafting it alone in the inventory crafting grid
☐ Blueprint Capsules can be recharged (left click while uncharged) using material from player inventory only
☐ materials from player inventory is consumed
☐ Blueprint Capsules can be linked to an inventory (shift + right click on inventory)
☐ The linked inventory is highligthed when the blueprint capsule is in hand
☐ Blueprint Capsules can be recharged (left click while uncharged) using material from linked inventory only
☐ materials from linked inventory is consumed
☐ Blueprint Capsules can be recharged (left click while uncharged) using material shared between player and linked inventory
☐ materials from both inventories are consumed

#### blueprint world recharge
1. create and deploy a blueprint that contains a dispenser and a few blocks
2. remove a block from deployed content
3. right click in air to try to undeploy and recharge from world blocks
☐ The content is not captured back
☐ A chat message tell the player that the structure don't match the blueprint
4. Place back the block anywhere in the blueprint area but not at it's original location
4b. right click in air to try to undeploy and recharge from world blocks
☐ The content is undeployed and the capsule is recharged
5. deploy the structure again
6. add any item in the dispenser
7. right click in air to try to undeploy and recharge from world blocks
☐ The content is not captured back
☐ A chat message tell the player that the structure don't match the blueprint
8. remove the item from dispenser
9. right click in air to try to undeploy and recharge from world blocks
☐ The content is undeployed and the capsule is recharged

#### Whitelist
1. Remove the "minecraft:chest" line from config/capsule/blueprint_whitelist.json
2. Place a chest + dirt block + note block and change the tune of the note block
3. Capture using a standard capsule
4. Craft a blueprint from that standard capsule
☐ A message in chat appears and tell the player the chest block couln't not be included
5. in survival mode, Recharge the blueprint by having a dirt block and a note block in player inventory
6. deploy
☐ The deployed note block have the same pitch as the captured one

#### Prefabs
Create a reward: https://github.com/Lythom/capsule/wiki/Modpack-making#create-a-reward-capsule

1. Create a reward with one iron block, name it "iron"
2. Create a reward with 2 iron blocks and 2 gold blocks, name it "iron_gold"
3. Create a reward with 2 iron blocks, 2 gold blocks and 2 diamond blocks, name it "iron_gold_diamond"
4. Create a reward with 2 iron blocks, 2 gold blocks, 2 diamond blocks and 2 lapis blocks, name it "iron_gold_diamond_lapis"
5. Add all the template created from `config/capsule/rewards/<structure_name>.nbt` into `config/capsule/prefabs/` 
6. Restart minecraft, start a game, look in JEI
☐ There is a blueprint capsule recipe "Iron" that takes an iron block to craft
☐ When crafted, iron block used in the recipe is given back
☐ There is a blueprint capsule recipe "Iron Gold" that takes an iron block and a gold block to craft
☐ There is a blueprint capsule recipe "Iron Gold Diamond" that takes an iron block, a gold block and a diamond to craft
☐ There is a blueprint capsule recipe "Iron Gold Diamond Lapis" that takes 3 ingredients out of the four blocks, among an iron block, a gold block, a diamond to craft and a lapis block


## Blocks 
### Capture Base
☐ The capture base is visible in creative tab
☐ The capture base is visible in JEI, the recipe is accessible, the info tab explains how it works
☐ The capture base have a 3D model in game
☐ The capture base have a 3D item model in hand
☐ The capture base is displayed illuminated when the player have an empty capsule in hand, with a wireframe on its top

## Preview
1. create a structure with a line of dirt and a column of stone in a standard capsule
2. capture the structure
3. right click once and point at the ground
☐ A wireframe preview of the structure appears, the line is displayed as one long box and the column is display as one other long box

## Translations

- set the game in english, all capsule translations are in english.
- set the game in french, all capsule translations are in french.

## Commands
### giveEmpty
https://github.com/Lythom/capsule/wiki/Commands#giveEmpty
    ☐ size 1 gives a capsule that work with instant mode (no throw when deploying)
    ☐ size 30 gives an empty capsule of size 29
    ☐ size 31 gives an empty capsule of size 31
    ☐ overpowered being omitted gives a standard capsule
    ☐ overpowered being true gives an op capsule
    ☐ overpowered being false gives an op capsule

### giveLinked
https://github.com/Lythom/capsule/wiki/Commands#giveLinked

### giveBlueprint
https://github.com/Lythom/capsule/wiki/Commands#giveBlueprint

### exportHeldItem
https://github.com/Lythom/capsule/wiki/Commands#exportHeldItem

### exportSeenBlock
https://github.com/Lythom/capsule/wiki/Commands#exportSeenBlock

### fromHeldCapsule
https://github.com/Lythom/capsule/wiki/Commands#fromHeldCapsule

### fromStructure
https://github.com/Lythom/capsule/wiki/Commands#fromStructure

### fromExistingReward
https://github.com/Lythom/capsule/wiki/Commands#fromExistingReward

### giveRandomLoot
https://github.com/Lythom/capsule/wiki/Commands#giveRandomLoot
- ☐ With configuration allowBlueprintReward = true, it should often give blueprints
- ☐ With configuration allowBlueprintReward = false, it should never give blueprints

### reloadLootList
https://github.com/Lythom/capsule/wiki/Commands#reloadLootList

### setBaseColor
https://github.com/Lythom/capsule/wiki/Commands#setBaseColor

### setMaterialColor
https://github.com/Lythom/capsule/wiki/Commands#setMaterialColor

### setAuthor
https://github.com/Lythom/capsule/wiki/Commands#setAuthor
 
## Features
### starters
Create a reward: https://github.com/Lythom/capsule/wiki/Modpack-making#create-a-reward-capsule

1. Create a reward with one iron block, name it "iron"
2. Create a reward with 2 iron blocks and 2 gold blocks, name it "iron_gold"
3. Remove all files from `config/capsule/starters/` 
4. Add the template created from `config/capsule/rewards/<structure_name>.nbt` into `config/capsule/starters/` 
5. in config/capsule-common.toml, configure `starterMode = "all"`
6. Start a new game (no restart required, config should auto reload starters when changed)
- ☐ The player is given a "Iron" and a "Iron Gold" capsules that match the created rewards.
7. Leave to main menu and re-enter the same game, no new starters are given and previously given starters are still there
8. Leave to main menu, in config/capsule-common.toml, configure `starterMode = "random"`
9. Start a new game
- ☐ The player is given either the "Iron" or the "Iron Gold" capsule.
10. Leave to main menu, in config/capsule-common.toml, configure `starterMode = "none"`
11. Start a new game
- ☐ The player is given no capsules at all.

### deploy
#### instant
1. Craft an empty wooden capsule of size 1
- ☐ The description should mention the instantaneous mode
- ☐ Any block can be captured without capture base
- ☐ The block captured can be deployed instantly without throwing the capsule

#### overridableBlocks
1. Check in `config/capsule-common.toml` that overridabledBlock contains an entry for "minecraft:grass"
2. Start a new game, deploy the capsule in a grass field
- ☐ The grass is removed and replaced by the blocks of the starter
3. use bonemeal to grow some grass in the starter area
4. undeploy
- ☐ The grown grass is undeployed within the capsule
5. deploy in a clear area
- ☐ The grown grass is deployed
6. note the exacted deployed location, undeploy the capsule
7. put a stone block where the grown grass is supposed to be deployed
8. deploy the capsule at the exact previous location
- ☐ The deploy works and grown grass that were inside the capsule is not deployed (removed)

#### transdimentional
1. Deploy a starter in the overworld
2. move to nether using a nether portal
3. undeploy the starer (located in overworld) and deploy it in the nether
- ☐ The starter is entierly present in the nether deployment

#### transdimentional 2
1. Create a capsule of size 3 (iron capsule) and capture one gold block inside
2. Activate the linked capsule and throw it throw a nether portal
3. enter the portal
- ☐ The gold block is deployed and the capsule is on the ground

4. Redo the exact same scenario but enchant the capsule with recall
- [if the nether is chunkloaded] ☐ The gold block is deployed in nether just after the throw and the caspule get back in thrower hand
- [if the nether is not chunkloaded] ☐ The gold block is deployed in nether when entering the portal and the caspule get back in thrower hand


#### underwater
1. Move underwater in a sea and deploy a starter there
- ☐ The capsule is deployed on the sea ground

2. Unploy then get out of water a throw the capule on top of water
- ☐ The capsule is deployed over the sea level

#### automated


### initial capture 
#### instant
#### from capture base
#### excludedBlocks
#### excludedOpBlocks
#### transdimentional
#### underwater

### undeploy
#### instant
#### excludedBlocks
#### excludedOpBlocks
#### transdimentional
#### underwater

### whitelist

### opWhitelist

### Blueprints

### throw from machine

### Relabeling

### Dynamic prefabs recipes
1. Add nbt files in "config/capsule/prefabs"
☐ Recipes should be automatically generated to craft this prefab

### Loot
1. In config, cange lootTablesList to only ["minecraft:chests/desert_pyramid"] and loot.lootTemplatesPaths to have only 
```toml
[[loot.lootTemplatesPaths]]
    path = "config/capsule/loot/rare"
    weight = 2
```
☐ Find a desert pyramid in creative on a standard world and check the chest contains a rare loot.
☐ Find an underground dungeon in spectator mode, switch back to creative and look in the chest, there should be no capsule.
2. remove your capsule-common.toml file in config folder to reset the config to default for other tests.