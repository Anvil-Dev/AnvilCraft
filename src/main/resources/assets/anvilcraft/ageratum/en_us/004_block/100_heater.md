---
navigation:
  title: "§2Heater"
  icon: "anvilcraft:heater"
items:
  - anvilcraft:heater
  - anvilcraft:burning_heater
---

# Heater

> Hot underfoot

- Heats the [heatable block](../001_feature/101_heated_block.md) above it
- Forms a structure with <ref item="minecraft:cauldron"/> to perform **Super-Heating** operations

---

## Burning Heater

<recipe id="anvilcraft:block_crush/burning_heater"/>

- Right-click with fuel in the main hand to insert 1 item, or double-click to insert 1 stack
- When burn time is below 500s, consumes one fuel item to replenish burn time
- When burn time reaches 240s, it has enough temperature to work
- Each batch of **Super-Heating** operations consumes 240s of burn time

---

## Electric Heater

<recipe id="anvilcraft:heater"/>

- Continuously consumes 16kW of power; cannot work when power is insufficient

# Super-Heating

<structure id="../../structures/super_heating.snbt"/>

Super-Heating is a processing method that can batch-process materials in a cauldron

1. Processes **furnace recipes** and **blast furnace recipes**
2. Processes exclusive recipes
3. Doubles ore smelting output

<warning>
Cannot cook food. For food processing, see [Item Processing: Cooking](../007_struct/000_anvil_processing.md#fast-cooking)
</warning>

<recipe id="anvilcraft:super_heating_warp_raw_copper_2_copper_ingot"/>
