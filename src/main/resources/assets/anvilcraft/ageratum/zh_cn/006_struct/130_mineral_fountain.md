---
navigation:
  title: "§2矿物涌泉"
  icon: "anvilcraft:mineral_fountain"
  parent: anvilcraft_guideme:struct.md
items:
  - anvilcraft:impact_pile
  - anvilcraft:mineral_fountain
  - anvilcraft:sturdy_deepslate
---

# 矿物涌泉

<row halign="center">
<item id="anvilcraft:impact_pile"/>
<item id="anvilcraft:mineral_fountain"/>
<item id="anvilcraft:sturdy_deepslate"/>
</row>

# 获得

<recipe id="anvilcraft:impact_pile"/>

1. 制作[<translate key="block.anvilcraft.impact_pile"/>](130_mineral_fountain.md)
2. 将[<translate key="block.anvilcraft.impact_pile"/>](130_mineral_fountain.md)放置在<translate key="block.minecraft.bedrock"/>或<translate key="block.minecraft.deepslate"/>上，
   并确保其位置**不高于**世界底部 8 格
3. 用至少 20 格的高度下落的**完好的**<translate key="block.minecraft.anvil"/>砸击它
4. 最终[<translate key="block.anvilcraft.impact_pile"/>](130_mineral_fountain.md)和<translate key="block.minecraft.anvil"/>都会消失，
   并生成一个包含[<translate key="block.anvilcraft.mineral_fountain"/>](130_mineral_fountain.md)、[<translate key="block.anvilcraft.sturdy_deepslate"/>](130_mineral_fountain.md)和熔岩的结构

> 生成[<translate key="block.anvilcraft.mineral_fountain"/>](130_mineral_fountain.md)的高度固定为世界最低高度+5
>
> 生成的结构会将除<translate key="block.minecraft.bedrock"/>外的方块替换

## <translate key="block.anvilcraft.sturdy_deepslate"/>

很硬的石头，没什么用

# 特性

- 坚硬抗爆
- 极难挖掘
- 破坏没有掉落物

# 功能

- 矿物涌泉仅在**不高于**世界底部 8 格的位置工作 （其他结构方块不参与工作，可随意破坏）

---

## 产矿

<structure id="../structures/mineral_fountain/raw_mineral.snbt"/>

- 如果[<translate key="block.anvilcraft.mineral_fountain"/>](130_mineral_fountain.md)的四面都是**同种粗矿块**，
  则将上方<translate key="block.minecraft.deepslate"/>转化为对应的**深层矿**
- 有概率转而生成[<translate key="block.anvilcraft.earth_core_shard_ore"/>](../002_material/140_earth_core_shard.md)或[<translate key="block.anvilcraft.void_stone"/>](../002_material/140_void_matter.md)

<info>
粗矿可通过[<translate key="block.anvilcraft.corrupted_beacon"/>](../005_block/200_corrupted_beacon.md)获得
</info>

| 世界  | 生成[<translate key="block.anvilcraft.earth_core_shard_ore"/>](../002_material/140_earth_core_shard.md)概率 | 生成[<translate key="block.anvilcraft.void_stone"/>](../002_material/140_void_matter.md)概率 | 
|:---:|:-------------------------------------------------------------------------------------------------------:|:----------------------------------------------------------------------------------------:|
| 主世界 |                                                   1%                                                    |                                            1%                                            |
| 下界  |                                                   10%                                                   |                                            0                                             |
| 末地  |                                                    0                                                    |                                           10%                                            |

---

## 产熔岩

<structure id="../structures/mineral_fountain/lava.snbt"/>

- 四周被**熔岩**环绕的[<translate key="block.anvilcraft.mineral_fountain"/>](130_mineral_fountain.md)可以生成**熔岩**

---

## 加热

<structure id="../structures/mineral_fountain/heat.snbt"/>

- 四周被**熔岩**环绕的[<translate key="block.anvilcraft.mineral_fountain"/>](130_mineral_fountain.md)可以将 [可加热方块](../001_feature/101_heated_block.md)
  加热到<color=#aa2222>红热等级</color>

---

## 产火山灰

<structure id="../structures/mineral_fountain/cinerite.snbt"/>

- 其他结构都不满足时，[<translate key="block.anvilcraft.mineral_fountain"/>](130_mineral_fountain.md)在上方生成<translate key="block.anvilcraft.cinerite"/>，可将其用于[筛矿](../007_recipe/001_basic_minerals.md)

