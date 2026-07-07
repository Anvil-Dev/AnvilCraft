---
navigation:
  title: "§6Ember Smithing Table"
  icon: "anvilcraft:ember_smithing_table"
items:
  - anvilcraft:ember_smithing_table
  - anvilcraft:two_to_one_smithing_template
  - anvilcraft:four_to_one_smithing_template
  - anvilcraft:eight_to_one_smithing_template
---

# Ember Smithing Table

<recipe id="anvilcraft:smithing/ember_smithing_table"/>

## Function

- A special smithing table that does not consume any smithing templates
- Only usable for multi-to-one recipes
- Usually needs to be used with <ref item="anvilcraft:four_to_one_smithing_template"/> and other multi-to-one templates

# Multi-to-One Smithing Templates

<row halign="center">
<item id="anvilcraft:two_to_one_smithing_template"/>
<item id="anvilcraft:four_to_one_smithing_template"/>
<item id="anvilcraft:eight_to_one_smithing_template"/>
</row>

Used at the [Ember Smithing Table](../004_block/221_ember_smithing_table.md) to craft equipment

## Crafting

- Created by stamping the corresponding number of different smithing templates on <ref item="anvilcraft:stamping_platform"/>

> Both upgrade smithing templates and armor trim smithing templates count; multi-to-one smithing templates also count as smithing templates

> e.g.: Netherite Upgrade Smithing Template + Royal Steel Upgrade Smithing Template -> Two-to-One Smithing Template

> e.g.: Snout Armor Trim + Rib Armor Trim + Ember Smithing Template + Coast Armor Trim -> Four-to-One Smithing Template

# Template Dissociation

1. Destroy an enchanted <ref item="anvilcraft:eight_to_one_smithing_template"/> item entity by any means
2. Randomly select one enchantment (selection may repeat) and generate a new item according to the list below
3. At most 4 selections (i.e. at most 4 new templates produced this way)

<row halign="center" valign="center">
<item id="minecraft:snout_armor_trim_smithing_template"/>
->  Soul Speed  ;
<item id="minecraft:rib_armor_trim_smithing_template"/>
->  Fire Protection, Fire Aspect, Flame  ;
<item id="minecraft:dune_armor_trim_smithing_template"/>
->  Blast Protection
</row>

<row halign="center" valign="center">
<item id="minecraft:silence_armor_trim_smithing_template"/>
->  Swift Sneak  ;
<item id="minecraft:ward_armor_trim_smithing_template"/>
->  Protection  ;
<item id="minecraft:vex_armor_trim_smithing_template"/>
->  Mending  ;
<item id="minecraft:sentry_armor_trim_smithing_template"/>
->  Infinity
</row>

<row halign="center" valign="center">
<item id="minecraft:bolt_armor_trim_smithing_template"/>
->  Density, Breach  ;
<item id="minecraft:flow_armor_trim_smithing_template"/>
->  Wind Burst  ;
<item id="minecraft:wild_armor_trim_smithing_template"/>
->  Projectile Protection  ;
</row>

<row halign="center" valign="center">
<item id="minecraft:spire_armor_trim_smithing_template"/>
->  Fortune  ;
<item id="minecraft:eye_armor_trim_smithing_template"/>
->  Looting  ;
<item id="minecraft:coast_armor_trim_smithing_template"/>
->  Luck of the Sea, Lure
</row>

<row halign="center" valign="center">
<item id="minecraft:tide_armor_trim_smithing_template"/>
->  Depth Strider, Respiration, Aqua Affinity, Impaling, Riptide
</row>
<row halign="center" valign="center">
<item id="minecraft:host_armor_trim_smithing_template"/>
<item id="minecraft:wayfinder_armor_trim_smithing_template"/>
<item id="minecraft:raiser_armor_trim_smithing_template"/>
<item id="minecraft:shaper_armor_trim_smithing_template"/>
->  Other Enchantments
</row>

<info>
If 4 selections are made and all 4 results (the newly generated templates) are different, an additional <ref item="anvilcraft:transcendium_upgrade_smithing_template"/> is generated
</info>