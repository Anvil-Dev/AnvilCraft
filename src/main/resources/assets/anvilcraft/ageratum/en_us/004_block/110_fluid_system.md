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
---

# Storing Flow Within

<recipe id="anvilcraft:fluid_tank"/>

- <ref item="anvilcraft:fluid_tank"/> can hold 16B of fluid

# Flowing Downstream

<recipe id="anvilcraft:pipe"/>

- <ref item="anvilcraft:pipe"/> can transfer fluids
- Affected by gravity, transfers fluid from higher containers to lower containers
- The greater the height difference, the faster the transfer speed, providing 50mB/gt per block, with a maximum speed of 2000mB/gt

<structure id="../../structures/gravity_pipe.nbt"/>

## <ref item="minecraft:cauldron"/> Support

- Pipes support <ref item="minecraft:cauldron"/>, but <ref item="minecraft:cauldron"/> is special because it has layered (250mB) or full cauldron (1000mB) states, requiring the pipe to input a sufficient amount of fluid in 1gt to successfully inject, i.e.: input speed > 250mB/t or input speed > 1000mB/t

# Flowing Upstream

<recipe id="anvilcraft:pump"/>

- <ref item="anvilcraft:pump"/> consumes 32kW of power
- Can be shut down with a redstone signal
- Has a *head* of 20 blocks, pumping fluids upward
- Multiple <ref item="anvilcraft:pump"/>s can be connected in series to stack the *head*

<structure id="../../structures/pump.nbt"/>

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
