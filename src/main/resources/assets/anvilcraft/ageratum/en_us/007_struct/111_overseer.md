---
navigation:
  title: "Overseer"
  icon: "anvilcraft:overseer"
items:
  - anvilcraft:overseer
---

# Overseer
<recipe id="anvilcraft:overseer"/>

# Function

- When players leave their base, chunk unloading can easily cause machines to break
- <ref item="anvilcraft:overseer"/> can maintain chunk loading within a certain range
- Using it requires building a multiblock structure:
  - The base must be filled with 0-3 layers of 3x3 building blocks made from <ref item="anvilcraft:royal_steel_ingot"/> or <ref item="anvilcraft:frost_metal_block"/>
  - 0-layer base loads the chunk it is located in
  - 1-layer base loads a 3x3 chunk area
  - 2-layer base loads a 5x5 chunk area
  - 3-layer base loads a 7x7 chunk area
  - When the base contains at least 4 waterlogged blocks, it can generate *random ticks*

<structure id="../structures/overseer.snbt"/>
