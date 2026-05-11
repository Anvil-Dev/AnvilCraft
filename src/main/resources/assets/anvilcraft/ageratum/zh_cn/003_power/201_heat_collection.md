---
navigation:
  title: "§6热能收集"
  icon: "anvilcraft:heat_collector"
items:
  - anvilcraft:heat_collector
---

# 热能收集

<item id="anvilcraft:heat_collector"/>

# <translate key="block.anvilcraft.heat_collector"/>

<recipe id="anvilcraft:heat_collector"/>

收集*可集热方块*的热能以发电

- 发电功率上限: 4096KW
- 工作范围: 以自己为中心5x5x5
- 范围内的*可集热方块*决定发电功率

## 可集热方块

|                                             方块                                              |                      转化结果                      | 提供能量(kW) |
|:-------------------------------------------------------------------------------------------:|:----------------------------------------------:|:--------:|
|                       <translate key="block.minecraft.magma_block"/>                        | <translate key="block.minecraft.netherrack"/>  |    2     |
|                         <translate key="block.minecraft.campfire"/>                         | 熄灭的<translate key="block.minecraft.campfire"/> |    4     |
|                                             熔岩                                              |  <translate key="block.minecraft.obsidian"/>   |    4     |
| [<translate key="block.anvilcraft.ember_metal_block"/>](../002_material/211_ember_metal.md) |                       不变                       |    4     |
|            <color=#661111>高温</color>的[可加热方块](../001_feature/101_heated_block.md)            |                       不变                       |    4     |  
|            <color=#aa2222>红热</color>的[可加热方块](../001_feature/101_heated_block.md)            |                       不变                       |    16    |
|            <color=#cc5533>炽热</color>的[可加热方块](../001_feature/101_heated_block.md)            |                       不变                       |    64    |
|            <color=#ee7744>白炽</color>的[可加热方块](../001_feature/101_heated_block.md)            |                       不变                       |   256    |

# 发电方法

前中期好用的方法的有以下二种：

## 太阳能发电

- 通过[<translate key="block.anvilcraft.heliostats"/>](../004_block/110_heliostats.md)将太阳能收集到可加热方块上，集热器再吸收热能发电
- 优点：耗材简单，且无须后续投入
- 缺点：但是占地较大，且光路上不可有方块阻挡

## 油离子发电

- [燃烧原油发热](../007_struct/201_plasma_jets.md)需要大量的肉，但是发电量绝对够本
- 优点：占地面积小，效率高
- 缺点：持续消耗肉类，难以自动运行，需要配套的牧场/刷怪塔/刷怪笼

# 相关

- [热能系统](../001_feature/101_heated_block.md)