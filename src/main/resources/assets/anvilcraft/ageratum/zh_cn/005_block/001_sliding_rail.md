---
navigation:
  title: "滑轨系统"
  icon: "anvilcraft:sliding_rail"
items:
  - anvilcraft:sliding_rail
  - anvilcraft:sliding_rail_stop
  - anvilcraft:powered_sliding_rail
  - anvilcraft:activator_sliding_rail
  - anvilcraft:detector_sliding_rail
---

# 滑轨系统

<row halign="center">
<item id="anvilcraft:sliding_rail"/>
<item id="anvilcraft:sliding_rail_stop"/>
<item id="anvilcraft:powered_sliding_rail"/>
<item id="anvilcraft:activator_sliding_rail"/>
<item id="anvilcraft:detector_sliding_rail"/>
</row>

# <translate key="block.anvilcraft.sliding_rail"/>

<recipe id="anvilcraft:sliding_rail"/>

- 类似于用于承载矿车的铁轨，滑轨用于运输*物品*和*方块*，并使得它们可以一直滑行下去
- 当一个*方块*被活塞推到滑轨上时，其将在滑轨上持续滑行
    - 若*方块*粘连别的*方块*一起移动(如<translate key="block.minecraft.slime_block"/>)，则作为一个整体一起滑动
- 可以用来划船

> 本模组为各种冰提供了[量产方式](../007_recipe/110_ice.md)

# <translate key="block.anvilcraft.sliding_rail_stop"/>

<recipe id="anvilcraft:sliding_rail_stop"/>

- 阻力极大 
  - 滑入的*物品*会停止在其中心
  - 滑入的*方块*会停在上面
- 可以吸住生物

# <translate key="block.anvilcraft.powered_sliding_rail"/>

<recipe id="anvilcraft:powered_sliding_rail"/>

- <color=#cccc44>未收到红石信号</color>时，与[<translate key="block.anvilcraft.sliding_rail_stop"/>](001_sliding_rail.md)表现一致
- <color=#cccc44>收到红石信号</color>时，
  - 令上方的*实体*和*方块*沿朝向滑动
  - 当[<translate key="block.anvilcraft.powered_sliding_rail"/>](001_sliding_rail.md)背后是[<translate key="block.anvilcraft.sliding_rail_stop"/>](001_sliding_rail.md)时，[<translate key="block.anvilcraft.sliding_rail_stop"/>](001_sliding_rail.md)上的*实体*和*方块*会被转移至[<translate key="block.anvilcraft.powered_sliding_rail"/>](001_sliding_rail.md)，并向前移动
## 特性

- 将物品转变为滑动状态的逻辑与<translate key="item.minecraft.piston"/>相同：
  - 最多移动12个方块的结构 
  - 部分方块不可动，(如 <translate key="block.minecraft.chest"/> )
  - 破坏部分方块，(如 <translate key="block.minecraft.shulker_box"/> )


# <translate key="block.anvilcraft.activator_sliding_rail"/>

<recipe id="anvilcraft:activator_sliding_rail"/>

- <color=#cccc44>未收到红石信号</color>时，与[<translate key="block.anvilcraft.sliding_rail"/>](001_sliding_rail.md)表现一致
- <color=#cccc44>收到红石信号</color>时：
  - 使有滑行的*方块*经过，使其在上方停顿一下，并对其发出脉冲信号
  - 如上方存在静止的*方块*，则持续对其发出信号

# <translate key="block.anvilcraft.detector_sliding_rail"/>

<recipe id="anvilcraft:detector_sliding_rail"/>

- 上方有*物品*和*方块*滑过时，自身向周围除了上方外的五个方向发出红石信号
- 可被<translate key="block.minecraft.comparator"/>检测，根据滑行的*方块*的所属结构的方块数量，输出红石信号
> eg：四个方块粘在一起经过时，<translate key="block.minecraft.comparator"/>发出信号强度为4