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

- 生产[<translate key="item.anvilcraft.neutronium_ingot"/>](../002_material/221_neutronium_ingot.md)：
  - 将任意金属块/锭/粒置于[<translate key="block.anvilcraft.space_overcompressor"/>](220_space_overcompressor.md)之上
  - 用铁砧砸入，转化为质量值
  - 当[<translate key="block.anvilcraft.space_overcompressor"/>](220_space_overcompressor.md)的积攒了足够的质量值后，在下方输出一个[<translate key="item.anvilcraft.neutronium_ingot"/>](../002_material/221_neutronium_ingot.md)
- 参与[多方块合成](210_giant_anvil.md#功能)

---

# 嵌套潜影盒

## 特性

- 继承了潜影盒的性质，会被活塞破坏
- 右键三种嵌套潜影盒，其分别会发出1,2,3次开启和关闭的声音，每次都会被<translate key="block.minecraft.observer"/>识别
- 可以解压回<translate key="block.minecraft.shulker_box"/>

<row halign="center">
<recipe id="anvilcraft:stamping/shulker_box_from_nesting_shulker_box"/>
<recipe id="anvilcraft:stamping/shulker_box_from_over_nesting_shulker_box"/>
<recipe id="anvilcraft:stamping/shulker_box_from_supercritical_nesting_shulker_box"/>
</row>