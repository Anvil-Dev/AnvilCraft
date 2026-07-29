---
navigation:
  title: "§6腐化信标"
  icon: "anvilcraft:corrupted_beacon"
items:
  - anvilcraft:corrupted_beacon
---

# 腐化信标

<row halign="center">
<item id="minecraft:beacon"/>
<item id="anvilcraft:corrupted_beacon"/>
</row>

<gradient start="#991155" end="#bbaa55">释放了曾被封印的凋灵之力</gradient>

# <ref item="anvilcraft:corrupted_beacon"/>

# 获取

1. 完全使用<ref item="anvilcraft:cursed_gold_block"/>作为*信标底座*
2. 消耗<ref item="anvilcraft:cursed_gold_ingot"/>激活<ref item="minecraft:beacon"/>
3. <ref item="minecraft:beacon"/>有概率转化为<ref item="anvilcraft:corrupted_beacon"/>(底座层数越多，转化概率越大)，此时天气被控制为雷雨天

| 层数 |  概率  | 基座诅咒金块 |   等价诅咒金锭    | 期望成功转化所需次数 | 95%的把握成功转化所需次数 |
|:--:|:----:|:------:|:-----------:|:----------:|:--------------:|
| 1  |  2%  |   9    |  81 = 1组15  |     50     |      149       |
| 2  |  5%  |   34   | 306 = 4组50  |     20     |       59       |
| 3  | 20%  |   83   | 747 = 11组43 |     5      |       14       |
| 4  | 100% |  164   | 1479 = 23组4 |     1      |       1        |

> **自动化**：使用铁砧将<ref item="anvilcraft:cursed_gold_ingot"/>压入<ref item="minecraft:beacon"/>

# 加工时移配方

- <ref item="anvilcraft:corrupted_beacon"/>只能使用<ref item="anvilcraft:cursed_gold_block"/>作为基座激活
- 激活后，下述结构可以实现时移操作

<structure id="../../structures/corrupted_beacon.snbt"/>

<info>
和<ref item="minecraft:beacon"/>不同，上方的方块不会妨碍<ref item="anvilcraft:corrupted_beacon"/>的激活
</info>

## 主要用途

- [量产世界基底方块](../008_recipe/200_world_block.md)
- [量产钻石](../008_recipe/201_diamond.md)
- [量产海洋之心](../008_recipe/205_sea_heart.md)
- [量产宝石](../008_recipe/204_gem.md)
- [量产下界合金](../008_recipe/210_netherite_ingot.md)

<info>
金属块都可被时移为粗矿形式，用于<ref item="anvilcraft:mineral_fountain"/>
</info>

<row halign="center">
<recipe id="anvilcraft:time_warp/raw_copper"/>
</row>

<row halign="center">
<recipe id="anvilcraft:time_warp/budding_amethyst"/>
<recipe id="anvilcraft:time_warp/wither_skeleton_skull"/>
</row>

<row halign="center">
<recipe id="anvilcraft:time_warp/wither_rose"/>
<recipe id="anvilcraft:time_warp/crying_obsidian"/>
</row>

# 生物交互

- 信标光柱会赋予生物**凋零**效果
- 转化特定生物：

|     原生物      |         转化结果         |
|:------------:|:--------------------:|
|      猪       |         疣猪兽          |
|      牛       |         劫掠兽          |
|     守卫者      |        远古守卫者         |
|      猪灵      |         猪灵蛮兵         |
|      村民      | 30%掠夺者、60%卫道士、10%唤魔者 |
|      悦灵      |          恼鬼          |
|      蝙蝠      |          幻翼          |
|      马       |    10%僵尸马、90%骷髅马     |
|      蠹虫      |         末影螨          |
|      骷髅      |    20%凋零骷髅、80%流浪者    |
| **村民召唤**的铁傀儡 |         监守者          |
