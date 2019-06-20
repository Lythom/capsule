# Overview

Bring your base! Capsules can capture a region containing any blocks or machines, then deploy and undeploy at will. Inspired by Dragon Ball capsules.

![Deploy / Undeploy demo](https://imgur.com/5q1Q8Uf.gif)

- [Overview](#overview)
  * [Getting started](#getting-started)
    + [Empty Capsule](#empty-capsule)
    + [Linked Capsule](#linked-capsule)
  * [Upgrading](#upgrading)
  * [Backup](#backup)
  * [Recall](#recall)
  * [Blueprints](#blueprints)
  * [Overpowered Capsules](#overpowered-capsules)
- [FAQ](#faq)

<small><i><a href='http://ecotrust-canada.github.io/markdown-toc/'>Table of contents generated with markdown-toc</a></i></small>


## Getting started ##

The first capsule you will have access to is made of wood. It's size is 1x1x1 which enables the "instant mode".

> ![empty, stone button, empty - wood plank, chest, wood plank - empty, wood slab, empty](https://imgur.com/cNPwMo5.png)
> 
> "Wooden capsule" recipe

It means you can capture any block (ie. a chest or a crafting table) by right clicking it, then deploy and undeploy it instantly by right clicking again with the capsule in hand.

If right click open a GUI, try to capture from a distance or while sneaking.


## Getting bigger##

To go bigger you need a capture base. This is where you can initialize a capsule with it's first content. You'll be able to capture the region on the top of it. Place it somewhere just below what you want to capture, or in a free space and build on top of it.

> ![capture-base-recipe.png](https://imgur.com/X7w0NVj.png)
> 
> "Capture Base" recipe

Then you need to craft at least one empty capsule (the top item is a stone button) :

> ![capsule-iron-recipe.png](https://imgur.com/AKGlHQo.png)
>
> "Iron Empty Capsule" recipe, default capture size : 1x1x1
>
> ![capsule-gold-recipe.png](https://imgur.com/Eb5tAsu.png)
>
> "Gold Empty Capsule" recipe, default capture size : 3x3x3
>
> ![capsule-diamond-recipe.png](https://imgur.com/hvNMJcm.png)
>
> "DiamondEmpty Capsule" recipe, default capture size : 5x5x5

Obsidian (9x9x9) and Emerald (11x11x11) capsules also exists, check JEI !


### Empty Capsule

* Right click : activate
* Right click while activated  : throw the capsule.
* Once the content is captured, the capsule can be deployed or undeployed at will. 

[Click to see the Initial capture demo    
![](https://imgur.com/J10q1ey.png)](https://i.imgur.com/WLG3AeZ.gifv)

Note that all the capsules can be dyed (affect the base color) and labeled (sneak + right click).

### Linked Capsule

Usage  :

* Deploy : Right click once to activate, right click again to throw at the preview positions (are ahead not aiming a block).
* Undeploy : Right click the "Deployed" capsule and the content will be stored again into the capsule, wherever it currently is.
* Label : Sneak + Right click to open the label editing screen.

![Deploy / Undeploy demo](https://imgur.com/5q1Q8Uf.gif)

## Upgrading ##

You need more space ? Here is the upgrade recipe (only works with empty capsule) :
 
> ![capsule-upgrade-recipe.png](https://imgur.com/9Jg8pUQ.png)
>
> "capsule upgrade" recipe, default max upgrades : 10

You can add several Popped Chorus Fruit at a time.

> ![Demo Upgrade](https://imgur.com/Oq3wSAZ.gif)
>
> Demo Upgrade

## Backup ##

It's highly recommanded that you create a recovery capsule and place it in a safe place ! If the capsule is lost (thrown in lava, on a cactus (…), at an impossible death location) a recovery capsule will allows you to get back the content.

> ![capsule-recovery-recipe.png](https://imgur.com/pASsF2b.png)
> 
> "Recovery capsule" recipe


## Recall ##

This mod also adds a unique enchantment : Recall.

Effect : whenever a recall enchanted item is dropped and touches the ground, it comes back into the thrower inventory after a last update. When in contact of water or lava the recall is immediate (prevent burning).

This enchant is intended to be used on capsules. Without this enchantment you have to pick up the thrown capsule manually.
It can as well be applied on any enchantable item by configuration (disabled by default). Can be useful to fight against inventory dropping monsters or to prevent unintentional drop.


## Blueprints ##

Blueprints capsule can be reloaded in order to deploy several times the same structure. You can link an inventory with sneak + right click to use it a source of building materials.

When unloaded, left click in the air to load from linked inventory and player inventory. If all materials are available they will be consumed to charge the capsule. Else, missing materials will be displayed.
You can also Right click to undo the last deployment.

When loaded :
- Right click to deploy
- Left click to rotate
- Sneak + Left click to mirror

See https://imgur.com/gallery/vN1sJrf for a showcase.

## Overpowered Capsules ##

Overpowered capsules can capture blocks that cannot be captures with standard capsules. Blocks that can be captured only by overpowered capsules can be configured in capsule.cfg by adjusting the excludedBlocks for standard and op capsules. By default the config is :

```
# List of block ids that will never be captured by a non overpowered capsule. While capturing, the blocks will stay in place.
S:excludedBlocks <
minecraft:bedrock
minecraft:mob_spawner
minecraft:end_portal
minecraft:end_portal_frame
minecraft:air
minecraft:structure_void
ic2:te
>

 

# List of block ids that will never be captured even with an overpowered capsule. While capturing, the blocks will stay in place.
S:opExcludedBlocks <
minecraft:air
minecraft:structure_void
ic2:te
>
```

That means that by default a standard capsule cannot capture bedrock, mob spawners or end portals whereas overpowered capsules can. OP Capsules can be crafted using a netherstar instead of an ender pearl.

# FAQ

**Will block X from mod Y work with capsules ?**

If compatible with vanilla structure blocks : yes.    
If not compatible with vanilla structure blocks : no.    
Latest versions of capsule have been tested a lot with very good results, give it a try ! Don't forget to backup your saves anyway it's always good to have one.

**Can I use in my modpack ?**

Sure ! Please simply give a link to this page and mention Lythom as the mod author.

If any problem with the mod feel free to PM or to submit an issue [https://github.com/Lythom/capsule/issues](https://github.com/Lythom/capsule/issues).

**How to create capsules with custom structures to put in my pack ?**

All the information is at [https://github.com/Lythom/capsule/wiki/Modpack-making](https://github.com/Lythom/capsule/wiki/Modpack-making).

**1.7.10 version ?**

Capsule now rely on structure blocks mechanics, so I'll only maintain 1.10+ versions.

**Is this open source ?**

Yes, there : [https://github.com/Lythom/capsule](https://github.com/Lythom/capsule).

Code, Textures and binaries are licensed under the MIT License.

**I have another question !**

You can get help either to use capsule mod as a player, or to configure it into your modpack via the following channels : 

* Github issues: https://github.com/Lythom/capsule/issues    
* Discord server: https://discord.gg/wZpBVdr    