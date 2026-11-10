# Capsule item formats

A capsule keeps its data in its NBT; its base color (the body) is the vanilla `display:{color:...}`. Modpacks can write
these items in `/give`, loot tables, quest rewards or recipe outputs (KubeJS, CraftTweaker...).
`/capsule exportHeldItem` prints the held capsule in this syntax.

## Keys

| Key | Type | Meaning |
|---|---|---|
| `state` | int | `0` empty, `1` activated, `2` linked, `3` deployed, `4` empty activated, `5` one-use, `6` one-use activated, `7` blueprint |
| `size` | int | side of the capsule cube, odd, at most 255 (an even size is rounded up) |
| `color` | int | material color (the cap) |
| `label` | string | name shown on the item |
| `structureName` | string | the template of the capsule, see below |
| `oneUse` | byte | consumed when deployed |
| `isReward` | byte | with `oneUse:1b`, deploys the config template `structureName` directly, never changes it |
| `templateShouldBeCopied` | byte | linked capsules: copy the config template `structureName` into a template of the capsule's own, see below. Blueprints: copy the source template into a blueprint template |
| `sourceInventory` | compound | makes the capsule a blueprint, whatever its `state` (`{}` for no linked inventory) |
| `overpowered` | byte | can capture the blocks of `opExcludedBlocks` |
| `upgraded` | int | upgrades applied (popped chorus fruit) |
| `author` | string | shown in the tooltip |
| `canRotate` | byte | set by the mod from the template |

## Templates

- A linked capsule (`state:2`) captured by a player points at a template of the world, `<world>/capsules/<structureName>.nbt`.
  Deploying it empties that template and capturing back fills it: the capsule owns it.
- A reward (`isReward:1b`, `oneUse:1b`) points at a template of the game folder, by its path from there:
  `config/capsule/rewards/my_house` for `config/capsule/rewards/my_house.nbt`.
  Rewards deploy this template without changing it, and are consumed.
- `/capsule giveLinked my_house` gives a reusable linked capsule with its own copy of `config/capsule/rewards/my_house`.

## Crafting linked capsules from a config template

A linked capsule with `templateShouldBeCopied:1b` and a `structureName` under `config/` gets, as soon as it is crafted,
the same capsule as `/capsule giveLinked`: its own copy of the template in the world, named after the player, that it
deploys, captures back and deploys again like any capsule. Each craft gets its own copy. The item keeps its colors,
label and size; a template larger than `size` makes the capsule larger.

```
/give @p capsule:capsule{color:6196919,label:"burner_researcher",size:3,state:2,structureName:"config/capsule/rewards/burner_researcher",templateShouldBeCopied:1b,display:{color:8072841}}
```

Recipe outputs (KubeJS, CraftTweaker...) take the same NBT.

- Do not add `sourceInventory`: with it the capsule is a blueprint whatever its `state`, and the copy keeps only the
  blocks of `blueprint_whitelist.json`, without their inventories.
- The copy is complete, like a reward or `/capsule giveLinked`: inventories, machine buffers and any item or fluid saved
  in the template are created again with each craft. Clean the template before shipping it.
- Only templates under `config/` (or under `rewardTemplatesPath`) are copied: a `structureName` naming a template of the
  world, captured by a player, is refused.
- The copy happens when the crafting table gives the item, or, for items crafted otherwise (autocrafters, commands),
  when the item reaches a player inventory, or at the latest when the capsule is deployed or dispensed. With no player,
  the template is named after the mod (`c-capsulemod-...`).
- When the template is missing or empty, nothing is copied: the server log names the template and the searched paths,
  the player is told, and the capsule refuses to deploy until the template exists (add it, then `/reload`).
- With `oneUse:1b` (and no `isReward`), the capsule deploys its copy once and is consumed.
