---
navigation:
  title: "Basic Block Processing"
  icon: "minecraft:stone"
---

# Anvil: Block Processing

Letting <ref item="minecraft:anvil"/> fall onto different blocks can trigger different effects. The following pages of this entry will introduce them in order:

<warning>
<ref item="minecraft:anvil"/> has a chance to be damaged when falling from a height of 2 or more blocks
</warning>

<tip>
Before reading this page, familiarize yourself with <ref item="anvilcraft:magnet_block"/> to make processing easier
</tip>

# Block + Stonecutter: Block Destruction

<structure id="../../structures/break.snbt"/>

- Can destroy blocks that cannot be destroyed by ordinary TNT explosions, such as <ref item="minecraft:obsidian"/>, but a normal anvil will always lose one durability level.
- To prevent the anvil from falling onto the stonecutter and becoming an item after the block is destroyed, you need to control the timing of the <ref item="anvilcraft:magnet_block"/> to retrieve the anvil.
- This is an implementation of [Anvil Mining](../001_feature/000_anvil_destroy.md)

# Single Block Processing: Block Crushing

<structure id="../../structures/block_crush.snbt"/>

- <ref item="minecraft:cobblestone"/> -> <ref item="minecraft:gravel"/> -> <ref item="minecraft:sand"/>
- <ref item="minecraft:polished_granite"/> -> <ref item="minecraft:granite"/> -> <ref item="minecraft:red_sand"/>
- Blocks with cracked variants -> Corresponding cracked variant
- ...

# Dual Block Processing: Block Pressing

<structure id="../../structures/press.snbt"/>

- Moss Block + Dirt -> Grass Block
- Leaves + Dirt -> Podzol
- Mushroom Block + Dirt -> Mycelium
- Nether Wart Block + Netherrack -> Crimson Nylium
- Warped Wart Block + Netherrack -> Warped Nylium
- Stone + Stone -> Deepslate
- Basalt + Basalt -> Blackstone
- ...

# Dual Block Processing: Block Smearing

The block above is not consumed; it converts the block below

<structure id="../../structures/smear.snbt"/>

- Moss Block + Cobblestone -> Mossy Cobblestone
- Moss Block + Stone Bricks -> Mossy Stone Bricks
- Honeycomb Block + Any Copper Block -> Corresponding Waxed Copper Block
- ...

# Block + Cauldron: Block Squeezing

Converts the block and generates resources in the cauldron

<structure id="../../structures/squeeze.snbt"/>

- Wet Sponge -> Sponge + Water
- Moss Block -> Moss Carpet + Water
- Magma Block -> Netherrack + Lava
- Snow Block -> Ice + Powder Snow
- Full Beehive -> Empty Beehive + Honey
- ...

<info>
Mod Improvement: When the cauldron is filled with 4 layers of honey, you can extract honey blocks using hoppers and other logistics blocks.
</info>

# Block Procedural Processing

- Accepts various processing methods including but not limited to those above as individual steps, allowing multiple different steps to be performed in sequence, cycled several times
- If the block being processed is destroyed, the processing fails and the dropped items of the original block are returned, but it can be pushed by <ref item="minecraft:piston"/> and <ref item="anvilcraft:sliding_rail"/>

<info>
Taking <ref item="anvilcraft:redstone_computer"/> as an example, it requires sequentially pressing <ref item="anvilcraft:circuit_board"/>, <ref item="anvilcraft:processor"/>, <ref item="anvilcraft:disk"/>
</info>

<recipe id="anvilcraft:procedural_process/redstone_computer_from_procedural"/>
    