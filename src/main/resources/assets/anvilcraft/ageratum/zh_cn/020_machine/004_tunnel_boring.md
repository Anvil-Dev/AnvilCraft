---
navigation:
  title: "资源：盾构机"
  icon: "minecraft:piston"
  parent: machine.md
---

# 资源：盾构机

盾构机：可以破坏前方方块，同时自身不断前进，从而方便的制造一条隧道的机器

[<translate key="block.anvilcraft.block_devourer"/>](../005_block/101_block_devourer.md)可以迅速精准的破坏前方一大片的方块，因此可以用来制作*盾构机*

# 简易盾构机（3x3)

在这里给出一种简单的*盾构机*的示例：

- 通过红石激活[<translate key="block.anvilcraft.block_devourer"/>](../005_block/101_block_devourer.md)，不断破坏掉前方3x3范围的方块
- 可以在开掘出的隧道中修建交通，或者挖矿

## 图示

<row halign="center">
<GameScene zoom="3" background="#888888" interactive={true}>
    <IsometricCamera yaw="185" pitch="10" />
    <structure id="../structures/machine/tunnel_boring_3x.snbt"/>
    <DiamondAnnotation pos="4.5 1.5 0.5" color="#ffffff">
        放置 存储矿车
    </DiamondAnnotation>
    <BlockAnnotation x="2" y="2" z="0" color="#00ff00">
        搭建时要最后放置
    </BlockAnnotation>
</GameScene>

<Column>

- 所有 <translate key="item.minecraft.smooth_stone"/> 可替换为 任意完整不透明方块
- 更新 <translate key="item.minecraft.sticky_piston"/> 启动机器
- 要停下就把机器拆了

> 右键调整位置
>
> 左键调整角度
>
> 鼠标悬浮于方块之上查看是什么方块

</Column>
</row>

# 大型盾构机（7x7)

通过控制铁砧下落撞击[<translate key="block.anvilcraft.block_devourer"/>](../005_block/101_block_devourer.md)，可以挖掘更大的隧道

从 2 格高落下的铁砧会使被砸到的[<translate key="block.anvilcraft.block_devourer"/>](../005_block/101_block_devourer.md)挖掘前方 7x7 的方块

这里给出一种简单的思路来挖掘一条截面 7x7 的宽敞隧道

## 图示

<row halign="center">
<GameScene zoom="3" background="#888888" interactive={true}>
    <structure id="../structures/machine/tunnel_boring_7x.snbt"/>
    <IsometricCamera yaw="185" pitch="10" />
    <DiamondAnnotation pos="5.5 1.5 0.5" color="#ffffff">
        放置 存储矿车
    </DiamondAnnotation>
    <BlockAnnotation x="0" y="2" z="0" color="#ff0000">
        搭建完成后激活此侦测器
    </BlockAnnotation>
    <DiamondAnnotation pos="0.5 0 0.5" color="#00ff00">
        清除黏液块下方和前方的方块
    </DiamondAnnotation>
    <DiamondAnnotation pos="4.5 0 0.5" color="#00ff00">
        清除黏液块下方和前方的方块。此黏液块会下移，所以要清除两层
    </DiamondAnnotation>
</GameScene>

<Column>

- 所有 <translate key="item.minecraft.smooth_stone"/> 可替换为 任意完整不透明方块
- 更新 <translate key="item.minecraft.sticky_piston"/> 启动机器
- 要停下就把机器拆了

> 右键调整位置
>
> 左键调整角度
> 
> 鼠标悬浮于方块之上查看是什么方块

</Column>
</row>