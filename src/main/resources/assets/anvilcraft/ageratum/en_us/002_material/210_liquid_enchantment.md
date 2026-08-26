---
navigation:
  title: "§6Liquid Enchantment"
  icon: "anvilcraft:frost_metal_ingot"
items:
  - anvilcraft:frost_metal_block
---

# Liquid Enchantment

> Use [Liquid Experience](004_exp_gem.md#experience-fluid) to convert enchantments into fluid form for storage, enabling automated production and duplication; liquid enchantment can also be used to craft <ref item="anvilcraft:enchanted_gold_ingot"/>

# Blank Liquid Enchantment

Create blank liquid enchantment in a <ref item="anvilcraft:large_cauldron"/>:

2000mB experience fluid + 3 <ref item="minecraft:lapis_lazuli"/> -> 1mB blank liquid enchantment

# Directed Enchanting

Consume specific materials to obtain the corresponding enchantment:

| Blank Liquid Enchantment Required | Required Material                           | Output Liquid Enchantment Type and Amount |
|-----------------------------------|---------------------------------------------|-------------------------------------------|
| 1mB                               | <ref item="anvilcraft:royal_steel_ingot"/>  | Silk Touch 1mB                            |
| 1mB                               | <ref item="anvilcraft:frost_metal_ingot"/>  | Disintegration 1mB                        |
| 16mB                              | <ref item="anvilcraft:ember_metal_ingot"/>  | Smelting 16mB                             |
| 128mB                             | <ref item="anvilcraft:transcendium_ingot"/> | Fortune 64mB + Looting 64mB               |
| 1mB                               | <ref item="minecraft:emerald"/>             | Mending 1mB                               |
| 8mB                               | <ref item="anvilcraft:ruby"/>               | Fire Protection 8mB                       |
| 2mB                               | <ref item="anvilcraft:sapphire"/>           | Frost Walker 2mB                          |
| 1mB                               | <ref item="anvilcraft:topaz"/>              | Channeling 1mB                            |
| 12mB                              | <ref item="minecraft:amethyst_block"/>      | Timber 4mB + Harvest 4mB + Beheading 4mB  |

# Using Liquid Enchantment

Apply liquid enchantment to items through an <ref item="anvilcraft:auto_enchanting_table"/>.

Enchantment levels 1 through 5 consume 1, 2, 4, 8, and 16mB of liquid enchantment respectively; higher levels continue doubling the amount consumed.

# Transport and Filtering

- Liquid enchantment cannot be released into the world or collected with a bucket; transport it through <ref item="anvilcraft:pipe"/>s
- Because all liquid enchantments use the same fluid, mark a specific enchantment in a <ref item="anvilcraft:control_valve"/> with an enchanted book. The same filtering method applies when configuring a <ref item="anvilcraft:creative_fluid_tank"/> in Creative mode

# Enchantment Duplication

In a <ref item="anvilcraft:large_cauldron"/>, blank liquid enchantment can be assimilated by *liquid enchantment*:

[Heater heating] 1mB blank liquid enchantment + 8mB liquid enchantment + 1 <ref item="minecraft:lapis_lazuli"/> -> 9mB liquid enchantment

# Enchantment Cleaning

- 8mB liquid enchantment + <ref item="anvilcraft:silver_nugget"/> -> 8mB blank liquid enchantment
- 1mB cursed liquid enchantment + 16 <ref item="minecraft:gold_ingot"/> -> 16 <ref item="anvilcraft:cursed_gold_ingot"/>
- 9mB cursed liquid enchantment + 16 <ref item="minecraft:gold_ingot"/> -> 16 <ref item="anvilcraft:cursed_gold_block"/>
