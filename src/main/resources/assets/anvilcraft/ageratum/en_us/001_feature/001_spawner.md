---
navigation:
  title: "Forced Spawner Activation"
  icon: "minecraft:spawner"
---

# Forced Spawner Activation

<row halign="center">
<item id="minecraft:spawner"/>
</row>

# Feature

When an anvil hits a <ref item="minecraft:spawner"/>, it will immediately attempt to spawn a mob.

<structure id="../structures/spawner.snbt"/>

- The anvil's drop height h determines the spawn probability p: **p = 1 - 1/h** (higher heights yield higher probability)
- Does *not* require **players nearby** (make sure the chunk is loaded)
- Requires **meeting the spawner's extra conditions for spawning certain mobs** (e.g., light level)
- Requires **the number of nearby monsters to be below the cap**, so quickly transporting or killing nearby mobs will greatly improve efficiency
