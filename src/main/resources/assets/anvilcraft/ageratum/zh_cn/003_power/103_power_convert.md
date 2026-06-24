---
navigation:
  title: "§2能量转换"
  icon: "anvilcraft:power_converter_big"
items:
  - anvilcraft:power_converter_small
  - anvilcraft:power_converter_middle
  - anvilcraft:power_converter_big
  - anvilcraft:fe_collector
---

# 能量转换

铁砧工艺的电能和其他模组的电能有本质上的不同，因此必需专门的转换方块

# 能量转换器

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

- 将铁砧工艺的电力，单向转换为FE/RF/AE能量
- 转换带有损耗
- 大小不同，转换能量不同，详情见下表

|                      能量转换器                      | 消耗(kW) | 等价能量(FE/t) | 损耗后转换出的能量(FE/t) |
|:-----------------------------------------------:|:------:|:----------:|:---------------:|
| <ref item="anvilcraft:power_converter_small"/>  |   1    |    100     |       90        |
| <ref item="anvilcraft:power_converter_middle"/> |   16   |    1600    |      1440       |
|  <ref item="anvilcraft:power_converter_big"/>   |  256   |   25600    |      23040      |

<info>
默认配置下：1kW = 100FE/t，转换损耗率10%；可以通过config配置
</info>

# <ref item="anvilcraft:fe_collector"/>

<recipe id="anvilcraft:fe_collector"/>

- 通过其他模组输入FE能量
- 当储存的FE达到上限的40%后，开始转换；低于2%后，停止转换
- 当储存的FE达到上限的50%后，开始向侧面可能的位置输出FE

|                 FE收集器                 | 消耗(FE/t) | 等价能量(kW) | 损耗后转换出的能量(kW) |
|:-------------------------------------:|:--------:|:--------:|:-------------:|
| <ref item="anvilcraft:fe_collector"/> |  10,000  |   100    |      90       |

<tip>
可以串联放置以一起工作
</tip>
