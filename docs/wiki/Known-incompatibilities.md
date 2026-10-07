<!--
TODO (before publishing): another agent is verifying these incompatibilities against Capsule 9.0 (1.21.1) and the
current versions of the mods. Until that review lands, the list below is the 1.12-era content with grammar fixes only.
Leads from docs/ISSUE_TRIAGE.md, BACKLOG.md and docs/MANUAL_VALIDATION.md to check while doing it:
- Waystones (#121): never captured on 1.21.1 (tag c:relocation_not_supported), doors next to them move fine.
- Integrated Dynamics cables and parts: deploy fine, invisible in the full preview (#94).
- Iris shader packs: on NeoForge the preview casts a shadow (#69, cosmetic).
- Mob Grinding Utils dirt (#81): preview crash hardened, no 1.21.1 build to verify.
- ItemPhysic (#72): thrown capsules may not deploy (later).
- SecurityCraft: blocks only captured by their owner; with it loaded, pre-1.13 schematics lose chest item counts.
- Sophisticated Storage (#115): fixed on 1.21.1.
- Corail Tombstone graves: still excluded by tag.
- IC2, GregTech, SuperFactoryManager, Refined Storage, Blood Magic alchemy table, Mekanism machines: still in the
  default excludedBlocks / opExcludedBlocks; check which of these mods exist on 1.21.1 and whether the entries still
  matter (Mekanism 1.21 block ids, Refined Storage 2).
-->

# Known incompatibilities

By design incompatibilities:
* Corail Tombstone player graves can't be moved. Forcing them out of the blacklist results in dupe bugs.

Important incompatibilities:
* IC2 machines will lose data after being moved and become unusable. (http://bt.industrial-craft.net/view.php?id=1957)
* Same for GregTech machines and addons.
* SuperFactoryManager will crash, keep it in opExcludedBlocks in the config.
* Refined Storage machines will lose all data when moved. Be sure to keep "refinedstorage:" in the default opExcludedBlocks in the config.

Annoying incompatibilities:
* The Digital Miner from Mekanism will be unbreakable and will stop working if deployed at a different position than the original one. To get it back, re-deploy it where it was when first captured and you will be able to pick it up.
* The Alchemy Table from Blood Magic will disappear if deployed at a different position than the original one.    
_Workaround: Pick up the table before moving your Blood Magic room._
* Wires from Immersive Engineering will disappear if deployed at a different position than the original one.     
_Workaround: Cut all wires before moving the machines so you don't lose the wires._
