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

|                      Anvil                       |  Parameter   | Base Value per Anvil |               Growth Trend               |
|:------------------------------------------------:|:------------:|:--------------------:|:----------------------------------------:|
|  <ref item="anvilcraft:confined_time_anvilon"/>  |     Age      |         2My          |    Doubles every 3 additional anvils     |
| <ref item="anvilcraft:confined_space_anvilon"/>  |    Radius    |       0.125R⊕        |    Doubles every 3 additional anvils     |
|  <ref item="anvilcraft:confined_mass_anvilon"/>  |     Mass     |       0.022M⊕        |    Doubles every 2 additional anvils     |
| <ref item="anvilcraft:confined_energy_anvilon"/> | Surface Temp |         50K          | Kelvin doubles every 6 additional anvils |

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

<structure id="../../structures/forging_stars.nbt"/>

After building the correct structure, <ref item="anvilcraft:celestial_forging_anvil"/> enters *Amplified State*

# Binding a Celestial Body

1. Only specific combinations of celestial parameters can bind to the corresponding celestial body
2. Provide sufficient power — 1MW is required, 4MW in *Amplified State*
3. If the parameter combination is correct, after 10s of searching, a celestial body can be bound
4. Repeated searching will bind to the same type of celestial body, but the specific material composition will change

<info>
In the GUI, you can use the **mouse wheel** to conveniently adjust the four parameters
In the center of the GUI, you can check whether a star can be searched based on the legend:
  - For non-star celestial bodies, only the intersection of the lower-left and upper-right diagrams needs to correspond to the same non-star type
  - For stellar celestial bodies, the intersection of three diagrams needs to correspond to the same stellar category
For more information, see [Celestial Types](../001_feature/331_celestial_type.md)
</info>

# Extracting Celestial Resources

1. After successfully binding a celestial body
2. Click the bind button at the bottom of the <ref item="anvilcraft:celestial_forging_anvil"/> GUI
3. On the right side of the <ref item="anvilcraft:celestial_forging_anvil"/> GUI, select *Mega Structure* and submit the corresponding **building materials**
4. Input raw materials through various interfaces, then extract resources

<tip>
To remove a mega structure, simply unbind and rebind the planet
</tip>

## Using Interfaces

### <ref item="anvilcraft:celestial_forging_anvil_logistics_interface"/>

<recipe id="anvilcraft:celestial_forging_anvil_logistics_interface"/>

- Can hold 16 types of items, each up to 1 stack
- When receiving a redstone signal, actively tries to output items forward

<warning>
In some cases, a large number of the same item may be output. If there are not enough <ref item="anvilcraft:celestial_forging_anvil_logistics_interface"/>, items may be lost
</warning>

### <ref item="anvilcraft:celestial_forging_anvil_fluid_interface"/>

<recipe id="anvilcraft:celestial_forging_anvil_fluid_interface"/>

- **Continuous power consumption** 128kW
- Can hold 4 types of fluids, each up to 80 buckets
- When receiving a redstone signal, actively tries to output fluid to pipes

### <ref item="anvilcraft:celestial_forging_anvil_laser_interface"/>

<row halign="center">
<recipe id="anvilcraft:celestial_forging_anvil_laser_interface"/>
<recipe id="anvilcraft:celestial_forging_anvil_laser_interface_from_large_laser"/>
</row>

- Receives lasers
- When receiving a redstone signal, actively tries to emit laser forward

## Maintaining Mega Structures

- Different mega structures have different construction conditions, requiring specific types of planets
- Some mega structures require input of items, fluids, lasers, or power — the first three need to be input through *interfaces*
- Some mega structures can produce items, fluids, lasers, or power — the first three need to be output through *interfaces*

## Obtaining Resources

- Normally, if there are multiple <ref item="anvilcraft:celestial_forging_anvil_logistics_interface"/>, they output in rotation, with a total of 20 items per second
- Normally, each <ref item="anvilcraft:celestial_forging_anvil_logistics_interface"/> independently outputs 5B (buckets) of fluid per second

## Planetary Mega Structures

- Can be built when not in *Amplified State*
- Generally only one mega structure can be built at a time

|     Mega Structure     |            Construction Condition            |                   Input                    |                Output                |
|:----------------------:|:--------------------------------------------:|:------------------------------------------:|:------------------------------------:|
|  Planetary Excavator   |        Large satellite, rocky planet         | Level 16 [Laser](201_basic_laser.md#laser) |           Items (minerals)           |
|  Planetary Extractor   |         Rocky planet with **liquid**         |                    None                    |     Fluid (planetary resources)      |
|      Eco Station       |  Rocky planet with **biological resources**  |                 Power 1MW                  | Items & Fluid (biological resources) |
|         Temple         | Rocky planet with **low-level civilization** |               Specific items               |          Items (offerings)           |
| Giant Planet Extractor |             Gas giant, ice giant             |                    None                    | Items & Fluid (planetary resources)  |

<info>
For *Planetary Excavator*, at most 4 <ref item="anvilcraft:celestial_forging_anvil_laser_interface"/> can receive input this way, granting up to 4x collection efficiency
Note that even with a level 64 laser input into one interface, it still counts as 1x efficiency
</info>

<info>
*Temple* inputs are items as divine blessings or punishments to maintain the faith of low-level civilizations. Item requirements are updated every MC day (cycling in the order of two blessings followed by one punishment)
After inputting items, the civilization will continue to offer worship until the next MC day. Therefore, it is not recommended to provide items at night, as the civilization always stops worship at dawn, at which point blessings or punishments must be given again
</info>

## Stellar Mega Structures

- Can be built when in *Amplified State*
- Generally only one mega structure can be built at a time

|        Mega Structure         |    Construction Condition    |     Input     |                                                                                                             Output/Effect                                                                                                             |
|:-----------------------------:|:----------------------------:|:-------------:|:-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------:|
|         Ring Collider         |          Small star          |   Power 4MW   | Executes [Anvil Impact Crafting](215_large_electromagnet.md#anvil-impact-crafting) recipes. The stronger the star's gravity and magnetic field, the faster it works; the higher the speed required by the recipe, the slower it works |
|         Dyson Sphere          |             Star             |     None      |                                                           Continuously generates power. Power output is positively correlated with the celestial body's *temperature* and *radius*                                                           |
| Stellar Evolution Accelerator | Star (excluding white dwarf) |     None      |                                                                                                     Accelerates stellar evolution                                                                                                     |
|         Magnetar Coil         |         Neutron star         |   Power 4MW   |                                             Continuously generates power. Power output is positively correlated with the celestial body's *magnetic field strength* and *rotation speed*                                              |
|      Wormhole Stabilizer      |          Black hole          |     None      |                                                                                                      [Wormhole](332_wormhole.md)                                                                                                      |
|        Penrose Sphere         |          Black hole          |     Laser     |                                                                                                       Same-level *Gamma Laser*                                                                                                        |
|      Matter Decompressor      |   Neutron star, black hole   | *Gamma Laser* |                                                Produces 1 Neutronium Ingot every 10 seconds (neutron star) or 1 Void Matter per gametick (black hole). Each level of *Gamma Laser* doubles efficiency                                                |

<info>
*Penrose Sphere* input and output [Lasers](201_basic_laser.md#laser) must be grouped on the same side of the forging anvil, using the left and right <ref item="anvilcraft:celestial_forging_anvil_laser_interface"/>
. Lasers on the four sides are independent of each other for input and output
</info>

# Stellar Evolution

- Special methods can be used to accelerate the aging of stars
- Some stars will ultimately trigger a *supernova explosion*, destroying their *mega structures* and causing a massive explosion
- All stars become *stellar remnants* at the end of their life

<info>
During acceleration, if a *Dyson Sphere* exists, it will collect **infinite electrical energy**
</info>

## Stellar Remnants

The original star's mass determines what type of stellar remnant it becomes

| Mass Anvil Count | Stellar Remnant |
|:----------------:|:---------------:|
|      [1,54]      |   White Dwarf   |
|     [55,58]      |  Neutron Star   |
|     [59,64]      |   Black Hole    |

# Identical Celestial Bodies

- Right-click <ref item="anvilcraft:celestial_forging_anvil"/> with <ref item="anvilcraft:disk"/> to copy the celestial body data
- Place this <ref item="anvilcraft:disk"/> into another celestial forging anvil, consuming the <ref item="anvilcraft:disk"/> to search for another celestial body with the exact same parameters — they become *Identical Celestial Bodies*
- For extreme celestial bodies (neutron stars, black holes), use <ref item="anvilcraft:singularity_crystal"/> as the medium instead

> There s■■■s to be ■n■■■■■tion here, but it has been ■■a■■■ by ■■■■
