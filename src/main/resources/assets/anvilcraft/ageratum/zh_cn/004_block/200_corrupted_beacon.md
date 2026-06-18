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

# 腐化信标

## 获取

1. 完全使用<ref item="anvilcraft:cursed_gold_block"/>作为信标底座，
2. 消耗<ref item="anvilcraft:cursed_gold_ingot"/>激活信标
3. 信标有概率转化为<ref item="anvilcraft:corrupted_beacon"/>(底座层数越多，转化概率越大)，此时天气被控制为雷雨天

| 层数 |  概率  | 基座诅咒金块 |   等价诅咒金锭    | 期望成功转化所需次数 | 95%的把握成功转化所需次数 |
|:--:|:----:|:------:|:-----------:|:----------:|:--------------:|
| 1  |  2%  |   9    |  81 = 1组15  |     50     |      149       |
| 2  |  5%  |   34   | 306 = 4组50  |     20     |       59       |
| 3  | 20%  |   83   | 747 = 11组43 |     5      |       14       |
| 4  | 100% |  164   | 1479 = 23组4 |     1      |       1        |

> **自动化**：使用铁砧将诅咒金锭压入信标

## 功能

<structure id="../../structures/corrupted_beacon.snbt"/>

- 此结构可以实现时移操作，此时腐化信标必须是激活状态

> 这意味着如果使用磁铁块控制顶上的铁砧，其必须为<ref item="anvilcraft:hollow_magnet_block"/>

## 特性

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

# 时移

<row halign="center">
<recipe id="anvilcraft:time_warp/raw_copper"/>
> 金属块可被时移为粗矿形式
<recipe id="anvilcraft:time_warp/budding_amethyst"/>
<recipe id="anvilcraft:time_warp/wither_skeleton_skull"/>
<recipe id="anvilcraft:time_warp/wither_rose"/>
<recipe id="anvilcraft:time_warp/crying_obsidian"/>
</row>