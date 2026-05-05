---
navigation:
  title: "§6虚空能量收集"
  icon: "anvilcraft:void_energy_collector"
  parent: anvilcraft_guideme:power.md
items:
  - anvilcraft:void_energy_collector
---

# <translate key="block.anvilcraft.void_energy_collector"/>

<recipe id="anvilcraft:void_energy_collector"/>

收集*非物质方块*的虚空能以发电

- 检测范围：以自己为中心5x5x5
- [<translate key="block.anvilcraft.void_energy_collector"/>](201_void_energy_collection.md)的检测范围不可重叠，否则停止工作
- 发电量受检测范围内的*物质方块*干扰而降低

|  物质方块数   | 发电量(kW) |
|:--------:|:-------:|
| [20, +∞) |    0    |
| [11, 20] |   128   |
| [3, 10]  |   256   |
|  [0, 2]  |   512   |

<info>
[<translate key="block.anvilcraft.void_energy_collector"/>](201_void_energy_collection.md)具有极高爆炸抗性
</info>

## 非物质方块

- *空气方块*
- *虚空*
- [<translate key="block.anvilcraft.void_matter_block"/>](../002_material/140_void_matter.md)
- [<translate key="block.anvilcraft.void_energy_collector"/>](201_void_energy_collection.md)

## 产生物质方块

- 每隔一段时间，<translate key="block.anvilcraft.void_energy_collector"/>将检测范围内的一个*空气方块*转化为*物质方块*，可能出现的方块与[虚空衰变](../001_feature/101_void_decay.md)一致。
- 这些*物质方块*会影响<translate key="block.anvilcraft.void_energy_collector"/>发电。

<tip>
通过*非物质方块*避免*物质方块*生成在内圈，再用任意自动挖掘手段破坏外圈的*物质方块*，维持发电机运行
</tip>

<tip>
使用TNT把除了<translate key="block.anvilcraft.void_energy_collector"/>以外的方块炸掉，能一次性清理大量*物质方块*，维持发电机运行
</tip>

