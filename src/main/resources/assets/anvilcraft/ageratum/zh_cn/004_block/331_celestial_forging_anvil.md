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

> 合成需要<ref item="anvilcraft:spacetime_supercomputer"/>

- 依赖[多方块合成](210_giant_anvil.md#2多方块合成)获得
- 需要3x2x3的空间放置
- 可以创造天体，包括恒星与行星，获得资源

# 锻造天体

- 使用<ref item="anvilcraft:confined_time_anvilon"/>、<ref item="anvilcraft:confined_space_anvilon"/>、<ref item="anvilcraft:confined_mass_anvilon"/>、<ref item="anvilcraft:confined_energy_anvilon"/>依次放在左边，并使其构成一个合理的参数，即可搜索并制造一个天体
- 锻造天体不消耗砧子

<tip>
调试参数：
  - 可以用对四个参数用**鼠标滚轮**便捷调整
  - 放入部分砧子后，将光标放在其他砧子物品上，可以显示匹配的砧子数量（如果前面先前放入砧子已无法满足条件则不显示）
</tip>

![褐矮星.png](../../textures/cfa/heai.png)

> 图中参数可以制造一个褐矮星

## 锻造行星

1. 想要制造一个行星，首先要准备1MW的电力供其运行
2. 使用四个砧子控制四个参数，使红线(能量)与绿线(时间)的**交点**和蓝线(空间)与黄线(质量)的**交点**落于同一个颜色区域上
3. 右下角的文本显示了三个焦点分别对应了什么星球，只要让第一行和第三行显示同一个星球即可
4. 确定制造按钮，消耗 10s 制造完成后，点击下面的**锁图标**即可开始对天体进行操作

## 锻造恒星

<structure id="../../structures/forging_stars.nbt"/>

1. 想要制造一个恒星，首先需要准备四个<ref item="anvilcraft:celestial_forging_anvil_amplifier"/>，分别放置在<ref item="anvilcraft:celestial_forging_anvil"/>的四个角落
2. 要准备4MW的电力供其运行
3. 使用四个砧子控制四个参数，确保四条线**三个焦点**落于同一个颜色区域上
4. 确定制造按钮，消耗 10s 制造完成后，点击下面的**锁图标**即可开始对天体进行操作

<info>
在装载增幅器时，即便制造行星也将花费4MW电力
</info>

<recipe id="anvilcraft:item_inject/celestial_forging_anvil_amplifier"/>

> 合成需要<ref item="anvilcraft:spacetime_supercomputer"/>

更多信息请查看[天体类型](../001_feature/331_celestial_type.md)

# 天体与世界的交互

- 当搜索完毕后，*束星环*中会出现搜索到的星球
- 周围会出现引力，任何进入引力的生物、物品、弹射物等都会被吸引至星球，并受到伤害
- 使用**红石信号**可将天体放大，其引力场将同步放大

# 提取天体资源

1. 在<ref item="anvilcraft:celestial_forging_anvil"/>的GUI下方，点击绑定按钮
2. 在<ref item="anvilcraft:celestial_forging_anvil"/>的GUI右侧，选择*巨构*并提交对应**建筑材料**
3. 通过各种接口输入原材料，提取资源（[点我](332_interface.md)查看详情）

<tip>
若要移除巨构，只需解绑再绑定星球
</tip>

## 行星级巨构

- 不处于*增幅状态*下可以建造，只能建造于卫星/行星
- 一般情况下最多只能修建一个巨构

|   巨构   |      建造条件       |                     输入                      |                   输出                    | 
|:------:|:---------------:|:-------------------------------------------:|:---------------------------------------:|
| 星球开采器  |    大型卫星、岩石行星    | 16级[激光](201_basic_laser.md#激光) (不可以是*伽马激光*) | 物品（矿物）； 兼容<ref item="anvilcraft:lens"/> |
| 星球抽取器  |  存在**液体**的岩石行星  |                      无                      |                流体（星球资源）                 |
|  生态站   | 存在**生物资源**的岩石行星 |                   耗电 1MW                    |               物品&流体（生物资源）               |
|   神殿   | 存在**低等文明**的岩石行星 |                    特定物品                     |                 物品（供奉）                  |
| 巨行星抽取器 |     气巨星、冰巨星     |                      无                      |      物品&流体（星球资源）；必须收集流体，才能顺带将物品抽上来      |

<info>
对于*星球开采器*，最多可以以此法向4个<ref item="anvilcraft:celestial_forging_anvil_laser_interface"/>输入，获得最高4倍采集效率
注意，即使向一个接口输入 64 级激光，仍算 1 倍效率
</info>

<info>
*神殿*输入的物品为作为神明给于的恩赐或天罚，用来维持低等文明的信仰，每mc日更新一次物品需求（以两次恩赐一次天罚的顺序循环）
输入物品后，文明将持续供奉直到下一MC日，因此，不建议在晚上提供物品，因为文明总是停止供奉于凌晨，届时需要再次给予恩赐或天罚
</info>

## 恒星级巨构

- 处于*增幅状态*下可以建造，只能建造于恒星级天体
- 一般情况下最多只能修建一个巨构

|   巨构    |    建造条件    |   输入   | 输出/作用                                                                                                                                                    | 
|:-------:|:----------:|:------:|:---------------------------------------------------------------------------------------------------------------------------------------------------------|
|  星环对撞机  |    小型恒星    | 耗电 4MW | 执行[铁砧撞击合成](215_large_electromagnet.md#铁砧撞击合成)配方，恒星引力和磁场越强，工作越快；配方需要的速度越高，工作越慢                                                                            |
|   戴森球   |     恒星     |   无    | 持续发电，发电量正相关于天体的*温度*和*半径*                                                                                                                                 |
| 恒星演化加速器 | 恒星（不包括白矮星） |   无    | 使恒星加速演化                                                                                                                                                  |
|  磁星线圈   |    中子星     |   无    | 持续发电，发电量正相关于天体的*磁场强度*和*转速*                                                                                                                               |
|  虫洞稳定器  |     黑洞     |   无    | [虫洞](332_teleportation.md)                                                                                                                               |
|  彭罗斯球   |     黑洞     |   激光   | 同等级[伽马激光](../001_feature/332_gamma_laser.md)；需注意**彭罗斯球**输入和输出的[激光](201_basic_laser.md#激光)需成组的输入和输出于锻星砧同侧的左右两边。四个侧面输入和输出的[激光](201_basic_laser.md#激光)相互独立。 |
|  物质解压器  |    中子星     | *伽马激光* | 每级*伽马激光*提供一倍工作效率，每 10s 开采一次，大概率产出1个<ref item="anvilcraft:neutronium_ingot"/>，小概率产出<ref item="anvilcraft:charged_neutronium_ingot"/>（需要磁场强度够高，概率正相关于磁场）   |
|  物质解压器  |     黑洞     | *伽马激光* | 每级*伽马激光*提供一倍工作效率，每 gt 开采一次，大概率产出1个<ref item="anvilcraft:void_matter"/>，小概率产出<ref item="anvilcraft:excited_state_void_matter"/>（需要磁场强度够高，概率正相关于磁场）        |

![彭罗斯球.png](../../textures/cfa/gama.png)

> 制造[伽马激光](../001_feature/332_gamma_laser.md)

# 恒星演化

- 使用*恒星演化加速器*可以加速恒星的衰老
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

# 全同天体

- 使用<ref item="anvilcraft:disk"/>右键<ref item="anvilcraft:celestial_forging_anvil"/>复制天体信息
- 将此<ref item="anvilcraft:disk"/>放入另一锻星砧，消耗该<ref item="anvilcraft:disk"/>锻造另一个天体，和源天体拥有完全相同的构成，它们互相为*全同天体*
- 对极端天体（中子星、黑洞），需改为使用<ref item="anvilcraft:singularity_crystal"/>作为承载天体信息的媒介来完成搜索

# 放大天体

对<ref item="anvilcraft:celestial_forging_anvil"/>施加红石信号以放大天体

每3级红石信号，天体渲染出的大小是原来的 2 倍

> 这⬛⬛⬛息，被⬛⬛⬛⬛抹去了
