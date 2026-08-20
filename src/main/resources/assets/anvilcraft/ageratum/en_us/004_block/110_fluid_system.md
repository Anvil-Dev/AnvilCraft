---
navigation:
  title: "§2Fluid System"
  icon: "anvilcraft:fluid_tank"
items:
  - anvilcraft:fluid_tank
  - anvilcraft:pipe
  - anvilcraft:pump
  - anvilcraft:drain
  - anvilcraft:control_valve
  - anvilcraft:check_valve
  - anvilcraft:fluid_tank_minecart
---

# Storing Flow Within

<recipe id="anvilcraft:fluid_tank"/>

- <ref item="anvilcraft:fluid_tank"/> can hold 16B of fluid
- When broken, the item form retains the fluid

# Flowing Downstream

<recipe id="anvilcraft:pipe"/>

- <ref item="anvilcraft:pipe"/> can transfer fluids
- Affected by gravity, transfers fluid from higher containers to lower containers
- The greater the height difference, the faster the transfer speed, providing 50mB/gt per block, with a maximum speed of 2000mB/gt

<structure id="../../structures/gravity_pipe.nbt"/>

## Glass Viewport

- Hold a <ref item="minecraft:glass_pane"/> and right-click <ref item="anvilcraft:pipe"/> to view the fluid inside the pipe

## <ref item="minecraft:cauldron"/> Support

- Pipes support <ref item="minecraft:cauldron"/>, but <ref item="minecraft:cauldron"/> is special, only allowing a full bucket (1000mB) of fluid to be inserted or extracted at once

# Flowing Upstream

<recipe id="anvilcraft:pump"/>

- <ref item="anvilcraft:pump"/> consumes 32kW of power
- Can be shut down with a redstone signal
- Has a *head* of 20 blocks, pumping fluids upward
- Multiple <ref item="anvilcraft:pump"/>s can be connected in series to stack the *head*

<structure id="../../structures/pump.nbt"/>

## Minecart

<recipe id="anvilcraft:unpack/fluid_tank_minecart"/>

If you have no power, try using the <ref item="anvilcraft:fluid_tank_minecart"/> to store and transport fluids.

# Discharging Flow Outside

<recipe id="anvilcraft:drain"/>

- <ref item="anvilcraft:drain"/> can discharge input fluids downward
- Can also be used in reverse, drawing fluids from **above**
- For same-level fluids, <ref item="anvilcraft:drain"/> does nothing; however, if the same-level fluid can form an infinite fluid source on its own, the drain will continuously be filled by that fluid

<structure id="../../structures/drain.nbt"/>

> Q: Xiao Ming is pumping fluid into a 100m³ pool at 7L/s while draining at 5L/s. How long until the pool is full?

# Master of Flow

<row>
<recipe id="anvilcraft:control_valve"/>
<recipe id="anvilcraft:check_valve"/>
</row>

- <ref item="anvilcraft:control_valve"/> restricts fluid *type* and *flow rate*, **disconnects** when receiving a redstone signal
- <ref item="anvilcraft:check_valve"/> allows fluid to flow in only one direction, **reverses** when receiving a redstone signal
