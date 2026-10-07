# Known incompatibilities

Checked on 2026-10-07 with Capsule 9.0 (NeoForge 1.21.1) and Capsule 8.0 (Forge 1.20.1), with the mod versions listed
below. Each case is an automated test that captures and deploys the mod's blocks in a real game, see
[How it is tested](#how-it-is-tested).

Excluded blocks are never captured: they stay in place when a capsule captures the area around them. Capsule excludes:

- on 1.21.1, every block tagged `c:relocation_not_supported` or `c:immovable` (most mods tag the blocks that break when
  moved) and the blocks of the `capsule:excluded` tag;
- the blocks listed in `excludedBlocks` (standard capsules) and `opExcludedBlocks` (all capsules) in
  `config/capsule-common.toml`, see [Excluded blocks](Modpack-making#excluded-blocks). To exclude a block yourself, add
  its id to both lists.

On 1.20.1, the next 8.0.x build adds `#forge:relocation_not_supported`, `bloodmagic:alchemytable` and the Immersive
Engineering wire connectors to both lists **for new installs**. The config file is never rewritten: on an existing
install, add the entries given below yourself.

## Summary

| Mod | 1.21.1 (Capsule 9.0) | 1.20.1 (Capsule 8.0) |
|---|---|---|
| Corail Tombstone graves | never captured (by design) | never captured (by design) |
| Refined Storage | excluded by default; works when not excluded | excluded by default; disks lost when not excluded |
| Super Factory Manager | moves, program needs relabelling | moves, program needs relabelling |
| Mekanism Digital Miner | never captured | breaks when moved: exclude it (new installs: excluded) |
| Mekanism, other machines | move with their content | move with their content |
| Blood Magic alchemy table | not tested (no 1.21.1 version found) | disappears when moved: exclude it (new installs: excluded) |
| Immersive Engineering wires | connectors never captured | wires lost when moved: exclude the connectors (new installs: excluded) |
| GregTech CEu Modern | not tested | moves with its content |
| Integrated Dynamics | moves; cables and parts not drawn in the preview | not tested |
| Waystones, SecurityCraft | never captured | not tested |
| IndustrialCraft 2 | no version for 1.21.1 | no version for 1.20.1 |

## By design

**Corail Tombstone**: player graves are never captured. Tombstone adds its graves to `capsule:excluded`, and Capsule
excludes `#tombstone:player_graves` too. Forcing graves out of the exclusions was reported to duplicate their items.
Tested: Tombstone 9.5.6 (1.21.1), 9.1.5 (1.20.1).

## Important

**Refined Storage**: excluded by default (`refinedstorage:` in `excludedBlocks` and `opExcludedBlocks`).
- 1.21.1 (Refined Storage 2.0.9): with the exclusion removed, a disk drive moves with its disks and their content.
- 1.20.1 (Refined Storage 1.12.4): a moved disk drive loses its disks and their content (Refined Storage keeps its
  network nodes by position). Keep `refinedstorage:` in both lists.

**Super Factory Manager**: no crash any more. A moved manager keeps its disk and program, but the disk's labels still
point to the original inventories, so the program moves nothing until you label the deployed inventories again with the
Label Gun. Tested: 4.34.0 on 1.21.1 and 1.20.1. (The default `superfactorymanager:` entry is a 1.12 id: the mod is
`sfm` today and is not excluded.)

**Blood Magic alchemy table** (1.20.1, Blood Magic 3.3.8): both halves deploy, then disappear as soon as a block next
to them changes, because each half remembers the other's absolute position. Workaround: exclude
`bloodmagic:alchemytable` (new installs of the next 8.0.x build do), or pick up the table before moving your Blood Magic
room. No NeoForge 1.21.1 version was found to test.

**IndustrialCraft 2**: no version for 1.20.1 or 1.21.1 was found. On 1.12.2, its machines lose their data when moved.

## Annoying

**Mekanism Digital Miner**
- 1.21.1 (Mekanism 10.7.19): never captured, Mekanism tags it `c:relocation_not_supported`.
- 1.20.1 (Mekanism 10.4.16): a moved miner keeps bounding blocks pointing to its old position: it can't be broken from
  them and stops working. Workaround: exclude `#forge:relocation_not_supported` (new installs of the next 8.0.x build
  do), or `mekanism:digital_miner` and `mekanism:bounding_block`. To get a moved miner back, deploy it where it was
  first captured.
- Other Mekanism machines (a bin with its items was tested) move with their content on both versions.

**Immersive Engineering wires**
- 1.21.1 (Immersive Engineering 12.4.2): connectors are never captured (tagged `c:relocation_not_supported`), so the
  wires stay in place.
- 1.20.1 (Immersive Engineering 10.2.0): connectors move but their wires are lost, and the coil is not given back.
  Workaround: cut the wires before moving the machines, or exclude the connectors (new installs of the next 8.0.x build
  do): `immersiveengineering:connector_lv`, `connector_lv_relay`, `connector_mv`, `connector_mv_relay`, `connector_hv`,
  `connector_hv_relay`, `connector_redstone`, `connector_bundled`, `connector_probe`, `connector_structural`,
  `transformer`, `transformer_hv`, `post_transformer`, `breaker_switch`, `redstone_breaker`, `current_transformer`,
  `feedthrough` (each with the `immersiveengineering:` prefix).

**Integrated Dynamics** (1.21.1, Integrated Dynamics 1.38.0): cables and parts move fine, but they are not drawn in the
full deploy preview ([#94](https://github.com/Lythom/capsule/issues/94)).

## Compatible

- **GregTech CEu Modern** (1.20.1, 7.5.3): machines move with their content. The 1.21.1 beta could not be tested (it
  does not start on a dedicated server).
- **Waystones** and **SecurityCraft** (1.21.1): never captured (`c:relocation_not_supported`); the blocks around them
  are captured normally.
- Modpacks using **Sinytra Connector** work (tested with Forgeulously Optimized 1.1.4).

## How it is tested

Every mod above has automated tests: on 1.21.1 GameTests in a NeoForge server, on 1.20.1 a real Forge server with the
release jar. They place the mod's blocks as a player would, capture and deploy them, and check the result, so a new
version of a mod that fixes or breaks something shows up at the next run. To run them again with the newest versions
of the mods, see [Known incompatibilities in TESTING.md](https://github.com/Lythom/capsule/blob/1.21.1/docs/TESTING.md#known-incompatibilities)
(`./gradlew :neoforge:runGameTestServer -Pincompat -PmodCompatLatest`).

Found another mod that breaks when moved? Please [open an issue](https://github.com/Lythom/capsule/issues) with the
mod, its version and what happens.

## Older versions

Notes from 1.12.2, not checked since:
- IndustrialCraft 2 machines lose their data and become unusable after being moved
  (http://bt.industrial-craft.net/view.php?id=1957). Same for GregTech machines and addons of that time.
- Super Factory Manager crashed: keep it in `opExcludedBlocks`.
- Refined Storage machines lose all their data: keep `refinedstorage:` in `opExcludedBlocks`.
- The Mekanism Digital Miner becomes unbreakable and stops working when deployed elsewhere: deploy it where it was
  first captured to pick it up.
- The Blood Magic alchemy table disappears when deployed elsewhere: pick it up before moving your Blood Magic room.
- Immersive Engineering wires disappear when deployed elsewhere: cut them before moving the machines.
