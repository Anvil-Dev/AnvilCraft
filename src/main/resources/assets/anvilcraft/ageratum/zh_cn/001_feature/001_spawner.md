---
navigation:
  title: "强制刷怪笼工作"
  icon: "minecraft:spawner"
  parent: anvilcraft_guideme:feature.md
---

# 强制刷怪笼工作

<row halign="center">
<item id="minecraft:spawner"/>
</row>

# 特性

被铁砧砸中的<translate key="block.minecraft.spawner"/>会立刻尝试一次刷怪

<structure id="../structures/spawner.snbt"/>

- 铁砧下落高度h决定刷怪概率p: **p = 1 - 1/h** (高度越高，概率越大)
- *不需要* **周围有玩家** (记得确保区块被加载)
- 需要 **满足刷怪笼刷部分生物的额外条件** (eg:光照条件)
- 需要 **周围的怪物数量低于上限** ，因此将附近怪物**快速**运走或击杀能大大提高效率