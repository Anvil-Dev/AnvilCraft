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

# 副手功能

- 放置在副手时生效；无论是否拥有能量，方块触及距离 +5，实体触及距离 +3

# 主手功能

## 填充模式

副手持有*方块*时启用

- 右键确认第一个位置，继续按住右键，在另一个位置松开以确认第二个位置；随后会尝试用副手中的方块填充框选的长方体区域
- 每放置一个方块消耗 100 FE，最多放置 4000 个方块

### 放置特性

- 自动调用玩家物品栏中的物品
- 材料不足时会取消放置，不会只放置一部分
- 单次最多放置 4000 个方块

<tip>
放置大型方块时，请确保框选范围大于该方块的体积
</tip>

## 蓝图模式

副手持有**录入结构的**<ref item="anvilcraft:structure_disk"/>时启用

1. 按住 Ctrl 可将投影悬停在空中，右键将其固定
2. 使用按键调整投影：
  - ↑、←、↓、→、PgUp、PgDn 调整位置
  - +、- 旋转
  - \ 镜像
  - 左键：取消放置
3. 再次右击，尝试放置

- 材料不足时会取消放置，不会只放置一部分；但按住 Shift 放置时，可以只放置材料充足的部分

## 额外特性

<tip>
填充大量方块时，即使电量不足以填充所有方块，也会完成本次填充。这是正常现象，不是 bug
</tip>
