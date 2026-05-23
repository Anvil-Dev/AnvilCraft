---
navigation:
  title: "§6流体储罐"
  icon: "anvilcraft:fluid_tank"
items:
  - anvilcraft:fluid_tank
  - anvilcraft:large_fluid_tank
---

# <ref item="anvilcraft:fluid_tank"/>

- 可以存放 16B 液体

<recipe id="anvilcraft:fluid_tank"/>

# <ref item="anvilcraft:large_fluid_tank"/>

1. 将27个<ref item="anvilcraft:fluid_tank"/>按3x3x3实心摆放，进行[多方块转化](210_giant_anvil.md#功能)获得
2. 将26个<ref item="anvilcraft:fluid_tank"/>按3x3x3中空摆放，进行[多方块转化](210_giant_anvil.md#功能)获得
- 可以存放 320B 液体

# <ref item="anvilcraft:menger_sponge"/>增幅

## <ref item="anvilcraft:menger_sponge"/>结构的判断条件

将3x3x3（也可以是9x9x9甚至更大）的空间均分为27份。保证每个面最中心的那块和正方体最中心的那块是空气，另外20个块都满足<ref item="anvilcraft:menger_sponge"/>结构。

如果是1x1x1的空间，则要求此方块为<ref item="anvilcraft:menger_sponge"/>

<info>
游戏内判定时，<ref item="anvilcraft:menger_sponge"/>结构中的空气部分可以是其他方块，但不能是<ref item="anvilcraft:menger_sponge"/>
</info>

<structure id="../structures/menger_sponge_struct.snbt"/>

## <ref item="anvilcraft:menger_sponge"/>对储罐的增幅

- 当<ItemLink id="anvilcraft:fluid_tank" />位于一个3x3x3<ref item="anvilcraft:menger_sponge"/>结构中心时，其储量变为 640B

- 当<ItemLink id="anvilcraft:large_fluid_tank" />位于一个9x9x9<ref item="anvilcraft:menger_sponge"/>结构中心时，其储量变为12800B。且一旦输入的流体达到了上限，接下来视为**无限流体储罐**，可无限输入与输出