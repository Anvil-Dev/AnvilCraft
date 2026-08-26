---
navigation:
  title: "铁砧加工"
  icon: "minecraft:anvil"
---

## 目录

- [物品加工](#物品加工)
  - [冲压](#冲压)
  - [粉碎](#粉碎)
  - [压缩](#压缩)
  - [分解](#分解)
  - [过筛](#过筛)
  - [固液反应](#固液反应)
  - [快速烹饪](#快速烹饪)
- [方块加工](#方块加工)
  - [方块 + 切石机：方块破坏](#方块--切石机方块破坏)
  - [单方块处理：方块粉碎](#单方块处理方块粉碎)
  - [双方块处理：方块压合](#双方块处理方块压合)
  - [双方块处理：方块涂抹](#双方块处理方块涂抹)
  - [方块 + 炼药锅：方块压榨](#方块--炼药锅方块压榨)
- [方块流程处理](#方块流程处理)


# 物品加工

让<ref item="minecraft:anvil"/>落在特定方块上就可以加工该方块顶部或内部的物品，不同的特定方块有不同的处理，本条目的后续页面依次介绍

<warning>
注意：<ref item="minecraft:anvil"/>从高度不低于2格的砸落时，有概率损坏
</warning>

<tip>
查看此页面之前，了解<ref item="anvilcraft:magnet_block"/>会让你更方便地加工
</tip>

## 冲压

下方是<ref item="anvilcraft:stamping_platform"/>时，执行**物品冲压**操作，原料放在平台上，产物从下方掉出

<structure id="../../structures/item_stamping.snbt"/>

- 铁锭→铁压力板
- 金锭→金压力板
- 雪球→雪片
- 樱花树叶→粉色花瓣

<info>
基本都是将物品砸成对应的薄片
</info>

## 粉碎

下方是<ref item="anvilcraft:crushing_table"/>时，执行**物品粉碎**操作，原料放在平台上，产物从下方掉出

<structure id="../../structures/item_crush.snbt"/>

- 可以回收工具、武器和盔甲并分解出原料，远多于熔炼得到的
- 处理头颅：骷髅头颅 → 64骨粉；苦力怕的头 → 64火药
- 粉碎变成**掉落物**的方块，完成本文中方块粉碎的所有配方，但是存在 **20%** 的损耗
- 对原版配方的[增产](../008_recipe/001_efficient_recipe.md)

<row>
<recipe id="anvilcraft:item_crush/armor/diamond_boots_2_diamond"/>
<recipe id="anvilcraft:item_crush/string"/>
</row>

## 压缩

下方是<ref item="minecraft:cauldron"/>时，执行**物品压缩**操作，原料和产物都在锅中

<structure id="../../structures/item_compress.snbt"/>

- 如果物品有2x2或3x3的合成配方则会被执行，例如9铁粒→铁锭；9铁锭→铁块；4线→羊毛
- 如果一个物品既可以2x2合成又可以3x3合成，则执行3x3合成
- 除了原版的配方外，增加了3骨头→1骨块的配方也可以在此合成

## 分解

下方是<ref item="minecraft:iron_trapdoor"/>或<ref item="anvilcraft:unpacking_table"/>时，执行**物品分解**操作，原料放在平台上，产物从下方掉出

<structure id="../../structures/unpack.snbt"/>

- 如果物品有1→n的合成配方则会被执行，例如1铁锭→9铁粒
- 额外地，原版可以通过打破方块来分解的也可执行，数量按最大来
  - 西瓜→9西瓜片；荧石→4荧石粉
- 一些原版无法分解的建筑方块也可以通过此方法分解：
  - 石英块→4石英；紫水晶块→4紫水晶碎片

## 过筛

下方是<ref item="minecraft:scaffolding"/>或<ref item="anvilcraft:sifting_table"/>时，执行**物品过筛**操作，原料放在平台上，产物从下方掉出

<structure id="../../structures/mesh.snbt"/>

<recipe id="anvilcraft:mesh/gravel"/>

<info>
过筛额外产出约一半原料，可循环利用
</info>

## 固液反应

下方是装水<ref item="minecraft:cauldron"/>时，执行**固液反应**操作，原料和产物都在锅中，消耗一层水

<structure id="../../structures/solid_liquid.snbt"/>

- 铜制方块→锈蚀铜制方块
- 泥土→黏土
- 下界菌→对应疣块
- 蘑菇→对应蘑菇块
- 蜘蛛眼→发酵蜘蛛眼
- 珊瑚→对应珊瑚块
- 干海带→海带

## 快速烹饪

下方是<ref item="minecraft:cauldron"/>和<ref item="minecraft:campfire"/>时， 执行**快速烹饪**操作，原料和产物都在锅中，有的配方需要水

<structure id="../../structures/fast_cooking.snbt"/>

- 自动兼容所有烟熏炉配方和营火配方，此时不需要水

<recipe id="anvilcraft:smoking_warp_beef_2_cooked_beef"/>

# 方块加工

让<ref item="minecraft:anvil"/>落在不同的方块上可以触发不同的效果，本条目的后续页面将会依次介绍：

<warning>
<ref item="minecraft:anvil"/>从高度不低于2格的砸落时，有概率损坏
</warning>

<tip>
查看此页面之前，了解<ref item="anvilcraft:magnet_block"/>会让你更方便地加工
</tip>

## 方块 + 切石机：方块破坏

<structure id="../../structures/break.snbt"/>

- 可以破坏无法被普通TNT爆炸破坏的方块，如<ref item="minecraft:obsidian"/>，但是普通铁砧将固定损坏一个耐久等级。
- 为了防止方块破坏后铁砧掉在切石机上变为掉落物，你需要控制<ref item="anvilcraft:magnet_block"/>收回铁砧的时间。
- 属于[铁砧挖掘](../001_feature/000_anvil_destroy.md)的一种实现

## 单方块处理：方块粉碎

<structure id="../../structures/block_crush.snbt"/>

- <ref item="minecraft:cobblestone"/> → <ref item="minecraft:gravel"/> → <ref item="minecraft:sand"/>
- <ref item="minecraft:polished_granite"/> → <ref item="minecraft:granite"/> → <ref item="minecraft:red_sand"/>
- 有裂纹变种的方块 → 对应的裂纹变种
- ...

## 双方块处理：方块压合

<structure id="../../structures/press.snbt"/>

- 苔藓块 + 泥土 → 草方块
- 树叶 + 泥土 → 灰化土
- 蘑菇块 + 泥土 → 菌丝体
- 下界疣块 + 下界岩 → 绯红菌岩
- 诡异疣块 + 下界岩 → 诡异菌岩
- 石头 + 石头 → 深板岩
- 玄武岩 + 玄武岩 → 黑石
- ...

## 双方块处理：方块涂抹

上方的方块不消耗，转化下方的方块

<structure id="../../structures/smear.snbt"/>

- 苔藓块 + 圆石 → 苔石
- 苔藓块 + 石砖 → 苔石砖
- 蜜脾块 + 任意铜制方块 → 对应的涂蜡铜制方块
- ...

## 方块 + 炼药锅：方块压榨

转化方块，并在炼药锅中生成资源

<structure id="../../structures/squeeze.snbt"/>

- 湿海绵 → 海绵 + 水
- 苔藓块 → 覆地苔藓 + 水
- 岩浆块 → 下界岩 + 熔岩
- 雪块 → 冰 + 细雪
- 满蜂巢 → 蜂巢 + 蜂蜜
- ...

<info>
本模组改进：当炼药锅集满4层蜂蜜时，可以用漏斗等物流方块取出蜂蜜块
</info>

# 方块流程处理

- 接受包括但不限于上述各种加工方式作为单个环节，可以进行多个不同环节的加工，并循环数次
- 加工过程中的方块被破坏视为加工失败，返还初始方块的掉落物，但是可以被<ref item="minecraft:piston"/>和<ref item="anvilcraft:sliding_rail"/>推动以调整位置

<info>

以高效制作<ref item="minecraft:netherite_block"/>为例，<ref item="minecraft:ancient_debris"/>需要与<ref item="minecraft:raw_gold_block"/>、<ref item="minecraft:ancient_debris"/>依次压合，再经过[高温熔炼](../004_block/100_heater.md#高温熔炼)；上述步骤共需重复完成两次

</info>

<recipe id="anvilcraft:procedural_process/netherite_block"/>
