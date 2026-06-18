---
navigation:
  title: "§2加热器"
  icon: "anvilcraft:heater"
items:
  - anvilcraft:heater
  - anvilcraft:burning_heater
---
<ref item="anvilcraft:burning_heater"/>

# 加热器

- 加热上方的[可加热方块](../001_feature/101_heated_block.md)
- 和<ref item="minecraft:cauldron"/>组成结构，执行**高温熔炼**操作

---

## 燃烧加热器

<recipe id="anvilcraft:block_crush/burning_heater"/>

- 放入燃料增加燃烧时间，上限 1200s
- 燃烧时间大于等于240秒时，才有足够的温度工作
- 每执行一批**高温熔炼**操作，消耗 240s 燃烧时间

---

## 电加热器

<recipe id="anvilcraft:heater"/>

- 持续耗电16kW，电力不足时无法工作

# 高温熔炼

<structure id="../../structures/super_heating.snbt"/>

高温熔炼是一种加工方法，可以批量处理锅中的原料

1. 处理**熔炉配方**和**高炉配方**
2. 处理专属配方
3. 双倍冶炼矿物

<warning>
不可以烹饪食物，请移步[物品加工：烹饪](../007_struct/000_item_processing.md#烹饪)
</warning>

<recipe id="anvilcraft:super_heating_warp_raw_copper_2_copper_ingot"/>