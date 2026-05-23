---
navigation:
  title: "加工：自动过筛"
  icon: "minecraft:scaffolding"
---

# 加工：自动过筛

- 阅读[矿物获取](../008_recipe/001_basic_minerals.md)可知，*过筛*可以从闪长岩粉碎而成的石英砂中筛选出石英
- 但每次*过筛*只有一部分原料能被使用，手动反复过筛费时又费力
- 下面给出一个自动过筛的示例机器

## 图示

<structure id="../structures/machine/mesh.snbt"/>

<tip>
左键调整位置；右键调整角度；PgUP/PgDN调整显示高度
</tip>

1. 脉冲发生器设置为循环模式：每间隔 5gt 发出长度 3gt 的信号；驱动活塞运行
2. 箱子上的磁性溜槽设置过滤：过筛原料

- 所有 <ref item="minecraft:smooth_stone"/> 可替换为 任意完整不透明方块
- 所有 <ref item="minecraft:smooth_stone_slab"/> 可替换为 任意台阶
- 所有 <ref item="minecraft:anvil"/> 可替换为 任意铁砧
