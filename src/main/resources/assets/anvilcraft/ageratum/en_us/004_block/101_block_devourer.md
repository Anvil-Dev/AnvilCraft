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
Killing the Ender Dragon with Decapitation III enchantment guarantees a 100% dragon head drop
</info>

# Function

- <ref item="anvilcraft:block_devourer"/> destroys blocks within a certain range in front when receiving a redstone signal or when struck by an anvil
- Drops attempt to enter the container or entity inventory behind the devourer; if impossible, they drop in place
- When activated by redstone, destruction range is 3x3
- When struck by an anvil, depending on fall height of 1, 2, or 3, the ranges are 5x5, 7x7, and 9x9 respectively
- When struck by an anvil, it is an implementation of [Anvil Mining](../001_feature/000_anvil_destroy.md)

# Properties

- Can be pushed and pulled by pistons
- World matrix blocks such as <ref item="minecraft:stone"/>, <ref item="minecraft:netherrack"/>, etc. have a very low drop rate
- A cheaper block destroyer can use [Anvil + Stonecutter](../007_struct/000_block_processing.md)