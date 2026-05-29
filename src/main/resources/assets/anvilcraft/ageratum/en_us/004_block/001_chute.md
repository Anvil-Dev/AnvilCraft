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

## Function

- <ref item="anvilcraft:chute"/> is a special type of hopper with 9 slots of capacity, transporting one stack of items at a time
- Can both input items into containers and drop items into the world
- Opening the GUI allows you to:
  - View inventory, change output direction, and set filters
  - Use the scroll wheel on slots to set item limits

> More filtering can be set via <ref item="anvilcraft:filter"/>

## Properties

- Can be locked by redstone signal
- When <ref item="anvilcraft:chute"/> forms a chain, the chute being pointed at becomes a **Simple Chute**

<block id="anvilcraft:simple_chute"/>

## Simple Chute

- Does not pull items
- Holds only 1 stack of items
- Cannot set filters
- Not controlled by redstone signals

# Magnetic Chute

<recipe id="anvilcraft:magnetic_chute"/>

## Function

- Has all the functions of <ref item="anvilcraft:chute"/>
- <ref item="anvilcraft:magnetic_chute"/> can pull items from the sides and even below, but cannot freely adjust output

## Properties

- If outputting items into the world, it ejects items at a certain velocity
- Does not become a **Simple Chute**

