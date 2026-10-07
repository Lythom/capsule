## 1. If your mod has some incompatibilities you want to fix

Tips:
- Anything working with vanilla structure blocks will work with Capsule. The easiest way to test is to set up 2 structure blocks, one saving to a template, the other loading the template. Adjust the code until the loaded area works as expected compared to the saved one.

- Due to the way Capsule works, no block entity can be duplicated. It means that a block entity carrying a unique id is safe, as it won't be duplicated: it will always be removed before being placed again (possibly elsewhere).

- Note that the use of absolute coordinates to find locations inside the capsule will break (the content will be moved). When it makes sense, relative coordinates should always be preferred over absolute coordinates.

- Capsule rotates and mirrors the content with the vanilla `rotate` and `mirror` methods of blocks and entities. Modded block entities are not rotated unless the modpack whitelists them, see [Blueprint whitelist](Modpack-making#whitelist).

## 2. If your mod has incompatibilities with Capsule and you want Capsule to ignore your block(s)

Are you sure it can't be fixed? :'(    
If so, it shouldn't take long, follow the steps for the version you are targeting:

### 1.15 or newer
Add the blocks to exclude to the `capsule:excluded` block tag.

[since 8.0 for 1.20.4] Capsule also excludes the blocks of the common tags `c:relocation_not_supported` and `c:immovable`: if your mod already puts its blocks in one of them (for other moving mods, like Waystones does), there is nothing to do.

Documentation: https://minecraft.wiki/w/Tag.     
TLDR: there should be a `data/capsule/tags/block/excluded.json` file (`data/capsule/tags/blocks/excluded.json` before Minecraft 1.21) in your mod resources folder that contains something like the example below. Be sure that `replace` is `false` to keep the configuration working for other mods. The block ids to exclude go into the values array.

Minecraft 1.21 and newer:
```json
{
    "replace": false,
    "values": [
        { "id": "#tombstone:player_graves", "required": false }
    ]
}
```

Minecraft 1.15 to 1.20 (Forge):
```json
{
    "replace": false,
    "values": [],
    "optional": [
        "#tombstone:player_graves"
    ]
}
```
In these examples, an optional entry (`"required": false`, or the Forge `optional` list) should be used if the mod is not a required dependency. The `#` before `tombstone:player_graves` indicates that all the blocks under the tag `tombstone:player_graves` should be included here. The value could also be a block id without `#` to refer to a single block instead of another tag.
 
### 1.12
Open an issue on GitHub and provide the block id to be excluded by default. I'll update the mod with the new configuration.

### Older
Not updated anymore. You can still add the block id in the `capsule.cfg` file under "opExcludedBlocks" when distributing a modpack.

## 3. If your mod protects areas (claims)

[since 9.0] Capsule asks claim mods before capturing or deploying. Without anything to do on your side:

* NeoForge: Capsule fires a block placement event (`BlockEvent.EntityPlaceEvent`, dirt) for the acting player at the positions it changes: every position up to size 31, one position per chunk column for bigger capsules. Cancel it in your claims and Capsule leaves them alone.
* Fabric: Capsule asks [Common Protection API](https://github.com/Patbox/common-protection-api) (`CommonProtection.canPlaceBlock`) the same way.
* The acting player may be a fake player with the profile of the real player (a Capture Base acts for the player who placed it, a capsule thrown by a player who went offline for that player) or the anonymous fake player `[Capsule]` (UUID `9c0b9b7b-b356-41c0-93b2-4bb6afe1586c`) when nobody is behind the capture: refuse it in your claims.

For exact and fast checks (your claims checked once per chunk or claim instead of per block), Capsule has dedicated support for Open Parties and Claims, Flan and Get Off My Lawn ReServed. Another claim mod can add its own: implement `capsule.plugins.claims.ClaimAdapter` (it returns the claims intersecting a box and whether the player may change them; `ClaimAdapter.perChunk` helps for chunk claims) and register it with `Claims.register(adapter)`, or `Claims.load("yourmodid", () -> new YourAdapter())` to have Capsule refuse every capture and deploy when your API is missing. Ask on [GitHub](https://github.com/Lythom/capsule/issues) or [Discord](https://discord.gg/wZpBVdr) before relying on it: this API is new in 9.0.
