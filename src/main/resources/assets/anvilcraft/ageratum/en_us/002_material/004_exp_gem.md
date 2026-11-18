---
navigation:
  title: "Experience Storage"
  icon: "anvilcraft:exp_gem"
items:
  - anvilcraft:exp_gem
  - anvilcraft:exp_gem_block
  - anvilcraft:exp_bucket
---

# Experience Fluid

> Store experience in item or fluid form

<info>
1000mB Experience Fluid = 1 * <ref item="anvilcraft:exp_gem"/> = 50 player experience points
</info>

## Cauldron Interaction

- Pour a Bottle o' Enchanting into a [cauldron](../001_feature/000_cauldron.md); 50% chance to gain one layer of experience fluid (yes, you lose some)
- Use an empty bottle on a [cauldron](../001_feature/000_cauldron.md) to obtain a Bottle o' Enchanting

## Player Absorption

- While inside a cauldron, the player absorbs 1B of liquid experience at a time, gaining 50 experience points
- Players can touch and absorb a source block of the fluid in the world to gain 50 experience points

## Sculk Extraction

- When a <ref item="minecraft:sculk"/> above a [cauldron](../001_feature/000_cauldron.md) is crushed, there is a 10% chance to add one layer of experience fluid
- If upgraded to a <ref item="anvilcraft:frost_anvil"/>, the chance increases to 40%

## Item Repair

- Experience fluid inside a [cauldron](../001_feature/000_cauldron.md) repairs items enchanted with Mending
- A vanilla <ref item="minecraft:cauldron"/> consumes its entire contents per repair, which easily wastes fluid

# <ref item="anvilcraft:exp_gem"/>

## Solid-Liquid Conversion

<row>
<recipe id="anvilcraft:solid_liquid/exp_gem"/>
<recipe id="anvilcraft:solid_liquid/exp_fluid_cauldron"/>
</row>

## Special Functions

- Right-click with an <ref item="anvilcraft:exp_gem"/> to gain 50 experience points; Shift-right-click to use all <ref item="anvilcraft:exp_gem"/>s in hand
- Use an <ref item="anvilcraft:exp_gem"/> on a villager to give it 20 trading experience points
- Use an <ref item="anvilcraft:exp_gem"/> on a baby villager to reduce its growth time by 2 minutes

## Storage Block

<row>
<recipe id="anvilcraft:exp_gem_block"/>
<recipe id="anvilcraft:exp_gem"/>
</row>
