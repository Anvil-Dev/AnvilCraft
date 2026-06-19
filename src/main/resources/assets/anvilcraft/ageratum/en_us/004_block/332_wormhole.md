---
navigation:
  title: "§5Celestial Forging Anvil: Wormhole"
  icon: "anvilcraft:black_hole"
items:
  - anvilcraft:celestial_forging_anvil_portal
---

# Wormhole

- Created from <ref item="anvilcraft:celestial_forging_anvil"/>
- All [Identical Celestial Bodies](331_celestial_forging_anvil.md#identical-celestial-bodies) with *Wormhole* are connected to each other

# Chunk Loading

- Connected <ref item="anvilcraft:celestial_forging_anvil"/> are all loaded as long as one of them is loaded
- Force loads a 3x3 chunk area centered on the bottom center block of the <ref item="anvilcraft:celestial_forging_anvil"/> itself

# Logistics Transport

- For connected <ref item="anvilcraft:celestial_forging_anvil"/>, the same positions around them can only have the same type of [Interface](331_celestial_forging_anvil.md#using-interfaces) placed
- At this point, interfaces at the same position share storage, achieving wireless transmission

# Entity Teleportation

## Through <ref item="anvilcraft:celestial_forging_anvil_portal"/>

<recipe id="anvilcraft:celestial_forging_anvil_portal"/>  

- Within the same group of identical celestial bodies, a maximum of two portals can be placed
- Entering one portal will exit from the other

## Through <ref item="minecraft:ender_pearl"/>

- A thrown <ref item="minecraft:ender_pearl"/> can enter the black hole and then randomly teleport to a connected black hole's position
