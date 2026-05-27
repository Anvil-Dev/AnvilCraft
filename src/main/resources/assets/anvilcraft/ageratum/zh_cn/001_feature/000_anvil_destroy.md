---
navigation:
  title: "铁砧挖掘"
  icon: "minecraft:anvil"
---

# 铁砧挖掘

# 定义

铁砧挖掘是一种概念。下述几种行为都实现了这个概念，因此拥有共同的性质

# 实现

- 通过[切石机配合铁砧](../007_struct/000_block_processing.md)破坏单个方块
- 通过[铁砧触发方块吞噬器](../004_block/101_block_devourer.md)破坏范围内方块
- 通过[巨型铁砧撼地](../004_block/210_giant_anvil.md)破坏大范围内方块

# 效果

使用不同的铁砧参与破坏方块的行为，具有不同的破坏效果：

<row>
<item id="minecraft:anvil"/>
<item id="anvilcraft:spectral_anvil"/>
<item id="anvilcraft:royal_anvil"/>
<item id="anvilcraft:ember_anvil"/>
<item id="anvilcraft:transcendence_anvil"/>
</row>

-  <ref item="minecraft:anvil"/>：常规挖掘
- <ref item="anvilcraft:spectral_anvil"/>：常规挖掘
- <ref item="anvilcraft:royal_anvil"/>：精准采集
- <ref item="anvilcraft:frost_anvil"/>：[崩解](100_enchantment.md#崩解)
- <ref item="anvilcraft:ember_anvil"/>：[熔炼](100_enchantment.md#熔炼)
- <ref item="anvilcraft:transcendence_anvil"/>：时运V