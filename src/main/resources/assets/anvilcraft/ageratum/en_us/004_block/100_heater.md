---
navigation:
  title: "§2Heater"
  icon: "anvilcraft:heater"
items:
  - anvilcraft:heater
---

# Heater

<recipe id="anvilcraft:heater"/>

## Function

- Heats the [heatable block] above it
- Forms a structure with <ref item="minecraft:cauldron"/> to perform **Super-Heating** operations
- Consumes 16kW of power when working; cannot work when power is insufficient

## Super-Heating

<structure id="../structures/super_heating.snbt"/>

Super-Heating is a processing method that can batch-process materials in a cauldron, with the following functions

- Processes **furnace recipes** and **blast furnace recipes**

<warning>
Cannot cook food. For food processing, see [Item Processing: Cooking](../007_struct/000_item_processing.md)
</warning>

- Processes exclusive recipes
- Doubles ore smelting output

<recipe id="anvilcraft:super_heating_warp_raw_copper_2_copper_ingot"/>