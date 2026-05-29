---
navigation:
  title: "§5钚"
  icon: "anvilcraft:plutonium_ingot"
items:
  - anvilcraft:plutonium_block
  - anvilcraft:plutonium_ingot
  - anvilcraft:plutonium_nugget
---

# 钚

<row halign="center">
<item id="anvilcraft:plutonium_block"/>
<item id="anvilcraft:plutonium_ingot"/>
<item id="anvilcraft:plutonium_nugget"/>
</row>

# 获得

- 通过<ref item="anvilcraft:neutron_irradiator"/>生产
- 过于活跃而没有粗矿形式，无法通过[矿物涌泉](../007_struct/130_mineral_fountain.md)量产

<row halign="center">
<recipe id="anvilcraft:neutron_irradiation/plutonium_nugget"/>
<recipe id="anvilcraft:time_warp/raw_uranium_from_plutonium_block"/>
</row>

---

# 用途

## 发电

- 每个<ref item="anvilcraft:plutonium_block"/>为<ref item="anvilcraft:heat_collector"/>提供 8kW 的发电量
- 时移<ref item="anvilcraft:plutonium_block"/>会在一瞬间爆发出通常需要数万年才能释放的能量,
  将与锅水平相邻的[可加热方块](../001_feature/101_heated_block.md#可加热方块)加热为<color=#ee7744>白炽</color>并持续10min，合计 1024kW
- 通过铁砧撞击<ref item="anvilcraft:plutonium_block"/>，加热至多16个<ref item="anvilcraft:overheated_ember_metal_block"/>并持续60s，合计
  16384kW."

<row halign="center">
<recipe id="anvilcraft:time_warp/raw_uranium_from_plutonium_block"/>
<recipe id="anvilcraft:anvil_collision/anvil_tier_2_and_plutonium_block_256"/>
</row>

# 特性

- 核辐射：携带18组任意钚物品会受到凋零效果