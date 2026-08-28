---
navigation:
  title: "§2Ionocraft Backpack"
  icon: "anvilcraft:ionocraft_backpack"
items:
  - anvilcraft:ionocraft
  - anvilcraft:ionocraft_backpack
---

# Ionocraft

<recipe id="anvilcraft:ionocraft"/>

## Functions

- Right-click to place on the ground, spawning an Ionocraft entity
- Within a power grid range, it consumes 16kW to ascend rapidly
- Outside a power grid range, it descends slowly
- Entities can stand on it

# Ionocraft Backpack

<recipe id="anvilcraft:ionocraft_backpack"/>

## Functions

- Can be worn in the chestplate slot
- While equipped, grants creative flight ability and consumes power

<info>
When a curio/accessory mod is installed, it can be placed in the curio slot
</info>

## Charging

### Grid Power Supply

- Has different charging efficiency tiers: 64, 128, 256, 512
- When the grid's remaining power >= 128kW (i.e., 2x the charging power), the charging rate is 64kW, and so on
- If multiple players in the grid are wearing Ionocraft Backpacks, the grid's remaining power is divided by the number of players first before comparing

<info>
In short, as long as the grid is not overloaded, it will absorb as much energy as possible
</info>

### Capacitor Power Supply

- Automatically uses <ref item="anvilcraft:capacitor"/> from the backpack to replenish energy
- Because <ref item="anvilcraft:supercapacitor"/> holds too much charge to use automatically without waste, it is not consumed automatically; charge it manually via [Charging Items](../002_material/101_capacitor.md#charging-items)