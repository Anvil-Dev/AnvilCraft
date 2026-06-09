---
navigation:
  title: "§2Smart Block Placer"
  icon: "anvilcraft:smart_block_placer"
items:
  - anvilcraft:smart_block_placer
---

# Smart Block Placer

<recipe id="anvilcraft:smart_block_placer"/>

# Range Placement

- <ref item="anvilcraft:smart_block_placer"/> takes items from the container, entity inventory, or dropped items behind it
- Can customize block placement within a 5x5x5 area in front
- Places one block every 1 second
- Continuously consumes 8kW of power

<info>
In normal mode there is no filtering; <ref item="anvilcraft:smart_block_placer"/> will place all items until there are no empty spots in range or no items available
</info>

# Move Mode

- Activated using the second button from the bottom right
- In this mode, <ref item="anvilcraft:smart_block_placer"/> attempts to directly place the block behind it at the selected position

<info>
The block must be movable by pistons, so bedrock and certain containers will not be moved
</info>

# Blueprint Mode

- Insert a <ref item="anvilcraft:structure_disk"/>; <ref item="anvilcraft:smart_block_placer"/> will place blocks according to the blueprint
- Power consumption increases to 64kW

<warning>
Cannot place blueprints larger than 5x5x5
</warning>

# Signal Control

- Stops working when receiving a redstone signal
- Can be detected by a comparator: outputs no signal when no blocks have been placed, outputs signal strength 15 when placement is complete, and linearly outputs signal strength 0-15 based on progress during placement