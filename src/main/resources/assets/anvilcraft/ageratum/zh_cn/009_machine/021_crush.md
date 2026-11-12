---
navigation:
  title: "加工：自动粉碎"
  icon: "anvilcraft:quartz_sand"
---

# 粉碎机

- 阅读[矿物获取](../008_recipe/001_basic_minerals.md)可知，*粉碎*是十分重要的加工步骤
- <ref item="anvilcraft:block_placer"/>后面箱子中的方块会被放出
- 方块立刻被<ref item="minecraft:anvil"/>砸个粉碎

如果需要更高效率，可以简单的延长，也可以自行设计更高效的机器

<structure id="../../structures/machine/crush.snbt"/>

<warning>
不能加工粉碎产物不是*下落方块*的配方，比如末地尘
</warning>

## 进阶版粉碎机

<structure id="../../structures/machine/crush_advanced.nbt"/>

这台粉碎机高效又智能，每秒能粉碎 10 个方块，把物品放入上面的箱子就会开始粉碎，产物进入下面的箱子，自动启停且能粉碎各种方块

<tip>
可以点击结构中的脉冲发生器来查看配置，或导出使用建筑杖粘贴
</tip>