---
navigation:
  title: "Anvil Processing"
  icon: "minecraft:anvil"
---

## Table of Contents

- [Anvil: Item Processing](#anvil-item-processing)
  - [Stamping](#stamping)
  - [Crushing](#crushing)
  - [Compacting](#compacting)
  - [Unpacking](#unpacking)
  - [Meshing](#meshing)
  - [Solid-Liquid Reaction](#solid-liquid-reaction)
  - [Fast Cooking](#fast-cooking)
- [Anvil: Block Processing](#anvil-block-processing)
  - [Block + Stonecutter: Block Destruction](#block--stonecutter-block-destruction)
  - [Single Block Processing: Block Crushing](#single-block-processing-block-crushing)
  - [Dual Block Processing: Block Pressing](#dual-block-processing-block-pressing)
  - [Dual Block Processing: Block Smearing](#dual-block-processing-block-smearing)
  - [Block + Cauldron: Block Squeezing](#block--cauldron-block-squeezing)
- [Block Procedural Processing](#block-procedural-processing)

# Anvil: Item Processing

Letting <ref item="minecraft:anvil"/> fall onto specific blocks can process items on top of or inside those blocks. Different specific blocks have different processing methods, which will be introduced in the following sections of this entry.

<warning>
Note: <ref item="minecraft:anvil"/> has a chance to be damaged when falling from a height of 2 or more blocks
</warning>

<tip>
Before reading this section, familiarize yourself with <ref item="anvilcraft:magnet_block"/> to make processing easier
</tip>

## Stamping

When placed on a <ref item="anvilcraft:stamping_platform"/>, the **Item Stamping** operation is performed. Materials are placed on the platform, and results drop out from below.

<structure id="../../structures/item_stamping.snbt"/>

- Iron Ingot -> Iron Pressure Plate
- Gold Ingot -> Gold Pressure Plate
- Snowball -> Snowflake
- Cherry Leaves -> Pink Petals

<info>
Mostly stamps items into corresponding thin pieces
</info>

## Crushing

When placed on a <ref item="anvilcraft:crushing_table"/>, the **Item Crushing** operation is performed. Materials are placed on the table, and results drop out from below.

<structure id="../../structures/item_crush.snbt"/>

- Can recycle tools, weapons, and armor to decompose them into raw materials, yielding far more than smelting
- Processing skulls: Skeleton Skull -> 64 Bone Meal; Creeper Head -> 64 Gunpowder
- Crushes blocks that become **dropped items**, completing all the block crushing recipes in this entry, but with **20%** loss
- Provides [efficient recipes](../008_recipe/001_efficient_recipe.md) for vanilla crafting

<row>
<recipe id="anvilcraft:item_crush/armor/diamond_boots_2_diamond"/>
<recipe id="anvilcraft:item_crush/string"/>
</row>

## Compacting

When placed on a <ref item="minecraft:cauldron"/>, the **Item Compacting** operation is performed. Both materials and results are in the cauldron.

<structure id="../../structures/item_compress.snbt"/>

- If the item has a 2x2 or 3x3 crafting recipe, that recipe will be executed, e.g., 9 Iron Nuggets -> Iron Ingot; 9 Iron Ingots -> Iron Block; 4 String -> Wool
- If an item can be crafted in both 2x2 and 3x3, the 3x3 recipe is executed
- In addition to vanilla recipes, a recipe of 3 Bones -> 1 Bone Block can also be crafted here

## Unpacking

When placed on an <ref item="minecraft:iron_trapdoor"/> or an <ref item="anvilcraft:unpacking_table"/>, the **Item Unpacking** operation is performed. Materials are placed on the table, and results drop out from below.

<structure id="../../structures/unpack.snbt"/>

- If the item has a 1->n crafting recipe, that recipe will be executed, e.g., 1 Iron Ingot -> 9 Iron Nuggets
- Additionally, vanilla items that can be unpacked by breaking blocks are also supported, with maximum yields:
  - Melon -> 9 Melon Slices; Glowstone -> 4 Glowstone Dust
- Some building blocks that cannot be unpacked in vanilla can also be unpacked through this method:
  - Block of Quartz -> 4 Quartz; Block of Amethyst -> 4 Amethyst Shards

## Meshing

When placed on <ref item="minecraft:scaffolding"/> or an <ref item="anvilcraft:sifting_table"/>, the **Item Meshing** operation is performed. Materials are placed on the table, and results drop out from below.

<structure id="../../structures/mesh.snbt"/>

<recipe id="anvilcraft:mesh/gravel"/>

<info>
Meshing produces about half of the material back as byproduct, allowing for recycling
</info>

## Solid-Liquid Reaction

When placed on a water-filled <ref item="minecraft:cauldron"/>, the **Solid-Liquid Reaction** operation is performed. Both materials and results are in the cauldron, consuming one layer of water.

<structure id="../../structures/solid_liquid.snbt"/>

- Copper Blocks -> Oxidized Copper Blocks
- Dirt -> Clay
- Nether Fungus -> Corresponding Wart Block
- Mushroom -> Corresponding Mushroom Block
- Spider Eye -> Fermented Spider Eye
- Coral -> Corresponding Coral Block
- Dried Kelp -> Kelp

## Fast Cooking

When placed on a <ref item="minecraft:cauldron"/> and a <ref item="minecraft:campfire"/>, the **Fast Cooking** operation is performed. Both materials and results are in the cauldron; some recipes require water.

<structure id="../../structures/fast_cooking.snbt"/>

- Automatically compatible with all smoker recipes and campfire recipes; water is not required in these cases

<recipe id="anvilcraft:smoking_warp_beef_2_cooked_beef"/>

# Anvil: Block Processing

Letting <ref item="minecraft:anvil"/> fall onto different blocks can trigger different effects. The following sections of this entry will introduce them in order:

<warning>
<ref item="minecraft:anvil"/> has a chance to be damaged when falling from a height of 2 or more blocks
</warning>

<tip>
Before reading this section, familiarize yourself with <ref item="anvilcraft:magnet_block"/> to make processing easier
</tip>

## Block + Stonecutter: Block Destruction

<structure id="../../structures/break.snbt"/>

- Can destroy blocks that cannot be destroyed by ordinary TNT explosions, such as <ref item="minecraft:obsidian"/>, but a normal anvil will always lose one durability level.
- To prevent the anvil from falling onto the stonecutter and becoming an item after the block is destroyed, you need to control the timing of the <ref item="anvilcraft:magnet_block"/> to retrieve the anvil.
- This is an implementation of [Anvil Mining](../001_feature/000_anvil_destroy.md)

## Single Block Processing: Block Crushing

<structure id="../../structures/block_crush.snbt"/>

- <ref item="minecraft:cobblestone"/> -> <ref item="minecraft:gravel"/> -> <ref item="minecraft:sand"/>
- <ref item="minecraft:polished_granite"/> -> <ref item="minecraft:granite"/> -> <ref item="minecraft:red_sand"/>
- Blocks with cracked variants -> Corresponding cracked variant
- ...

## Dual Block Processing: Block Pressing

<structure id="../../structures/press.snbt"/>

- Moss Block + Dirt -> Grass Block
- Leaves + Dirt -> Podzol
- Mushroom Block + Dirt -> Mycelium
- Nether Wart Block + Netherrack -> Crimson Nylium
- Warped Wart Block + Netherrack -> Warped Nylium
- Stone + Stone -> Deepslate
- Basalt + Basalt -> Blackstone
- ...

## Dual Block Processing: Block Smearing

The block above is not consumed; it converts the block below

<structure id="../../structures/smear.snbt"/>

- Moss Block + Cobblestone -> Mossy Cobblestone
- Moss Block + Stone Bricks -> Mossy Stone Bricks
- Honeycomb Block + Any Copper Block -> Corresponding Waxed Copper Block
- ...

## Block + Cauldron: Block Squeezing

Converts the block and generates resources in the cauldron

<structure id="../../structures/squeeze.snbt"/>

- Wet Sponge -> Sponge + Water
- Moss Block -> Moss Carpet + Water
- Magma Block -> Netherrack + Lava
- Snow Block -> Ice + Powder Snow
- Full Beehive -> Beehive + Honey
- ...

<info>
Mod Improvement: When the cauldron is filled with 4 layers of honey, you can extract honey blocks using hoppers and other logistics blocks.
</info>

# Block Procedural Processing

- Accepts various processing methods including but not limited to those above as individual steps, allowing multiple different steps to be performed in sequence, cycled several times
- If the block being processed is destroyed, the processing fails and the dropped items of the original block are returned, but it can be pushed by <ref item="minecraft:piston"/> and <ref item="anvilcraft:sliding_rail"/> to adjust position

<info>

Taking efficient production of <ref item="minecraft:netherite_block"/> as an example, <ref item="minecraft:ancient_debris"/> needs to be sequentially pressed with <ref item="minecraft:raw_gold_block"/> and <ref item="minecraft:ancient_debris"/>, then [super heating](../004_block/100_heater.md#super-heating); the above steps need to be completed twice

</info>

<recipe id="anvilcraft:procedural_process/netherite_block"/>