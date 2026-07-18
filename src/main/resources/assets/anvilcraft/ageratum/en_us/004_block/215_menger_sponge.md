---
navigation:
  title: "§6Menger Sponge"
  icon: "anvilcraft:menger_sponge"
items:
  - anvilcraft:menger_sponge
---

# Menger Sponge

<item id="anvilcraft:menger_sponge"/>

A sponge with infinite surface area

## Acquisition

- Obtained through [multi-block crafting](210_giant_anvil.md#function)

<tip>
Recommended: [mass-produce sponges](../008_recipe/002_sponge_gemmule.md)
</tip>

## Function

- As a block, when touching fluids, it absorbs and destroys any fluid within a 6-block radius, and never saturates
- As an item, it can empty fluid from cauldrons (automatable via dispenser)

## Tank Amplification

- When <ref item="anvilcraft:fluid_tank"/> is at the center of a 3x3x3 <ref item="anvilcraft:menger_sponge"/> structure, its capacity becomes 12800B. Once full, it accepts and supplies fluid infinitely; breaking the structure clears that state and restores its 16B capacity

- A <ref item="anvilcraft:large_fluid_tank"/> normally stores any number of fluid types with a shared 320B capacity. At the center of a 9x9x9 <ref item="anvilcraft:menger_sponge"/> structure, its total capacity is unlimited; each fluid independently becomes infinite after reaching 12800B

### Structure Requirements

Divide a 3x3x3 (or 9x9x9 or even larger) space evenly into 27 parts. Ensure that the center of each face and the very center of the cube are air, and the other 20 blocks all satisfy the <ref item="anvilcraft:menger_sponge"/> structure.

For a 1x1x1 space, the block must be <ref item="anvilcraft:menger_sponge"/>

<info>
During in-game detection, the air portion in the <ref item="anvilcraft:menger_sponge"/> structure can be other blocks, but cannot be <ref item="anvilcraft:menger_sponge"/>
</info>

<structure id="../../structures/menger_sponge_struct.snbt"/>

