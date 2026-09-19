---
navigation:
  title: "§2Building Rod"
  icon: "anvilcraft:building_rod"
items:
  - anvilcraft:building_rod
---

# Building Rod

<recipe id="anvilcraft:building_rod"/>

# Energy Tool

- Can be charged with a <ref item="anvilcraft:capacitor"/> or <ref item="anvilcraft:charger"/>
- Stores up to 8 MFE

# Passive Effect

- Active while carried; whether charged or not, grants +3 block and entity interaction range

# Active Functions

## Fill Mode

Enabled when the <ref item="anvilcraft:building_rod"/> is held in one hand and a *block* is held in the other.

- Right-click to confirm the first position; keep holding right-click and release at another position to confirm the second position, then attempt to fill the selected cuboid with the block from the other hand
- Each placed block consumes 100 FE, up to 4,000 blocks per placement

<tip>
When placing infinite fluids such as water, at most two buckets of water are consumed at a time
</tip>

### Placement Behavior

- Automatically uses items from the player's inventory
- Cancels placement when materials are insufficient instead of placing only part of the selection
- Places at most 4,000 blocks at a time

## Blueprint Mode

Enabled when the <ref item="anvilcraft:building_rod"/> is held in one hand and a <ref item="anvilcraft:structure_disk"/> with a **saved structure** is held in the other.

1. Hold Ctrl to fix the projection distance, then right-click to lock the projection
2. Adjust the projection with the controls:
  - ↑, ←, ↓, →, PgUp, and PgDn move it
  - **-** and **=** rotate it
  - \ mirrors it
  - Left-click: cancel placement
3. Right-click again to attempt placement

- Cancels placement when materials are insufficient instead of placing only part of the blueprint; holding Shift builds only the portions for which materials are available

<info>
If the blueprint is missing materials and you are carrying a book, a material list is written into the book
</info>

<info>
If you prefer not to adjust it with keybinds, you can configure scroll-wheel adjustment in the settings (config)
</info>

## Extra Behavior

<tip>
When filling a large area, the placement completes even if the remaining power is insufficient to fill every block. This is intended behavior, not a bug
</tip>

<tip>
Press Ctrl+Z to undo the last placement
</tip>