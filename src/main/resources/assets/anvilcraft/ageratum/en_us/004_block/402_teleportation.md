---
navigation:
  title: "§5Celestial Forging Anvil: Teleportation"
  icon: "anvilcraft:black_hole"
items:
  - anvilcraft:celestial_forging_anvil_portal
---

# Unlimited Transmission

1. First, forge several [Identical Celestial Bodies](401_celestial_forging_anvil.md#identical-celestial-bodies) with *Wormhole*
2. Install [Wormhole Stabilizers](402_mega_structure.md#wormhole-stabilizer) on these *black holes* to connect the <ref item="anvilcraft:celestial_forging_anvil"/>s they belong to
3. Then, by placing interfaces in the same positions around the <ref item="anvilcraft:celestial_forging_anvil"/>s, interfaces in the same position share storage, achieving wireless transmission
4. You can also place <ref item="anvilcraft:celestial_forging_anvil_portal"/> to teleport creatures. However, within the same group of *Identical Celestial Bodies*, a maximum of two portals can be placed (but you can place one group on each of the four sides for one-to-one mapping)

<recipe id="anvilcraft:celestial_forging_anvil_portal"/>  

<structure id="../../structures/teleportation.nbt"/>

> Different colors represent different groups

<structure id="../../structures/teleportation_2.nbt"/>

# Chunk Loading

- Connected <ref item="anvilcraft:celestial_forging_anvil"/> are all loaded as long as one of them is loaded
- Each <ref item="anvilcraft:celestial_forging_anvil"/> force loads a 3x3 chunk area centered on its bottom center block
