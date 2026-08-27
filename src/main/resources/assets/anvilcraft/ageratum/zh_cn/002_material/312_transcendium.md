---
navigation:
  title: "§5超限合金"
  icon: "anvilcraft:transcendium_ingot"
items:
  - anvilcraft:transcendium_block
  - anvilcraft:transcendium_ingot
  - anvilcraft:transcendium_nugget
  - anvilcraft:multiphase_transcendium
---

# 超限合金

<row halign="center">
<item id="anvilcraft:transcendium_block"/>
<item id="anvilcraft:transcendium_ingot"/>
<item id="anvilcraft:transcendium_nugget"/>
<item id="anvilcraft:multiphase_transcendium"/>
</row>

# 合成

将<ref item="anvilcraft:charged_neutronium_ingot"/>用铁砧压入<ref item="anvilcraft:overheated_ember_metal_block"/>，
根据<ref item="anvilcraft:charged_neutronium_ingot"/>上的附魔数量，决定输出超限合金的产量

|  附魔数量 n  | 返还<ref item="anvilcraft:neutronium_ingot"/>概率 |                                                产量                                                |
|:--------:|:---------------------------------------------:|:------------------------------------------------------------------------------------------------:|
| [0, 10]  |                    n * 10%                    | 4 <ref item="anvilcraft:transcendium_ingot"/> + 3*n <ref item="anvilcraft:transcendium_nugget"/> |
| [11, 14] |                     100%                      | 4 <ref item="anvilcraft:transcendium_ingot"/> + 3*n <ref item="anvilcraft:transcendium_nugget"/> |
|    15    |                     100%                      |                          1 <ref item="anvilcraft:transcendium_block"/>                           |
| [16, +∞) |                     100%                      |  1 <ref item="anvilcraft:transcendium_block"/> + n <ref item="anvilcraft:transcendium_nugget"/>  |

<info>
锭和粒以掉落物形式产生；块生成于原方块的位置
</info>

# 功能

- 用于合成机器或升级工具
- 为*附魔台*提供等同于 10 个<ref item="minecraft:bookshelf"/>的附魔能力

# 超限合金工具

- 通过[余烬金属工具](211_ember_metal.md)与[浮霜金属工具](202_frost_metal.md)二合一制作
- 拥有[属性: 永恒](../001_feature/201_properties.md#永恒)
- 拥有[属性: 强运](../001_feature/201_properties.md#强运)

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

# 相关

- [铁砧锤](../005_tool/000_anvil_hammer.md)
- [龙杖](../005_tool/101_dragon_rod.md)
- [共振器](../005_tool/301_resonator.md)
- [重戟](../005_tool/301_heavy_halberd.md)
