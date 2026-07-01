---
navigation:
  title: "基本方块处理"
  icon: "minecraft:stone"
---

# 铁砧：方块加工

让<ref item="minecraft:anvil"/>落在不同的方块上可以触发不同的效果，本条目的后续页面将会依次介绍：

<warning>
<ref item="minecraft:anvil"/>从高度不低于2格的砸落时，有概率损坏
</warning>

<tip>
查看此页面之前，了解<ref item="anvilcraft:magnet_block"/>会让你更方便地加工
</tip>

# 方块 + 切石机：方块破坏

<structure id="../../structures/break.snbt"/>

- 可以破坏无法被普通TNT爆炸破坏的方块，如<ref item="minecraft:obsidian"/>，但是普通铁砧将固定损坏一个耐久等级。
- 为了防止方块破坏后铁砧掉在切石机上变为掉落物，你需要控制<ref item="anvilcraft:magnet_block"/>收回铁砧的时间。
- 属于[铁砧挖掘](../001_feature/000_anvil_destroy.md)的一种实现

# 单方块处理：方块粉碎

<structure id="../../structures/block_crush.snbt"/>

- <ref item="minecraft:cobblestone"/> → <ref item="minecraft:gravel"/> → <ref item="minecraft:sand"/>
- <ref item="minecraft:polished_granite"/> → <ref item="minecraft:granite"/> → <ref item="minecraft:red_sand"/>
- 有裂纹变种的方块 → 对应的裂纹变种
- ...

# 双方块处理：方块压合

<structure id="../../structures/press.snbt"/>

- 苔藓块 + 泥土 → 草方块
- 树叶 + 泥土 → 灰化土
- 蘑菇块 + 泥土 → 菌丝体
- 下界疣块 + 下界岩 → 绯红菌岩
- 诡异疣块 + 下界岩 → 诡异菌岩
- 石头 + 石头 → 深板岩
- 玄武岩 + 玄武岩 → 黑石
- ...

# 双方块处理：方块涂抹

上方的方块不消耗，转化下方的方块

<structure id="../../structures/smear.snbt"/>

- 苔藓块 + 圆石 → 苔石
- 苔藓块 + 石砖 → 苔石砖
- 蜜脾块 + 任意铜制方块 → 对应的涂蜡铜制方块
- ...

# 方块 + 炼药锅：方块压榨

转化方块，并在炼药锅中生成资源

<structure id="../../structures/squeeze.snbt"/>

- 湿海绵 → 海绵 + 水
- 苔藓块 → 覆地苔藓 + 水
- 岩浆块 → 下界岩 + 熔岩
- 雪块 → 冰 + 细雪
- 满蜂巢 → 空蜂巢 + 蜂蜜
- ...

<info>
本模组改进：当炼药锅集满4层蜂蜜时，可以用漏斗等物流方块取出蜂蜜块
</info>

# 方块流程处理

- 接受包括但不限于上述各种加工方式作为单个环节，可以进行多个不同环节的加工，并循环数次
- 加工过程中的方块被破坏视为加工失败，返还初始方块的掉落物，但是可以被<ref item="minecraft:piston"/>和<ref item="anvilcraft:sliding_rail"/>推动

<info>
以<ref item="anvilcraft:redstone_computer"/>为例，它需要依次压入<ref item="anvilcraft:circuit_board"/>、<ref item="anvilcraft:processor"/>、<ref item="anvilcraft:disk"/>
</info>

<recipe id="anvilcraft:procedural_process/redstone_computer_from_procedural"/>
    