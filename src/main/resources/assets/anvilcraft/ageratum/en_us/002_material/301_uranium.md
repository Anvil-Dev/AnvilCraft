---
navigation:
  title: "§5Uranium"
  icon: "anvilcraft:uranium_ingot"
items:
  - anvilcraft:uranium_block
  - anvilcraft:uranium_ingot
  - anvilcraft:uranium_nugget
  - anvilcraft:raw_uranium
  - anvilcraft:raw_uranium_block
  - anvilcraft:deepslate_uranium_ore
---

# Uranium

<row halign="center">
<item id="anvilcraft:uranium_block"/>
<item id="anvilcraft:uranium_ingot"/>
<item id="anvilcraft:uranium_nugget"/>
<item id="anvilcraft:raw_uranium"/>
<item id="anvilcraft:raw_uranium_block"/>
<item id="anvilcraft:deepslate_uranium_ore"/>
</row>

# Acquisition

- First obtained via [Anvil Collision Crafting](../004_block/215_large_electromagnet.md#铁砧撞击合成)
- Subsequently mass-produced via [Mineral Fountain](../007_struct/130_mineral_fountain.md)

<row halign="center">
<recipe id="anvilcraft:anvil_collision/anvil_tier_1_and_redstone_block_32"/>
<recipe id="anvilcraft:time_warp/raw_uranium_from_uranium_block"/>
</row>

---

# Uses

## Power Generation

- Each <ref item="anvilcraft:uranium_block"/> provides 2kW of power to <ref item="anvilcraft:heat_collector"/>
- Time-warping <ref item="anvilcraft:uranium_block"/> will release in an instant the energy that would normally take tens of thousands of years,
  heating horizontally adjacent [Heatable Blocks](../001_feature/101_heated_block.md#可加热方块) to <color=#ee7744>Incandescent</color> for 5min, totaling 1024kW
- Collide an anvil with <ref item="anvilcraft:uranium_block"/> to heat up to 16 <ref item="anvilcraft:overheated_ember_metal_block"/> for 20s, totaling
  16384kW

<row halign="center">
<recipe id="anvilcraft:time_warp/raw_uranium_from_uranium_block"/>
<recipe id="anvilcraft:anvil_collision/anvil_tier_2_and_uranium_block_256"/>
</row>

# Properties

- Nuclear Radiation: Carrying 18 stacks of any uranium items will apply the Wither effect
