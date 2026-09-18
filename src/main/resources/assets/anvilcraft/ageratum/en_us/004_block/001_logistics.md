---
navigation:
  title: "Logistics Components"
  icon: "anvilcraft:chute"
items:
  - anvilcraft:chute
  - anvilcraft:magnetic_chute
---

# <ref item="anvilcraft:chute"/>

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

# <ref item="anvilcraft:magnetic_chute"/>

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

## Storage Characteristics

- Does not pull items
- Holds only 1 stack of items
- Does not merge items: it rejects new input while its slot is occupied, waits for the item to be transferred out, and only then accepts the next batch, reducing buffering in the pipeline system

## Control Characteristics

- Cannot set filters
- Not controlled by redstone signals

# <ref item="anvilcraft:overflow_chute"/>

<recipe id="anvilcraft:overflow_chute"/>

- No GUI
- Holds only 1 stack of items
- Right-click a side with an <ref item="anvilcraft:anvil_hammer"/> to open or close the corresponding overflow port; when the main output is blocked, items are distributed as evenly as possible through the overflow ports

# <ref item="anvilcraft:item_splitter"/>

<recipe id="anvilcraft:item_splitter"/>

- No GUI
- Holds up to 16 stacks of items, but only one item type at a time
- When there is a container in front, it automatically distributes its contents evenly among the **connected** containers ahead, up to 16 blocks away
- When there is no container in front, an *anvil* strike makes it divide the items according to the fall height and throw them into unobstructed spaces ahead (they do not need to be connected), up to 16 blocks away

<structure id="../../structures/item_splitter.nbt"/>

<info>
- When distributing into open space, obstacles are skipped by continuing forward until the required number of shares is reached or the 16-block limit is reached
- For example, if an anvil falls from three blocks high and the first block ahead is obstructed, the items are distributed into the second, third, and fourth blocks
- The maximum distance is 16 blocks; any portion that cannot be distributed remains inside the Item Splitter and is not included in that distribution
</info>
