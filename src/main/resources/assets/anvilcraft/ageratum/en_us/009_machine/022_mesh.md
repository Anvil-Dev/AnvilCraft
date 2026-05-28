---
navigation:
  title: "Processing: Automatic Meshing"
  icon: "minecraft:scaffolding"
---

# Processing: Automatic Meshing

- From reading [Mineral Acquisition](../008_recipe/001_basic_minerals.md), you know that *meshing* can sift quartz from quartz sand produced by crushing diorite
- However, only a portion of the material is used each *meshing* operation, and manually repeating the process is time-consuming and laborious
- Below is an example machine for automatic meshing

## Diagram

<structure id="../structures/machine/mesh.snbt"/>

<tip>
Left-click to adjust position; right-click to adjust angle; PgUp/PgDn to adjust display height
</tip>

1. Set the pulse generator to loop mode: emit a 3gt signal every 5gt interval; drives the piston operation
2. Set the magnetic chute on the chest to filter: meshing material

- All <ref item="minecraft:smooth_stone"/> can be replaced with any full opaque block
- All <ref item="minecraft:smooth_stone_slab"/> can be replaced with any slab
- All <ref item="minecraft:anvil"/> can be replaced with any anvil
