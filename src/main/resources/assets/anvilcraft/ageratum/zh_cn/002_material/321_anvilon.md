---
navigation:
  title: "§5钚"
  icon: "anvilcraft:confinement_chamber"
items:
  - anvilcraft:confinement_chamber
  - anvilcraft:confined_time_anvilon
  - anvilcraft:confined_space_anvilon
  - anvilcraft:confined_mass_anvilon
  - anvilcraft:confined_energy_anvilon
  - anvilcraft:confined_neutronium_ingot
---

# <ref item="anvilcraft:confinement_chamber"/>

<recipe id="anvilcraft:confinement_chamber"/>

- 可以从[铁砧撞击](../004_block/215_large_electromagnet.md#铁砧撞击合成)中收集四大基本粒子并约束

# 四大基本砧子

<row halign="center">
<item id="anvilcraft:confined_time_anvilon"/>
<item id="anvilcraft:confined_space_anvilon"/>
<item id="anvilcraft:confined_mass_anvilon"/>
<item id="anvilcraft:confined_energy_anvilon"/>
</row>

## 合成

<info>
以不同速度撞击<ref item="anvilcraft:giant_anvil"/>、<ref item="anvilcraft:corrupted_beacon"/>和<ref item="anvilcraft:space_overcompressor"/>，可以得到不同的砧子
</info>

<row halign="center">
<recipe id="anvilcraft:anvil_collision/anvil_tier_0_and_corrupted_beacon_32"/>
<recipe id="anvilcraft:anvil_collision/anvil_tier_0_and_corrupted_beacon_128"/>
</row>

<row halign="center">
<recipe id="anvilcraft:anvil_collision/anvil_tier_0_and_space_overcompressor_32"/>
<recipe id="anvilcraft:anvil_collision/anvil_tier_0_and_space_overcompressor_128"/>
</row>

<row halign="center">
<recipe id="anvilcraft:anvil_collision/anvil_tier_0_and_giant_anvil_32"/>
<recipe id="anvilcraft:anvil_collision/anvil_tier_0_and_giant_anvil_128"/>
</row>

## 砧子辐照

<structure id="../structures/anvilon_irradiator.nbt"/>

<ref item="anvilcraft:neutron_irradiator"/>同一水平面周围3x3区域内的八个格子内存在6个相同的砧子时，变为对应的“砧子辐照器”

# <ref item="anvilcraft:confined_neutronium_ingot"/>

<recipe id="anvilcraft:item_inject/confined_neutronium_ingot"/>