---
navigation:
  title: "§6Giant Anvil"
  icon: "anvilcraft:giant_anvil"
items:
  - anvilcraft:giant_anvil
  - anvilcraft:transparent_crafting_table
---

# Giant Anvil

<row halign="center">
<item id="anvilcraft:giant_anvil"/>
<item id="anvilcraft:transparent_crafting_table"/>
</row>

# First Acquisition

- Right-click a zombie while holding <ref item="minecraft:anvil"/> to place the anvil in its hands
- Have the anvil-holding zombie irradiated by <ref item="anvilcraft:corrupted_beacon"/>
- The zombie has a [number of held anvils * 5%] chance to become a **Giant Zombie** holding <ref item="anvilcraft:giant_anvil"/>
- Kill the **Giant Zombie** to obtain <ref item="anvilcraft:giant_anvil"/>

<tip>
You can use <ref item="anvilcraft:resin_block"/> to capture zombies
</tip>

<warning>
**Giant Zombie** has been given AI by this mod and is extremely powerful. Make sure to surround it with blocks to trap it beforehand, or defeat it as a PVE expert
</warning>

# Convenient Crafting

After obtaining the first <ref item="anvilcraft:giant_anvil"/>,
you can produce <ref item="anvilcraft:giant_anvil"/> through **multi-block conversion**

# Function

## 1. Multi-Block Conversion

When <ref item="anvilcraft:giant_anvil"/> strikes <ref item="minecraft:crafting_table"/>, it converts the multi-block structure below, creating new blocks

<structure id="../../structures/mutiblock_convert.snbt"/>

<tip>
If you don't like the look of <ref item="minecraft:crafting_table"/>, try <ref item="anvilcraft:transparent_crafting_table"/>
</tip>

<recipe id="anvilcraft:transparent_crafting_table"/>

## 2. Multi-Block Crafting

- Replace the central <ref item="minecraft:crafting_table"/> with <ref item="anvilcraft:space_overcompressor"/> to perform **multi-block crafting**
- The result is produced in **dropped item** form
- Compatible with **multi-block conversion** recipes that produce one block
- Processes additional special recipes

> Much more convenient than multi-block conversion

## 3. Ground Shaking

- When the center of <ref item="anvilcraft:giant_anvil"/> strikes <ref item="anvilcraft:heavy_iron_block"/>, it performs a **ground shaking** operation
- At this point, it affects blocks or entities on the same horizontal plane. Depending on the blocks surrounding <ref item="anvilcraft:heavy_iron_block"/>, the shaking produces different effects
- Each additional block of fall height expands the affected range by 1 ring

### Definitions

- For convenience, blocks adjacent to <ref item="anvilcraft:heavy_iron_block"/> are called "adjacent blocks", and blocks in the multi-block structure not adjacent to <ref item="anvilcraft:heavy_iron_block"/> are called "corner blocks"
- **Adjacent blocks** determine the shaking work mode
- **Corner blocks** determine the work type within the mode

<structure id="../../structures/giant_anvil_shocking.snbt"/>

### Work Mode: Default

- Activated when **adjacent blocks** and **corner blocks** do not meet any work mode conditions
- **Ground shaking** deals minimal damage, avoidable by wearing boots

### Work Mode: Bounce

- Activated when both **adjacent blocks** and **corner blocks** are <ref item="anvilcraft:resin_block"/>
- **Ground shaking** causes nearby small anvils of any type to bounce up 1 block

### Work Mode: Damage

- Activated when **adjacent blocks** are <ref item="anvilcraft:cursed_gold_block"/>
- The higher the fall height of <ref item="anvilcraft:giant_anvil"/>, the more damage **ground shaking** deals
- **Corner blocks** determine the damage type, as follows:

|                       Corner Block                       |     Damage Type      |
|:-------------------------------------------------------:|:--------------------:|
|    <ref item="anvilcraft:ruby_block"/>                  |     Fire Damage      |
|  <ref item="anvilcraft:sapphire_block"/>                |     Frost Damage     |
|    <ref item="anvilcraft:topaz_block"/>                 |    Lightning Damage   |
| <ref item="anvilcraft:void_matter_block"/>              |     Void Damage      |
|                       Other                             | Fall Damage (avoidable by wearing boots) |

### Work Mode: Destruction

- Activated when **adjacent blocks** are anvils
- Belongs to [Anvil Mining](../001_feature/000_anvil_destroy.md); the anvil type determines different destruction effects
- **Corner blocks** determine which type of blocks are destroyed:

|                      Corner Block                      | Block Type                                             |
|:------------------------------------------------------:|:-------------------------------------------------------|
|     <ref item="minecraft:obsidian"/>                   | Any block                                              |
|   <ref item="minecraft:grass_block"/>                  | Flowers, grass, fungi, bushes, vines, crops, and snow layers |
|    <ref item="minecraft:hay_block"/>                   | Harvests and replants crops including wheat, pumpkins, berries, cocoa beans, and nether wart. Can operate on cocoa beans on connected logs above the work plane |
| <ref item="minecraft:oak_log"/> and any other **log**   | Logs, leaves, stems, wart blocks, cacti, chorus plants, and sugar cane. Can destroy connected blocks above the work plane |
|  <ref item="minecraft:amethyst_block"/>                | Amethyst clusters                                      |
