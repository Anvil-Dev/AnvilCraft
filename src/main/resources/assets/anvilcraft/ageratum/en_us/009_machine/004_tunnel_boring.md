---
navigation:
  title: "Resource: Tunnel Boring Machine"
  icon: "minecraft:piston"
---

# Resource: Tunnel Boring Machine

Tunnel Boring Machine: A machine that can destroy blocks ahead of it while continuously advancing itself, making it convenient for creating tunnels.

<ref item="anvilcraft:block_devourer"/> can quickly and precisely destroy a large area of blocks ahead, making it suitable for building a *Tunnel Boring Machine*.

# Simple TBM (3x3)

Here is an example of a simple *Tunnel Boring Machine*:

- Activate <ref item="anvilcraft:block_devourer"/> with redstone to continuously destroy blocks in a 3x3 area ahead
- Can be used to build transportation routes or mine in the excavated tunnels

## Diagram

<structure id="../../structures/machine/tunnel_boring_3x.snbt"/>

<tip>
Left-click to adjust position; right-click to adjust angle; PgUp/PgDn to adjust display height
</tip>

1. When building the structure, the redstone block should be placed last
2. Place a storage minecart on the rail
3. Update the <ref item="minecraft:sticky_piston"/> to start the machine
4. To stop, dismantle the machine

- All <ref item="minecraft:smooth_stone"/> can be replaced with any full opaque block

# Large TBM (7x7)

By controlling the anvil's fall onto the <ref item="anvilcraft:block_devourer"/>, you can excavate larger tunnels.

An anvil dropped from a height of 2 blocks will cause the <ref item="anvilcraft:block_devourer"/> it hits to excavate a 7x7 area ahead.

Here is a simple approach to excavate a spacious 7x7 cross-section tunnel.

## Diagram

<structure id="../../structures/machine/tunnel_boring_7x.snbt"/>

<tip>
Left-click to adjust position; right-click to adjust angle; PgUp/PgDn to adjust display height
</tip>


1. Place a storage minecart on the rail
2. After building, activate the *observer* at the very back
3. Clear the two layers of blocks below the machine to prevent slime blocks from sticking
4. Update the <ref item="minecraft:sticky_piston"/> to start the machine
5. To stop, dismantle the machine

- All <ref item="minecraft:smooth_stone"/> can be replaced with any full opaque block
