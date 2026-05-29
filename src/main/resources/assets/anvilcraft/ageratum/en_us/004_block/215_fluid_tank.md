---
navigation:
  title: "§6Fluid Tank"
  icon: "anvilcraft:fluid_tank"
items:
  - anvilcraft:fluid_tank
  - anvilcraft:large_fluid_tank
---

# <ref item="anvilcraft:fluid_tank"/>

- Can hold 16B of fluid

<recipe id="anvilcraft:fluid_tank"/>

# <ref item="anvilcraft:large_fluid_tank"/>

1. Place 27 <ref item="anvilcraft:fluid_tank"/> in a solid 3x3x3 arrangement and perform [multi-block conversion](210_giant_anvil.md#function) to obtain
2. Place 26 <ref item="anvilcraft:fluid_tank"/> in a hollow 3x3x3 arrangement and perform [multi-block conversion](210_giant_anvil.md#function) to obtain
- Can hold 320B of fluid

# <ref item="anvilcraft:menger_sponge"/> Amplification

## <ref item="anvilcraft:menger_sponge"/> Structure Requirements

Divide a 3x3x3 (or 9x9x9 or even larger) space evenly into 27 parts. Ensure that the center of each face and the very center of the cube are air, and the other 20 blocks all satisfy the <ref item="anvilcraft:menger_sponge"/> structure.

For a 1x1x1 space, the block must be <ref item="anvilcraft:menger_sponge"/>

<info>
During in-game detection, the air portion in the <ref item="anvilcraft:menger_sponge"/> structure can be other blocks, but cannot be <ref item="anvilcraft:menger_sponge"/>
</info>

<structure id="../structures/menger_sponge_struct.snbt"/>

## <ref item="anvilcraft:menger_sponge"/> Amplification for Tanks

- When <ItemLink id="anvilcraft:fluid_tank" /> is at the center of a 3x3x3 <ref item="anvilcraft:menger_sponge"/> structure, its capacity becomes 640B

- When <ItemLink id="anvilcraft:large_fluid_tank" /> is at the center of a 9x9x9 <ref item="anvilcraft:menger_sponge"/> structure, its capacity becomes 12800B. And once the input fluid reaches the upper limit, it thereafter acts as an **Infinite Fluid Tank**, allowing infinite input and output