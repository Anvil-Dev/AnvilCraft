---
navigation:
  title: "§2Fluid System"
  icon: "anvilcraft:fluid_tank"
items:
  - anvilcraft:fluid_tank
  - anvilcraft:pipe
  - anvilcraft:pump
---

# <ref item="anvilcraft:fluid_tank"/>

- Can hold 16B of fluid

# <ref item="anvilcraft:pipe"/>

<recipe id="anvilcraft:pipe"/>

- Can transfer fluids
- Affected by gravity, transfers fluid from higher containers to lower containers
- The greater the height difference, the faster the transfer speed, providing 50mB/gt per block, with a maximum speed of 2000mB/gt

<structure id="../../structures/gravity_pipe.nbt"/>

## Pipe Node

<block id="anvilcraft:pipe_node"/>

- When a pipe is connected on 3 or more sides, it automatically becomes a *node*

# <ref item="anvilcraft:pump"/>

<recipe id="anvilcraft:pump"/>

- Consumes 32kW of power
- Can be shut down with a redstone signal
- When working, applies a *head* of 10 blocks to the fluid
- Multiple <ref item="anvilcraft:pump"/>s can be connected in series to stack the *head*

<structure id="../../structures/pump.nbt"/>

# <ref item="minecraft:cauldron"/> Support

- Pipes support <ref item="minecraft:cauldron"/>
- However, <ref item="minecraft:cauldron"/> is special because it has layered (250mB) or full cauldron (1000mB) states, requiring the pipe to input a sufficient amount of fluid in 1gt to successfully inject
