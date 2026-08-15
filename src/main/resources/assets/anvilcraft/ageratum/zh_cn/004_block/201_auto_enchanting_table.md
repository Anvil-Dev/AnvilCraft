---
navigation:
  title: "§6自动附魔台"
  icon: "anvilcraft:auto_enchanting_table"
items:
  - anvilcraft:auto_enchanting_table
---

# <ref item="anvilcraft:auto_enchanting_table"/>

> 自动完成各种附魔工作

<row halign="center">
<recipe id="anvilcraft:auto_enchanting_table"/>
</row>

# 随机附魔

- 持续耗电 16 kW，电力不足时无法工作
- 统计<ref item="minecraft:enchanting_table"/>检测范围内的<ref item="minecraft:bookshelf"/>（或其他提供附魔能力的方块）提供的附魔能力，最高 15 级
- 放入物品后，根据附魔能力 n，单次消耗 n * 400 mB [液态经验](../002_material/004_exp_gem.md#经验流体)，耗时 4 秒完成一次随机附魔

# 定向附魔

![auto_enchanting_table.png](../../textures/auto_enchanting_table.png)

放入<ref item="anvilcraft:emerald_amulet"/>

- 持续耗电 64 kW，电力不足时无法工作
- 统计<ref item="minecraft:enchanting_table"/>检测范围内的<ref item="minecraft:bookshelf"/>（或其他提供附魔能力的方块）提供的附魔能力，无上限
- 可自选所有能通过与村民交易获得的附魔，支持多选，数量与等级受*附魔能力*限制
- 放入物品并**关闭选择界面**后，根据附魔能力 n，单次消耗 n * 400 mB [液态经验](../002_material/004_exp_gem.md#经验流体)，耗时 4 秒完成一次定向附魔

<tip>
只能附上物品兼容的附魔，例如不能给*剑*附上效率
</tip>

# 液态魔咒附魔

![auto_enchanting_table_2.png](../../textures/auto_enchanting_table_2.png)

放入[液态魔咒](../002_material/210_liquid_enchantment.md)**而非**[液态经验](../002_material/004_exp_gem.md#经验流体)

- 持续耗电 64 kW，电力不足时无法工作
- 消耗*液态魔咒*为物品附魔，消耗量按附魔等级递增：1 级 1 mB、2 级 2 mB、3 级 4 mB、4 级 8 mB、5 级 16 mB……以此类推
- 不再需要附魔能力
- 放入<ref item="anvilcraft:ember_anvil"/>或<ref item="anvilcraft:transcendence_anvil"/>可解锁以下能力：
  - 强行给物品打上彼此不兼容的附魔
  - 附魔等级可以突破上限
- 若物品已有附魔，根据已有附魔种类数 n，耗电增加 n * 64 kW