---
navigation:
  title: "§6Large Electromagnet"
  icon: "anvilcraft:acceleration_ring"
items:
  - anvilcraft:acceleration_ring
  - anvilcraft:deflection_ring
---

# Large Electromagnet

<row halign="center">
<item id="anvilcraft:acceleration_ring"/>
<item id="anvilcraft:deflection_ring"/>
</row>

# <ref item="anvilcraft:acceleration_ring"/>

- Obtained through [multi-block conversion](210_giant_anvil.md#function)
- Consumes 256kW of power when active; disabled by redstone signal
- When placed facing upward and active, attracts <ref item="anvilcraft:giant_anvil"/> within 12 blocks. Must not be blocked by any blocks in between
- Two active acceleration rings facing the same direction form an *acceleration interval*

## Acceleration Interval

Accelerates the following blocks or entities along the track:

- Anvils (excluding giant anvils and spectral anvils)
- Projectiles
- Players (equipped with at least two armor pieces and wearing an anvil hammer on the head)

# <ref item="anvilcraft:deflection_ring"/>

- Obtained through [multi-block conversion](210_giant_anvil.md#function)
- Consumes 256kW of power when active; disabled by redstone signal
- When placed vertically and active, attracts <ref item="anvilcraft:giant_anvil"/> within 12 blocks. Must not be blocked by any blocks in between
- When active, deflects the movement of entities accelerated earlier, rotating their direction 90 degrees along the direction indicated by the **arrow outside the ring**

# Anvil Impact Crafting

Using the power of large electromagnets, build a ring accelerator. Make anvils collide with specific blocks at high speed to create entirely new substances

<tip>
Using vanilla mechanics like stacked TNT acceleration, water blast protection, and precise redstone timing, you can create an anvil accelerator that requires no electricity, though it is quite challenging
</tip>

<structure id="../structures/accelerate_machine.snbt"/>

## Impact Chamber

- The impact produces a large explosion; an impact chamber made of blast-proof blocks is necessary

<structure id="../structures/collision_room.snbt"/>

## Speed Control

- <ref item="minecraft:comparator"/> can read the speed (unit: m/tick) of anvils passing through <ref item="anvilcraft:deflection_ring"/> and output a redstone signal
- Output formula: Redstone Signal = 2*[(log base 2 of speed) - 1] rounded up, final value clamped to 0-15

> 32 < V <= 45.25 : Strength 9

> 64 < V <= 90.51 : Strength 11

> 128 < V <= 181.02 : Strength 13

> V > 256 : Strength 15

<warning>
Anvils will turn into items after moving for 30 seconds; pay attention to the running time
</warning>

1. After detecting that the anvil has reached a certain speed, apply a redstone signal to <ref item="anvilcraft:deflection_ring"/> to disable it
2. The anvil can then fly out of the accelerator and enter the impact chamber
3. Don't forget to use an anvil to strike <ref item="anvilcraft:block_placer"/> beforehand so it places the target block through the wall
4. Use <ref item="anvilcraft:item_collector"/> to collect through the wall

## Crafting Details

- Impact recipes consume the target block and require the anvil to reach a certain speed; some impacts do not consume the anvil
- Detailed recipes will be mentioned when introducing crafting methods for new materials

---

## Related

- <ref item="anvilcraft:uranium_ingot"/>
- <ref item="anvilcraft:negative_matter"/>
- <ref item="anvilcraft:multiphase_matter"/>
- [Overheated Ember Metal Block](../001_feature/301_overheated_block.md)