---
navigation:
  title: "资源：刷铁机"
  icon: "minecraft:iron_ingot"
---

# 资源：刷铁机

<ref item="minecraft:iron_ingot"/>十分重要，因此需要制作一台刷铁机。

利用[铁砧抢夺](../001_feature/000_anvil_loot.md)配合[发射器修补铁傀儡](../001_feature/000_dispenser.md)的特性，自动生产大量<ref item="minecraft:iron_ingot"/>

1. 足够高的<ref item="minecraft:anvil"/>砸到铁傀儡时会掉落<ref item="minecraft:iron_ingot"/>
2. 通过<ref item="minecraft:dispenser"/>将一部分铁来修复受伤的铁傀儡
3. 在一只铁傀儡身上得到无限的<ref item="minecraft:iron_ingot"/>

## 图示

<structure id="../../structures/machine/iron.nbt"/>

<tip>
左键调整位置；右键调整角度；PgUP/PgDN调整显示高度
</tip>

- 在*按钮*所在位置安置铁傀儡，*玻璃板*会限制其移动
- 中继器都调为3档（过高的频率会使得<ref item="minecraft:dispenser"/>发射出的多余铁锭来不及被溜槽收回）
- <ref item="minecraft:dispenser"/>边上的<ref item="anvilcraft:magnetic_chute"/>设置过滤：铁锭
- 投掷器边上的<ref item="anvilcraft:magnetic_chute"/>过滤：铁砧
- 使用**实心**<ref item="minecraft:glass"/>柱子把物品挤上去
- 做完后记得放入一个铁砧

<info>
- 所有 _钢筋混凝土_ 不必搭建，只是为了方便数格子
- 所有 <ref item="minecraft:glass"/> 可替换为 任意完整方块
- 所有 <ref item="minecraft:smooth_stone"/> 可替换为 任意完整不透明方块
- 所有 <ref item="minecraft:smooth_stone_slab"/> 可替换为 任意台阶
- 所有 <ref item="minecraft:anvil"/> 可替换为 任意铁砧 ，除了<ref item="minecraft:damaged_anvil"/>，因为它会直接消失没有掉落物
</info>

<warning>
最上面的<ref item="anvilcraft:magnetic_chute"/>不可替换，否则*铁砧*有概率从侧边飞出去
</warning>