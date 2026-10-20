---
navigation:
  title: "§5Infinite Collector"
  icon: "anvilcraft:infinite_collector"
items:
  - anvilcraft:infinite_collector
---

# <ref item="anvilcraft:infinite_collector"/>

<recipe id="anvilcraft:infinite_collector"/>

1. Has the capabilities of <ref item="anvilcraft:charge_collector"/>, <ref item="anvilcraft:void_energy_collector"/>, and <ref item="anvilcraft:heat_collector"/>, with no power limit
2. Works within a 7x7x7 range

# Void Energy Generation

- Has a base power generation of 256kW, similar to the <ref item="anvilcraft:void_energy_collector"/>
- Does not decay or generate junk blocks
- Power generation is affected by material blocks and <ref item="anvilcraft:negative_matter_block"/>:

|  Material Blocks  | Power Output (kW) |
|:-----------------:|:-----------------:|
| [-10, +inf)       |        256        |
| [-20, -11]        |        768        |
| [-40, -21]        |       2048        |
| (-inf, -41]       |       5120        |

<tip>
<ref item="anvilcraft:negative_matter_block"/> provides -1 material block
</tip>

## Properties

- Has transcendent alloy-level properties: blast-resistant, wither-proof, dragon-proof
