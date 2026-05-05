---
navigation:
  title: "§2能量：压电晶体发电"
  icon: "anvilcraft:piezoelectric_crystal"
  parent: machine.md
---

# 能量：压电晶体发电

# 简易压电发电机

这大概是你的第一个发电机，它材料简单，前期即可搭建，即使发电量不多

发电量 4kW/个铁砧；共计 32kW

可以在此基础上继续展开

## 图示

<row halign="center">
<GameScene zoom="3" background="#888888" interactive={true}>
    <structure id="../structures/machine/piezoelectric_crystal_0.snbt"/>
    <BlockAnnotation x="4" y="3" z="1" color="#ff0000">
        设置为循环模式：每间隔 5gt 输出信号 5gt ，控制铁砧下落
    </BlockAnnotation>
</GameScene>

<Column>

- 所有 <translate key="item.minecraft.smooth_stone"/> 可替换为 任意完整不透明方块
- 所有 <translate key="item.minecraft.anvil"/> 可替换为 任意铁砧

> 右键调整位置
>
> 左键调整角度

</Column>
</row>

# 皇家铁砧下弹压电发电机

- 随着进一步的发展，第一台发电机的电量很可能不够用了，是时候进行升级了
- 铁砧从高处落下的撞击会穿透多层压电晶体，产生更多电量。[<translate key="item.anvilcraft.royal_anvil"/>](../005_block/103_royal_anvil.md)从高处落下不会摔碎，正是合适的选择
- 虽然高处落下的铁砧发电量更多，但从高处落下也需要更长的时间。有什么办法可以让铁砧更快下落呢？答案是用 <translate key="block.minecraft.slime_block"/>
  向下弹射

发电量 15kW/个铁砧；共计 150kW

## 图示

<row halign="center">
<GameScene zoom="3" background="#888888" interactive={true}>
    <structure id="../structures/machine/piezoelectric_crystal_1.snbt"/>
    <BlockAnnotation x="6" y="9" z="2" color="#ff0000">
        设置为循环模式：每间隔 1gt 输出信号 9gt
    </BlockAnnotation>
    <BlockAnnotation x="5" y="10" z="1" color="#ff0000">
        设置为上升沿模式：延时 3gt 输出信号 3gt
    </BlockAnnotation>
</GameScene>

<Column>

- 所有 <translate key="item.minecraft.smooth_stone"/> 可替换为 任意完整不透明方块

> 右键调整位置
>
> 左键调整角度

</Column>
</row>