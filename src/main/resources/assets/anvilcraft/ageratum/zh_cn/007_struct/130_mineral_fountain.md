---
navigation:
  title: "§2矿物涌泉"
  icon: "anvilcraft:mineral_fountain"
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

## 获得

<recipe id="anvilcraft:impact_pile"/>

1. 制作<ref item="anvilcraft:impact_pile"/>
2. 将<ref item="anvilcraft:impact_pile"/>放置在<ref item="minecraft:bedrock"/>或<ref item="minecraft:deepslate"/>上，并确保其位置**不高于**世界底部 8 格
3. 用至少 20 格的高度下落的**完好的**<ref item="minecraft:anvil"/>砸击它
4. 最终<ref item="anvilcraft:impact_pile"/>和<ref item="minecraft:anvil"/>都会消失，并生成一个包含<ref item="anvilcraft:mineral_fountain"/>、<ref item="anvilcraft:sturdy_deepslate"/>和熔岩的结构

<info>
生成<ref item="anvilcraft:mineral_fountain"/>的高度固定为世界最低高度+5
</info>

<warning>
生成的结构会将除<ref item="minecraft:bedrock"/>外的方块替换
</warning>

### <ref item="anvilcraft:sturdy_deepslate"/>

很硬的石头，没什么用

## 特性

- 坚硬抗爆
- 极难挖掘
- 破坏没有掉落物

# 功能

- 矿物涌泉仅在**不高于**世界底部 8 格的位置工作 （其他结构方块不参与工作，可随意破坏）

---

## 产矿

<structure id="../../structures/mineral_fountain/raw_mineral.snbt"/>

- 如果<ref item="anvilcraft:mineral_fountain"/>的四面都是**同种粗矿块**，则将上方<ref item="minecraft:deepslate"/>转化为对应的**深层矿**
- 有概率转而生成<ref item="anvilcraft:earth_core_shard_ore"/>或<ref item="anvilcraft:void_stone"/>

<info>
粗矿可通过<ref item="anvilcraft:corrupted_beacon"/>获得
</info>

| 世界  | 生成<ref item="anvilcraft:earth_core_shard_ore"/>概率 | 生成<ref item="anvilcraft:void_stone"/>概率 | 
|:---:|:-------------------------------------------------:|:---------------------------------------:|
| 主世界 |                        1%                         |                   1%                    |
| 下界  |                        10%                        |                    0                    |
| 末地  |                         0                         |                   10%                   |

---

## 产熔岩

<structure id="../../structures/mineral_fountain/lava.snbt"/>

- 四周被**熔岩**环绕的<ref item="anvilcraft:mineral_fountain"/>可以生成**熔岩**

---

## 加热

<structure id="../../structures/mineral_fountain/heat.snbt"/>

- 四周被**熔岩**环绕的<ref item="anvilcraft:mineral_fountain"/>可以将[可加热方块](../001_feature/101_heated_block.md)加热到<color=#aa2222>红热等级</color>

---

## 产火山灰

<structure id="../structures/mineral_fountain/cinerite.snbt"/>

- 其他结构都不满足时，<ref item="anvilcraft:mineral_fountain"/>在上方生成<ref item="anvilcraft:cinerite"/>，可将其用于[筛矿](../008_recipe/001_basic_minerals.md)

