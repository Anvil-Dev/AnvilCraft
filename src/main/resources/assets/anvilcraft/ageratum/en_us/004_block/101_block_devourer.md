---
navigation:
  title: "§2Block Devourer"
  icon: "anvilcraft:block_devourer"
items:
  - anvilcraft:block_devourer
---

# Block Devourer

<recipe id="anvilcraft:block_devourer"/>

<info>
Killing the Ender Dragon with Beheading III enchantment guarantees a 100% dragon head drop
</info>

# Function

- <ref item="anvilcraft:block_devourer"/> destroys blocks within a certain range in front when receiving a redstone signal or when struck by an anvil
- Drops attempt to enter the container or entity inventory behind the devourer; if impossible, they drop in place
- When struck by an anvil, it is an implementation of [Anvil Mining](../001_feature/000_anvil_destroy.md)

| Activation Method | Destruction Range |
|--------------|------|
| Redstone signal | 3x3 |
| Anvil falling from 1 block high | 5x5 |
| Anvil falling from 2 blocks high | 7x7 |
| Anvil falling from 3 or more blocks high | 9x9 |

# Properties

- Can be pushed and pulled by pistons
- World matrix blocks such as <ref item="minecraft:stone"/>, <ref item="minecraft:netherrack"/>, etc. have a very low drop rate
- A cheaper block destroyer can use [Anvil + Stonecutter](../007_struct/000_block_processing.md)