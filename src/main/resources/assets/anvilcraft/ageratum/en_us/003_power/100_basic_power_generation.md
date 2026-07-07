---
navigation:
  title: "§2Basic Power Generation"
  icon: "anvilcraft:charge_collector"
items:
  - anvilcraft:charge_collector
  - anvilcraft:piezoelectric_crystal
---

# Basic Power Generation

<row halign="center">
<item id="anvilcraft:charge_collector"/>
<item id="anvilcraft:piezoelectric_crystal"/>
</row>

# <ref item="anvilcraft:charge_collector"/>

<recipe id="anvilcraft:charge_collector"/>

The <ref item="anvilcraft:charge_collector"/> is the core component of power generation facilities

## Function

- Maximum generation power: 128kW
- Working range: 5x5x5 centered on itself
- The number of charges received by the charge collector within one cycle becomes its generation power for the next cycle (charges -> kW). The cycle defaults to 2 seconds.

# Charges

The following block behaviors generate charges:

- <ref item="minecraft:anvil"/> falling onto <ref item="anvilcraft:piezoelectric_crystal"/>
- A piston pushing or pulling <ref item="anvilcraft:magnet_block"/> adjacent to <ref item="minecraft:copper_block"/>
- <ref item="minecraft:lightning_rod"/> being struck by lightning.

## Piezoelectric Crystal Charge Generation

<row halign="center">
<recipe id="anvilcraft:piezoelectric_crystal"/>
<recipe id="anvilcraft:piezoelectric_crystal_amethyst"/>
</row>

- When <ref item="anvilcraft:piezoelectric_crystal"/> is hit by a falling <ref item="minecraft:anvil"/>, it generates charges
- Depending on the **anvil type** and **fall height**, the amount of charges generated varies, as listed in the table below
- Stacking <ref item="anvilcraft:piezoelectric_crystal"/> vertically increases the amount of charges produced. The <ref item="anvilcraft:piezoelectric_crystal"/> below generates half the charge of the <ref item="anvilcraft:piezoelectric_crystal"/> above it, rounded down.

|                     Anvil Type                     | Height=1 | Height=2 | Height=3 | Height>=4 |
|:--------------------------------------------------:|:---------|:---------|:---------|:----------|
|   <ref item="anvilcraft:spectral_anvil"/>          | 1        | 2        | 3        | 4         |
|        <ref item="minecraft:anvil"/>               | 1        | 2        | 4        | 8         |
|     <ref item="anvilcraft:royal_anvil"/>           | 1        | 2        | 4        | 8         |
|     <ref item="anvilcraft:ember_anvil"/>           | 1        | 2        | 5        | 12        |
| <ref item="anvilcraft:transcendence_anvil"/>       | 2        | 5        | 15       | 60        |

|                 Anvil Type                 | Height=1 | Height=2 | Height=3 | Height=4 | Height=5 | Height=6 | Height=7 | Height>=8 | 
|:------------------------------------------:|:---------|:---------|:---------|:---------|:---------|:---------|:---------|:----------|
| <ref item="anvilcraft:giant_anvil"/>       | 1        | 2        | 3        | 4        | 5        | 6        | 7        | 8         |

## Triboelectric Charge Generation

<structure id="../../structures/triboelectric_power.snbt"/>

- When <ref item="anvilcraft:magnet_block"/> is pushed or pulled by a piston, if it is adjacent to <ref item="minecraft:copper_block"/>, it generates charges
- Oxidation of <ref item="minecraft:copper_block"/> reduces the amount of charge generated. The charges per movement are shown in the table below

| Type | <ref item="minecraft:copper_block"/> | <ref item="minecraft:exposed_copper"/> | <ref item="minecraft:weathered_copper"/> | <ref item="minecraft:oxidized_copper"/> | Any <ref item="minecraft:waxed_copper_block"/> |
|:----:|:------------------------------------|:---------------------------------------|:-----------------------------------------|:----------------------------------------|:----------------------------------------------|
| Charge | 1/4                                 | 1/8                                    | 1/16                                     | 0                                       | 0                                             |

## Collecting Lightning Charges

A *lightning rod* struck by lightning generates charges. Each strike produces 32 charges.

