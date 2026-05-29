---
navigation:
  title: "Anvil Mining"
  icon: "minecraft:anvil"
---

# Anvil Mining

# Definition

Anvil Mining is a concept. The following behaviors all implement this concept and therefore share common properties.

# Implementation

- Destroy a single block via [Stonecutter + Anvil](../007_struct/000_block_processing.md)
- Destroy blocks in an area via [Anvil-triggered Block Devourer](../004_block/101_block_devourer.md)
- Destroy blocks in a large area via [Giant Anvil Ground Pound](../004_block/210_giant_anvil.md)

# Effects

Using different anvils to destroy blocks yields different effects:

<row>
<item id="minecraft:anvil"/>
<item id="anvilcraft:spectral_anvil"/>
<item id="anvilcraft:royal_anvil"/>
<item id="anvilcraft:ember_anvil"/>
<item id="anvilcraft:transcendence_anvil"/>
</row>

- <ref item="minecraft:anvil"/>: Normal mining
- <ref item="anvilcraft:spectral_anvil"/>: Normal mining
- <ref item="anvilcraft:royal_anvil"/>: Silk Touch
- <ref item="anvilcraft:frost_anvil"/>: [Disintegration](100_enchantment.md#崩解)
- <ref item="anvilcraft:ember_anvil"/>: [Smelting](100_enchantment.md#熔炼)
- <ref item="anvilcraft:transcendence_anvil"/>: Fortune V
