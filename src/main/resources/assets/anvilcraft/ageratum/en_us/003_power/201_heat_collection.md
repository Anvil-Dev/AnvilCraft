---
navigation:
  title: "§6Heat Collection"
  icon: "anvilcraft:heat_collector"
items:
  - anvilcraft:heat_collector
---

# Heat Collection

<item id="anvilcraft:heat_collector"/>

# <ref item="anvilcraft:heat_collector"/>

<recipe id="anvilcraft:heat_collector"/>

Collects heat energy from *heat-collectible blocks* to generate power

- Maximum generation power: 4096kW
- Working range: 5x5x5 centered on itself
- The *heat-collectible blocks* within range determine the generation power

## Heat-Collectible Blocks

|                                             Block                                             |                      Conversion Result                      | Energy Provided (kW) |
|:---------------------------------------------------------------------------------------------:|:----------------------------------------------------------:|:--------------------:|
|                       <ref item="minecraft:magma_block"/>                                     | <ref item="minecraft:netherrack"/>                         |          2           |
|                         <ref item="minecraft:campfire"/>                                      | Extinguished <ref item="minecraft:campfire"/>              |          4           |
|                                            Lava                                               | <ref item="minecraft:obsidian"/>                           |          4           |
| <ref item="anvilcraft:ember_metal_block"/>                                                    | Unchanged                                                  |          4           |
|            <color=#661111>Hot</color> [heatable blocks](../001_feature/101_heated_block.md)   | Unchanged                                                  |          4           |  
|            <color=#aa2222>Red-hot</color> [heatable blocks](../001_feature/101_heated_block.md)| Unchanged                                                  |          16          |
|            <color=#cc5533>Blazing</color> [heatable blocks](../001_feature/101_heated_block.md)| Unchanged                                                  |          64          |
|            <color=#ee7744>Incandescent</color> [heatable blocks](../001_feature/101_heated_block.md)| Unchanged                                              |         256          |

# Power Generation Methods

The following two methods are practical in the early-to-mid game:

## Solar Power Generation

- Use <ref item="anvilcraft:heliostats"/> to collect solar energy onto heatable blocks, then the heat collector absorbs the heat to generate power
- Pros: Simple materials, no ongoing investment required
- Cons: Large footprint, and the light path must not be blocked by any blocks

## Oil Ion Power Generation

- [Burning crude oil for heat](../007_struct/201_plasma_jets.md) requires a lot of meat, but the power output is absolutely worth it
- Pros: Small footprint, high efficiency
- Cons: Continuous consumption of meat, difficult to automate, requires a supporting animal farm/mob farm/spawner

# Related

- [Heat System](../001_feature/101_heated_block.md)