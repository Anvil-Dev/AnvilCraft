---
navigation:
  title: "§2Thermal System"
  icon: "anvilcraft:redhot_tungsten_block"
items:
  - anvilcraft:heated_tungsten_block
  - anvilcraft:redhot_tungsten_block
  - anvilcraft:glowing_tungsten_block
  - anvilcraft:incandescent_tungsten_block
  - anvilcraft:heated_netherite_block
  - anvilcraft:redhot_netherite_block
  - anvilcraft:glowing_netherite_block
  - anvilcraft:incandescent_netherite_block
---

# Thermal System

<row halign="center">
<item id="anvilcraft:tungsten_block"/>
<item id="anvilcraft:heated_tungsten_block"/>
<item id="anvilcraft:redhot_tungsten_block"/>
<item id="anvilcraft:glowing_tungsten_block"/>
<item id="anvilcraft:incandescent_tungsten_block"/>
</row>

---

<row halign="center">
<item id="minecraft:netherite_block"/>
<item id="anvilcraft:heated_netherite_block"/>
<item id="anvilcraft:redhot_netherite_block"/>
<item id="anvilcraft:glowing_netherite_block"/>
<item id="anvilcraft:incandescent_netherite_block"/>
</row>

# Heatable Blocks

- Definition: Blocks that have several **temperature levels** and a **duration**

## Temperature Levels

- An <color=#cc44cc>abstract concept</color> reflecting a block's temperature
- Temperature levels can be raised and maintained via external heating
- <ref item="minecraft:netherite_block"/> and <ref item="anvilcraft:tungsten_block"/> have 5 temperature levels:
  - <color=#666666>Normal</color>
  - <color=#661111>Heated</color>
  - <color=#aa2222>Redhot</color>
  - <color=#cc5533>Glowing</color>
  - <color=#ee7744>Incandescent</color>

## Duration

- An <color=#cc44cc>abstract concept</color> reflecting how long before the block's **temperature level** drops due to heat dissipation
- Automatically decreases; if it reaches zero, the **temperature level** drops and **duration** resets to 10s
- Increases when receiving external heating; maximum: 20min
- <ref item="minecraft:comparator"/> can detect the duration and output a redstone signal

# High-Temperature Blocks

- Definition: Heatable blocks that are not at the <color=#666666>Normal</color> level

## Features

- Can provide heat to <ref item="anvilcraft:heat_collector"/> for power generation
- When mined, if the tool does not have *Silk Touch/Smelting* enchantment, the **temperature level** drops by one
- When placed, the **temperature level** drops by one
- Blocks at <color=#aa2222>Redhot</color> and above will burn mobs standing on them
- Blocks at <color=#aa2222>Redhot</color> and above periodically evaporate water within a certain range, similar to sponges

# Heating and Heating Capacity

- Heating Capacity: Determines which **temperature level** the block will ultimately be heated to
- Different heating methods have different **heating capacities** (determining which temperature level the block will be heated to)
- When heating:
  - Temperature Level < Heating Capacity: the block rises to the corresponding **temperature level**
  - Temperature Level = Heating Capacity: **duration** increases
  - Temperature Level > Heating Capacity: the block is unaffected

# Heating Methods

|                     Heating Method                      |                Heating Capacity                 | Duration Increase (s) |
|:---------------------------------------------:|:-----------------------------------:|:--------|
|      <ref item="anvilcraft:heater"/>      | <color=#661111>Heated</color> | 0.1     |
| <ref item="anvilcraft:mineral_fountain"/> | <color=#aa2222>Redhot</color> | 1       |
|    <ref item="anvilcraft:heliostats"/>    |                 Variable                  | 4       |
|    <ref item="anvilcraft:ruby_laser"/>    |                 Variable                  | 0.1     |
|       [Plasma Jets](../007_struct/201_plasma_jets.md)       | <color=#cc5533>Glowing</color> | 0.1 / 1 |
