# Capsule tiers

Approved by the owner (round 2, #120). Every empty capsule is crafted with the same pattern: a stone button
on top, the material left and right of an ender pearl, an iron ingot below. The material sets the capture size and
the material color (the cap of the capsule); the base color (the body) is the tier's.

- **Size**: by how hard the material is to get. Common ores 3, other ores and gems 5, alloys and diamond 7, alloys of
  rarer metals and blocks needing a trip (obsidian, ocean monument) 9, end-game and magic metals 11, the rarest 13.
  Upgrades (popped chorus fruit) add 2 per level on top, up to `upgradeLimit`.
- **Base color**: undyed (white) for every metal and gem tier, as today, so the body stays free for dyeing and the
  material color carries the information; wood keeps its plank brown body; the overpowered capsule is white with the
  enchanted glint.
- **Material color**: the dominant color of the material's item texture (vanilla: measured on the 1.21.1 textures,
  brightened when too dark to read on the cap); for modded metals, the color most mods use for that ingot.
- Modded recipes (`addons_capsule_*`) load only when a mod fills their `c:ingots/<metal>` tag (NeoForge
  `neoforge:tag_empty`, Fabric `fabric:tags_populated`).

| Material | Ingredient | Size | Base | Material color | Recipe | Status |
|---|---|---|---|---|---|---|
| Wood | `#minecraft:planks`, wooden chest, wooden slab | 1 | `#B4915A` | `#BC9862` | `capsule_wood` | existing |
| Iron | `#c:ingots/iron` | 3 | white | `#CCCCCC` | `capsule_iron` | existing |
| Copper | `#c:ingots/copper` (vanilla) | 3 | white | `#D78D5B` | `addons_capsule_copper` | existing |
| Tin | `#c:ingots/tin` | 3 | white | `#BAD6DD` | `addons_capsule_tin` | existing |
| Zinc | `#c:ingots/zinc` | 3 | white | `#B8C7B0` | `addons_capsule_zinc` | new |
| Aluminum | `#c:ingots/aluminum` | 3 | white | `#D5DBE0` | `addons_capsule_aluminum` | new |
| Gold | `#c:ingots/gold` | 5 | white | `#FFD700` | `capsule_gold` | existing |
| Lead | `#c:ingots/lead` | 5 | white | `#7B8BB6` | `addons_capsule_lead` | existing |
| Silver | `#c:ingots/silver` | 5 | white | `#CFDCE6` | `addons_capsule_silver` | existing |
| Osmium | `#c:ingots/osmium` | 5 | white | `#9CB4C8` | `addons_capsule_osmium` | new |
| Amethyst | `#c:gems/amethyst` | 5 | white | `#9A5CC6` | `capsule_amethyst` | new |
| Quartz | `#c:gems/quartz` | 5 | white | `#E3DBD0` | `capsule_quartz` | new |
| Diamond | `#c:gems/diamond` | 7 | white | `#00FFF2` | `capsule_diamond` | existing |
| Bronze | `#c:ingots/bronze` | 7 | white | `#F9AB0D` | `addons_capsule_bronze` | existing |
| Brass | `#c:ingots/brass` | 7 | white | `#E6B84F` | `addons_capsule_brass` | new |
| Invar | `#c:ingots/invar` | 7 | white | `#CAD1C9` | `addons_capsule_invar` | existing |
| Nickel | `#c:ingots/nickel` | 7 | white | `#F1EAB5` | `addons_capsule_nickel` | existing |
| Obsidian | `#c:obsidians` | 9 | white | `#1E182B` | `capsule_obsidian` | existing |
| Prismarine | `#c:gems/prismarine` (crystals) | 9 | white | `#91C5B5` | `capsule_prismarine` | new |
| Steel | `#c:ingots/steel` | 9 | white | `#8A8F94` | `addons_capsule_steel` | new |
| Uranium | `#c:ingots/uranium` | 9 | white | `#5FAE4F` | `addons_capsule_uranium` | new |
| Constantan | `#c:ingots/constantan` | 9 | white | `#F4C86B` | `addons_capsule_constantan` | existing |
| Electrum | `#c:ingots/electrum` | 9 | white | `#FBF17B` | `addons_capsule_electrum` | existing |
| Emerald | `#c:gems/emerald` | 11 | white | `#17DD62` | `capsule_emerald` | existing |
| Enderium | `#c:ingots/enderium` | 11 | white | `#1D7E8C` | `addons_capsule_enderium` | existing |
| Lumium | `#c:ingots/lumium` | 11 | white | `#E7F2C0` | `addons_capsule_lumium` | existing |
| Signalum | `#c:ingots/signalum` | 11 | white | `#FD641A` | `addons_capsule_signalum` | existing |
| Platinum | `#c:ingots/platinum` | 13 | white | `#A3E7FE` | `addons_capsule_platinum` | existing |
| Netherite | `#c:ingots/netherite` | 13 | white | `#443E40` | `capsule_netherite` | new, the vanilla 13³ capsule (#120) |
| Overpowered | `#c:ingots/iron`, nether star | 1 | white | `#FFFFFF` | `capsule_op` | existing |

Other ways to a 13³ capsule in vanilla: an emerald capsule with one upgrade, or a gold capsule with four.

Left out on purpose: lapis lazuli (blueprints are blue), redstone, glowstone, coal and other dusts (not a construction
material), copper and iron nuggets, raw ores, and blocks of a material (a block would cost nine times the ingot for the
same capsule).

The new vanilla recipes are unlocked with the other tiers (advancement `capsule:recipes/tools/capsule`). The GameTest
resources fill every modded `c:ingots/*` tag with a placeholder item, so `everyCapsuleRecipeLoadsWithResolvedIngredients`
loads and checks every recipe on both loaders, and the client smoke test checks that JEI shows a recipe for every tier.
