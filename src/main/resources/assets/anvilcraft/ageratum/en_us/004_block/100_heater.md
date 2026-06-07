---
navigation:
  title: "§2Heater"
  icon: "anvilcraft:heater"
items:
  - anvilcraft:heater
  - anvilcraft:burning_heater
---
<ref item="anvilcraft:burning_heater"/>

# Heater

- Heats the [heatable block](../001_feature/101_heated_block.md) above it
- Forms a structure with <ref item="minecraft:cauldron"/> to perform **Super-Heating** operations

---

## Burning Heater

<recipe id="anvilcraft:block_crush/burning_heater"/>

- Add fuel to increase burn time, up to 1200s
- When burn time is >= 240 seconds, has enough temperature to work
- Each batch of **Super-Heating** operations consumes 240s of burn time

---

## Electric Heater

<recipe id="anvilcraft:heater"/>

- Continuously consumes 16kW of power; cannot work when power is insufficient

# Super-Heating

<structure id="../structures/super_heating.snbt"/>

Super-Heating is a processing method that can batch-process materials in a cauldron

1. Processes **furnace recipes** and **blast furnace recipes**
2. Processes exclusive recipes
3. Doubles ore smelting output

<warning>
Cannot cook food. For food processing, see [Item Processing: Cooking](../007_struct/000_item_processing.md#Cooking)
</warning>

<recipe id="anvilcraft:super_heating_warp_raw_copper_2_copper_ingot"/>
