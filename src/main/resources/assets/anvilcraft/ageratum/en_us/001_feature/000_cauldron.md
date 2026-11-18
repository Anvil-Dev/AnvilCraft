---
navigation:
  title: "Cauldron"
  icon: "minecraft:cauldron"
---

# Cauldron Concept

- The cauldron concept originates from vanilla's <ref item="minecraft:cauldron"/>
- Can interact with <ref item="minecraft:glass_bottle"/> and <ref item="minecraft:bucket"/> to insert and extract fluids
- In this mod, it is commonly used with an anvil to [process](../007_struct/000_anvil_processing.md) items, fluids, and blocks

# <ref item="minecraft:cauldron"/>

- The most basic cauldron
- Items are stored inside as dropped items
- Stores fluids in discrete layers, so fluids cannot be inserted or extracted at 1mB precision like most modded fluid containers; each transfer must be a whole number of layers
- (Added by this mod) Right-clicking the side with an <ref item="anvilcraft:anvil_hammer"/> creates an output port that automatically exports products

<info>
The vanilla lava cauldron layer is 1B; this mod changes it to 250mB
</info>

<info>
## Layers
Using the modified lava cauldron as an example, the cauldron can contain 250mB, 500mB, 750mB, or 1B of lava, but never 1mB of lava
</info>

# <ref item="anvilcraft:fish_tank"/>

- An easier-to-use cauldron
- Stores items permanently like a chest
- Stores fluids at 1mB precision, but can still hold only one type of fluid
- Right-clicking the side with an <ref item="anvilcraft:anvil_hammer"/> creates an output port that automatically exports products

# <ref item="anvilcraft:large_cauldron"/>

- A very large cauldron
- Stores items permanently like a chest
- Stores fluids at 1mB precision and can hold multiple types of fluids
- Has no output port and follows complex input and output rules
