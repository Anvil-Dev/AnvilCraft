---
navigation:
  title: "§6巨型蛋糕"
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

# 巨型蛋糕

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

<color=#cccc88>巨型蛋糕是个弥天大谎!</color>

# 食用

|                                       方块                                       | 饱食度 | 饱和度 |
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
原料方块使用铲子加速挖掘
</info>

<info>
使用铲子右键，将其整块吃掉
</info>

# 制造

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
- 使用[多方块转化](../004_block/210_giant_anvil.md#功能)配方制作

<tip>
来试试巨型蛋糕自动化，成为自动化“糕”手
</tip>