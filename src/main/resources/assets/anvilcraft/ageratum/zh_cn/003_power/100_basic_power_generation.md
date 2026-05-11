---
navigation:
  title: "§2基础发电"
  icon: "anvilcraft:charge_collector"
items:
  - anvilcraft:charge_collector
  - anvilcraft:piezoelectric_crystal
---

# 基础发电

<row halign="center">
<item id="anvilcraft:charge_collector"/>
<item id="anvilcraft:piezoelectric_crystal"/>
</row>

# <translate key="block.anvilcraft.charge_collector"/>

<recipe id="anvilcraft:charge_collector"/>

[<translate key="block.anvilcraft.charge_collector"/>](100_basic_power_generation.md)是发电设施的核心部件

## 功能

- 发电功率上限: 128kW
- 工作范围: 以自己为中心5x5x5
- 集电器一个周期之内收到的电荷数量将成为它下个周期的发电功率（个→KW）。周期默认为2秒。

# 电荷

以下方块行为会产生电荷：

- <translate key="block.minecraft.anvil"/>砸到[<translate key="block.anvilcraft.piezoelectric_crystal"/>](100_basic_power_generation.md)
- 活塞推拉紧邻<translate key="block.minecraft.copper_block"/>的[<translate key="block.anvilcraft.magnet_block"/>](../002_material/001_magnet.md)
- <translate key="item.minecraft.lightning_rod"/>被雷劈。

## 压电晶体生产电荷

<row halign="center">
<recipe id="anvilcraft:piezoelectric_crystal"/>
<recipe id="anvilcraft:piezoelectric_crystal_amethyst"/>
</row>

- [<translate key="block.anvilcraft.piezoelectric_crystal"/>](100_basic_power_generation.md)被<translate key="block.minecraft.anvil"/>砸时，可以产生电荷
- 根据**铁砧种类**和**下落高度**不同，产生电荷量各不相同，已在下表列出
- 将[<translate key="block.anvilcraft.piezoelectric_crystal"/>](100_basic_power_generation.md)竖向堆叠，可以增加产电荷的量。下方的[<translate key="block.anvilcraft.piezoelectric_crystal"/>](100_basic_power_generation.md)产生它上方[<translate key="block.anvilcraft.piezoelectric_crystal"/>](100_basic_power_generation.md)一半的电荷量，小数向下取整。

|                       铁砧种类                       | 高度=1 | 高度=2 | 高度=3 | 高度≥4 |
|:------------------------------------------------:|:-----|:-----|:-----|:-----|
|   [<translate key="block.anvilcraft.spectral_anvil"/>](../004_block/105_spectral_anvil.md)    | 1    | 2    | 3    | 4    |
|        <translate key="block.minecraft.anvil"/>         | 1    | 2    | 4    | 8    |
|     [<translate key="block.anvilcraft.royal_anvil"/>](../004_block/103_royal_anvil.md)     | 1    | 2    | 4    | 8    |
|     [<translate key="block.anvilcraft.ember_anvil"/>](../004_block/223_ember_anvil.md)     | 1    | 2    | 5    | 12   |
| [<translate key="block.anvilcraft.transcendence_anvil"/>](../004_block/311_transcendence_anvil.md) | 2    | 5    | 15   | 60   |

|                   铁砧种类                   | 高度=1 | 高度=2 | 高度=3 | 高度=4 | 高度=5 | 高度=6 | 高度=7 | 高度≥8 | 
|:----------------------------------------:|:-----|:-----|:-----|:-----|:-----|:-----|:-----|:-----|
| [<translate key="block.anvilcraft.giant_anvil"/>](../004_block/210_giant_anvil.md) | 1    | 2    | 3    | 4    | 5    | 6    | 7    | 8    |

## 摩擦生产电荷

<structure id="../structures/triboelectric_power.snbt"/>

- [<translate key="block.anvilcraft.magnet_block"/>](../002_material/001_magnet.md)被活塞推拉时，如果紧贴着<translate key="block.minecraft.copper_block"/>，可以产生电荷
- <translate key="block.minecraft.copper_block"/>生锈会使电荷生产量减少，每次移动产生的电荷见下表

| 种类  | <translate key="block.minecraft.copper_block"/> | <translate key="block.minecraft.exposed_copper"/> | <translate key="block.minecraft.weathered_copper"/> | <translate key="block.minecraft.oxidized_copper"/> | 任何<translate key="block.minecraft.waxed_copper_block"/> |
|:---:|:------------------------------------------------|:--------------------------------------------------|:----------------------------------------------------|:---------------------------------------------------|:--------------------------------------------------------|
| 电荷量 | 1/4                                             | 1/8                                               | 1/16                                                | 0                                                  | 0                                                       |

## 收集雷电电荷

*避雷针*被雷劈可以产生电荷。每次产生32个电荷。

