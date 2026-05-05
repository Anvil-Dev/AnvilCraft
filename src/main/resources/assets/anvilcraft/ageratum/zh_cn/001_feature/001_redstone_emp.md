---
navigation:
  title: "红石EMP"
  icon: "minecraft:redstone_block"
  parent: anvilcraft_guideme:feature.md
---

# 红石EMP

<row halign="center">
<item id="minecraft:redstone_block"/>
<item id="minecraft:redstone_torch"/>
</row>

# 特性

被铁砧砸中的<translate key="block.minecraft.redstone_block"/>，
会让同水平的，一定距离内的<translate key="item.minecraft.redstone_torch"/>熄灭1gt

<GameScene zoom="2">
    <structure id="../ac_assets/structure/redstone_emp.snbt"/>
</GameScene>

- 紧贴红石块的铁活版门阻止这个方向的EMP
- 距离r与铁砧下落高度h的关系：r = 6h， r的最大值为24