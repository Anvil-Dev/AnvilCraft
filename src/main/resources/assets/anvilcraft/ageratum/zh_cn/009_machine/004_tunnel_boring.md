---
navigation:
  title: "资源：盾构机"
  icon: "minecraft:piston"
---

# 资源：盾构机

盾构机：可以破坏前方方块，同时自身不断前进，从而方便的制造一条隧道的机器

<ref item="anvilcraft:block_devourer"/>可以迅速精准的破坏前方一大片的方块，因此可以用来制作*盾构机*

# 简易盾构机(3x3)

在这里给出一种简单的*盾构机*的示例：

- 通过红石激活<ref item="anvilcraft:block_devourer"/>，不断破坏掉前方3x3范围的方块
- 可以在开掘出的隧道中修建交通，或者挖矿

## 图示

<structure id="../../structures/machine/tunnel_boring_3x.snbt"/>

<tip>
左键调整位置；右键调整角度；PgUP/PgDN调整显示高度
</tip>

1. 搭建方块时，红石块要在最后放置
2. 铁轨上放置存储矿车
3. 更新 <ref item="minecraft:sticky_piston"/> 启动机器
4. 要停下就把机器拆了

- 所有 <ref item="minecraft:smooth_stone"/> 可替换为 任意完整不透明方块

# 大型盾构机(7x7)

通过控制铁砧下落撞击<ref item="anvilcraft:block_devourer"/>，可以挖掘更大的隧道

从 2 格高落下的铁砧会使被砸到的<ref item="anvilcraft:block_devourer"/>挖掘前方 7x7 的方块

这里给出一种简单的思路来挖掘一条截面 7x7 的宽敞隧道

## 图示

<structure id="../../structures/machine/tunnel_boring_7x.snbt"/>

<tip>
左键调整位置；右键调整角度；PgUP/PgDN调整显示高度
</tip>


1. 铁轨上放置存储矿车
2. 搭建完成后激活最后面的*侦测器*
3. 清除机器下方的两层方块，防止粘液块粘住
4. 更新 <ref item="minecraft:sticky_piston"/> 启动机器
5. 要停下就把机器拆了

- 所有 <ref item="minecraft:smooth_stone"/> 可替换为 任意完整不透明方块
