---
navigation:
  title: "Energy: Piezoelectric Crystal Generator"
  icon: "anvilcraft:piezoelectric_crystal"
---

# Simple Piezoelectric Generator

This is probably your first generator. It uses simple materials and can be built in the early game, even if the power output is not high.

Output: 4kW per anvil; 32kW total

You can further expand upon this foundation.

## Diagram

<structure id="../../structures/machine/piezoelectric_crystal_0.snbt"/>

<tip>
Left-click to adjust position; right-click to adjust angle; PgUp/PgDn to adjust display height
</tip>

1. Set the pulse generator to loop mode: emit a 5gt signal every 5gt interval, controlling the anvil drop

- All <ref item="minecraft:smooth_stone"/> can be replaced with any full opaque block
- All <ref item="minecraft:anvil"/> can be replaced with any anvil

# Royal Anvil Downward-Bounce Piezoelectric Generator

- As you progress further, the power from your first generator will likely become insufficient. It's time to upgrade.
- An anvil falling from a height will penetrate through multiple layers of piezoelectric crystals, generating more power. <ref item="anvilcraft:royal_anvil"/> does not break when falling from great heights, making it the perfect choice.
- Although a higher fall generates more power, it also takes longer to fall. Is there a way to make the anvil fall faster? The answer is to bounce it downwards using <ref item="minecraft:slime_block"/>.

Output: 15kW per anvil; 150kW total

## Diagram

<structure id="../../structures/machine/piezoelectric_crystal_1.snbt"/>

<tip>
Left-click to adjust position; right-click to adjust angle; PgUp/PgDn to adjust display height
</tip>

1. Set the *pulse generator* next to the piston to rising edge mode: 3gt delay, emit a 3gt signal
2. Set the farther *pulse generator* to loop mode: emit a 9gt signal every 1gt interval

- All <ref item="minecraft:smooth_stone"/> can be replaced with any full opaque block
