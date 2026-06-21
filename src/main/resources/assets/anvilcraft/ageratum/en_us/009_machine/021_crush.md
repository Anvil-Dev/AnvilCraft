---
navigation:
  title: "Processing: Automatic Crushing"
  icon: "anvilcraft:quartz_sand"
---

# Processing: Automatic Crushing

- From reading [Mineral Acquisition](../008_recipe/001_basic_minerals.md), you know that *crushing* is a very important processing step
- Blocks in the chest behind the <ref item="anvilcraft:block_placer"/> will be dispensed
- The block is immediately crushed by the <ref item="minecraft:anvil"/>

If you need higher efficiency, you can simply extend it, or design your own more efficient machine.

## Diagram

<structure id="../../structures/machine/crush.snbt"/>

<tip>
Left-click to adjust position; right-click to adjust angle; PgUp/PgDn to adjust display height
</tip>

1. Set the pulse generator to loop mode: emit an 8gt signal every 8gt interval

- All <ref item="minecraft:smooth_stone"/> can be replaced with any full opaque block
- All <ref item="minecraft:smooth_stone_slab"/> can be replaced with any slab
- All <ref item="minecraft:anvil"/> can be replaced with any anvil

<warning>
Cannot process recipes whose crushing result is not a *falling block*, such as End Dust
</warning>
