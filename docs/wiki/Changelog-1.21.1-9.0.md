# Capsule 9.0 for Minecraft 1.21.1: Fabric, Loyalty, claim mods

<!-- media: banner.png, the logo or a wide screenshot of a deployed base (optional) -->

Capsule comes to Minecraft 1.21.1 on **NeoForge** and **Fabric**, with the same features, recipes and config file on both. Download it on [CurseForge](https://www.curseforge.com/minecraft/mc-mods/capsule/files) or [Modrinth](https://modrinth.com/mod/capsule/versions). Full list of changes in the [changelog](https://github.com/Lythom/capsule/blob/master/CHANGELOG.md).

## Fabric

The Fabric build (`Capsule-fabric-1.21.1-...jar`) needs Fabric API and Forge Config API Port. The NeoForge jar is now named `Capsule-neoforge-1.21.1-...jar` and needs NeoForge 21.1 or later.

<!-- media: fabric.png, the same capsule deployed on Fabric (e.g. with the mod menu showing Capsule) -->

## Loyalty instead of Recall

Enchant a capsule with vanilla Loyalty (enchanting table, or a book on an anvil) and it comes back once thrown. Capsules already enchanted with Recall keep coming back. See [Loyalty](Home#loyalty).

<!-- media: loyalty.gif, a Loyalty capsule thrown, deployed and flying back to the inventory -->

## Fire and lava proof

Capsules no longer burn, existing ones included: a capsule thrown into lava deploys, and comes back with Loyalty.

<!-- media: lava.gif, a capsule thrown into lava that deploys and comes back -->

## New capsule tiers

Amethyst and quartz (5), prismarine crystals (9) and netherite (13, the vanilla 13x13x13 capsule); with mods: zinc and aluminum (3), osmium (5), brass (7), steel and uranium (9). See [Capsule tiers](Home#capsule-tiers).

<!-- media: tiers.png, recipe viewer page or inventory showing the new tiers -->

## Translucent preview

The full deploy preview is translucent and no longer hides what is behind it.

<!-- media: translucent-preview.png, before/after of the preview over terrain or over the deployed structure -->

## Capture animation

Captured blocks are sucked into the capsule with a particle trail. It can be turned off with `captureAnimation = false` in `config/capsule-client.toml`.

<!-- media: capture-animation.gif, a house captured on a Capture Base -->

## Claim mods

Captures and deploys respect Open Parties and Claims and Flan (NeoForge and Fabric), Get Off My Lawn ReServed (Fabric) and the other protection mods through a block placement check. Capture Bases act for the player who placed them, and when a claim mod cannot be checked, captures and deploys are refused instead of ignoring its claims. See [Claim protection](Home#claim-protection).

<!-- media: claims.gif, a capture next to a claim: the claimed blocks stay in place, then a refused deploy with its chat message -->

## REI and EMI

The capsule recipes and information pages show in REI and EMI, and in JEI on Fabric too, with the recovery and blueprint recipes.

<!-- media: rei-emi.png, the capsule recipes in REI and EMI -->

## Schematics

Sponge v3 schematics and `.schem` files (the WorldEdit 7.3 default) can be used as templates, next to `.nbt`, MCEdit and Sponge v1 and v2 files. See [Template files](Modpack-making#template-files).

## Blueprint whitelist for 1.21.1

Signs keep their text, banners their patterns, and heads, campfires, shulker boxes, ender chests, decorated pots, chiseled bookshelves and crafters can be used in blueprints. Inventories are never kept. Existing installs: delete `config/capsule/blueprint_whitelist.json` to get the new list.

<!-- media: blueprint-signs.png, a blueprint deploying a structure with signs and banners -->

## Major bug fixes

- Prefab blueprint recipes giving back the wrong items, and shift-click crafting of blueprints and recovery capsules creating extra capsules (#84)
- Blueprints created empty without a message when their template could not be read (#124)
- Server crash or kick with install paths containing reserved Windows names, e.g. Flatpak ATLauncher (#125)
- Furnaces dropping their stored experience on every capture (#122)
- SecurityCraft blocks captured by players who do not own them (#119)
- Deploys floating above snow layers and grass (#116)
- Starter and reward capsules sharing block entity data with their template, which emptied Sophisticated Storage containers (#115)
- "Invalid player data" disconnections with mods serializing loot tables (#109)
- Recall bringing capsules back before they could deploy (#98)
- Blind throws deploying one block above the ground (#89)
- Instant capsules that could not be undeployed after a relog or restart (#75)
- Blocks without item (potted plants…) free in blueprints (#56)
- Capsules never appearing in dungeon loot on 1.21.1
- Template files with uppercase letters or spaces disconnecting players on login
- The full preview hardened against crashes with modded blocks (Ad Astra, Integrated Dynamics, farmland, Mob Grinding Utils) (#117, #94, #76, #81); the preview and the capture animation show with Iris shader packs (#69)
- The starter huts' axe item frame covering the crafting table (#126): delete `config/capsule/starters` to get the new huts

The bug fixes and the claim protection are also in the 1.20.1, 1.18.2 and 1.16.5 Forge builds released with 9.0.
