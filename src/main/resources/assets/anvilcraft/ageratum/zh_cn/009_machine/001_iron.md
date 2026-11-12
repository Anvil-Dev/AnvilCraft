---
navigation:
  title: "资源：刷铁机"
  icon: "minecraft:iron_ingot"
---

# 刷铁机

<ref item="minecraft:iron_ingot"/>十分重要，因此需要制作一台刷铁机。

利用[铁砧抢夺](../001_feature/000_anvil_loot.md)配合[发射器修补铁傀儡](../001_feature/000_dispenser.md)的特性，自动生产大量<ref item="minecraft:iron_ingot"/>

1. 足够高的<ref item="minecraft:anvil"/>砸到铁傀儡时会掉落<ref item="minecraft:iron_ingot"/>
2. 通过<ref item="minecraft:dispenser"/>将一部分铁来修复受伤的铁傀儡
3. 在一只铁傀儡身上得到无限的<ref item="minecraft:iron_ingot"/>

<structure id="../../structures/machine/iron.nbt"/>

<warning>
只能放入一个铁砧(顶多错开放两个)
</warning>

机器造完最后塞一只铁傀儡，这么塞：

<structure id="../../structures/machine/iron_golem_placement.nbt"/>

1. 铁傀儡生成后不会乱跑，但是注意别碰到它
2. 按下活塞上的按钮下推方块，然后拆除活塞放上方块
3. 对着漏斗顶面按shift放置按钮，会放在方块侧面，完成

- 这台刷铁机使用**实心**<ref item="minecraft:glass"/>柱子把物品挤上去，柱子里的溜槽收集铁和花。
- 至少放三个溜槽在柱子里才能确保完全收集不卡住机器

<info>
- 所有 <ref item="minecraft:glass"/> 可替换为 任意完整方块
- 所有 <ref item="minecraft:oak_planks"/> 可替换为 任意完整不透明方块
- 板条箱不能替换成箱子，可以替换成桶
</info>

<warning>
刷铁机顶部结构必须和手册一样，不然铁砧可能丢失
</warning>

## 后期刷铁机

<structure id="../../structures/machine/iron_advanced.nbt"/>

后期刷铁机使用中子辐照快速增生铁块，这台机器每秒能刷出270块铁，虽然效率未必比得上优秀的铁傀儡刷铁机，但是占地和性能上优势很大。

<tip>
可以点击结构中的脉冲发生器来查看配置，或导出使用建筑杖粘贴
</tip>