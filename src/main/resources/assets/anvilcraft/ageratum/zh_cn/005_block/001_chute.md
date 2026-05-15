---
navigation:
  title: "溜槽"
  icon: "anvilcraft:chute"
items:
  - anvilcraft:chute
  - anvilcraft:magnetic_chute
---

# 溜槽

<recipe id="anvilcraft:chute"/>

## 功能

- [<translate key="block.anvilcraft.chute"/>](001_chute.md)是一种特殊的漏斗，有9格容量，一次性输送一组物品
- 既可以将物品输入容器，也可以将物品丢出至世界上
- 打开gui可以:
    - 查看库存、改变输出方向和设置过滤
    - 在槽位中使用滚轮可以设置物品上限

> 可以通过 [<translate key="item.anvilcraft.filter"/>](../004_prop/003_filter.md) 设置更多过滤

## 特性

- 可以被红石信号锁定
- [<translate key="block.anvilcraft.chute"/>](001_chute.md)成链时，被指向的[<translate key="block.anvilcraft.chute"/>](001_chute.md)变为**简化溜槽**

<block id="anvilcraft:simple_chute"/>

## 简化溜槽

- 不吸取物品
- 仅容纳1组物品
- 不能设置过滤
- 不受红石信号控制

# 磁性溜槽

<recipe id="anvilcraft:magnetic_chute"/>

## 功能

- 具有[<translate key="block.anvilcraft.chute"/>](001_chute.md)的所有功能
- [<translate key="block.anvilcraft.magnetic_chute"/>](001_chute.md)可以吸取侧方甚至下方的物品，但无法自由调整输出

## 特性

- 如果向世界输出物品，则会将物品以一定速度喷出
- 不会变成**简化溜槽**

