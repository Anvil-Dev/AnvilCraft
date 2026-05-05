---
navigation:
  title: "§6资源：树厂"
  icon: "minecraft:spruce_sapling"
  parent: machine.md
---

# 资源：树厂

# 简易树厂

想要取之不尽用之不竭的木材吗？

如果你已经看过 原木钻石机 ，你一定知道了在铁砧工艺中木材可以加工成钻石

建造这样一台**巨型铁砧树场**，原木就能源源不断进入箱子，并且不消耗骨粉

1. 使用[<translate key="item.anvilcraft.induction_light"/>](../005_block/101_induction_light.md)加快大树的生长
2. 使用[<translate key="item.anvilcraft.giant_anvil"/>](../005_block/210_giant_anvil.md)撼地的冲击破破坏大树
3. 使用[<translate key="item.anvilcraft.item_collector"/>](../005_block/101_item_collector.md)范围收集掉落物

## 图示

<row halign="center">
<GameScene zoom="3" background="#888888" interactive={true}>
    <IsometricCamera yaw="30" pitch="30" />
    <structure id="../structures/machine/tree.snbt"/>
    <BlockAnnotation x="4" y="1" z="3" color="#ffffff">
        设置收集半径: 8
    </BlockAnnotation>
    <BlockAnnotation x="5" y="1" z="3" color="#ffffff">
        设置过滤: 树苗
    </BlockAnnotation>
    <BlockAnnotation x="4" y="0" z="3" color="#ffffff">
        设置过滤: 原木、木棍
    </BlockAnnotation>
</GameScene>

<Column>

- 所有 <translate key="item.minecraft.glass"/> 可替换为 任意完整方块
- 所有 <translate key="item.minecraft.smooth_stone"/> 可替换为 任意完整不透明方块
- 机器**_不可旋转_**，确保树苗在机器的*西北角*，否则树无法生长！

> 右键调整位置
>
> 左键调整角度

</Column>
</row>