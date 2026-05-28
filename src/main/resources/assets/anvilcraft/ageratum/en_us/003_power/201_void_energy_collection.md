---
navigation:
  title: "§6Void Energy Collection"
  icon: "anvilcraft:void_energy_collector"
items:
  - anvilcraft:void_energy_collector
---

# <ref item="anvilcraft:void_energy_collector"/>

<recipe id="anvilcraft:void_energy_collector"/>

Collects void energy from *non-material blocks* to generate power

- Detection range: 5x5x5 centered on itself
- The detection ranges of <ref item="anvilcraft:void_energy_collector"/> must not overlap, otherwise they stop working
- Power generation is reduced by *material blocks* within the detection range

|  Material Blocks  | Power Output (kW) |
|:-----------------:|:-----------------:|
| [20, +inf)        |         0         |
| [11, 20]          |        128        |
| [3, 10]           |        256        |
|  [0, 2]           |        512        |

<info>
<ref item="anvilcraft:void_energy_collector"/> has extremely high blast resistance
</info>

## Non-Material Blocks

- *Air blocks*
- *Void*
- <ref item="anvilcraft:void_matter_block"/>
- <ref item="anvilcraft:void_energy_collector"/>

## Material Block Generation

- Periodically, <ref item="anvilcraft:void_energy_collector"/> converts an *air block* within its detection range into a *material block*. The possible blocks that appear are consistent with [Void Decay](../001_feature/101_void_decay.md).
- These *material blocks* affect the power generation of <ref item="anvilcraft:void_energy_collector"/>.

<tip>
Use *non-material blocks* to prevent *material blocks* from spawning in the inner ring, then use any automated mining method to destroy the *material blocks* in the outer ring, keeping the generator running
</tip>

<tip>
Use TNT to blast away all blocks except <ref item="anvilcraft:void_energy_collector"/> to clear large amounts of *material blocks* at once, keeping the generator running
</tip>

