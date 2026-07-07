---
navigation:
  title: "§2能量：压电晶体发电"
  icon: "anvilcraft:piezoelectric_crystal"
---

# 简易压电发电机

这大概是你的第一个发电机，它材料简单，前期即可搭建，即使发电量不多

发电量 4kW/个铁砧；共计 32kW

可以在此基础上继续展开

## 图示

<structure id="../../structures/machine/piezoelectric_crystal_0.snbt"/>

<tip>
左键调整位置；右键调整角度；PgUP/PgDN调整显示高度
</tip>

1. 脉冲发生器设置为循环模式：每间隔 5gt 输出信号 5gt ，控制铁砧下落

- 所有 <ref item="minecraft:smooth_stone"/> 可替换为 任意完整不透明方块
- 所有 <ref item="minecraft:anvil"/> 可替换为 任意铁砧

# 皇家铁砧下弹压电发电机

- 随着进一步的发展，第一台发电机的电量很可能不够用了，是时候进行升级了
- 铁砧从高处落下的撞击会穿透多层压电晶体，产生更多电量。<ref item="anvilcraft:royal_anvil"/>从高处落下不会摔碎，正是合适的选择
- 虽然高处落下的铁砧发电量更多，但从高处落下也需要更长的时间。有什么办法可以让铁砧更快下落呢？答案是用 <ref item="minecraft:slime_block"/> 向下弹射

发电量 15kW/个铁砧；共计 150kW

## 图示

<structure id="../../structures/machine/piezoelectric_crystal_1.snbt"/>

<tip>
左键调整位置；右键调整角度；PgUP/PgDN调整显示高度
</tip>

1. 活塞边上的*脉冲发生器*设置为上升沿模式：延时 3gt 输出信号 3gt
2. 更远的*脉冲发生器*设置为循环模式：每间隔 1gt 输出信号 9gt

- 所有 <ref item="minecraft:smooth_stone"/> 可替换为 任意完整不透明方块