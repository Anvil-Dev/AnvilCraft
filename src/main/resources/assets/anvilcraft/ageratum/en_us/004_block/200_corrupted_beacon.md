---
navigation:
  title: "§6Corrupted Beacon"
  icon: "anvilcraft:corrupted_beacon"
items:
  - anvilcraft:corrupted_beacon
---

# Corrupted Beacon

<row halign="center">
<item id="minecraft:beacon"/>
<item id="anvilcraft:corrupted_beacon"/>
</row>

<gradient start="#991155" end="#bbaa55">Releases the sealed Wither power</gradient>

# <ref item="anvilcraft:corrupted_beacon"/>

## Acquisition

1. Fully use <ref item="anvilcraft:cursed_gold_block"/> as the beacon base,
2. Consume <ref item="anvilcraft:cursed_gold_ingot"/> to activate the beacon
3. The beacon has a chance to transform into <ref item="anvilcraft:corrupted_beacon"/> (more base layers = higher conversion chance), and the weather is controlled to become thunderstorm

| Layers |  Chance  | Cursed Gold Blocks in Base |   Equivalent Cursed Gold Ingots    | Expected Attempts to Succeed | Attempts for 95% Confidence |
|:------:|:--------:|:--------------------------:|:----------------------------------:|:----------------------------:|:---------------------------:|
|   1    |    2%    |             9              |          81 = 1 stack + 15         |              50              |             149             |
|   2    |    5%    |             34             |         306 = 4 stacks + 50        |              20              |             59              |
|   3    |   20%    |             83             |        747 = 11 stacks + 43        |              5               |             14              |
|   4    |   100%   |            164             |        1479 = 23 stacks + 4        |              1               |              1              |

> **Automation**: Use an anvil to press cursed gold ingots into the beacon

# Time-Warp Recipes

- <ref item="anvilcraft:corrupted_beacon"/> can only be activated using <ref item="anvilcraft:cursed_gold_block"/> as the base
- Once activated, the following structure enables time-warp operations

<structure id="../../structures/corrupted_beacon.snbt"/>

<info>
Unlike <ref item="minecraft:beacon"/>, blocks above do not prevent <ref item="anvilcraft:corrupted_beacon"/> from activating
</info>

## Main Uses

- [Mass Production of World Base Blocks](../008_recipe/200_world_block.md)
- [Mass Production of Diamond](../008_recipe/201_diamond.md)
- [Mass Production of Heart of the Sea](../008_recipe/205_sea_heart.md)
- [Mass Production of Gems](../008_recipe/204_gem.md)
- [Mass Production of Netherite](../008_recipe/210_netherite_ingot.md)

<info>
All metal blocks can be time-warped into raw ore form, used for <ref item="anvilcraft:mineral_fountain"/>
</info>

<row halign="center">
<recipe id="anvilcraft:time_warp/raw_copper"/>
</row>

<row halign="center">
<recipe id="anvilcraft:time_warp/budding_amethyst"/>
<recipe id="anvilcraft:time_warp/wither_skeleton_skull"/>
</row>

<row halign="center">
<recipe id="anvilcraft:time_warp/wither_rose"/>
<recipe id="anvilcraft:time_warp/crying_obsidian"/>
</row>

# Mob Interactions

- The beacon beam inflicts the **Wither** effect on mobs
- Converts specific mobs:

|     Original Mob      |         Conversion Result         |
|:---------------------:|:---------------------------------:|
|          Pig          |             Hoglin                |
|          Cow          |             Ravager               |
|        Guardian       |         Elder Guardian            |
|        Piglin         |         Piglin Brute              |
|        Villager       | 30% Pillager, 60% Vindicator, 10% Evoker |
|         Allay         |              Vex                  |
|          Bat          |             Phantom               |
|         Horse         |    10% Zombie Horse, 90% Skeleton Horse    |
|       Silverfish      |            Endermite              |
|       Skeleton        |    20% Wither Skeleton, 80% Stray    |
| **Villager-summoned** Iron Golem |           Warden           |