---
navigation:
  title: "§5超温状态"
  icon: "anvilcraft:overheated_ember_metal_block"
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
- [铁砧撞击合成](../004_block/215_large_electromagnet.md#铁砧撞击合成)

<warning>
不看前置课程，会看不懂这章
</warning>

---

# <ref item="anvilcraft:ember_metal_block"/>

- <ref item="anvilcraft:ember_metal_block"/>也属于*可加热方块*，具有2个温度等级：
  - <color=#666666>普通</color>
  - <color=#6688cc>超温</color>
- 不能被常规方法加热，但可以被后文介绍的方式加热
- 非常不稳定，往往只能维持一小段时间。且在冷却后，有 5% 的概率变为<ref item="minecraft:netherite_block"/>
- 可以为<ref item="anvilcraft:heat_collector"/>提供1024kW能量
- 烫伤踩在上面的生物、蒸发一定距离内的水

# 超温加热

<row halign="center">
<recipe id="anvilcraft:anvil_collision/anvil_tier_2_and_uranium_block_256"/>
<recipe id="anvilcraft:anvil_collision/anvil_tier_2_and_plutonium_block_256"/>
</row>

- 消耗<ref item="anvilcraft:uranium_block"/>进行撞击，最多加热16个余烬金属块，最多持续 20s
- 消耗<ref item="anvilcraft:plutonium_block"/>进行撞击，最多加热16个余烬金属块，最多持续 60s