---
navigation:
  title: "Crafting Spawners"
  icon: "minecraft:spawner"
items:
  - anvilcraft:resentful_amber_block
---

# Crafting Spawners

<row halign="center">
<item id="anvilcraft:resin_block"/>
<item id="anvilcraft:mob_amber_block"/>
<item id="anvilcraft:resentful_amber_block"/>
<item id="anvilcraft:cursed_gold_block"/>
<item id="minecraft:spawner"/>
</row>

## Resentful Amber Block

### Resentment Value

- Only <ref item="anvilcraft:resin_block"/> that has captured a monster has a *Resentment Value* greater than 0
- Resentment Value = [0%-10% random] + [Number of negative effects * 5%] + [1% for every 5% HP lost from max HP]

- <ref item="anvilcraft:resin_block"/> can be [Time Warped](../004_block/200_corrupted_beacon.md) into <ref item="anvilcraft:amber_block"/>
- If <ref item="anvilcraft:resin_block"/> contains a **non-passive mob**, it has a [*Resentment Value*] chance to produce <ref item="anvilcraft:resentful_amber_block"/>
- Using <ref item="anvilcraft:resentful_amber_block"/> and <ref item="anvilcraft:cursed_gold_block"/>, you can produce the corresponding <ref item="minecraft:spawner"/>

## Creating Spawners


<structure id="../../structures/muti_spawner.snbt"/>

<info>
Crafting <ref item="minecraft:spawner"/> only accepts [Multiblock Conversion](../004_block/210_giant_anvil.md#function), not [Multiblock Crafting](../004_block/210_giant_anvil.md#function)!
</info>

<warning>
If you do not have a means of moving <ref item="minecraft:spawner"/>, plan the location in advance
</warning>

<tip>
Recommended to use in conjunction with [Forced Mob Spawning](../001_feature/001_spawner.md)
</tip>
