---
navigation:
  title: "§2能量转换"
  icon: "anvilcraft:power_converter_big"
  parent: anvilcraft_guideme:power.md
items:
  - anvilcraft:power_converter_small
  - anvilcraft:power_converter_middle
  - anvilcraft:power_converter_big
---

# 能量转换

<row halign="center">
<item id="anvilcraft:power_converter_small"/>
<item id="anvilcraft:power_converter_middle"/>
<item id="anvilcraft:power_converter_big"/>
</row>

# 能量转换

铁砧工艺的电能和其他模组的电能有本质上的不同，因此只能在转换后才能为别的模组使用

# 能量转换器

## 合成

<row halign="center">
<recipe id="anvilcraft:power_converter_big"/>
<recipe id="anvilcraft:stonecutting/power_converter_middle"/>
<recipe id="anvilcraft:stonecutting/power_converter_small_from_big"/>
</row>
<row halign="center">
<recipe id="anvilcraft:power_converter_middle_from_small"/>
<recipe id="anvilcraft:power_converter_big_from_middle"/>
<recipe id="anvilcraft:power_converter_big_from_small"/>
</row>

## 功能

- 将铁砧工艺的电力，单向转换为FE/RF/AE能量
- 转换带有10%的损耗
- 大小不同，转换能量不同，详情见下表


|                                       能量转换器                                        | 消耗(kW) | 等价能量(FE/t) | 损耗后转换出的能量(FE/t) |
|:----------------------------------------------------------------------------------:|:------:|:----------:|:---------------:|
| [<translate key="block.anvilcraft.power_converter_small"/>](103_power_convert.md)  |   1    |     80     |       72        |
| [<translate key="block.anvilcraft.power_converter_middle"/>](103_power_convert.md) |   6    |    480     |       432       |
|  [<translate key="block.anvilcraft.power_converter_big"/>](103_power_convert.md)   |   36   |    2880    |      2592       |

> 默认配置下，1kW = 80FE/t