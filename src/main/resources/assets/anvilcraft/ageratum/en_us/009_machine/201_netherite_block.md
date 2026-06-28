---
navigation:
  title: "Processing: Netherite"
  icon: "minecraft:netherite_block"
---

# Processing: Netherite

The following two machines implement [Mass Production of Netherite](../008_recipe/210_netherite_ingot.md)

## <ref item="minecraft:ancient_debris"/> Machine

<structure id="../../structures/machine/ancient_debris.nbt"/>

- Set the <ref item="anvilcraft:pulse_generator"/> to (Loop Mode | 15gt | 0gt)
- All <ref item="minecraft:smooth_stone"/> can be replaced with any full opaque block
- Configure the <ref item="anvilcraft:chute"/> that outputs <ref item="minecraft:netherite_scrap"/> to output only one at a time (use scroll wheel to adjust quantity)

![chute](../../textures/netherite_machine.png)

## <ref item="minecraft:netherite_block"/> Machine

<structure id="../../structures/machine/netherite_block.nbt"/>

- Fill both barrels with sufficient <ref item="minecraft:gold_block"/> and <ref item="minecraft:ancient_debris"/>
- Set the main loop <ref item="anvilcraft:pulse_generator"/> (controlled by <ref item="minecraft:lever"/>) to (Loop Mode | 15gt | 0gt)
- Set the <ref item="anvilcraft:pulse_generator"/> for the <ref item="minecraft:gold_block"/> side to (Rising Edge Mode | 1gt | 50gt)
- Set the <ref item="anvilcraft:pulse_generator"/> for the <ref item="minecraft:ancient_debris"/> side to (Rising Edge Mode | 22gt | 30gt)
- Set the <ref item="anvilcraft:pulse_generator"/> for the <ref item="minecraft:piston"/> side to (Rising Edge Mode | 8"90 | 0gt)
- All <ref item="minecraft:smooth_stone"/> can be replaced with any full opaque block
