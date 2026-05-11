---
navigation:
  title: "§5铀"
  icon: "anvilcraft:uranium_ingot"
items:
  - anvilcraft:uranium_block
  - anvilcraft:uranium_ingot
  - anvilcraft:uranium_nugget
  - anvilcraft:raw_uranium
  - anvilcraft:raw_uranium_block
  - anvilcraft:deepslate_uranium_ore
---

# 铀

<row halign="center">
<item id="anvilcraft:uranium_block"/>
<item id="anvilcraft:uranium_ingot"/>
<item id="anvilcraft:uranium_nugget"/>
<item id="anvilcraft:raw_uranium"/>
<item id="anvilcraft:raw_uranium_block"/>
<item id="anvilcraft:deepslate_uranium_ore"/>
</row>

# 获得

- 首次获得通过[铁砧撞击合成](../004_block/215_large_electromagnet.md#铁砧撞击合成)
- 后续通过[矿物涌泉](../007_struct/130_mineral_fountain.md)量产

<row halign="center">
<recipe id="anvilcraft:anvil_collision/anvil_tier_1_and_redstone_block_32"/>
<recipe id="anvilcraft:time_warp/raw_uranium_from_uranium_block"/>
</row>

---

# 用途

## 发电

- 每个[<translate key="block.anvilcraft.uranium_block"/>](301_uranium.md)为[<translate key="block.anvilcraft.heat_collector"/>](../003_power/201_heat_collection.md)提供 2kW 的发电量
- 时移[<translate key="block.anvilcraft.uranium_block"/>](301_uranium.md)会在一瞬间爆发出通常需要数万年才能释放的能量,
  将与锅水平相邻的[可加热方块](../001_feature/101_heated_block.md#可加热方块)加热为<color=#ee7744>白炽</color>并持续5min，合计 1024kW
- 通过铁砧撞击[<translate key="block.anvilcraft.uranium_block"/>](301_uranium.md)，加热至多16个[<translate key="block.anvilcraft.overheated_ember_metal_block"/>](../001_feature/301_overheated_block.md)并持续20s，合计
  16384kW."

<row halign="center">
<recipe id="anvilcraft:time_warp/raw_uranium_from_uranium_block"/>
<recipe id="anvilcraft:anvil_collision/anvil_tier_2_and_uranium_block_256"/>
</row>

# 特性

- 核辐射：携带18组任意铀物品会受到凋零效果

