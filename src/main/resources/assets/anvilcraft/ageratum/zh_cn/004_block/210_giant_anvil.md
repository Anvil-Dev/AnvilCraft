---
navigation:
  title: "§6巨型铁砧"
  icon: "anvilcraft:giant_anvil"
items:
  - anvilcraft:giant_anvil
  - anvilcraft:transparent_crafting_table
---

# 巨型铁砧

<row halign="center">
<item id="anvilcraft:giant_anvil"/>
<item id="anvilcraft:transparent_crafting_table"/>
</row>

# 首次获得

- 手持<ref item="minecraft:anvil"/>右键僵尸，将铁砧塞到它手上
- 使得持有铁砧的僵尸被<ref item="anvilcraft:corrupted_beacon"/>照射
- 僵尸有 [手持铁砧数量*5%] 的概率变为拿着<ref item="anvilcraft:giant_anvil"/>的**巨人僵尸**
- 击杀**巨人僵尸**，就可以<ref item="anvilcraft:giant_anvil"/>

<tip>
可以使用<ref item="anvilcraft:resin_block"/>抓捕僵尸
</tip>

<warning>
**巨人僵尸**被本模组添加了AI，极其强大，请确保周围提前围好方块困住它，或是作为PVE高手战胜它
</warning>

# 便捷合成

获得第一个<ref item="anvilcraft:giant_anvil"/>之后，
就可以通过**多方块转化**这一方式生产<ref item="anvilcraft:giant_anvil"/>

# 功能

## 1.多方块转换

<ref item="anvilcraft:giant_anvil"/>砸中<ref item="minecraft:crafting_table"/>时，对下方多方块结构进行转化，生成新的方块

<structure id="../structures/mutiblock_convert.snbt"/>

<tip>
如果觉得<ref item="minecraft:crafting_table"/>不好看，可以试试<ref item="anvilcraft:transparent_crafting_table"/>
</tip>

<recipe id="anvilcraft:transparent_crafting_table"/>

## 2.多方块合成

- 将正中间的<ref item="minecraft:crafting_table"/>替换为<ref item="anvilcraft:space_overcompressor"/>，就能执行**多方块合成**
- 生产的结果为**掉落物**形式
- 兼容转换出一个方块的**多方块转换**配方
- 处理额外特殊配方

> 比多方块转化方便得多

## 3.撼地

- <ref item="anvilcraft:giant_anvil"/>的正中心砸中<ref item="anvilcraft:heavy_iron_block"/>时，执行**撼地**操作
- 此时，其可以影响同一水平面上的方块或实体。根据<ref item="anvilcraft:heavy_iron_block"/>周围方块的不同，撼地产生的效果也不尽相同
- 坠落高度提高 1 格，影响范围扩大 1 圈

### 定义

- 方便起见，我们将与<ref item="anvilcraft:heavy_iron_block"/>
  相邻的方块称为“邻块”，多方块结构中不与<ref item="anvilcraft:heavy_iron_block"/>相邻的方块称为“角块”
- **邻块**决定撼地的工作模式
- **角块**决定模式的工作类型

<structure id="../structures/giant_anvil_shocking.snbt"/>

[//]: # (    <DiamondAnnotation pos="0.5 0.5 0.5" color="#ff0000">)

[//]: # (        角块)

[//]: # (    </DiamondAnnotation>)

[//]: # (    <DiamondAnnotation pos="1.5 0.5 0.5" color="#ffff00">)

[//]: # (        邻块)

[//]: # (    </DiamondAnnotation>)

### 工作模式：默认

- **邻块**和**角块**不符合任意工作模式时启用
- **撼地**仅造成极少的伤害，穿有靴子既可避免

### 工作模式：震起

- **邻块**和**角块**均为<ref item="anvilcraft:resin_block"/>时启用
- **撼地**可以使周围的，任意种类的小型铁砧震起 1 格

### 工作模式：伤害

- **邻块**为<ref item="anvilcraft:cursed_gold_block"/>时启用
- <ref item="anvilcraft:giant_anvil"/>落点高度越高，**撼地**造成的伤害越高
- **角块**决定伤害类型，对应关系如下：

|                       角块                       |     伤害类型      |
|:----------------------------------------------:|:-------------:|
|    <ref item="anvilcraft:ruby_block"/>     |     火焰伤害      |
|  <ref item="anvilcraft:sapphire_block"/>   |     冰冻伤害      |
|    <ref item="anvilcraft:topaz_block"/>    |     雷电伤害      |
| <ref item="anvilcraft:void_matter_block"/> |     虚空伤害      |
|                       其他                       | 摔落伤害(可以穿靴子避免) |

### 工作模式：破坏

- **邻块**为铁砧时启用
- 属于[铁砧挖掘](../001_feature/000_anvil_destroy.md)，铁砧类型会决定不同的破坏效果
- **角块**决定破坏哪类方块：

|                      角块                      | 方块类型                                             |
|:--------------------------------------------:|:-------------------------------------------------|
|     <ref item="minecraft:obsidian"/>     | 任意方块                                             |
|   <ref item="minecraft:grass_block"/>    | 花、草、菌、灌木、藤蔓、农作物和雪片                               |
|    <ref item="minecraft:hay_block"/>     | 收割并补种包括小麦、南瓜、浆果、可可豆与下界疣等农作物。可以操作高于工作平面的相连原木上的可可豆 |
| <ref item="minecraft:oak_log"/>等任意**原木** | 原木、树叶、菌柄、疣块、仙人掌、紫颂植株与甘蔗。可以破坏高于工作平面的相连方面          |
|  <ref item="minecraft:amethyst_block"/>  | 紫水晶簇                                             |
