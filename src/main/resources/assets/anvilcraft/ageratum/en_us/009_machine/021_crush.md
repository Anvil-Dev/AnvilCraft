---
navigation:
  title: "Processing: Automatic Crushing"
  icon: "anvilcraft:quartz_sand"
---

# Crusher

- From reading [Mineral Acquisition](../008_recipe/001_basic_minerals.md), you know that *crushing* is a very important processing step
- Blocks in the chest behind the <ref item="anvilcraft:block_placer"/> will be dispensed
- The block is immediately crushed by the <ref item="minecraft:anvil"/>

If you need higher efficiency, you can simply extend it, or design your own more efficient machine.

<structure id="../../structures/machine/crush.snbt"/>

<warning>
Cannot process recipes whose crushing result is not a *falling block*, such as End Dust
</warning>

## Advanced Crusher

<structure id="../../structures/machine/crush_advanced.nbt"/>

This efficient, automated crusher processes 10 blocks per second. Put items in the upper chest to start crushing; the products go into the lower chest. It starts and stops automatically and can crush a wide variety of blocks.

<tip>
Click a pulse generator in the structure to view its configuration, or export the structure and paste it with a Building Rod.
</tip>
