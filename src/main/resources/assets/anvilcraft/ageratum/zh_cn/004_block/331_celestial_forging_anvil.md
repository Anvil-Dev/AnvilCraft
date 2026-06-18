---
navigation:
  title: "§5锻星砧"
  icon: "anvilcraft:celestial_forging_anvil"
items:
  - anvilcraft:celestial_forging_anvil
  - anvilcraft:celestial_forging_anvil_amplifier
  - anvilcraft:celestial_forging_anvil_logistics_interface
  - anvilcraft:celestial_forging_anvil_fluid_interface
  - anvilcraft:celestial_forging_anvil_laser_interface
---

# <ref item="anvilcraft:celestial_forging_anvil"/>

<item id="anvilcraft:celestial_forging_anvil"/>

- 依赖[多方块合成](210_giant_anvil.md#2多方块合成)获得
- 需要3x2x3的空间放置
- 可以超距绑定一个宇宙中的天体，获得资源

# 调整天体参数

打开<ref item="anvilcraft:celestial_forging_anvil"/>的GUI，在左侧可以摆放任意数量的四大基本砧子，其决定了天体的四项参数

|                        砧子                        |  参数  | 1个砧子对应的基准值 |           增长趋势           |
|:------------------------------------------------:|:----:|------------|:------------------------:|
|  <ref item="anvilcraft:confined_time_anvilon"/>  | 天体年龄 | 2My        |  每增加 3 个砧子, 年龄是原来的 2 倍   |
| <ref item="anvilcraft:confined_space_anvilon"/>  | 天体半径 | 0.125R⊕    |  每增加 3 个砧子, 半径是原来的 2 倍   |
|  <ref item="anvilcraft:confined_mass_anvilon"/>  | 天体质量 | 0.022M⊕    |  每增加 2 个砧子, 质量是原来的 2 倍   |
| <ref item="anvilcraft:confined_energy_anvilon"/> | 表面温度 | 50K        | 每增加 6 个砧子, 开尔文温度是原来的 2 倍 |

> 以下内容可跳过

<info>
**天文单位**
1. 时间单位：My（10^6年，百万年），By（10^9年，十亿年），Ty（10^12年，万亿年）
2. 长度单位：R⊕（倍地球半径），R☉（倍太阳半径）
3. 质量单位：M⊕（倍地球质量），M☉（倍太阳质量）
4. 温度单位：℃（摄氏度），K（开尔文）；℃ ≈ K-273, 0℃ = 273K, 100℃ = 373K
</info>

# 绑定恒星

- 只依靠<ref item="anvilcraft:celestial_forging_anvil"/>只能绑定行星
- 若要绑定恒星，需要合成并放置<ref item="anvilcraft:celestial_forging_anvil_amplifier"/>

<recipe id="anvilcraft:item_inject/celestial_forging_anvil_amplifier"/>

> 合成需要<ref item="anvilcraft:spacetime_supercomputer"/>

<structure id="../../structures/forging_stars.nbt"/>

按照正确结构搭建后，<ref item="anvilcraft:celestial_forging_anvil"/>进入*增幅状态*

# 绑定天体

1. 只有特定组合的天体参数，才能绑定到对应的天体
2. 如果参数组合正确，经过10s搜索后可以绑定到天体
3. 反复搜索指挥绑定到相同类型的天体，但具体物质构成会改变

## 搜索耗能

- 搜索过程中，<ref item="anvilcraft:celestial_forging_anvil"/>会消耗电力
- 默认持续耗电1MW(=1024kW)
- *增幅状态*下耗电为4MW

## 确定天体类型

1. 左、上、右、下四个轴依次表示天体的四个参数
2. 参数组会在左上、右上、左下构成3个焦点
3. 如果三个焦点颜色恰好相同，说明参数组成立，可以绑定到天体

![star-info](../../textures/star_info.png)

# 全同天体

- 使用<ref item="anvilcraft:disk"/>右键<ref item="anvilcraft:celestial_forging_anvil"/>复制天体信息
- 可以将此<ref item="anvilcraft:disk"/>放入另一锻星砧，消耗该<ref item="anvilcraft:disk"/>搜索另一个天体，和源天体拥有完全相同的参数
- 对极端天体（中子星、黑洞）需要改为使用<ref item="anvilcraft:singularity_crystal"/>作为承载天体信息的媒介来完成搜索

# 提取天体资源

1. 成功绑定天体后
2. 在<ref item="anvilcraft:celestial_forging_anvil"/>的GUI下方，点击绑定按钮
3. 在<ref item="anvilcraft:celestial_forging_anvil"/>的GUI右侧，选择*巨构*并提交对应建筑材料
4. 通过各种接口输入原材料，然后提取资源

<tip>
若要移除巨构，只需解绑再绑定星球
</tip>

## <ref item="anvilcraft:celestial_forging_anvil"/>锻星砧接口

### <ref item="anvilcraft:celestial_forging_anvil_logistics_interface"/>

<recipe id="anvilcraft:celestial_forging_anvil_logistics_interface"/>

### <ref item="anvilcraft:celestial_forging_anvil_fluid_interface"/>

<recipe id="anvilcraft:celestial_forging_anvil_fluid_interface"/>

### <ref item="anvilcraft:celestial_forging_anvil_laser_interface"/>

<row halign="center">
<recipe id="anvilcraft:celestial_forging_anvil_laser_interface"/>
<recipe id="anvilcraft:celestial_forging_anvil_laser_interface_from_large_laser"/>
</row>

## 行星巨构

- 不处于*增幅状态*下可以建造

### 星球开采器

- 需要向<ref item="anvilcraft:celestial_forging_anvil_laser_interface"/>输入16级[激光](201_basic_laser.md#激光)
- 最多可以以此法向4个<ref item="anvilcraft:celestial_forging_anvil_laser_interface"/>输入，获得最高4倍采集效率
- 1倍采集效率下，每秒采集20次矿物，概率可在GUI内查看
- 产物从<ref item="anvilcraft:celestial_forging_anvil_logistics_interface"/>获得

<warning>
即使向一个接口输入 64 级激光，仍算 1 倍效率
</warning>

### 星球抽取器

- 适用于存在液体的岩石行星
- 周围每有一个<ref item="anvilcraft:celestial_forging_anvil_fluid_interface"/>，获得1倍抽取效率
- 1倍采集效率下，每秒获得5B(桶)流体