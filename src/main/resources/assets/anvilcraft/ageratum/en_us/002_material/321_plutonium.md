---
navigation:
  title: "§5Plutonium"
  icon: "anvilcraft:plutonium_ingot"
items:
  - anvilcraft:plutonium_block
  - anvilcraft:plutonium_ingot
  - anvilcraft:plutonium_nugget
---

# Plutonium

<row halign="center">
<item id="anvilcraft:plutonium_block"/>
<item id="anvilcraft:plutonium_ingot"/>
<item id="anvilcraft:plutonium_nugget"/>
</row>

# Acquisition

- Produced via <ref item="anvilcraft:neutron_irradiator"/>
- Too active to exist in raw ore form; cannot be mass-produced via [Mineral Fountain](../007_struct/130_mineral_fountain.md)

<row halign="center">
<recipe id="anvilcraft:neutron_irradiation/plutonium_nugget"/>
<recipe id="anvilcraft:time_warp/raw_uranium_from_plutonium_block"/>
</row>

---

# Power Generation

- Each <ref item="anvilcraft:plutonium_block"/> provides 8kW of power to <ref item="anvilcraft:heat_collector"/>
- Time-warping <ref item="anvilcraft:plutonium_block"/> will release in an instant the energy that would normally take tens of thousands of years, heating horizontally adjacent [Heatable Blocks](../001_feature/101_heated_block.md#heatable-blocks) to <color=#ee7744>Incandescent</color> for 10min, totaling 1024kW
- Collide an anvil with <ref item="anvilcraft:plutonium_block"/> to heat up to 16 <ref item="anvilcraft:overheated_ember_metal_block"/> for 60s, totaling 16384kW

<row halign="center">
<recipe id="anvilcraft:time_warp/raw_uranium_from_plutonium_block"/>
<recipe id="anvilcraft:anvil_collision/anvil_tier_2_and_plutonium_block_256"/>
</row>

# Nuclear Radiation

Carrying 18 stacks of any plutonium or uranium items will apply the **Wither** effect


# Meltdown

- <ref item="anvilcraft:plutonium_block"/> is a *radioactive block*
- When all six sides are free of water or waterlogged blocks, <ref item="anvilcraft:plutonium_block"/> melts down into <ref item="minecraft:magma_block"/>
- If any side touches water or a waterlogged block, <ref item="anvilcraft:plutonium_block"/> will not melt down
