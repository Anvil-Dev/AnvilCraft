---
navigation:
  title: "水泥"
  icon: "anvilcraft:gray_cement_bucket"
items:
  - anvilcraft:white_cement_bucket
  - anvilcraft:light_gray_cement_bucket
  - anvilcraft:gray_cement_bucket
  - anvilcraft:black_cement_bucket
  - anvilcraft:brown_cement_bucket
  - anvilcraft:red_cement_bucket
  - anvilcraft:orange_cement_bucket
  - anvilcraft:yellow_cement_bucket
  - anvilcraft:lime_cement_bucket
  - anvilcraft:green_cement_bucket
  - anvilcraft:cyan_cement_bucket
  - anvilcraft:light_blue_cement_bucket
  - anvilcraft:blue_cement_bucket
  - anvilcraft:purple_cement_bucket
  - anvilcraft:magenta_cement_bucket
  - anvilcraft:pink_cement_bucket
---

# 水泥

<row halign="center">
<item id="anvilcraft:gray_cement_bucket"/>
</row>

# 获取

<recipe id="anvilcraft:solid_liquid/cement_cauldron"/>
- 制作出的水泥默认为灰色，向锅中投入染料并砸击可改变颜色

# 功能

- 可以用来[合成混凝土](../008_recipe/005_concrete.md)

# 转移

![cement.png](../../textures/cement.png)

- 水泥更像现实中的液体，表现为液体源会缓慢地向下方随机位置**转移**
- 接触<ref item="minecraft:slime_block"/>和<ref item="minecraft:honey_block"/>的源头不会**转移**

# 凝固

- 水泥源头无法向下移动时，有概率带着附近流出去 1 格和 2 格的流动水泥一起**凝固**为混凝土
- 接触<ref item="anvilcraft:sugar_block"/>和<ref item="minecraft:honey_block"/>的源头不会**凝固**
