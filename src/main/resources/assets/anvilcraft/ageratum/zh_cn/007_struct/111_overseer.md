---
navigation:
  title: "§2监督者"
  icon: "anvilcraft:overseer"
items:
  - anvilcraft:overseer
---

# 监督者
<recipe id="anvilcraft:overseer"/>

# 功能

- 玩家离开基地时，区块卸载很容易导致机器损坏
- [<translate key="block.anvilcraft.overseer"/>](111_overseer.md)可以维持一定范围内的区块加载
- 使用它需要搭建多方块结构：
  - 底座需用0-3层3*3的[<translate key="item.anvilcraft.royal_steel_ingot"/>](../002_material/110_royal_steel.md)制作的建筑方块或[<translate key="block.anvilcraft.frost_metal_block"/>](../002_material/202_frost_metal.md)填充
  - 搭建0层底座加载所处区块
  - 搭建1层底座加载3*3区块
  - 搭建2层底座加载5*5的区块
  - 搭建3层底座加载7*7的区块
  - 底座中存在至少4个含水方块时，可以产生*随机刻*

<structure id="../structures/overseer.snbt"/>