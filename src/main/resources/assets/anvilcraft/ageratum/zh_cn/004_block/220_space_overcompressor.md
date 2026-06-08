---
navigation:
  title: "§6空间超压器"
  icon: "anvilcraft:space_overcompressor"
items:
  - anvilcraft:nesting_shulker_box
  - anvilcraft:over_nesting_shulker_box
  - anvilcraft:supercritical_nesting_shulker_box
  - anvilcraft:space_overcompressor
---

# 空间超压器

<row halign="center">
<item id="anvilcraft:nesting_shulker_box"/>
<item id="anvilcraft:over_nesting_shulker_box"/>
<item id="anvilcraft:supercritical_nesting_shulker_box"/>
<item id="anvilcraft:space_overcompressor"/>
</row>

# 空间超压器

## 合成

<row halign="center">
<recipe id="anvilcraft:item_inject/nesting_shulker_box"/>
<recipe id="anvilcraft:item_inject/over_nesting_shulker_box"/>
<recipe id="anvilcraft:item_inject/supercritical_nesting_shulker_box"/>
<recipe id="anvilcraft:block_compress/space_overcompressor"/>
</row>

## 功能

- 生产<ref item="anvilcraft:neutronium_ingot"/>：
  - 将任意金属块/锭/粒置于<ref item="anvilcraft:space_overcompressor"/>之上
  - 用铁砧砸入，转化为质量值
  - 当<ref item="anvilcraft:space_overcompressor"/>的积攒了足够的质量值后，在下方输出一个<ref item="anvilcraft:neutronium_ingot"/>
- 参与[多方块合成](210_giant_anvil.md#功能)

---

# 嵌套潜影盒

## 特性

- 继承了潜影盒的性质，会被活塞破坏
- 右键三种嵌套潜影盒，其分别会发出1,2,3次开启和关闭的声音，每次都会被<ref item="minecraft:observer"/>识别
- 拥有 27/54/108 组物品容量，无法手动存储，只能使用物流方块交互
- 可以解压回<ref item="minecraft:shulker_box"/>

<row halign="center">
<recipe id="anvilcraft:stamping/shulker_box_from_nesting_shulker_box"/>
<recipe id="anvilcraft:stamping/shulker_box_from_over_nesting_shulker_box"/>
<recipe id="anvilcraft:stamping/shulker_box_from_supercritical_nesting_shulker_box"/>
</row>