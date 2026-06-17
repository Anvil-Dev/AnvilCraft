---
navigation:
  title: "§5Celestial Forging Anvil"
  icon: "anvilcraft:celestial_forging_anvil"
items:
  - anvilcraft:celestial_forging_anvil
  - anvilcraft:celestial_forging_anvil_amplifier
  - anvilcraft:celestial_forging_anvil_logistics_interface
  - anvilcraft:celestial_forging_anvil_fluid_interface
  - anvilcraft:celestial_forging_anvil_laser_interface
---

# <ref item="anvilcraft:celestial_forging_anvil"/>

<item id="anvilcraft:celestial_forging_anvil"/>

- Obtained through [Multi-block Crafting](210_giant_anvil.md#2-multi-block-crafting)
- Requires 3x2x3 space to place
- Can remotely bind to a celestial body in the universe to obtain resources

# Adjusting Celestial Parameters

Open the GUI of <ref item="anvilcraft:celestial_forging_anvil"/>. On the left side, you can place any number of the four basic anvils, which determine the four parameters of the celestial body.

|                         Anvil                         |  Parameter  | Base Value per Anvil |         Growth Trend         |
|:----------------------------------------------------:|:-----------:|:--------------------:|:---------------------------:|
|  <ref item="anvilcraft:confined_time_anvilon"/>      | Age         | 2My                  | Doubles every 3 additional anvils |
|  <ref item="anvilcraft:confined_space_anvilon"/>     | Radius      | 0.125R⊕              | Doubles every 3 additional anvils |
|  <ref item="anvilcraft:confined_mass_anvilon"/>      | Mass        | 0.022M⊕              | Doubles every 2 additional anvils |
|  <ref item="anvilcraft:confined_energy_anvilon"/>    | Surface Temp | 50K                 | Kelvin doubles every 6 additional anvils |

> The following can be skipped

<info>
**Astronomical Units**
1. Time: My (10^6 years, million years), By (10^9 years, billion years), Ty (10^12 years, trillion years)
2. Length: R⊕ (times Earth radius), R☉ (times Solar radius)
3. Mass: M⊕ (times Earth mass), M☉ (times Solar mass)
4. Temperature: ℃ (Celsius), K (Kelvin); ℃ ≈ K-273, 0℃ = 273K, 100℃ = 373K
</info>

# Binding a Star

- Relying only on <ref item="anvilcraft:celestial_forging_anvil"/> can only bind planets
- To bind a star, you need to craft and place <ref item="anvilcraft:celestial_forging_anvil_amplifier"/>

<recipe id="anvilcraft:item_inject/celestial_forging_anvil_amplifier"/>

> Crafting requires <ref item="anvilcraft:spacetime_supercomputer"/>

<structure id="../structures/forging_stars.nbt"/>

After building the correct structure, <ref item="anvilcraft:celestial_forging_anvil"/> enters *Amplified State*

# Binding a Celestial Body

1. Only specific combinations of celestial parameters can bind to the corresponding celestial body
2. If the parameter combination is correct, after 10s of searching, a celestial body can be bound
3. Repeated searching will bind to the same type of celestial body, but the specific material composition will change

## Search Energy Consumption

- During the search, <ref item="anvilcraft:celestial_forging_anvil"/> consumes power
- Default continuous power consumption is 1MW (=1024kW)
- In *Amplified State*, power consumption is 4MW

## Determining Celestial Body Type

1. The left, top, right, and bottom axes represent the four parameters of the celestial body
2. The parameter groups form 3 focal points at the top-left, top-right, and bottom
3. If the three focal points are exactly the same color, the parameters are valid and the celestial body can be bound

![star-info](../textures/star_info.png)

# Identical Celestial Bodies

- Right-click <ref item="anvilcraft:celestial_forging_anvil"/> with <ref item="anvilcraft:disk"/> to copy the celestial body data
- Place this <ref item="anvilcraft:disk"/> into another celestial forging anvil, consuming the <ref item="anvilcraft:disk"/> to search for another celestial body with the exact same parameters
- For extreme celestial bodies (neutron stars, black holes), use <ref item="anvilcraft:singularity_crystal"/> as the medium instead

# Extracting Celestial Resources

1. After successfully binding a celestial body
2. Click the bind button at the bottom of the <ref item="anvilcraft:celestial_forging_anvil"/> GUI
3. On the right side of the <ref item="anvilcraft:celestial_forging_anvil"/> GUI, select *Mega Structure* and submit the corresponding building materials
4. Input raw materials through various interfaces, then extract resources

<tip>
To remove a mega structure, simply unbind and rebind the planet
</tip>

## <ref item="anvilcraft:celestial_forging_anvil"/> Interfaces

### <ref item="anvilcraft:celestial_forging_anvil_logistics_interface"/>

<recipe id="anvilcraft:celestial_forging_anvil_logistics_interface"/>

### <ref item="anvilcraft:celestial_forging_anvil_fluid_interface"/>

<recipe id="anvilcraft:celestial_forging_anvil_fluid_interface"/>

### <ref item="anvilcraft:celestial_forging_anvil_laser_interface"/>

<row halign="center">
<recipe id="anvilcraft:celestial_forging_anvil_laser_interface"/>
<recipe id="anvilcraft:celestial_forging_anvil_laser_interface_from_large_laser"/>
</row>

## Planetary Mega Structures

- Can be built when not in *Amplified State*

### Planetary Excavator

- Requires inputting level 16 [Laser](201_basic_laser.md#laser) into <ref item="anvilcraft:celestial_forging_anvil_laser_interface"/>
- At most 4 <ref item="anvilcraft:celestial_forging_anvil_laser_interface"/> can receive input this way, granting up to 4x collection efficiency
- At 1x collection efficiency, ores are mined 20 times per second, with probabilities viewable in the GUI
- Products are obtained from <ref item="anvilcraft:celestial_forging_anvil_logistics_interface"/>

<warning>
Even with a level 64 laser input into one interface, it still counts as 1x efficiency
</warning>

### Planetary Extractor

- Suitable for rocky planets with liquids
- Each <ref item="anvilcraft:celestial_forging_anvil_fluid_interface"/> around provides 1x extraction efficiency
- At 1x extraction efficiency, 5B (buckets) of fluid are obtained per second
