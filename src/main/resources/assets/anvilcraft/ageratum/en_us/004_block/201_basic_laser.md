---
navigation:
  title: "§6Laser"
  icon: "anvilcraft:ruby_laser"
items:
  - anvilcraft:ruby_laser
  - anvilcraft:ruby_prism
  - anvilcraft:laser_receiver
  - anvilcraft:large_laser
---

# Laser

<row halign="center">
<item id="anvilcraft:ruby_laser"/>
<item id="anvilcraft:ruby_prism"/>
<item id="anvilcraft:laser_receiver"/>
</row>

<tip>
To set up this mod's laser system, prepare a large amount of <ref item="anvilcraft:ruby"/>. [Click here](../008_recipe/204_gem.md) for methods
</tip>

- Lasers are straight beams that can be blocked by **opaque blocks**
- Lasers have a **level** that determines their effects

## Function

- Deals damage: Damage[max:16] = Laser Level - 4
- Heats [heatable blocks](../001_feature/101_heated_block.md)
- Extracts ores, leaving stone behind; the minerals are ejected from the back of the <ref item="anvilcraft:ruby_prism"/> that emitted the laser, or deposited into a container

<info>
If the back of <ref item="anvilcraft:ruby_prism"/> also has a <ref item="anvilcraft:ruby_prism"/> in the **same direction**, the output comes from the back of the last <ref item="anvilcraft:ruby_prism"/>
</info>

|   Level    | Extraction Cooldown (s) |
|:----------:|:-----------------------:|
|   [4, 7]   |           24            |
|  [8, 11]   |            6            |
|  [12, 15]  |            3            |
| [16, +inf) |            1            |

|   Level   |           Heating Ability           |
|:---------:|:-----------------------------------:|
|  [1, 3]   |     <color=#661111>Hot</color>      |
|  [4, 15]  |   <color=#aa2222>Red-hot</color>    |
| [16, 63]  |   <color=#cc5533>Blazing</color>    |
| [64,+inf) | <color=#ee7744>Incandescent</color> |

# <ref item="anvilcraft:ruby_laser"/>

<recipe id="anvilcraft:ruby_laser"/>

- Power consumption: 16kW
- Disabled by redstone signal
- When active, emits a laser [Level: 1, Range: 128 blocks]

# <ref item="anvilcraft:ruby_prism"/>

<recipe id="anvilcraft:ruby_prism"/>

- Does not consume power
- Combines lasers and their levels from the other 5 directions, emits forward [Range: 128 blocks]

# <ref item="anvilcraft:laser_receiver"/>

<recipe id="anvilcraft:laser_receiver"/>

- Except the bottom face, can receive lasers and generate power, while also emitting redstone signal based on laser level
- Power generation cap = Laser Level * 15kW
- Reaches maximum power after continuously receiving for 10 seconds

# <ref item="anvilcraft:large_laser"/>

- Power consumption: 256kW
- Disabled by redstone signal
- When active, emits a laser [Level: 16, Range: 128 blocks]
