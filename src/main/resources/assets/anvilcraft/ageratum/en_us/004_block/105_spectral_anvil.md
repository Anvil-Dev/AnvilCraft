---
navigation:
  title: "§2Spectral Anvil"
  icon: "anvilcraft:spectral_anvil"
items:
  - anvilcraft:spectral_anvil
---

# Spectral Anvil

<row halign="center">
<item id="anvilcraft:spectral_anvil"/>
</row>

# Production
[An anvil falling into an end portal](../001_feature/002_end_portal.md) has a chance to become <ref item="anvilcraft:spectral_anvil"/>:
- <ref item="minecraft:damaged_anvil"/>: 1%
- <ref item="minecraft:chipped_anvil"/>: 2%
- <ref item="minecraft:anvil"/>: 3%
- <ref item="anvilcraft:royal_anvil"/>: 50%
- <ref item="anvilcraft:ember_anvil"/>: 100%
- <ref item="anvilcraft:transcendence_anvil"/>: 100%

<row>
<recipe id="anvilcraft:portal_conversion/anvil"/>
<recipe id="anvilcraft:portal_conversion/chipped_anvil"/>
<recipe id="anvilcraft:portal_conversion/damaged_anvil"/>
</row>

<row>
<recipe id="anvilcraft:portal_conversion/royal_anvil"/>
<recipe id="anvilcraft:portal_conversion/frost_anvil"/>
<recipe id="anvilcraft:portal_conversion/ember_anvil"/>
<recipe id="anvilcraft:portal_conversion/transcendence_anvil"/>
</row>

# Properties
- Not affected by gravity
- Can be pushed by pistons
- Does not take damage when used
- When the <ref item="anvilcraft:magnet_block"/> above it disappears or loses magnetism, it drops a **phantom image** downward
- The **phantom image**, regardless of how far it falls onto an object, always has the impact equivalent to an anvil falling from 2 blocks high

# Differences from Anvils
- Cannot perform [Anvil Looting](../001_feature/000_anvil_loot.md)
- Produces fewer charges when striking <ref item="anvilcraft:piezoelectric_crystal"/>