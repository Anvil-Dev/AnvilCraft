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

- 手持<translate key="block.minecraft.anvil"/>右键僵尸，将铁砧塞到它手上
- 使得持有铁砧的僵尸被[<translate key="block.anvilcraft.corrupted_beacon"/>](200_corrupted_beacon.md)照射
- 僵尸有 [手持铁砧数量*5%] 的概率变为拿着[<translate key="block.anvilcraft.giant_anvil"/>](210_giant_anvil.md)的**巨人僵尸**
- 击杀**巨人僵尸**，就可以[<translate key="block.anvilcraft.giant_anvil"/>](210_giant_anvil.md)

<tip>
可以使用[<translate key="block.anvilcraft.resin_block"/>](../004_prop/000_resin_block.md)抓捕僵尸
</tip>

<warning>
**巨人僵尸**被本模组添加了AI，极其强大，请确保周围提前围好方块困住它，或是作为PVE高手战胜它
</warning>

# 便捷合成

获得第一个[<translate key="block.anvilcraft.giant_anvil"/>](210_giant_anvil.md)之后，
就可以通过**多方块转化**这一方式生产[<translate key="block.anvilcraft.giant_anvil"/>](210_giant_anvil.md)

# 功能

## 1.多方块转换

[<translate key="block.anvilcraft.giant_anvil"/>](210_giant_anvil.md)砸中<translate key="block.minecraft.crafting_table"/>时，对下方多方块结构进行转化，生成新的方块

<structure id="../structures/mutiblock_convert.snbt"/>

<tip>
如果觉得<translate key="block.minecraft.crafting_table"/>不好看，可以试试[<translate key="block.anvilcraft.transparent_crafting_table"/>](210_giant_anvil.md)
</tip>

<recipe id="anvilcraft:transparent_crafting_table"/>

## 2.多方块合成

- 将正中间的<translate key="block.minecraft.crafting_table"/>替换为[<translate key="block.anvilcraft.space_overcompressor"/>](220_space_overcompressor.md)，就能执行**多方块合成**
- 生产的结果为**掉落物**形式
- 兼容转换出一个方块的**多方块转换**配方
- 处理额外特殊配方

> 比多方块转化方便得多

## 3.撼地

- [<translate key="block.anvilcraft.giant_anvil"/>](210_giant_anvil.md)的正中心砸中[<translate key="block.anvilcraft.heavy_iron_block"/>](../002_material/007_heavy_iron_block.md)时，执行**撼地**操作
- 此时，其可以影响同一水平面上的方块或实体。根据[<translate key="block.anvilcraft.heavy_iron_block"/>](../002_material/007_heavy_iron_block.md)周围方块的不同，撼地产生的效果也不尽相同
- 坠落高度提高 1 格，影响范围扩大 1 圈

### 定义

- 方便起见，我们将与[<translate key="block.anvilcraft.heavy_iron_block"/>](../002_material/007_heavy_iron_block.md)
  相邻的方块称为“邻块”，多方块结构中不与[<translate key="block.anvilcraft.heavy_iron_block"/>](../002_material/007_heavy_iron_block.md)相邻的方块称为“角块”
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

- **邻块**和**角块**均为[<translate key="block.anvilcraft.resin_block"/>](../004_prop/000_resin_block.md)时启用
- **撼地**可以使周围的，任意种类的小型铁砧震起 1 格

### 工作模式：伤害

- **邻块**为[<translate key="block.anvilcraft.cursed_gold_block"/>](../002_material/122_cruse_gold.md)时启用
- [<translate key="block.anvilcraft.giant_anvil"/>](210_giant_anvil.md)落点高度越高，**撼地**造成的伤害越高
- **角块**决定伤害类型，对应关系如下：

|                       角块                       |     伤害类型      |
|:----------------------------------------------:|:-------------:|
|    [<translate key="block.anvilcraft.ruby_block"/>](../002_material/000_gems.md)     |     火焰伤害      |
|  [<translate key="block.anvilcraft.sapphire_block"/>](../002_material/000_gems.md)   |     冰冻伤害      |
|    [<translate key="block.anvilcraft.topaz_block"/>](../002_material/000_gems.md)    |     雷电伤害      |
| [<translate key="block.anvilcraft.void_matter_block"/>](../002_material/140_void_matter.md) |     虚空伤害      |
|                       其他                       | 摔落伤害(可以穿靴子避免) |

### 工作模式：破坏

- **邻块**为铁砧时启用
- 属于[铁砧挖掘](../001_feature/000_anvil_destroy.md)，铁砧类型会决定不同的破坏效果
- **角块**决定破坏哪类方块：

|                      角块                      | 方块类型                                             |
|:--------------------------------------------:|:-------------------------------------------------|
|     <translate key="block.minecraft.obsidian"/>     | 任意方块                                             |
|   <translate key="block.minecraft.grass_block"/>    | 花、草、菌、灌木、藤蔓、农作物和雪片                               |
|    <translate key="block.minecraft.hay_block"/>     | 收割并补种包括小麦、南瓜、浆果、可可豆与下界疣等农作物。可以操作高于工作平面的相连原木上的可可豆 |
| <translate key="block.minecraft.oak_log"/>等任意**原木** | 原木、树叶、菌柄、疣块、仙人掌、紫颂植株与甘蔗。可以破坏高于工作平面的相连方面          |
|  <translate key="block.minecraft.amethyst_block"/>  | 紫水晶簇                                             |
