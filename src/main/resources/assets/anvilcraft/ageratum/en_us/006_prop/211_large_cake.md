---
navigation:
  title: "§6Large Cake"
  icon: "anvilcraft:large_cake"
items:
  - anvilcraft:large_cake
  - anvilcraft:cake_base_block
  - anvilcraft:cream_block
  - anvilcraft:berry_cream_block
  - anvilcraft:chocolate_cream_block
  - anvilcraft:cake_block
  - anvilcraft:berry_cake_block
  - anvilcraft:chocolate_cake_block
---

# Large Cake

<row halign="center">
<item id="anvilcraft:cake_base_block"/>
<item id="anvilcraft:cream_block"/>
<item id="anvilcraft:berry_cream_block"/>
<item id="anvilcraft:chocolate_cream_block"/>
<item id="anvilcraft:cake_block"/>
<item id="anvilcraft:berry_cake_block"/>
<item id="anvilcraft:chocolate_cake_block"/>
<item id="anvilcraft:large_cake"/>
</row>

<color=#cccc88>The Large Cake is a big lie!</color>

# Eating

|                                       Block                                       | Hunger | Saturation |
|:------------------------------------------------------------------------------:|:---:|:---:|
|    <ref item="anvilcraft:cake_base_block"/>    |  5  |  4  |
|      <ref item="anvilcraft:cream_block"/>      |  5  |  2  |
|   <ref item="anvilcraft:berry_cream_block"/>   |  8  | 3.2 |
| <ref item="anvilcraft:chocolate_cream_block"/> | 12  | 4.8 |
|      <ref item="anvilcraft:cake_block"/>       | 10  |  6  |
|   <ref item="anvilcraft:berry_cake_block"/>    | 14  | 8.4 |
| <ref item="anvilcraft:chocolate_cake_block"/>  | 20  | 12  |
|      <ref item="anvilcraft:large_cake"/>       | 15  | 12  |

<info>
Use a shovel to accelerate mining the ingredient blocks
</info>

<info>
Right-click with a shovel to eat the entire block
</info>

# Production

<recipe id="anvilcraft:cooking/cake_base_block"/>
<row halign="center">
<recipe id="anvilcraft:item_compress/cream_block"/>
<recipe id="anvilcraft:block_compress/cake_block"/>
</row>
<row halign="center">
<recipe id="anvilcraft:item_compress/berry_cream_block"/>
<recipe id="anvilcraft:block_compress/berry_cake_block"/>
</row>
<row halign="center">
<recipe id="anvilcraft:item_compress/chocolate_cream_block"/>
<recipe id="anvilcraft:block_compress/chocolate_cake_block"/>
</row>

<structure id="../structures/large_cake.snbt"/>
- Made using the [Multiblock Transformation](../004_block/210_giant_anvil.md#功能) recipe

<tip>
Try automating the Large Cake and become an automation "cake" master
</tip>
