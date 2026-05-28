---
navigation:
  title: "§5Transcendium"
  icon: "anvilcraft:transcendium_ingot"
categories:
  - misc ingredients blocks
items:
  - anvilcraft:transcendium_block
  - anvilcraft:transcendium_ingot
  - anvilcraft:transcendium_nugget
  - anvilcraft:transcendence_anvil_hammer
  - anvilcraft:transcendence_dragon_rod
  - anvilcraft:multiphase_transcendium
  - anvilcraft:transcendence_heavy_halberd
  - anvilcraft:transcendence_resonator
---

# Transcendium

<row halign="center">
<item id="anvilcraft:transcendium_block"/>
<item id="anvilcraft:transcendium_ingot"/>
<item id="anvilcraft:transcendium_nugget"/>
<item id="anvilcraft:multiphase_transcendium"/>
</row>

---

<row halign="center">
<item id="anvilcraft:transcendence_anvil_hammer"/>
<item id="anvilcraft:transcendence_dragon_rod"/>
<item id="anvilcraft:transcendence_heavy_halberd"/>
<item id="anvilcraft:transcendence_resonator"/>
</row>

<color=#cc00ff> So strong?! </color>  

# Crafting

Use an anvil to press <ref item="anvilcraft:charged_neutronium_ingot"/> into <ref item="anvilcraft:overheated_ember_metal_block"/>.
The number of enchantments on <ref item="anvilcraft:charged_neutronium_ingot"/> determines the yield of Transcendium.

|  Enchantments n  | <ref item="anvilcraft:neutronium_ingot"/> return chance |                                                          Yield                                                          |
|:--------:|:----------------------------------------------------------------------------------:|:--------------------------------------------------------------------------------------------------------------------:|
| [0, 10]  |                                      n * 10%                                       | 4 <ref item="anvilcraft:transcendium_ingot"/> + 3*n <ref item="anvilcraft:transcendium_nugget"/> |
| [11, 14] |                                        100%                                        | 4 <ref item="anvilcraft:transcendium_ingot"/> + 3*n <ref item="anvilcraft:transcendium_nugget"/> |
|    15    |                                        100%                                        |                               1 <ref item="anvilcraft:transcendium_block"/>                               |
| [16, +inf) |                                        100%                                        | 1 <ref item="anvilcraft:transcendium_block"/> + n <ref item="anvilcraft:transcendium_nugget"/>  |

<info>
Ingots and nuggets are produced as dropped items; blocks are generated at the position of the original block
</info>

# Functions

- Used to craft machines
- Combined with <ref item="anvilcraft:transcendium_upgrade_smithing_template"/> to upgrade tools

# Transcendium Tools

- Infinite durability
- Possesses [Property: Eternal](../001_feature/201_properties.md#永恒)
- Possesses [Property: Fortune's Favor](../001_feature/201_properties.md#强运)

<row halign="center">
<recipe id="anvilcraft:multiphase_transcendium"/>
<recipe id="anvilcraft:smithing/transcendence_anvil_hammer"/>
<recipe id="anvilcraft:smithing/transcendence_dragon_rod"/>
<recipe id="anvilcraft:transcendence_dragon_rod"/>
</row>


<row halign="center">
<recipe id="anvilcraft:two_to_one_smithing/transcendence_heavy_halberd"/>
<recipe id="anvilcraft:two_to_one_smithing/transcendence_resonator"/>
</row>

# Related

- [Anvil Hammer](../005_tool/000_anvil_hammer.md)
- [Dragon Rod](../005_tool/101_dragon_rod.md)
- [Ember Tools](211_ember_metal.md)
- [Resonator](../005_tool/301_resonator.md)
