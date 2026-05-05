---
navigation:
  title: "§5超温状态"
  icon: "anvilcraft:overheated_ember_metal_block"
  parent: anvilcraft_guideme:feature.md
items:
  - anvilcraft:overheated_ember_metal_block
---

# 超温状态

<row halign="center">
<item id="anvilcraft:ember_metal_block"/>
<item id="anvilcraft:overheated_ember_metal_block"/>
</row>

---

# 前置必修课：

- [热能系统](../001_feature/101_heated_block.md)
- [铁砧撞击合成](../005_block/215_large_electromagnet.md#铁砧撞击合成)

<warning>
不看前置课程，会看不懂这章
</warning>

---

# <translate key="block.anvilcraft.ember_metal_block"/>

- [<translate key="block.anvilcraft.ember_metal_block"/>](../002_material/211_ember_metal.md)也属于*可加热方块*，具有2个温度等级：
  - <color=#666666>普通</color>
  - <color=#6688cc>超温</color>
- 不能被常规方法加热，但可以被后文介绍的方式加热
- 非常不稳定，往往只能维持一小段时间。且在冷却后，有 5% 的概率变为<translate key="block.minecraft.netherite_block"/>
- 可以为[<translate key="block.anvilcraft.heat_collector"/>](../003_power/201_heat_collection.md)提供1024kW能量
- 烫伤踩在上面的生物、蒸发一定距离内的水

# 超温加热

<row halign="center">
<recipe id="anvilcraft:anvil_collision/anvil_tier_2_and_uranium_block_256"/>
<recipe id="anvilcraft:anvil_collision/anvil_tier_2_and_plutonium_block_256"/>
</row>

- 消耗[<translate key="block.anvilcraft.uranium_block"/>](../002_material/301_uranium.md)进行撞击，最多加热16个余烬金属块，最多持续 20s
- 消耗[<translate key="block.anvilcraft.plutonium_block"/>](../002_material/321_plutonium.md)进行撞击，最多加热16个余烬金属块，最多持续 60s