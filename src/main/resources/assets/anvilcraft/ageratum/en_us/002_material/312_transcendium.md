---
navigation:
  title: "§5Transcendium"
  icon: "anvilcraft:transcendium_ingot"
items:
  - anvilcraft:transcendium_block
  - anvilcraft:transcendium_ingot
  - anvilcraft:transcendium_nugget
  - anvilcraft:multiphase_transcendium
---

# Transcendium

<row halign="center">
<item id="anvilcraft:transcendium_block"/>
<item id="anvilcraft:transcendium_ingot"/>
<item id="anvilcraft:transcendium_nugget"/>
<item id="anvilcraft:multiphase_transcendium"/>
</row>

# Crafting

Use an anvil to press <ref item="anvilcraft:charged_neutronium_ingot"/> into <ref item="anvilcraft:overheated_ember_metal_block"/>.
The number of enchantments on <ref item="anvilcraft:charged_neutronium_ingot"/> determines the yield of Transcendium.

|  Enchantments n  | <ref item="anvilcraft:neutronium_ingot"/> return chance |                                                 Yield                                                 |
|:--------:|:---------------------------------------------:|:------------------------------------------------------------------------------------------------:|
| [0, 10]  |                    n * 10%                    | 4 <ref item="anvilcraft:transcendium_ingot"/> + 3*n <ref item="anvilcraft:transcendium_nugget"/> |
| [11, 14] |                     100%                      | 4 <ref item="anvilcraft:transcendium_ingot"/> + 3*n <ref item="anvilcraft:transcendium_nugget"/> |
|    15    |                     100%                      |                          1 <ref item="anvilcraft:transcendium_block"/>                           |
| [16, +inf) |                     100%                      |  1 <ref item="anvilcraft:transcendium_block"/> + n <ref item="anvilcraft:transcendium_nugget"/>  |

<info>
Ingots and nuggets are produced as dropped items; blocks are generated at the position of the original block
</info>

# Functions

- Used to craft machines
- Combined with <ref item="anvilcraft:transcendium_upgrade_smithing_template"/> to upgrade tools
- Provides an *enchanting table* with enchantment power equivalent to 10 <ref item="minecraft:bookshelf"/>s

# Transcendium Tools

- Created by combining [Ember Metal Tools](211_ember_metal.md) and [Frost Metal Tools](202_frost_metal.md)
- Possesses [Property: Eternal](../001_feature/201_properties.md#eternal)
- Possesses [Property: Providence](../001_feature/201_properties.md#providence)

<row halign="center">
<recipe id="anvilcraft:multiphase_transcendium"/>
</row>

<row halign="center">
<recipe id="anvilcraft:two_to_one_smithing/transcendence_heavy_halberd"/>
<recipe id="anvilcraft:two_to_one_smithing/transcendence_resonator"/>
</row>

<row halign="center">
<recipe id="anvilcraft:two_to_one_smithing/transcendence_anvil_hammer"/>
<recipe id="anvilcraft:two_to_one_smithing/transcendence_dragon_rod"/>
</row>

# Related

- [Anvil Hammer](../005_tool/000_anvil_hammer.md)
- [Dragon Rod](../005_tool/101_dragon_rod.md)
- [Resonator](../005_tool/301_resonator.md)
- [Heavy Halberd](../005_tool/301_heavy_halberd.md)
