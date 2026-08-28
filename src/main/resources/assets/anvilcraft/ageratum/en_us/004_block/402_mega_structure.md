---
navigation:
  title: "§5Celestial Forging Anvil: Mega Structures"
  icon: "anvilcraft:celestial_forging_anvil_logistics_interface"
items:
  - anvilcraft:dyson_sphere_component
  - anvilcraft:penrose_sphere_component
  - anvilcraft:wormhole_stabilizer_component
  - anvilcraft:matter_decompressor_component
  - anvilcraft:stellar_ring_component
  - anvilcraft:magnetar_coil_component
  - anvilcraft:stellar_evolution_accelerator_component
---

# Mega Structures

Mega structures extract resources from forged celestial bodies, generate power, or alter celestial body states.

# Building Mega Structures

- Forge and bind an eligible celestial body in the <ref item="anvilcraft:celestial_forging_anvil"/>, then select the target mega structure on the right side of the GUI and submit the corresponding **building materials** to complete construction.
- Normally, only one mega structure can be built on a celestial body.

<tip>
To remove a mega structure, simply unbind and rebind the planet.
</tip>

# Mega Structure Categories

- Planetary mega structures: can only be built in a celestial forging anvil without a loaded <ref item="anvilcraft:celestial_forging_anvil_amplifier"/>.
- Stellar mega structures: can only be built in a celestial forging anvil loaded with four <ref item="anvilcraft:celestial_forging_anvil_amplifier"/> and in an *amplified state*.

## Planetary Miner

- **Construction requirement**: Large satellite or rocky planet
- **Crafting method**: Select Planetary Miner on the Mega Structures page of the celestial forging anvil and submit materials.
- **Crafting materials**: <ref item="anvilcraft:ruby_prism"/> × 16
- **Input**: Level 16 [Laser](201_basic_laser.md#laser)
- **Output**: Planetary mineral resources
- **Output efficiency**: Each laser outputs 20 items/s, divided evenly among all connected <ref item="anvilcraft:celestial_forging_anvil_logistics_interface"/>.
- **Properties**:
  - Lasers may be input to <ref item="anvilcraft:celestial_forging_anvil_laser_interface"/> from up to four directions for up to 4× mining efficiency. Each interface provides only 1× efficiency, even if supplied with a level 64 laser.
  - Inputting a *Gamma Laser* will directly shatter the planet.

## Planetary Extractor

- **Construction requirement**: Rocky planet with **liquids**
- **Crafting method**: Select Planetary Extractor on the Mega Structures page of the celestial forging anvil and submit materials.
- **Crafting materials**: <ref item="anvilcraft:pump"/> × 16
- **Input**: None
- **Output**: Planetary fluid resources
- **Output efficiency**: Each connected <ref item="anvilcraft:celestial_forging_anvil_fluid_interface"/> independently outputs 5 B (buckets)/s of fluid.

## Ecological Station

- **Construction requirement**: Rocky planet with **biological resources**
- **Crafting method**: Select Ecological Station on the Mega Structures page of the celestial forging anvil and submit materials.
- **Crafting materials**: <ref item="anvilcraft:tempering_glass"/> × 64
- **Input**: Continuous power consumption of 1 MW
- **Output**: Items and fluids from the planet's biological resources
- **Output efficiency**:
  - Outputs 20 items/s, divided evenly among all connected <ref item="anvilcraft:celestial_forging_anvil_logistics_interface"/>.
  - Each connected <ref item="anvilcraft:celestial_forging_anvil_fluid_interface"/> independently outputs 5 B (buckets)/s of fluid.

## Temple

- **Construction requirement**: Rocky planet with a **primitive civilization**
- **Crafting method**: Select Temple on the Mega Structures page of the celestial forging anvil and submit materials.
- **Crafting materials**: <ref item="anvilcraft:enchanted_gold_block"/> × 64
- **Input**: Specific items given as divine blessings or punishments
- **Output**: Offerings from the civilization
- **Properties**: The Temple refreshes its item requirement once a day. After cycling through two blessings and one punishment, the civilization continuously offers until dawn on the next Minecraft day.

<tip>
Providing an item at night is not recommended, as a blessing or punishment must be provided again soon afterward, at dawn the next day.
</tip>

## Gas Giant Extractor

- **Construction requirement**: Gas giant or ice giant
- **Crafting method**: Select Gas Giant Extractor on the Mega Structures page of the celestial forging anvil and submit materials.
- **Crafting materials**: <ref item="anvilcraft:pump"/> × 32
- **Input**: None
- **Output**: Items and fluids from gas giant resources
- **Output efficiency**:
  - Outputs 20 items/s, divided evenly among all connected <ref item="anvilcraft:celestial_forging_anvil_logistics_interface"/>.
  - Each connected <ref item="anvilcraft:celestial_forging_anvil_fluid_interface"/> independently outputs 5 B (buckets)/s of fluid.
- **Properties**: Fluids must be collected before item resources can be extracted as well.

## Dyson Sphere

<recipe id="anvilcraft:dyson_sphere_component"/>

- **Construction requirement**: Brown dwarf or normal star
- **Crafting method**: Select Dyson Sphere on the Mega Structures page of the celestial forging anvil and submit materials.
- **Crafting materials**: <ref item="anvilcraft:dyson_sphere_component"/> × 8 for a brown dwarf, <ref item="anvilcraft:dyson_sphere_component"/> × 16 for a small star, or <ref item="anvilcraft:dyson_sphere_component"/> × 32 for a large star
- **Input**: None
- **Output**: Continuously generates power. Power output is positively correlated with the celestial body's *temperature* and *radius*.

### Primordial Matter Amplification

- A Dyson Sphere around a brown dwarf can consume primordial matter from a fluid interface to increase its power generation. The more it is supplied, the more power it generates—at 2 B/t, its power output reaches the maximum 5× increase.
- If a brown dwarf receives more than 2 B/t, the excess gradually converts into brown dwarf mass. After accumulating 12,800 B, the brown dwarf becomes a special red dwarf that does not require *amplifiers*.
- Some small stars with a Dyson Sphere can also consume primordial matter to increase power generation:
  - Red dwarf: a fixed 2 B/gt; power generation ×2 (including red dwarfs transformed from brown dwarfs)
  - Orange dwarf: a fixed 2 B/gt; power generation ×1.5
  - Yellow dwarf: a fixed 2 B/gt; power generation ×1.25

<tip>
Celestial bodies consume all primordial matter in <ref item="anvilcraft:celestial_forging_anvil_fluid_interface"/> at once; consider using <ref item="anvilcraft:control_valve"/> to limit the rate.
</tip>

## Stellar Ring Collider

<recipe id="anvilcraft:stellar_ring_component"/>

- **Construction requirement**: Small star
- **Crafting method**: Select Stellar Ring Collider on the Mega Structures page of the celestial forging anvil and submit materials.
- **Crafting materials**: <ref item="anvilcraft:stellar_ring_component"/> × 8
- **Input**: Continuous power consumption of 4 MW, plus collision materials and anvils through a logistics interface
- **Output**: Performs [Anvil Collision Crafting](215_large_electromagnet.md#anvil-impact-crafting).
- **Properties**:
  - The stronger the recipe star's gravity and magnetic field, the faster it operates.
  - The greater the speed required by the recipe, the slower it operates.

## Stellar Evolution Accelerator

<recipe id="anvilcraft:stellar_evolution_accelerator_component"/>

- **Construction requirement**: Star other than a white dwarf, neutron star, black hole, or special red dwarf
- **Crafting method**: Select Stellar Evolution Accelerator on the Mega Structures page of the celestial forging anvil and submit materials.
- **Crafting materials**: <ref item="anvilcraft:stellar_evolution_accelerator_component"/> × 8
- **Input**: None
- **Effect**: Accelerates stellar evolution. When a star reaches the end of its life, it becomes a white dwarf or triggers a supernova explosion and becomes a neutron star or black hole.
- **Properties**: Can coexist with other stellar mega structures.

### Supernova Explosion

A supernova explosion destroys all mega structures on the celestial body and causes a massive explosion that reaches over ten blocks away.

<info>
During acceleration, if a Dyson Sphere exists, it collects **infinite electrical energy** during the main-sequence phase and is destroyed during the giant phase.
</info>

## Magnetar Coil

<recipe id="anvilcraft:magnetar_coil_component"/>

- **Construction requirement**: Neutron star
- **Crafting method**: Select Magnetar Coil on the Mega Structures page of the celestial forging anvil and submit materials.
- **Crafting materials**: <ref item="anvilcraft:magnetar_coil_component"/> × 4
- **Input**: None
- **Output**: Continuously generates power. Power output is positively correlated with the celestial body's *magnetic field strength* and *rotation speed*.

## Wormhole Stabilizer

<recipe id="anvilcraft:wormhole_stabilizer_component"/>

- **Construction requirement**: Black hole in an amplified state
- **Crafting method**: Select Wormhole Stabilizer on the Mega Structures page of the celestial forging anvil and submit materials.
- **Crafting materials**: <ref item="anvilcraft:wormhole_stabilizer_component"/> × 4
- **Input**: Identical black hole
- **Output**: [Wormhole](402_teleportation.md)

## Penrose Sphere

<recipe id="anvilcraft:penrose_sphere_component"/>

- **Construction requirement**: Black hole
- **Crafting method**: Select Penrose Sphere on the Mega Structures page of the celestial forging anvil and submit materials.
- **Crafting materials**: <ref item="anvilcraft:penrose_sphere_component"/> × 8
- **Input**: Laser
- **Output**: Same-level [Gamma Laser](../001_feature/402_gamma_laser.md)

Penrose Sphere laser inputs and outputs must be grouped on the left and right sides of the same side of the celestial forging anvil. Laser inputs and outputs on the four sides are independent, and the middle interface on a side cannot be used.

![Penrose Sphere.png](../../textures/cfa/gama.png)

> Producing [Gamma Laser](../001_feature/402_gamma_laser.md)

## Matter Decompressor

<recipe id="anvilcraft:matter_decompressor_component"/>

- **Construction requirement**: Neutron star or black hole
- **Crafting method**: Select Matter Decompressor on the Mega Structures page of the celestial forging anvil and submit materials.
- **Crafting materials**: <ref item="anvilcraft:matter_decompressor_component"/> × 2
- **Input**: *Gamma Laser*; each Gamma Laser level provides 1× working efficiency
- **Output**:
  - Neutron stars are extracted once every 10 seconds, most often producing 1 <ref item="anvilcraft:neutronium_ingot"/>. With sufficiently high magnetic field strength, there is a small chance to produce <ref item="anvilcraft:charged_neutronium_ingot"/>—the chance is positively correlated with magnetic field strength.
  - Black holes are extracted once per gametick, most often producing 1 <ref item="anvilcraft:void_matter"/>. With sufficiently high magnetic field strength, there is a small chance to produce <ref item="anvilcraft:excited_state_void_matter"/>—the chance is positively correlated with magnetic field strength.
