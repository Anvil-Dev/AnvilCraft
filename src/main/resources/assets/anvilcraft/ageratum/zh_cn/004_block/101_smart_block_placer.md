---
navigation:
  title: "§2智能方块放置器"
  icon: "anvilcraft:smart_block_placer"
items:
  - anvilcraft:smart_block_placer
---

# 智能方块放置器

<recipe id="anvilcraft:smart_block_placer"/>

# 范围放置

- <translate key="block.anvilcraft.smart_block_placer"/>从其背后的容器方块、实体库存、掉落物中取用物品
- 可以自定义在前面5x5x5的范围内放置方块
- 每隔 1s 放置一个方块
- 持续消耗电能16kW
- 收到红石信号停止工作

<info>
常规模式下没有过滤功能，放置器会放置所有物品，直到范围内没有空位或者没有物品可以使用
</info>

# 移动模式

- 使用 右下第二个按钮 启动
- 该模式下，<translate key="block.anvilcraft.smart_block_placer"/>会尝试将背后的方块直接放置在选取的点位上

<info>
该方块必须可被活塞移动，所以基岩和特定容器不会被移动
</info>

# 蓝图模式

- 将[<translate key="item.anvilcraft.structure_disk"/>](101_structure_scanner_alternative.md)放入<translate key="block.anvilcraft.smart_block_placer"/>，放置器会按照蓝图放置方块

<warning>
无法放置大小超过5x5x5的蓝图
</warning>