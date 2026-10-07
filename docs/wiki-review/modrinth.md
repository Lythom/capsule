## Capsule mod

![Capsule Mod Logo](https://raw.githubusercontent.com/wiki/Lythom/capsule/images/logo.png)

*Bring your base! Capsules can capture a region containing any blocks or machines, then deploy and undeploy at will. Inspired by Dragon Ball capsules.*

**Join us on Discord! [https://discord.gg/wZpBVdr](https://discord.gg/wZpBVdr)**

**Detailed documentation on the wiki: [https://github.com/Lythom/capsule/wiki](https://github.com/Lythom/capsule/wiki)**

## Versions

* **Minecraft 1.21.1**: NeoForge and Fabric (Fabric needs Fabric API and Forge Config API Port). JEI, REI and EMI supported.
* **Minecraft 1.20.1, 1.18.2 and 1.16.5**: Forge.
* Older versions (1.20.4, 1.19.2, 1.15.2, 1.12.2 and before) stay available in the files: [Previous versions](https://github.com/Lythom/capsule/wiki/Previous-versions).

## Biggest Features

* [Move your base!](https://github.com/Lythom/capsule/wiki/Changelog-1.12.2-Bring-your-base-update) An entire base packed in a capsule (up to 33x33x33 with a fully upgraded netherite capsule) can be moved entirely, including machines. You can be a real traveler now.
* [Build faster with blueprints!](https://github.com/Lythom/capsule/wiki/Changelog-1.12.2-Builders-daydream-update) Blueprint capsules can take materials from chests or other inventories to duplicate a structure. They support rotation and mirroring.
* [Modpack making options](https://github.com/Lythom/capsule/wiki/Modpack-making), so that players can be rewarded with ready-to-deploy structures or loot them in chests.
* A ton of possibilities! Capsules can be used as an early backpack moving a chest, as a portable ladder, to deploy protecting walls to recover during a fight, to move machines or multiblocks, [and more…](https://github.com/Lythom/capsule/wiki/Ideas-and-uses) Unleash your creativity!

## New in 9.0 (Minecraft 1.21.1)

* **Fabric** support, next to NeoForge.
* Thrown capsules come back with vanilla **Loyalty** (it replaces the Recall enchantment).
* Capsules are **fire and lava proof**.
* **New tiers**: amethyst, quartz, prismarine and **netherite (13x13x13)**, plus more modded metals.
* **Translucent preview**: see what is behind the structure you are about to deploy.
* **Capture animation**: captured blocks are sucked into the capsule.
* **Claim mods respected**: Open Parties and Claims, Flan, Get Off My Lawn and other protection mods.
* Recipes and information pages in **JEI, REI and EMI**.
* Sponge v3 and `.schem` schematics as templates, and an updated blueprint whitelist.

Full list in the [9.0 changelog](https://github.com/Lythom/capsule/wiki/Changelog-1.21.1-9.0).

## Getting started

The first capsule you will have access to is made of wood. Its size is 1x1x1, which enables the "instant mode".

> ![empty, stone button, empty - wood plank, chest, wood plank - empty, wood slab, empty](https://raw.githubusercontent.com/wiki/Lythom/capsule/images/recipes/capsule-wood.png)
>
> "Wooden capsule" recipe

It means you can capture any block (i.e. a chest or a crafting table) by right clicking it, then deploy and undeploy it instantly by right clicking again with the capsule in hand.

If right clicking opens a GUI, try to capture from a distance or while sneaking.

### Getting bigger

To get to the big things, you first need a Capture Base. This is where you can initialize a capsule with its first content: place it while looking down, below what you want to capture (or in a free space, and build on top of it), and the region on top of it will be captured.

> "Capture Base" recipe: a compass on top, a torch in the middle and a dispenser at the bottom, surrounded by 6 cobblestone.

Then you need an empty capsule (the top item is a stone button):

> ![stone button, iron ingot, ender pearl, iron ingot, iron ingot](https://raw.githubusercontent.com/wiki/Lythom/capsule/images/recipes/capsule-iron.png)
>
> "Iron Empty Capsule" recipe, default capture size: 3x3x3

> ![stone button, gold ingot, ender pearl, gold ingot, iron ingot](https://raw.githubusercontent.com/wiki/Lythom/capsule/images/recipes/capsule-gold.png)
>
> "Gold Empty Capsule" recipe, default capture size: 5x5x5

> ![stone button, diamond, ender pearl, diamond, iron ingot](https://raw.githubusercontent.com/wiki/Lythom/capsule/images/recipes/capsule-diamond.png)
>
> "Diamond Empty Capsule" recipe, default capture size: 7x7x7

*Obsidian (9x9x9), Emerald (11x11x11) and Netherite (13x13x13) capsules also exist, and many more materials: check JEI, REI or EMI!*

**Empty Capsule**

* Right click: activate.
* Right click while activated: throw the capsule. Throw it near the Capture Base!
* Once the content is captured, the capsule can be deployed or undeployed at will.

> ![Initial capture demo](https://raw.githubusercontent.com/wiki/Lythom/capsule/images/demo/initial-capture.gif)
>
> Initial capture

Note that all the capsules can be dyed (it changes the base color) and labeled (sneak + right click).

**Linked capsule**

* Deploy: right click once to activate, right click again to throw at the previewed position (or straight ahead when not aiming at a block).
* Undeploy: right click the "Deployed" capsule and the content will be stored again into the capsule, wherever it currently is.
* Label: sneak + right click to open the label editing screen.

> ![Deploy / undeploy demo](https://raw.githubusercontent.com/wiki/Lythom/capsule/images/demo/deploy-undeploy.gif)
>
> Deploy / undeploy

Enchant your capsules with **Loyalty** to have them come back after deploying, and upgrade empty capsules with popped chorus fruits to capture bigger regions!

## Rotations

**Linked** capsules can be rotated and mirrored by players using left click / sneak + left click while previewing deployment. All vanilla blocks and non-living entities (minecarts, etc.) are supported, but modded blocks with specific behaviors (machines, etc.) will prevent a rotation by default. This is to prevent messing with modded blocks that have specific considerations about their orientation that Capsule cannot know.

The "can rotate" state might need to be updated when you first acquire a capsule (blueprints will always rotate): if rotation is locked and you think it should work, deploy and undeploy the capsule to update it and be able to rotate. If it still can't, there is a special block in the content that does not support rotation.

As a modpack maker, if you want Capsule to still try to rotate some specific block, you can whitelist it for blueprints: it will allow the block both to rotate in regular capsules and to be used in blueprints. Check out the [Blueprint whitelist section](https://github.com/Lythom/capsule/wiki/Modpack-making#whitelist).

## Other features and modpackers documentation

### Wiki

Everything else (blueprints, recovery capsules, upgrades, Loyalty, claim protection, commands, modpack making, FAQ) is on the wiki: [https://github.com/Lythom/capsule/wiki](https://github.com/Lythom/capsule/wiki)

* [Player guide](https://github.com/Lythom/capsule/wiki)
* [Modpack making](https://github.com/Lythom/capsule/wiki/Modpack-making)
* [Commands](https://github.com/Lythom/capsule/wiki/Commands)
* [FAQ](https://github.com/Lythom/capsule/wiki#faq)
