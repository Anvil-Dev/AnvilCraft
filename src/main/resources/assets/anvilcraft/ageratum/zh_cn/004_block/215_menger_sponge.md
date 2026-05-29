---
navigation:
  title: "§6门格海绵"
  icon: "anvilcraft:menger_sponge"
items:
  - anvilcraft:menger_sponge
---

# 门格海绵

<item id="anvilcraft:menger_sponge"/>

具有无限表面积的海绵

## 获取

- 依赖[多方块合成](210_giant_anvil.md#功能)

<tip>
建议[量产海绵](../008_recipe/002_sponge_gemmule.md)
</tip>

## 功能

- 作为方块时，接触到流体时，可以吸收半径销毁6格及以内的任何流体，且永不饱和
- 作为物品时可以清空炼药锅内的流体(可通过发射器自动化)


## 对储罐的增幅

- 当<ref item="anvilcraft:fluid_tank"/>位于一个3x3x3<ref item="anvilcraft:menger_sponge"/>结构中心时，其储量变为 640B

- 当<ref item="anvilcraft:large_fluid_tank"/>位于一个9x9x9<ref item="anvilcraft:menger_sponge"/>结构中心时，其储量变为12800B。且一旦输入的流体达到了上限，接下来视为**无限流体储罐**，可无限输入与输出

### 结构的判断条件

将3x3x3（也可以是9x9x9甚至更大）的空间均分为27份。保证每个面最中心的那块和正方体最中心的那块是空气，另外20个块都满足<ref item="anvilcraft:menger_sponge"/>结构。

如果是1x1x1的空间，则要求此方块为<ref item="anvilcraft:menger_sponge"/>

<info>
游戏内判定时，<ref item="anvilcraft:menger_sponge"/>结构中的空气部分可以是其他方块，但不能是<ref item="anvilcraft:menger_sponge"/>
</info>

<structure id="../structures/menger_sponge_struct.snbt"/>