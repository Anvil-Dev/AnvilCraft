---
navigation:
  title: "§2诅咒金"
  icon: "anvilcraft:cursed_gold_ingot"
categories:
  - misc ingredients blocks
items:
  - anvilcraft:cursed_gold_block
  - anvilcraft:cursed_gold_ingot
  - anvilcraft:cursed_gold_nugget
---

# 诅咒金

<row halign="center">
<item id="anvilcraft:cursed_gold_block"/>
<item id="anvilcraft:cursed_gold_ingot"/>
<item id="anvilcraft:cursed_gold_nugget"/>
</row>

<gradient start="#ff5544" end="#bbaa55">被禁忌知识所诅咒的</gradient>

# 获取

在[<translate key="block.anvilcraft.royal_grindstone"/>](../004_block/102_royal_grindstone.md)上使用金锭(块)驱除诅咒附魔或附魔惩罚以获得

# 功能

- 制作[<translate key="block.anvilcraft.corrupted_beacon"/>](../004_block/200_corrupted_beacon.md)
- 拿到诅咒金的猪灵会被僵尸化，死亡掉落额外的诅咒金

<row halign="center" valign="center">
<entity id="minecraft:piglin" />
+ 
<item id="anvilcraft:cursed_gold_ingot"/>
=
<entity id="minecraft:zombified_piglin" />
</row>

<row halign="center" valign="center">

击杀
<entity id="minecraft:zombified_piglin" />
=
<item id="anvilcraft:cursed_gold_ingot"/>
+
<item id="anvilcraft:cursed_gold_nugget"/>
</row>

<tip>
有兴趣的话，可以考虑以此自动生产诅咒金
</tip>

# 特性

- 玩家背包装有诅咒金系列物品时（无论是粒还是锭还是块），按数量获得负面效果（未持有相关物品后10s消失）
  - 1到8个，虚弱II
  - 9到64个，虚弱II+缓慢II
  - 65个以及以上，虚弱II+缓慢II+饥饿II