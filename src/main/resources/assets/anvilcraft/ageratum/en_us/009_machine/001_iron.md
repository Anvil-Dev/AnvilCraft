---
navigation:
  title: "Resource: Iron Farm"
  icon: "minecraft:iron_ingot"
---

# Resource: Iron Farm

<ref item="minecraft:iron_ingot"/> is very important, so you need to build an iron farm.

Utilize [Anvil Looting](../001_feature/000_anvil_loot.md) combined with the [Dispenser Iron Golem Repair](../001_feature/000_dispenser.md) feature to automatically produce large amounts of <ref item="minecraft:iron_ingot"/>

1. A <ref item="minecraft:anvil"/> dropped from a sufficient height onto an Iron Golem will cause it to drop <ref item="minecraft:iron_ingot"/>
2. Use a <ref item="minecraft:dispenser"/> to use some of the iron to repair the injured Iron Golem
3. Obtain infinite <ref item="minecraft:iron_ingot"/> from a single Iron Golem

## Diagram

<structure id="../../structures/machine/iron.snbt"/>

<tip>
Left-click to adjust position; right-click to adjust angle; PgUp/PgDn to adjust display height
</tip>

- Place the Iron Golem at the button's location; the glass panes will restrict its movement
- Set all repeaters to 3 ticks (excessively high frequency will cause surplus iron ingots fired by the dispenser to not be retrieved by the chute in time)
- Set the chute next to the dispenser to filter: Iron Ingot
- Set the chute next to the dropper to filter: Anvil
- Remember to place an anvil after building

<info>
- All <ref item="minecraft:glass"/> can be replaced with any full block
- All <ref item="minecraft:smooth_stone"/> can be replaced with any full opaque block
- All <ref item="minecraft:smooth_stone_slab"/> can be replaced with any slab
</info>
