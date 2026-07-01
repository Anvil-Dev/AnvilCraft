---
navigation:
  title: "Chute"
  icon: "anvilcraft:chute"
items:
  - anvilcraft:chute
  - anvilcraft:magnetic_chute
---

# Chute

<recipe id="anvilcraft:chute"/>

- <ref item="anvilcraft:chute"/> is a special type of <ref item="minecraft:hopper"/> with 9 slots of capacity, transporting one stack of items at a time
- Can both input items into containers and drop items into the world
- Can be locked by redstone signal
- Opening the GUI allows you to:
  - View inventory, change output direction, and set filters
  - Use the scroll wheel on slots to set item limits

<tip>
More filtering can be set via <ref item="anvilcraft:filter"/>
</tip>

# Magnetic Chute

<recipe id="anvilcraft:magnetic_chute"/>

## Function

- Has all the functions of <ref item="anvilcraft:chute"/>
- <ref item="anvilcraft:magnetic_chute"/> can pull items from the sides and even below, but cannot freely adjust output
- If outputting items into the world, it ejects items at a certain velocity

# Simple Chute

- When a <ref item="anvilcraft:chute"/> is pointed at by a <ref item="anvilcraft:chute"/> or <ref item="anvilcraft:magnetic_chute"/>, it becomes a **Simple Chute**
- When a <ref item="anvilcraft:magnetic_chute"/> is pointed at by a <ref item="anvilcraft:magnetic_chute"/>, it becomes a **Simple Magnetic Chute**

<row>
<block id="anvilcraft:simple_chute"/>
<block id="anvilcraft:simple_magnetic_chute"/>
</row>

- Does not pull items
- Holds only 1 stack of items
- Cannot set filters
- Not controlled by redstone signals
