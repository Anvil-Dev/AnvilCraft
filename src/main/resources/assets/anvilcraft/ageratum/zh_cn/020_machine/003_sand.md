---
navigation:
  title: "资源：三模沙机"
  icon: "minecraft:sand"
  parent: machine.md
---

# 资源：三模沙机

# 前言

制造沙砾和沙子的机器，结构和刷石机类似

接下来这台三模式砸沙机，可以控制拉杆切换产出圆石、沙砾还是沙子

# 图示

<row halign="center">
<GameScene zoom="3" background="#888888" interactive={true}>
    <structure id="../structures/machine/sand.snbt"/>
    <BlockAnnotation x="1" y="6" z="2" color="#ff0000">
        设置为循环模式：每间隔 10gt 发出长度 5gt 的信号
    </BlockAnnotation>
    <BlockAnnotation x="5" y="6" z="2" color="#ff0000">
        设置为上升沿模式：延迟 6gt 发出长度 17gt 的信号
    </BlockAnnotation>
    <BlockAnnotation x="1" y="6" z="0" color="#ffff00">
        1号开关：机器总开关
    </BlockAnnotation>
    <BlockAnnotation x="1" y="3" z="0" color="#ffff00">
        2号开关：开启则生产圆石；关闭则启用3号开关
    </BlockAnnotation>
    <BlockAnnotation x="2" y="6" z="0" color="#ffff00">
        3号开关：开启则生产沙子；关闭则生产沙砾
    </BlockAnnotation>
    <DiamondAnnotation pos="1.5 1.5 3.5" color="#ffffff">
        放一个漏斗矿车
    </DiamondAnnotation>
</GameScene>

<Column>

- 所有 <translate key="item.minecraft.smooth_stone"/> 可替换为 任意完整不透明方块
- 所有 <translate key="item.minecraft.glass"/> 可替换为 任意透明方块
- 关闭总开关后，再调整模式

> 右键调整位置
> 
> 左键调整角度

</Column>
</row>