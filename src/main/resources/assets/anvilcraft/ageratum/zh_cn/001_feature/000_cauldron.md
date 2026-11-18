---
navigation:
  title: "锅"
  icon: "minecraft:cauldron"
---

# 锅的概念

- 锅的概念起源于原版的<ref item="minecraft:cauldron"/>
- 可以和<ref item="minecraft:glass_bottle"/>、<ref item="minecraft:bucket"/>交互以存取流体
- 在本模组中，通常用于和铁砧一起[加工](../007_struct/000_anvil_processing.md)物品、流体和方块。

# <ref item="minecraft:cauldron"/>

- 最基础的锅
- 物品在其中以掉落物形式存放
- 按离散档位存储流体，不能像大部分模组流体容器那样以 1mB 精度存取，每次必须以单档容量的整数倍存取
- （模组添加）使用<ref item="anvilcraft:anvil_hammer"/>右击侧面，可以生成输出口，将产物自动向外导出

<info>
原版熔岩锅档位为 1B，本模组修改为 250mB
</info>

<info>
## 档位
以修改过的熔岩锅为例，锅中可以存在 250mB, 500mB, 750mB 或 1B 的熔岩，但永远不会出现 1mB 的熔岩
</info>

# <ref item="anvilcraft:fish_tank"/>

- 更好用的锅
- 以类似箱子的方式永久存放物品
- 以 1mB 精度存取流体，但仍只能存储一种流体
- 使用<ref item="anvilcraft:anvil_hammer"/>右击侧面，可以生成输出口，将产物自动向外导出

# <ref item="anvilcraft:large_cauldron"/>

- 很大的锅
- 以类似箱子的方式永久存放物品
- 以 1mB 精度存取流体，可以存储多种流体
- 没有输出口，有复杂的输入输出规则

