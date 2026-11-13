---
navigation:
  title: "Energy: Piezoelectric Crystal Generator"
  icon: "anvilcraft:piezoelectric_crystal"
---

# Simple Piezoelectric Generator

This is probably your first generator. It uses simple materials and can be built in the early game, even if the power output is not high.

Output: 4kW per anvil; 32kW total

You can further expand upon this foundation.

<structure id="../../structures/machine/piezoelectric_crystal_0.snbt"/>

1. Set the pulse generator to loop mode: emit a 5gt signal every 5gt interval, controlling the anvil drop

- All <ref item="minecraft:smooth_stone"/> can be replaced with any full opaque block
- All <ref item="minecraft:anvil"/> can be replaced with any anvil

## Advanced Piezoelectric Generator

You can also improve your first generator's efficiency with a few tricks:

Output: 10kW per anvil; 80kW total

<structure id="../../structures/machine/piezoelectric_crystal_0.nbt"/>

1. This design uses the boat-impact trick and 0t pulses to make the magnet switch rapidly

- When an anvil hits a boat within 1 block of the ground, it lands instantly. A flower pot keeps the boat at just the right height within that range.
- A pulse generator emitting a 0gt signal every 4gt makes the magnet switch as fast as possible.

<tip>
You can use these tricks elsewhere to improve your machines' efficiency.
</tip>

# Royal Anvil Downward-Bounce Piezoelectric Generator

- As you progress further, the power from your first generator will likely become insufficient. It's time to upgrade.
- An anvil falling from a height will penetrate through multiple layers of piezoelectric crystals, generating more power. <ref item="anvilcraft:royal_anvil"/> does not break when falling from great heights, making it the perfect choice.
- Although a higher fall generates more power, it also takes longer to fall. Is there a way to make the anvil fall faster? The answer is to bounce it downwards using <ref item="minecraft:slime_block"/>.

Output: 15kW per anvil; 150kW total

<structure id="../../structures/machine/piezoelectric_crystal_1.snbt"/>

<tip>
Resin blocks are as bouncy as slime blocks, but do not stick to adjacent blocks.
</tip>

## Advanced Royal Anvil Downward-Bounce Piezoelectric Generator

Precise timing can greatly improve power output. The machine above is an older design to look over; this is the practical one.

Output: 75kW per anvil; 300kW total

<structure id="../../structures/machine/piezoelectric_crystal_1.nbt"/>

<tip>
You can lengthen the machine to increase its power output.
</tip>
