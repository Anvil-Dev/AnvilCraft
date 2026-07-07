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

<structure id="../../structures/machine/iron.snbt"/>

<tip>
左键调整位置；右键调整角度；PgUP/PgDN调整显示高度
</tip>

- 在按钮所在位置安置铁傀儡，玻璃板会限制其移动
- 中继器都调为3档（过高的频率会使得发射器发射出的多余铁锭来不及被溜槽收回）
- 发射器边上的溜槽设置过滤：铁锭
- 投掷器边上的设置过滤：铁砧
- 做完后记得放入一个铁砧

<info>
- 所有 <ref item="minecraft:glass"/> 可替换为 任意完整方块
- 所有 <ref item="minecraft:smooth_stone"/> 可替换为 任意完整不透明方块
- 所有 <ref item="minecraft:smooth_stone_slab"/> 可替换为 任意台阶
</info>