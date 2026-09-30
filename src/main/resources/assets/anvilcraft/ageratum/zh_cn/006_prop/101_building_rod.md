---
navigation:
  title: "§2建筑杖"
  icon: "anvilcraft:building_rod"
items:
  - anvilcraft:building_rod
---

# 建筑杖

<recipe id="anvilcraft:building_rod"/>

# 能量工具

- 可通过<ref item="anvilcraft:capacitor"/>或<ref item="anvilcraft:charger"/>充能
- 最多存储 8 MFE

# 被动效果

- 随身携带时生效；无论是否拥有能量，方块和实体触及距离均 +3

# 主动功能

## 填充模式

一只手持有<ref item="anvilcraft:building_rod"/>，另一只手持有*方块*时启用

- 右键确认第一个位置；继续按住右键并在另一位置松开以确认第二个位置，然后尝试用另一只手中的方块填充所选长方体区域
- 每放置一个方块消耗 100 FE，最多放置 4,000 个方块

<tip>
放置水等无限流体时，一次最多只会消耗两桶水
</tip>

### 放置特性

- 自动使用玩家物品栏中的物品
- 材料不足时会取消放置，不会只放置一部分
- 单次最多放置 4,000 个方块

## 蓝图模式

一只手持有<ref item="anvilcraft:building_rod"/>，另一只手持有**已录入结构的**<ref item="anvilcraft:structure_disk"/>时启用

1. 按住 Ctrl 可固定投影距离，右键可固定投影
2. 使用按键调整投影：
  - ↑、←、↓、→、PgUp、PgDn 调整位置
  - **-**、**=** 旋转
  - \ 镜像
  - 左键：取消放置
3. 再次右键尝试放置

- 材料不足时会取消放置，不会只放置一部分；但按住 Shift 放置时，可以只放置材料充足的部分

<info>
蓝图缺少材料时，若随身携带书，会在书中生成材料列表
</info>

<info>
如果不喜欢按键调整，可以在设置（config）里设为滚轮调整
</info>


## 额外特性

<tip>
填充大量方块时，即使电量不足以填充所有方块，也会完成本次填充。这是正常现象，不是 bug
</tip>

<tip>
按 Ctrl+Z 可撤回上一次放置
</tip>