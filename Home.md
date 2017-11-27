# Overview (1.10.2)

Bring your base! Capsules can capture a region containing any blocks or machines, then deploy and undeploy at will. Inspired by Dragon Ball capsules.

> ![Initial Capture](http://i.imgur.com/shwmqn9.gif)

If you are a modpack maker or a content creator, you may want to check to [How To's page](https://bitbucket.org/Lythom/mccapsule/wiki/Modpack%20maker%20How%20To's) !

---
This page is an overview of the mod, [see all wiki entries](https://bitbucket.org/Lythom/mccapsule/wiki/browse/) for more details.

Page content :

[TOC]

---

## Getting started ##

You first need a capsule base. This is where you can initialize a capsule with it's first content. You'll be able to capture the region on the top of it. Place it somewhere just below what you want to capture, or in a free space and build on top of it.

> ![capture-base-recipe.png](https://bitbucket.org/repo/GMLRrB/images/753172639-capture-base-recipe.png)
> 
> "Capture Base" recipe

Then you need to craft at least one empty capsule (the top item is a stone button) :

> ![capsule-iron-recipe.png](https://bitbucket.org/repo/GMLRrB/images/1624303790-capsule-iron-recipe.png)
>
> "Iron Empty Capsule" recipe, default capture size : 1x1x1
>
> ![capsule-gold-recipe.png](https://bitbucket.org/repo/GMLRrB/images/4145052491-capsule-gold-recipe.png)
>
> "Gold Empty Capsule" recipe, default capture size : 3x3x3
>
> ![capsule-diamond-recipe.png](https://bitbucket.org/repo/GMLRrB/images/3956664424-capsule-diamond-recipe.png)
>
> "DiamondEmpty Capsule" recipe, default capture size : 5x5x5

Capture the zone defined by the capture base, the capture zone will be visualized when holding the Empty Capsule.
Usage (empty capsule) :

* Right click : activate
* Right click while activated  : throw the capsule.
* Once the content is captured, the capsule can be deployed or undeployed at will. 

Note that all the capsule can be dyed (affect the base color) and labeled (sneak + right click).

> ![Initial Capture](http://i.imgur.com/shwmqn9.gif)
>
> Demo initial capture

Usage (linked capsule)  :

* Deploy : Right click once to activate, right click again to throw at the preview positions (are ahead not aiming a block).
* Undeploy : Right click the "Deployed" capsule and the content will be stored again into the capsule, whereever it currently is.
* Label : Sneak + Right click to open the label editing screen.

> ![Demo deploy / undeploy](http://i.imgur.com/FgEsbkr.gif)
>
> Demo deploy / undeploy

## Upgrading ##

You need more space ? Here is the upgrade recipe (only works with empty capsule that have no content yet) :
 
> ![capsule-upgrade-recipe.png](https://bitbucket.org/repo/GMLRrB/images/2118435896-capsule-upgrade-recipe.png)
>
> "capsule upgrade" recipe, default max upgrades : 10

You can add several Popped Chorus Fruit at a time.

> ![Demo Upgrade](http://i.imgur.com/FBc7t3w.gif)
>
> Demo Upgrade

## Backup ##

It's highly recommanded that you create a recovery capsule and place it in a safe place ! If the capsule is lost (thrown in lava, on a cactus (…), at an impossible death location) a recovery capsule will allows you to get back the content.

> ![capsule-recovery-recipe.png](https://bitbucket.org/repo/GMLRrB/images/1578832247-capsule-recovery-recipe.png)
> 
> "Recovery capsule" recipe


## Recall ##

This mod also adds a unique enchantment : Recall.

Effect : whenever a recall enchanted item is dropped and touches the ground, it comes back into the thrower inventory after a last update. When in contact of water or lava the recall is immediate (prevent burning).

This enchant is of courses intended to be used on capsules. Without this enchantment you have to pick up the thrown capsule manually.
It can as well be applied by default (configurable) on any enchantable item. Can be usefull to fight against inventory dropping monsters or to prevent unintentional drop.

# FAQ

**Will block X from mod Y work with capsules ?**

If compatible with vanilla structure blocks : yes  
If not compatible with vanilla structure blocks : no.  
Before reporting vanilla structure block compatibility issue to the mod author, [Check the Known incompatibilities](https://bitbucket.org/Lythom/mccapsule/wiki/Known%20incompatibilities). Issues have already been open for those.

**Can I use in my modpack ?**
Sure ! Please simply give a link to this page and mention Lythom as the mod author.

If any problem with the mod feel free to PM or to submit an issue http://minecraft.curseforge.com/projects/capsule/settings/issues.

**Is this open source ?**
Yes, there : [https://bitbucket.org/Lythom/mccapsule](https://bitbucket.org/Lythom/mccapsule).

Code, Textures and binaries are licensed under the MIT License.

Pull request are welcome if it fits the capsule feature, but please notify me by PM before coding so that I give you a direct feedback on the PR acceptance chances.