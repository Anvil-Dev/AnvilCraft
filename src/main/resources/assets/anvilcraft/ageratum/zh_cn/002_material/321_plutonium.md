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

- 通过[<translate key="block.anvilcraft.neutron_irradiator"/>](../004_block/320_neutron_irradiator.md)生产
- 过于活跃而没有粗矿形式，无法通过[矿物涌泉](../007_struct/130_mineral_fountain.md)量产

<row halign="center">
<recipe id="anvilcraft:neutron_irradiation/plutonium_nugget"/>
<recipe id="anvilcraft:time_warp/raw_uranium_from_plutonium_block"/>
</row>

---

# 用途

## 发电

- 每个[<translate key="block.anvilcraft.plutonium_block"/>](321_plutonium.md)为[<translate key="block.anvilcraft.heat_collector"/>](../003_power/201_heat_collection.md)提供 8kW 的发电量
- 时移[<translate key="block.anvilcraft.plutonium_block"/>](321_plutonium.md)会在一瞬间爆发出通常需要数万年才能释放的能量,
  将与锅水平相邻的[可加热方块](../001_feature/101_heated_block.md#可加热方块)加热为<color=#ee7744>白炽</color>并持续10min，合计 1024kW
- 通过铁砧撞击[<translate key="block.anvilcraft.plutonium_block"/>](321_plutonium.md)，加热至多16个[<translate key="block.anvilcraft.overheated_ember_metal_block"/>](../001_feature/301_overheated_block.md)并持续60s，合计
  16384kW."

<row halign="center">
<recipe id="anvilcraft:time_warp/raw_uranium_from_plutonium_block"/>
<recipe id="anvilcraft:anvil_collision/anvil_tier_2_and_plutonium_block_256"/>
</row>

# 特性

- 核辐射：携带18组任意钚物品会受到凋零效果