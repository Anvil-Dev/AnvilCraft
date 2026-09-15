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

# Offhand Function

- Active in the offhand; whether charged or not, grants +5 block interaction range and +3 entity interaction range

# Main Hand Functions

## Fill Mode

Enabled when holding a *block* in the offhand

- Right-click to confirm the first position, keep holding, then release at another position to confirm the second; attempts to fill the selected cuboid with the offhand block
- Each placed block consumes 100 FE, up to 4,000 blocks per placement

### Placement Behavior

- Automatically draws items from the player's inventory; a *terminal* such as <ref item="anvilcraft:local_terminal"/> also allows items to be drawn from a *storage station*
- Cancels placement when materials are insufficient instead of placing only part of the selection
- Places at most 4,000 blocks at a time

<tip>
When placing a large block, make sure the selected area is larger than the block's volume
</tip>

## Blueprint Mode

Enabled when holding a <ref item="anvilcraft:structure_disk"/> with a saved structure in the offhand

1. Right-click while holding the rod to lock the projection; left-click to cancel
2. Adjust the projection with the controls:
  - ↑, ←, ↓, →, PgUp, and PgDn move it
  - + and - rotate it
  - \ mirrors it
3. Right-click again to attempt placement

- Cancels placement when materials are insufficient instead of placing only part of the blueprint; holding Shift builds only the portions for which materials are available

## Extra Behavior

<tip>
When filling a large area, the placement completes even if the remaining power is insufficient to fill every block. This is intended behavior, not a bug
</tip>
