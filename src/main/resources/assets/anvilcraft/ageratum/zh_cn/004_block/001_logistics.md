---
navigation:
  title: "物流元件"
  icon: "anvilcraft:chute"
items:
  - anvilcraft:chute
  - anvilcraft:magnetic_chute
  - anvilcraft:overflow_chute
  - anvilcraft:item_splitter
---

# <ref item="anvilcraft:chute"/>

<recipe id="anvilcraft:chute"/>

- <ref item="anvilcraft:chute"/>是一种特殊的<ref item="minecraft:hopper"/>，有9格容量，一次性输送一组物品
- 既可以将物品输入容器，也可以将物品丢出至世界上
- 可以被红石信号锁定
- 打开gui可以:
  - 查看库存、改变输出方向和设置过滤
  - 在槽位中使用滚轮可以设置物品上限

<tip>
通过<ref item="anvilcraft:filter"/>设置更多过滤
</tip>

# <ref item="anvilcraft:magnetic_chute"/>

<recipe id="anvilcraft:magnetic_chute"/>

## 功能

- 具有<ref item="anvilcraft:chute"/>的所有功能
- <ref item="anvilcraft:magnetic_chute"/>可以吸取侧方甚至下方的物品，但无法自由调整输出
- 如果向世界输出物品，则会将物品以一定速度喷出

# 简化溜槽

- 被<ref item="anvilcraft:chute"/>、<ref item="anvilcraft:magnetic_chute"/>指向的<ref item="anvilcraft:chute"/>变为**简化溜槽**
- 被<ref item="anvilcraft:magnetic_chute"/>指向的<ref item="anvilcraft:magnetic_chute"/>变为**简化磁性溜槽**

<row>
<block id="anvilcraft:simple_chute"/>
<block id="anvilcraft:simple_magnetic_chute"/>
</row>

## 存储特性

- 不吸取物品
- 仅容纳1组物品
- 不合并物品：槽内已有物品时会拒绝新的输入，待其传递出去后再接收下一批，以减少管道系统中的缓存

## 控制特性

- 不能设置过滤
- 不受红石信号控制

# <ref item="anvilcraft:overflow_chute"/>

<recipe id="anvilcraft:overflow_chute"/>

- 无 GUI
- 仅容纳1组物品
- 使用<ref item="anvilcraft:anvil_hammer"/>右击侧面可打开或关闭对应溢流口；主出口堵塞时，物品会尽量均分地从各溢流口输出

# <ref item="anvilcraft:item_splitter"/>

<recipe id="anvilcraft:item_splitter"/>

- 无 GUI
- 可容纳 16 组物品，但只能容纳同一种物品
- 正前方有容器时，会自动将内部物品平均分配给前方**相连的**容器，最多 16 格
- 正前方没有容器时，被*铁砧*砸中会按下落高度均分物品，抛向前方没有遮挡的空间（无需相连），最远 16 格

<structure id="../../structures/item_splitter.nbt"/>

<info>
- 向空间均分时，遇到遮挡会继续向前寻找空位，直到凑足份数或达到 16 格上限
- 例如铁砧从三格高度下落，正前方一格有遮挡时，物品会被均分到第二、第三、第四格中
- 最远 16 格；未能分配的部分会留在物品分配器内，不参与本次分配
</info>
