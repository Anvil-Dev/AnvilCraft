---
navigation:
  title: "基本方块处理"
  icon: "minecraft:stone"
  parent: anvilcraft_guideme:struct.md
---

# 铁砧：方块加工

让<translate key="item.minecraft.anvil"/>落在不同的方块上可以触发不同的效果，本条目的后续页面将会依次介绍：
> 注意：<translate key="item.minecraft.anvil"/>从高度不低于2格的砸落时，有概率损坏

> 在此之前，了解[<translate key="block.anvilcraft.magnet_block"/>](../002_material/001_magnet.md)会让你更方便地加工

# 方块 + 切石机：方块破坏

<row halign="center">
<GameScene zoom="2">
    <structure id="../structures/break.snbt"/>
    <ItemEntity pos="1.5 0.5 0.5" id="minecraft:cobblestone"></ItemEntity>
</GameScene>

- 可以破坏无法被普通TNT爆炸破坏的方块，如<translate key="block.minecraft.obsidian"/>，但是普通铁砧将固定损坏一个耐久等级。
- 为了防止方块破坏后铁砧掉在切石机上变为掉落物，你需要控制[<translate key="block.anvilcraft.magnet_block"/>](../002_material/001_magnet.md)收回铁砧的时间。
- 属于[铁砧挖掘](../001_feature/000_anvil_destroy.md)的一种实现

</row>


# 单方块处理：方块粉碎

<row halign="center">
<GameScene zoom="2"><structure id="../structures/block_crush.snbt"/></GameScene>

- <translate key="block.minecraft.cobblestone"/> → <translate key="block.minecraft.gravel"/> → <translate key="block.minecraft.sand"/>
- <translate key="block.minecraft.polished_granite"/> → <translate key="block.minecraft.granite"/> → <translate key="block.minecraft.red_sand"/>
- 有裂纹变种的方块 → 对应的裂纹变种
- ...

</row>

# 双方块处理：方块压合

<row halign="center">
<GameScene zoom="2">
    <structure id="../structures/press.snbt"/>
</GameScene>

- 苔藓块 + 泥土 → 草方块
- 树叶 + 泥土 → 灰化土
- 蘑菇块 + 泥土 → 菌丝体
- 下界疣块 + 下界岩 → 绯红菌岩
- 诡异疣块 + 下界岩 → 诡异菌岩
- 石头 + 石头 → 深板岩
- 玄武岩 + 玄武岩 → 黑石
- ...

</row>

# 双方块处理：方块涂抹

<row halign="center">
<GameScene zoom="2"><structure id="../structures/smear.snbt"/></GameScene>

<Column>
上方的方块不消耗
- 苔藓块 + 圆石 → 苔石
- 苔藓块 + 石砖 → 苔石砖
- 蜜脾块 + 任意铜制方块 → 对应的涂蜡铜制方块
- ...
</Column>
</row>

# 方块 + 炼药锅：方块压榨

转化方块，并在炼药锅中生成资源

<row halign="center">
<structure id="../structures/squeeze.snbt"/>

- 湿海绵 → 海绵 + 水
- 苔藓块 → 覆地苔藓 + 水
- 岩浆块 → 下界岩 + 熔岩
- 雪块 → 冰 + 细雪
- 满蜂巢 → 空蜂巢 + 蜂蜜
- ...

</row>

<info>
本模组改进：当炼药锅集满4层蜂蜜时，可以用漏斗等物流方块取出蜂蜜块
</info>
    