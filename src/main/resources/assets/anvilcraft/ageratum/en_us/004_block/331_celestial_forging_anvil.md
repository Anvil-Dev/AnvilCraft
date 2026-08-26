---
navigation:
  title: "§5Celestial Forging Anvil"
  icon: "anvilcraft:celestial_forging_anvil"
items:
  - anvilcraft:celestial_forging_anvil
  - anvilcraft:celestial_forging_anvil_amplifier
---

# <ref item="anvilcraft:celestial_forging_anvil"/>

<item id="anvilcraft:celestial_forging_anvil"/>

> Crafting requires <ref item="anvilcraft:spacetime_supercomputer"/>

- Obtained through [Multi-block Crafting](210_giant_anvil.md#2-multi-block-crafting)
- Requires 3x2x3 space to place
- Can create celestial bodies, including stars and planets, to obtain resources

# Forging Celestial Bodies

- Use <ref item="anvilcraft:confined_time_anvilon"/>, <ref item="anvilcraft:confined_space_anvilon"/>, <ref item="anvilcraft:confined_mass_anvilon"/>, <ref item="anvilcraft:confined_energy_anvilon"/> placed on the left side in sequence, and configure reasonable parameters to search for and forge a celestial body
- Forging celestial bodies does not consume Anvilons

<tip>
Debugging parameters:
  - Use the **mouse wheel** to conveniently adjust the four parameters
  - After placing some Anvilons, hover over other Anvilon items to display the matching Anvilon count (will not display if previously placed Anvilons already cannot satisfy the condition)
</tip>

![Brown Dwarf.png](../../textures/cfa/heai.png)

> The parameters in the image can be used to forge a brown dwarf

## Forging Planets

1. To forge a planet, first prepare 1MW of power for operation
2. Use the four Anvilons to control the four parameters, making the **intersection** of the red line (energy) and green line (time) AND the **intersection** of the blue line (space) and yellow line (mass) fall on the same color region
3. The text at the bottom right shows what celestial body each of the three focus areas corresponds to. As long as the first and third lines show the same celestial body, it works
4. Click the confirm button. After 10s of forging, click the **lock icon** below to start operating on the celestial body

## Forging Stars

<structure id="../../structures/forging_stars.nbt"/>

1. To forge a star, first prepare four <ref item="anvilcraft:celestial_forging_anvil_amplifier"/> and place them at the four corners of the <ref item="anvilcraft:celestial_forging_anvil"/>
2. Prepare 4MW of power for operation
3. Use the four Anvilons to control the four parameters, ensuring all **three focus areas** of the four lines fall on the same color region
4. Click the confirm button. After 10s, click the **lock icon** to start operating on the celestial body

<info>
When amplifiers are installed, even forging a planet will cost 4MW of power
</info>

<recipe id="anvilcraft:item_inject/celestial_forging_anvil_amplifier"/>

> Crafting requires <ref item="anvilcraft:spacetime_supercomputer"/>

For more information, see [Celestial Types](../001_feature/331_celestial_type.md)

# Celestial Body & World Interaction

- After searching, the searched celestial body will appear in the *binding ring*
- Gravity will appear around it. Any creatures, items, projectiles, etc. entering the gravity field will be attracted to the celestial body and take damage
- Using a **redstone signal** can amplify the celestial body, and its gravity field will also scale up

# Extracting Celestial Resources

1. Click the bind button at the bottom of the <ref item="anvilcraft:celestial_forging_anvil"/> GUI
2. On the right side of the GUI, select *Mega Structure* and submit the corresponding **building materials**
3. Input raw materials through various interfaces to extract resources ([Click here](332_interface.md) for details)

<tip>
To remove a mega structure, simply unbind and rebind the planet
</tip>

## Planetary Mega Structures

- Can be built when not in *Amplified State*, only buildable on satellites/planets
- Generally only one mega structure can be built at a time

|     Mega Structure     |            Construction Condition            |                            Input                            |                                       Output                                        |
|:----------------------:|:--------------------------------------------:|:-----------------------------------------------------------:|:-----------------------------------------------------------------------------------:|
|  Planetary Excavator   |        Large satellite, rocky planet         | Level 16 [Laser](201_basic_laser.md#laser) (cannot be *Gamma Laser*) |                                  Items (minerals); Compatible with <ref item="anvilcraft:lens"/>                                   |
|  Planetary Extractor   |         Rocky planet with **liquid**         |                            None                            |                             Fluid (planetary resources)                             |
|      Eco Station       |  Rocky planet with **biological resources**  |                         Power 1MW                          |                        Items & Fluid (biological resources)                         |
|         Temple         | Rocky planet with **low-level civilization** |                       Specific items                       |                                  Items (offerings)                                  |
| Giant Planet Extractor |             Gas giant, ice giant             |                            None                            | Items & Fluid (planetary resources), must collect fluid to bring up items alongside |

<info>
For *Planetary Excavator*, at most 4 <ref item="anvilcraft:celestial_forging_anvil_laser_interface"/> can receive input this way, granting up to 4x collection efficiency
Note that even with a level 64 laser input into one interface, it still counts as 1x efficiency
</info>

<info>
*Temple* inputs are items as divine blessings or punishments to maintain the faith of low-level civilizations. Item requirements are updated every MC day (cycling in the order of two blessings followed by one punishment)
After inputting items, the civilization will continue to offer worship until the next MC day. Therefore, it is not recommended to provide items at night, as the civilization always stops worship at dawn, at which point blessings or punishments must be given again
</info>

## Stellar Mega Structures

- Can be built when in *Amplified State*, only buildable on stellar celestial bodies
- Generally only one mega structure can be built at a time

|        Mega Structure         |    Construction Condition    |     Input     |                                                                                                                                                             Output/Effect                                                                                                                                                             |
|:-----------------------------:|:----------------------------:|:-------------:|:-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------:|
|         Ring Collider         |          Small star          |   Power 4MW   |                                                 Executes [Anvil Impact Crafting](215_large_electromagnet.md#anvil-impact-crafting) recipes. The stronger the star's gravity and magnetic field, the faster it works; the higher the speed required by the recipe, the slower it works                                                 |
|         Dyson Sphere          |             Star             |     None      |                                                                                                       Continuously generates power. Power output is positively correlated with the celestial body's *temperature* and *radius*                                                                                                        |
| Stellar Evolution Accelerator | Star (excluding white dwarf) |     None      |                                                                                                                                                     Accelerates stellar evolution                                                                                                                                                     |
|         Magnetar Coil         |         Neutron star         |     None      |                                                                                             Continuously generates power. Power output is positively correlated with the celestial body's *magnetic field strength* and *rotation speed*                                                                                              |
|      Wormhole Stabilizer      |          Black hole          |     None      |                                                                                                                                                      [Wormhole](332_teleportation.md)                                                                                                                                                      |
|        Penrose Sphere         |          Black hole          |     Laser     | Same-level [Gamma Laser](../001_feature/332_gamma_laser.md). Note that *Penrose Sphere* input and output [Lasers](201_basic_laser.md#laser) must be grouped on the left and right sides of the same side of the forging anvil. Lasers on the four sides are independent of each other for input and output |
|      Matter Decompressor      |         Neutron star         | *Gamma Laser* | Each level of *Gamma Laser* doubles efficiency. Extracts once every 10 seconds, mostly producing 1 <ref item="anvilcraft:neutronium_ingot"/>, with a small chance of producing <ref item="anvilcraft:charged_neutronium_ingot"/> (requires sufficient magnetic field strength, probability positively correlated with magnetic field) |
|      Matter Decompressor      |          Black hole          | *Gamma Laser* |     Each level of *Gamma Laser* doubles efficiency. Extracts once per gametick, mostly producing 1 <ref item="anvilcraft:void_matter"/>, with a small chance of producing <ref item="anvilcraft:excited_state_void_matter"/> (requires sufficient magnetic field strength, probability positively correlated with magnetic field)     |

![Penrose Sphere.png](../../textures/cfa/gama.png)

> Producing [Gamma Laser](../001_feature/332_gamma_laser.md)

# Stellar Evolution

- Using *Stellar Evolution Accelerator* can accelerate the aging of stars
- Some stars will ultimately trigger a *supernova explosion*, destroying all of their *mega structures* and causing a massive explosion that reaches over ten blocks away
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

# Amplifying Celestial Bodies

Apply a redstone signal to <ref item="anvilcraft:celestial_forging_anvil"/> to amplify the celestial body

Every 3 levels of redstone signal, the rendered size of the celestial body doubles

> There s■■■s to be ■n■■■■■tion here, but it has been ■■a■■■ by ■■■■
