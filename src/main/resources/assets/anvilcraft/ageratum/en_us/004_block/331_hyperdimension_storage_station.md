---
navigation:
  title: "§5Hyperdimensional Storage Station"
  icon: "anvilcraft:hyperdimension_storage_station"
items:
  - anvilcraft:hyperdimension_storage_station
  - anvilcraft:hyperdimension_terminal
  - anvilcraft:hyperdimension_uploader
---

# <ref item="anvilcraft:hyperdimension_storage_station"/>

## Crafting

Smash 1 <ref item="anvilcraft:singularity_crystal"/> and 16 <ref item="anvilcraft:hypercube"/>s into a <ref item="anvilcraft:shulker_container"/> from above to upgrade it.

## Function

- Holds **unlimited** items.
- Breaking it preserves its contents.
- It cannot be automatically inserted into or extracted from by blocks such as <ref item="minecraft:hopper"/>. Use a <ref item="anvilcraft:storage_port"/> instead.

# <ref item="anvilcraft:hyperdimension_terminal"/>

<recipe id="anvilcraft:hyperdimension_terminal"/>

1. Right-click to bind it to a <ref item="anvilcraft:hyperdimension_storage_station"/>. The binding cannot be changed or removed.
2. Right-click to open the bound <ref item="anvilcraft:hyperdimension_storage_station"/>'s GUI, even if that storage station has been broken in the three-dimensional world.

<tip>
Multiple players can bind to the same storage station. Breaking it after binding prevents others from binding to it.
</tip>

# Remote Input

<item id="anvilcraft:hyperdimension_uploader"/>

## Crafting

Place a <ref item="anvilcraft:singularity_crystal"/>, then hold a <ref item="anvilcraft:hyperdimension_terminal"/> bound to a <ref item="anvilcraft:hyperdimension_storage_station"/> and right-click the crystal. It becomes an <ref item="anvilcraft:hyperdimension_uploader"/> bound to that storage station.

## Function

- Has 16 slots and can only hold 16 stacks of the same item. It supports input and output through blocks such as <ref item="minecraft:hopper"/>.
- All contained items attempt to enter the bound <ref item="anvilcraft:hyperdimension_storage_station"/>.

<warning>
It does not load chunks.
</warning>
