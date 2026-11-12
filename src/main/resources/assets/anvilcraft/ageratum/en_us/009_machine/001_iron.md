---
navigation:
  title: "Resource: Iron Farm"
  icon: "minecraft:iron_ingot"
---

# Iron Farm

<ref item="minecraft:iron_ingot"/> is very important, so you need to build an iron farm.

Utilize [Anvil Looting](../001_feature/000_anvil_loot.md) combined with the [Dispenser Iron Golem Repair](../001_feature/000_dispenser.md) feature to automatically produce large amounts of <ref item="minecraft:iron_ingot"/>

1. A <ref item="minecraft:anvil"/> dropped from a sufficient height onto an Iron Golem will cause it to drop <ref item="minecraft:iron_ingot"/>
2. Use a <ref item="minecraft:dispenser"/> to use some of the iron to repair the injured Iron Golem
3. Obtain infinite <ref item="minecraft:iron_ingot"/> from a single Iron Golem

<structure id="../../structures/machine/iron.nbt"/>

<warning>
Use only one anvil (at most two, with their drops staggered).
</warning>

After building the machine, add one Iron Golem as shown below:

<structure id="../../structures/machine/iron_golem_placement.nbt"/>

1. The Iron Golem will stay in place after spawning, but be careful not to bump into it.
2. Press the button on the piston to push the block down, then remove the piston and replace it with a block.
3. Hold Shift and place a button while aiming at the hopper's top face. It will attach to the side of the block, completing the setup.

- This iron farm uses **solid** <ref item="minecraft:glass"/> pillars to push items upward. Chutes inside the pillars collect iron and poppies.
- Place at least three chutes inside the pillar to collect all the items and keep the machine from jamming.

<info>
- All <ref item="minecraft:glass"/> can be replaced with any full block.
- All <ref item="minecraft:oak_planks"/> can be replaced with any full opaque block.
- Crates can be replaced with barrels, but not chests.
</info>

<warning>
The structure at the top of the iron farm must match the handbook, or anvils may be lost.
</warning>

## Late-Game Iron Farm

<structure id="../../structures/machine/iron_advanced.nbt"/>

The late-game iron farm uses neutron irradiation to rapidly multiply iron blocks, producing 270 iron blocks per second. It may not match the output of an excellent Iron Golem farm, but it has major advantages in space requirements and performance.

<tip>
Click a pulse generator in the structure to view its configuration, or export the structure and paste it with a Building Rod.
</tip>
