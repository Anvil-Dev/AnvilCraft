---
navigation:
  title: "§5锻星砧"
  icon: "anvilcraft:celestial_forging_anvil"
items:
  - anvilcraft:celestial_forging_anvil
  - anvilcraft:celestial_forging_anvil_amplifier
---

# <ref item="anvilcraft:celestial_forging_anvil"/>

<item id="anvilcraft:celestial_forging_anvil"/>

> 合成需要<ref item="anvilcraft:spacetime_supercomputer"/>与<ref item="anvilcraft:mass_energy_inverter"/>

- 依赖[多方块合成](210_giant_anvil.md#2多方块合成)获得
- 需要3x2x3的空间放置
- 可以创造天体，包括恒星与行星，获得资源

# 锻造天体

- 使用<ref item="anvilcraft:confined_time_anvilon"/>、<ref item="anvilcraft:confined_space_anvilon"/>、<ref item="anvilcraft:confined_mass_anvilon"/>、<ref item="anvilcraft:confined_energy_anvilon"/>依次放在左边，并使其构成一个合理的参数，即可搜索并锻造一个天体
- 锻造天体不消耗砧子

<tip>
调试参数：
  - 可以用对四个参数用**鼠标滚轮**便捷调整
  - 放入部分砧子后，将光标放在其他砧子物品上，可以显示匹配的砧子数量（如果前面先前放入砧子已无法满足条件则不显示）
</tip>

![褐矮星.png](../../textures/cfa/heai.png)

> 图中参数可以锻造一个褐矮星

## 锻造行星

1. 使用四个砧子控制四个参数，使<color=#bf6060>能量</color>与<color=#78bf78>时间</color>的**交点**和<color=#00bfbf>空间</color>与<color=#bfbf78>质量</color>的**交点**落于同一个颜色区域上
2. 图片右下角的文本会显示当前参数所对应的星球类型，锻造行星要求第一行和第三行为同一星球类型
3. 点击锻造按钮，消耗 1MW 电力并耗时 10s 锻造完成，之后不再消耗电力

## 锻造恒星

<structure id="../../structures/forging_stars.nbt"/>

1. 想要锻造一个恒星，首先需要准备四个<ref item="anvilcraft:celestial_forging_anvil_amplifier"/>，分别放置在<ref item="anvilcraft:celestial_forging_anvil"/>的四个角落
2. 使用四个砧子控制四个参数，确保四条线的**三个焦点**落于同一个颜色区域上
3. 图片右下角的文本会显示当前参数所对应的星球类型，锻造行星要求三行为同一星球类型
4. 点击锻造按钮，消耗 32MW 电力并耗时 10s 锻造完成，之后不再消耗电力

<info>
在装载增幅器时，即便锻造行星也将花费 32MW 电力
</info>

<recipe id="anvilcraft:item_inject/celestial_forging_anvil_amplifier"/>

> 合成需要<ref item="anvilcraft:spacetime_supercomputer"/>

更多信息请查看[天体类型](../001_feature/331_celestial_type.md)

# 天体与世界的交互

- 当锻造完毕后，*束星环*中会出现锻造出的星球
- 周围会出现引力，任何进入引力的生物、物品、弹射物等都会被吸引至星球，并受到伤害
- 对<ref item="anvilcraft:celestial_forging_anvil"/>通入**红石信号**可将天体放大，其引力场将同步放大

# 提取天体资源

## 建造巨构

1. 在<ref item="anvilcraft:celestial_forging_anvil"/>的GUI下方，点击绑定按钮
2. 在<ref item="anvilcraft:celestial_forging_anvil"/>的GUI右侧，选择*巨构*并提交对应**建筑材料**

巨构的建造条件、合成方式、合成材料和功能说明，请查看[巨构](332_mega_structure.md)。

<tip>
若要移除巨构，只需解绑再绑定星球
</tip>

## 物流交互

- 通过各种接口输入原材料，提取资源（[点我](332_interface.md)查看详情）

# 恒星演化

- 使用[恒星演化加速器](332_mega_structure.md#恒星演化加速器)可以加速恒星的衰老
- 部分恒星在最后会引发*超新星爆发*，摧毁其拥有的所有*巨构*，并产生巨大爆炸，波及十几格远
- 所有恒星在结束生命后都会变为*恒星残骸*

<info>
加速过程中，如果存在*戴森球*，其会收集到**无限电能**
</info>

## 恒星残骸

原恒星的质量决定其变为何种恒星残骸

| 质量砧子数量  | 恒星残骸 |
|:-------:|:----:|
| [1,54]  | 白矮星  |
| [55,58] | 中子星  |
| [59,64] |  黑洞  |

# 种子物品

GUI左下角可以塞入某些消耗物品，赋予锻星特殊效果

## 矿物富集

粗矿可以提高搜索到的天体对应矿物资源的出率和占比

## 隐秘天体

### 类主世界（Overworld Like）

使用<ref item="minecraft:grass_block"/>作为*种子物品*
砧子数量设置为：时间32，空间14，质量20，能量16

可以使用<ref item="anvilcraft:celestial_forging_anvil_portal"/>登陆该星球

### ⬛肉⬛球（⬛esh P⬛n⬛）

> 这⬛⬛⬛息，被⬛⬛⬛⬛抹去了...

# 全同天体

- 使用<ref item="anvilcraft:disk"/>右键<ref item="anvilcraft:celestial_forging_anvil"/>复制天体信息
- 将此<ref item="anvilcraft:disk"/>放入另一锻星砧的*种子物品*槽，消耗该<ref item="anvilcraft:disk"/>锻造另一个天体，和源天体拥有完全相同的构成，它们互相为*全同天体*
- 对极端天体（中子星、黑洞），需改为使用<ref item="anvilcraft:singularity_crystal"/>作为承载天体信息的媒介来完成锻造

# 放大天体

对<ref item="anvilcraft:celestial_forging_anvil"/>施加红石信号以放大天体

每3级红石信号，天体渲染出的大小是原来的 2 倍
