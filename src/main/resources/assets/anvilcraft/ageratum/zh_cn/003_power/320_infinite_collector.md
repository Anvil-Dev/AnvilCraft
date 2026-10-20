---
navigation:
  title: "§5无上限能量收集器"
  icon: "anvilcraft:infinite_collector"
items:
  - anvilcraft:infinite_collector
---

# <ref item="anvilcraft:infinite_collector"/>

<recipe id="anvilcraft:infinite_collector"/>

1. 拥有<ref item="anvilcraft:charge_collector"/>、<ref item="anvilcraft:void_energy_collector"/>和<ref item="anvilcraft:heat_collector"/>的能力，且没有功率上限
2. 其工作范围为7x7x7

# 虚空能发电

- 拥有256kW的基础发电功率，类似于<ref item="anvilcraft:void_energy_collector"/>
- 不会衰变而产生垃圾方块
- 可以受到物质方块和<ref item="anvilcraft:negative_matter_block"/>影响而改变发电量：

|   物质方块数    | 发电量(kW) |
|:----------:|:-------:|
| [-10, +∞)  |   256   |
| [-20, -11] |   768   |
| [-40, -21] |  2048   |
| (-∞, -41]  |  5120   |

<tip>
<ref item="anvilcraft:negative_matter_block"/>提供 -1 的物质方块数
</tip>

## 特性

- 拥有超限合金级别的属性：防炸防凋零防龙