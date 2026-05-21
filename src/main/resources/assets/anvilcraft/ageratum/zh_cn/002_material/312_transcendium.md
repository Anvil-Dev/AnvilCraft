---
navigation:
  title: "§5超限合金"
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

# 超限合金

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

<color=#cc00ff> 这么强?! </color>  

# 合成

将[<translate key="item.anvilcraft.charged_neutronium_ingot"/>](221_neutronium_ingot.md)用铁砧压入[<translate key="block.anvilcraft.overheated_ember_metal_block"/>](../001_feature/301_overheated_block.md)，
根据[<translate key="item.anvilcraft.charged_neutronium_ingot"/>](221_neutronium_ingot.md)上的附魔数量，决定输出超限合金的产量

|  附魔数量 n  | 返还[<translate key="item.anvilcraft.neutronium_ingot"/>](221_neutronium_ingot.md)概率 |                                                          产量                                                          |
|:--------:|:----------------------------------------------------------------------------------:|:--------------------------------------------------------------------------------------------------------------------:|
| [0, 10]  |                                      n * 10%                                       | 4 <translate key="item.anvilcraft.transcendium_ingot"/> + 3*n <translate key="item.anvilcraft.transcendium_nugget"/> |
| [11, 14] |                                        100%                                        | 4 <translate key="item.anvilcraft.transcendium_ingot"/> + 3*n <translate key="item.anvilcraft.transcendium_nugget"/> |
|    15    |                                        100%                                        |                               1 <translate key="block.anvilcraft.transcendium_block"/>                               |
| [16, +∞) |                                        100%                                        | 1 <translate key="block.anvilcraft.transcendium_block"/> + n <translate key="item.anvilcraft.transcendium_nugget"/>  |

<info>
锭和粒以掉落物形式产生；块生成于原方块的位置
</info>

# 功能

- 用于合成机器
- 与[<translate key="item.anvilcraft.transcendium_upgrade_smithing_template"/>](311_transcendium_template.md)配合，升级工具

# 超限合金工具

- 无限耐久
- 拥有[属性: 永恒](../001_feature/201_properties.md#永恒)
- 拥有[属性: 强运](../001_feature/201_properties.md#强运)

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

# 相关

- [铁砧锤](../005_tool/000_anvil_hammer.md)
- [龙杖](../005_tool/101_dragon_rod.md)
- [余烬工具](211_ember_metal.md)
- [共振器](../005_tool/301_resonator.md)