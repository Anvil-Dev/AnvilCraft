---
navigation:
  title: "Basic Item Processing"
  icon: "minecraft:stone"
items:
  - anvilcraft:stamping_platform
  - anvilcraft:crushing_table
---

# Anvil: Item Processing

Letting <ref item="minecraft:anvil"/> fall onto specific blocks can process items on top of or inside those blocks. Different specific blocks have different processing methods, which will be introduced in the following pages of this entry.

<warning>
Note: <ref item="minecraft:anvil"/> has a chance to be damaged when falling from a height of 2 or more blocks
</warning>

<tip>
Before reading this page, familiarize yourself with <ref item="anvilcraft:magnet_block"/> to make processing easier
</tip>

<row halign="center">
<recipe id="anvilcraft:stamping_platform"/>
<recipe id="anvilcraft:shaped_crushing_table_recipe"/>
</row>

# Stamping
When placed on a <ref item="anvilcraft:stamping_platform"/>, the **Item Stamping** operation is performed, and the result drops out from the front of the platform.

<structure id="../structures/item_stamping.snbt"/>

- Iron Ingot -> Iron Pressure Plate
- Gold Ingot -> Gold Pressure Plate
- Snowball -> Snowflake
- Cherry Leaves -> Pink Petals

<info>
Mostly stamps items into corresponding thin pieces
</info>

# Crushing
When placed on a <ref item="anvilcraft:crushing_table"/>, the **Item Crushing** operation is performed. Materials are placed on the table, and results drop out from below.

<structure id="../structures/item_crush.snbt"/>

- Can recycle tools, weapons, and armor to decompose them into raw materials, yielding far more than smelting
- Processing skulls: Skeleton Skull -> 64 Bone Meal; Creeper Head -> 64 Gunpowder
- Compatible with all recipes from [Block Crushing](000_block_processing.md), but with **20%** loss

<recipe id="anvilcraft:item_crush/armor/diamond_boots_2_diamond"/>

# Compacting

When placed on a <ref item="minecraft:cauldron"/>, the **Item Compacting** operation is performed. Both materials and results are in the cauldron.

<structure id="../structures/item_compress.snbt"/>

- If the item has a 2x2 or 3x3 crafting recipe, that recipe will be executed, e.g., 9 Iron Nuggets -> Iron Ingot; 9 Iron Ingots -> Iron Block; 4 String -> Wool
- If an item can be crafted in both 2x2 and 3x3, the 3x3 recipe is executed
- In addition to vanilla recipes, a recipe of 3 Bones -> 1 Bone Block can also be crafted here

# Unpacking

When placed on an <ref item="minecraft:iron_trapdoor"/>, the **Item Unpacking** operation is performed. Materials are placed on the iron trapdoor, and results appear below it.

<structure id="../structures/unpack.snbt"/>

- If the item has a 1->n crafting recipe, that recipe will be executed, e.g., 1 Iron Ingot -> 9 Iron Nuggets
- Additionally, vanilla items that can be unpacked by breaking blocks are also supported, with maximum yields:
  - Melon -> 9 Melon Slices; Glowstone -> 4 Glowstone Dust
- Some building blocks that cannot be unpacked in vanilla can also be unpacked through this method:
  - Block of Quartz -> 4 Quartz; Block of Amethyst -> 4 Amethyst Shards
# Meshing

When placed on <ref item="minecraft:scaffolding"/>, the **Item Meshing** operation is performed. Materials are placed on the scaffolding, and results appear below it.

<structure id="../structures/mesh.snbt"/>

<recipe id="anvilcraft:mesh/gravel"/>

<info>
Meshing produces about half of the material back as byproduct, allowing for recycling
</info>

# Bulging

When placed on a water-filled <ref item="minecraft:cauldron"/>, the **Item Bulging** operation is performed. Both materials and results are in the cauldron, consuming one layer of water.

<structure id="../structures/bulging.snbt"/>

- Copper Blocks -> Oxidized Copper Blocks
- Dirt -> Clay
- Nether Fungus -> Corresponding Wart Block
- Mushroom -> Corresponding Mushroom Block
- Spider Eye -> Fermented Spider Eye
- Coral -> Corresponding Coral Block
- Dried Kelp -> Kelp

# Cooking

When placed on a <ref item="minecraft:cauldron"/> and <ref item="minecraft:campfire"/>, the **Item Cooking** operation is performed. Both materials and results are in the cauldron. Some recipes require water.

<structure id="../structures/cooking.snbt"/>

- Automatically compatible with all smoker recipes and campfire recipes; water is not required in these cases

<recipe id="anvilcraft:smoking_warp_beef_2_cooked_beef"/>

