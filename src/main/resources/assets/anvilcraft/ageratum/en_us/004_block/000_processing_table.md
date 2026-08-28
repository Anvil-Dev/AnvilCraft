---
navigation:
  title: "Processing Table"
  icon: "anvilcraft:stamping_platform"
items:
  - anvilcraft:stamping_platform
  - anvilcraft:crushing_table
  - anvilcraft:sifting_table
  - anvilcraft:unpacking_table
---

# Processing Table

<recipe id="anvilcraft:stamping_platform"/>

- Can store input materials, but does not store output products
- Hold an item and right-click the top surface of a processing table to place it directly
- Can directly absorb dropped items from above
- Let an <ref item="minecraft:anvil"/> fall from above to [process](../007_struct/000_anvil_processing.md) the item on the table

# Refitting

Using the following items on the sides of a <ref item="anvilcraft:stamping_platform"/> or any processing table will switch it to the corresponding processing table:

- <ref item="minecraft:grindstone"/>: <ref item="anvilcraft:crushing_table"/>
- <ref item="minecraft:scaffolding"/>: <ref item="anvilcraft:sifting_table"/>
- <ref item="minecraft:iron_trapdoor"/>: <ref item="anvilcraft:unpacking_table"/>

Switching tables consumes the refit material in hand and returns the previous refit material. Hold an <ref item="anvilcraft:anvil_hammer"/> and right-click the side of any processing table to restore it to a <ref item="anvilcraft:stamping_platform"/> and return the refit material.