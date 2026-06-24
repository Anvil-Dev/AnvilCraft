---
navigation:
  title: "Sliding Rail System"
  icon: "anvilcraft:sliding_rail"
items:
  - anvilcraft:sliding_rail
  - anvilcraft:sliding_rail_stop
  - anvilcraft:powered_sliding_rail
  - anvilcraft:activator_sliding_rail
  - anvilcraft:detector_sliding_rail
---

# Sliding Rail System

<row halign="center">
<item id="anvilcraft:sliding_rail"/>
<item id="anvilcraft:sliding_rail_stop"/>
<item id="anvilcraft:powered_sliding_rail"/>
<item id="anvilcraft:activator_sliding_rail"/>
<item id="anvilcraft:detector_sliding_rail"/>
</row>

# <ref item="anvilcraft:sliding_rail"/>

<recipe id="anvilcraft:sliding_rail"/>

- Similar to minecart rails, sliding rails are used to transport *items* and *blocks*, allowing them to slide continuously
- When a *block* is pushed onto a sliding rail by a piston, it will continue sliding on the rail
    - If the *block* is attached to other *blocks* moving together (e.g. <ref item="minecraft:slime_block"/>), they slide as a whole
- Can be used for boating

> This mod provides [mass production methods](../008_recipe/110_ice.md) for various types of ice

# <ref item="anvilcraft:sliding_rail_stop"/>

<recipe id="anvilcraft:sliding_rail_stop"/>

- Extremely high friction
  - Sliding *items* stop at its center
  - Sliding *blocks* stop on top of it
- Can hold mobs in place

# <ref item="anvilcraft:powered_sliding_rail"/>

<recipe id="anvilcraft:powered_sliding_rail"/>

- When <color=#999922>not receiving a redstone signal</color>, behaves the same as <ref item="anvilcraft:sliding_rail_stop"/>
- When <color=#999922>receiving a redstone signal</color>,
  - Makes *entities* and *blocks* above it slide in the facing direction
  - When the back of <ref item="anvilcraft:powered_sliding_rail"/> touches <ref item="anvilcraft:sliding_rail_stop"/>, the *entities* and *blocks* on <ref item="anvilcraft:sliding_rail_stop"/> are transferred to <ref item="anvilcraft:powered_sliding_rail"/> and moved forward
## Properties

- The logic for converting items to sliding state is the same as <ref item="minecraft:piston"/>:
  - Can move structures of up to 12 blocks
  - Some blocks are immovable (e.g. <ref item="minecraft:chest"/>)
  - Destroys some blocks (e.g. <ref item="minecraft:shulker_box"/>)


# <ref item="anvilcraft:activator_sliding_rail"/>

<recipe id="anvilcraft:activator_sliding_rail"/>

- When <color=#999922>not receiving a redstone signal</color>, behaves the same as <ref item="anvilcraft:sliding_rail"/>
- When <color=#999922>receiving a redstone signal</color>:
  - When a sliding *block* passes over it, the block pauses briefly on top and receives a pulse signal
  - If a stationary *block* is above it, continuously sends a signal to it

# <ref item="anvilcraft:detector_sliding_rail"/>

<recipe id="anvilcraft:detector_sliding_rail"/>

- When *items* and *blocks* slide over it, emits redstone signals in five directions around it (excluding above)
- Can be detected by <ref item="minecraft:comparator"/>, outputting a redstone signal based on the number of blocks in the sliding *block's* structure
> e.g.: When four blocks stuck together pass over, <ref item="minecraft:comparator"/> emits signal strength 4