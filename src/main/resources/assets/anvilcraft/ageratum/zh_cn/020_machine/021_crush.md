---
navigation:
  title: "加工：自动粉碎"
  icon: "anvilcraft:quartz_sand"
  parent: machine.md
---

# 加工：自动粉碎

# 前言

- 阅读[矿物获取](../007_recipe/001_basic_minerals.md)可知，*粉碎*是十分重要的加工步骤
- [<translate key="block.anvilcraft.block_placer"/>](../005_block/001_block_placer.md)后面箱子中的方块会被放出
- 方块立刻被<translate key="item.minecraft.anvil"/>砸个粉碎

如果需要更高效率，可以简单的延长，可以自行设计更高效的机器

# 图示

<row halign="center">
<GameScene zoom="3" background="#888888" interactive={true}>
    <structure id="../structures/machine/crush.snbt"/>
    <IsometricCamera yaw="200" pitch="45" />
    <BlockAnnotation x="3" y="5" z="1" color="#ff0000">
        设置为循环模式：每间隔 8gt 发出长度 8gt 的信号
    </BlockAnnotation>
</GameScene>

<Column>

- 所有 <translate key="item.minecraft.smooth_stone"/> 可替换为 任意完整不透明方块
- 所有 <translate key="item.minecraft.smooth_stone_slab"/> 可替换为 任意台阶
- 所有 <translate key="item.minecraft.anvil"/> 可替换为 任意铁砧
- 不能加工粉碎产物不是*下落方块*的配方

> 右键调整位置
>
> 左键调整角度
>
> 结构右上角的眼睛按钮可以隐藏图示

</Column>
</row>