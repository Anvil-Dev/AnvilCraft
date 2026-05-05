---
navigation:
  title: "§6虚空能量收集"
  icon: "anvilcraft:void_energy_collector"
  parent: anvilcraft_guideme:power.md
items:
  - anvilcraft:void_energy_collector
---

# 虚空能量收集

<item id="anvilcraft:void_energy_collector"/>

# [<translate key="item.anvilcraft.void_energy_collector"/>](201_void_energy_collection.md)

<recipe id="anvilcraft:void_energy_collector"/>

收集*非物质方块*的虚空能以发电

- 检测范围：以自己为中心5x5x5
- [<translate key="item.anvilcraft.void_energy_collector"/>](201_void_energy_collection.md)的检测范围不可重叠，否则停止工作
- 发电量受检测范围内的*物质方块*干扰而降低

|  物质方块数   | 发电量(kW) |
|:--------:|:-------:|
| [20, +∞) |    0    |
| [11, 20] |   128   |
| [3, 10]  |   256   |
|  [0, 2]  |   512   |

## 非物质方块

- *空气方块*
- *虚空*
- [<translate key="block.anvilcraft.void_matter_block"/>](../002_material/140_void_matter.md)
- [<translate key="item.anvilcraft.void_energy_collector"/>](201_void_energy_collection.md)

## 产生物质方块

- 每隔一段时间，*虚空能收集器*将检测范围内的一个*空气方块*转化为*物质方块*，可能出现的方块与[虚空衰变](../001_feature/101_void_decay.md)一致。
- 这些*物质方块*会影响*虚空能收集器*发电。
- 可以采取以下手段：
  - 及时清理*物质方块*，比如使用TNT
  - 使用其他*非物质方块*取代*空气方块*，需要注意[<translate key="block.anvilcraft.void_matter_block"/>](../002_material/140_void_matter.md)的[虚空衰变](../001_feature/101_void_decay.md)特性

> [<translate key="item.anvilcraft.void_energy_collector"/>](201_void_energy_collection.md)具有极高爆炸抗性

<GameScene interactive={true} zoom={2}>
  <Block x="0" y="0" z="1" id="minecraft:andesite" />
  <Block x="0" y="0" z="4" id="anvilcraft:end_dust" />
  <Block x="1" y="1" z="2" id="anvilcraft:void_matter_block" />
  <Block x="2" y="1" z="2" id="anvilcraft:void_matter_block" />
  <Block x="3" y="1" z="2" id="anvilcraft:void_matter_block" />
  <Block x="2" y="2" z="0" id="anvilcraft:end_dust" />
  <Block x="1" y="2" z="2" id="anvilcraft:void_matter_block" />
  <Block x="2" y="2" z="2" id="anvilcraft:void_energy_collector" />
  <Block x="3" y="2" z="2" id="anvilcraft:void_matter_block" />
  <Block x="4" y="2" z="3" id="anvilcraft:cursed_gold_block" />
  <Block x="1" y="3" z="2" id="anvilcraft:negative_matter_block" />
  <Block x="2" y="3" z="2" id="anvilcraft:negative_matter_block" />
  <Block x="3" y="3" z="2" id="anvilcraft:negative_matter_block" />
</GameScene>

- 通过*非物质方块*避免*物质方块*生成在内圈
- 再用任意自动挖掘手段破坏外圈的*物质方块*，即可维护*虚空能收集器*的运行

