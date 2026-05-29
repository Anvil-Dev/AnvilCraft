---
navigation:
  title: "Resource: Three-Mode Sand Machine"
  icon: "minecraft:sand"
---

# Resource: Three-Mode Sand Machine

A machine for producing gravel and sand, with a structure similar to the cobblestone generator.

This three-mode sand-crushing machine can be controlled via levers to switch between producing cobblestone, gravel, or sand.

<structure id="../structures/machine/sand.snbt"/>

<tip>
Left-click to adjust position; right-click to adjust angle; PgUp/PgDn to adjust display height
</tip>

1. The piston 2 blocks to the right is *Switch 1*: the machine master switch
2. Below *Switch 1* is *Switch 2*: when enabled, produces cobblestone; when disabled, activates *Switch 3*
3. To the left of *Switch 1* is *Switch 3*: when enabled, produces sand; when disabled, produces gravel
4. Set the pulse generator behind *Switch 1* to loop mode: emit a 5gt signal every 10gt interval
5. Set the other pulse generator to rising edge mode: 6gt delay, emit a 17gt signal
6. Place a hopper minecart

<warning>
Turn off the *master switch* before adjusting modes
</warning>

- All <ref item="minecraft:smooth_stone"/> can be replaced with any full opaque block
- All <ref item="minecraft:glass"/> can be replaced with any transparent block
