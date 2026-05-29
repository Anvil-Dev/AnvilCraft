---
navigation:
  title: "Plasma Jets"
  icon: "anvilcraft:oil_bucket"
---

# Plasma Jets

<row halign="center">
<item id="anvilcraft:heater"/>
<item id="anvilcraft:magnet_block"/>
<item id="anvilcraft:tungsten_block"/>
<item id="anvilcraft:charge_collector"/>
<item id="anvilcraft:heat_collector"/>
</row>

## Structure

<structure id="../structures/plasma_jets.snbt"/>

- Requires a [burning cauldron](../002_material/201_oil.md)
- A working <ref item="anvilcraft:heater"/> beneath the cauldron
- A 1x8x1 space directly above the cauldron with no blocks
- The pipe walls can be up to four blocks high

# Function

- Heats the [regular heatable blocks](../001_feature/101_heated_block.md) used as pipe walls to <color=#cc5533>scorching-hot</color> and adds 0.1 seconds of duration
- If a layer of pipe walls has <ref item="anvilcraft:magnet_block"/> on one pair of opposite sides and [regular heatable blocks](../001_feature/101_heated_block.md) on the other pair:
    - Heats the [regular heatable blocks](../001_feature/101_heated_block.md) in that layer to <color=#cc5533>scorching-hot</color> and adds 1 second of duration
    - Generates 256 charge on the [regular heatable blocks](../001_feature/101_heated_block.md)

# Properties

- The jet stream consumes crude oil from the cauldron to sustain itself
    - Each layer (250mb) of crude oil adds +5min to sustain time
    - Maximum sustain time is 10min
- Entities in the jet stream take 4 times the fire-type damage compared to lava

---

## Related

- [Heat System](../001_feature/101_heated_block.md)
