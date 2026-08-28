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

<structure id="../../structures/machine/iron.nbt"/>

<tip>
Left-click to adjust position; right-click to adjust angle; PgUp/PgDn to adjust display height
</tip>

- Place the Iron Golem at the *button*'s location; the *glass panes* will restrict its movement
- Set all repeaters to 3 ticks (excessively high frequency will cause surplus iron ingots fired by the <ref item="minecraft:dispenser"/> to not be retrieved by the chute in time)
- Set the <ref item="anvilcraft:magnetic_chute"/> next to the <ref item="minecraft:dispenser"/> to filter: Iron Ingot
- Set the <ref item="anvilcraft:magnetic_chute"/> next to the dropper to filter: Anvil
- Use solid <ref item="minecraft:glass"/> pillars to push items upward
- Remember to place an anvil after building

<info>
- All _reinforced concrete_ does not need to be built; it is only for counting blocks
- All <ref item="minecraft:glass"/> can be replaced with any full opaque block
- All <ref item="minecraft:smooth_stone"/> can be replaced with any full block
- All <ref item="minecraft:smooth_stone_slab"/> can be replaced with any slab
- All <ref item="minecraft:anvil"/> can be replaced with any anvil, except <ref item="minecraft:damaged_anvil"/>, as it will disappear directly without dropping anything
</info>

<warning>
The top <ref item="anvilcraft:magnetic_chute"/> must not be replaced, otherwise the *anvil* may fly out from the side
</warning>
