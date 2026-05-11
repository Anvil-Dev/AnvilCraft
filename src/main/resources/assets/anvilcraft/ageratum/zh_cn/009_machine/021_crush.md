---
navigation:
  title: "加工：自动粉碎"
  icon: "anvilcraft:quartz_sand"
---

# 加工：自动粉碎

- 阅读[矿物获取](../008_recipe/001_basic_minerals.md)可知，*粉碎*是十分重要的加工步骤
- [<translate key="block.anvilcraft.block_placer"/>](../004_block/001_block_placer.md)后面箱子中的方块会被放出
- 方块立刻被<translate key="block.minecraft.anvil"/>砸个粉碎

如果需要更高效率，可以简单的延长，也可以自行设计更高效的机器

## 图示

<structure id="../structures/machine/crush.snbt"/>

<tip>
左键调整位置；右键调整角度；PgUP/PgDN调整显示高度
</tip>

1. 脉冲发生器设置为循环模式：每间隔 8gt 发出长度 8gt 的信号

- 所有 <translate key="block.minecraft.smooth_stone"/> 可替换为 任意完整不透明方块
- 所有 <translate key="block.minecraft.smooth_stone_slab"/> 可替换为 任意台阶
- 所有 <translate key="block.minecraft.anvil"/> 可替换为 任意铁砧

<warning>
不能加工粉碎产物不是*下落方块*的配方，比如末地尘
</warning>