---
navigation:
  title: "Redstone EMP"
  icon: "minecraft:redstone_block"
---

# Redstone EMP

<row halign="center">
<item id="minecraft:redstone_block"/>
<item id="minecraft:redstone_torch"/>
<item id="minecraft:iron_trapdoor"/>
</row>

# Feature

When an anvil hits a <ref item="minecraft:redstone_block"/>, it will extinguish <ref item="minecraft:redstone_torch"/> torches within a certain distance at the same level for 1gt.

<structure id="../structures/redstone_emp.snbt"/>

- The relationship between distance r and anvil drop height h: r = 6h, with a maximum r of 24
- An <ref item="minecraft:iron_trapdoor"/> placed flush against the <ref item="minecraft:redstone_block"/> will block the EMP in that direction
