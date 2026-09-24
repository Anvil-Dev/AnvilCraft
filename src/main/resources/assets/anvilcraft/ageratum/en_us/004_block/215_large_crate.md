---
navigation:
  title: "§6Large Crate"
  icon: "anvilcraft:large_crate"
items:
  - anvilcraft:large_crate
  - anvilcraft:shulker_container
  - anvilcraft:storage_port
  - anvilcraft:local_terminal
  - anvilcraft:shulker_terminal
  - anvilcraft:storage_fluid_port
  - anvilcraft:storage_port_consolidator
---

# <ref item="anvilcraft:large_crate"/>

> super~big~crate~

<item id="anvilcraft:large_crate"/>

- Obtained through [multi-block conversion](210_giant_anvil.md#function).

## Function

- Holds 1024 stacks of items.
- Partial stacks can share the same stack space, similar to a bundle.

<tip>
Hold a <ref item="anvilcraft:large_crate"/> and Shift-right-click to replace a 3x3x3 area of <ref item="anvilcraft:crate"/>s.
</tip>

# <ref item="anvilcraft:shulker_container"/>

## Crafting

Smash 1 <ref item="anvilcraft:space_overcompressor"/> and 6 <ref item="minecraft:netherite_block"/>s into a <ref item="anvilcraft:large_crate"/> from above to upgrade it.

## Function

- Holds about 1024 item types, each stored independently with a capacity of 1024 stacks.
- Breaking it preserves its contents.
- Smash <ref item="anvilcraft:space_overcompressor"/> into a <ref item="anvilcraft:shulker_container"/> to double its item types and capacity, up to four times.
- It cannot be automatically inserted into or extracted from by blocks such as <ref item="minecraft:hopper"/>. Use a <ref item="anvilcraft:storage_port"/> instead.

<tip>
Container blocks provided by AnvilCraft have similar GUIs with features such as a [crafting window](003_crate.md#crafting-window).
</tip>

<info>
A <ref item="anvilcraft:shulker_container"/> cannot be placed into a <ref item="anvilcraft:shulker_container"/> or its storage ports
</info>

# Automated Storage Access

## <ref item="anvilcraft:storage_port"/>

<recipe id="anvilcraft:storage_port"/>

- Must be placed adjacent to a <ref item="anvilcraft:shulker_container"/> or another <ref item="anvilcraft:storage_port"/>.
- Supports automatic input through blocks such as <ref item="minecraft:hopper"/>.
- Mark an item to automatically input and output that item.
- With an item marked, players can left-click to withdraw it or right-click to insert it.
- Hold right-click with an <ref item="anvilcraft:anvil_hammer"/> to remove the item mark.

## <ref item="anvilcraft:storage_fluid_port"/>

<recipe id="anvilcraft:storage_fluid_port"/>

- Similar to a <ref item="anvilcraft:storage_port"/>, but handles fluid interaction
- Has a capacity of 128 B (128,000 mB), expanding the fluid storage of a *storage station*; its contents can be viewed and used in the *storage station* GUI
- Adjusts its own head lift to drain or draw in the stored fluid, keeping its fill level between 50% and 75%
- When a fluid bucket is inserted, the storage system automatically converts it into the fluid plus an empty bucket, and does the reverse when extracting it

<tip>
Right-click to insert while holding a fluid bucket to keep the fluid intact
</tip>

## <ref item="anvilcraft:storage_port_consolidator"/>

- Similar to a *Drawer Manager*, it combines the contents of all connected <ref item="anvilcraft:storage_port"/>s and <ref item="anvilcraft:storage_fluid_port"/>s and exposes the combined contents to external logistics
- *Connection* rule: connections cannot pass through a container or *storage station*; they can only extend through adjacent *storage ports* (including *storage fluid ports*)

# Terminals

<row halign="center">
<item id="anvilcraft:local_terminal"/>
<item id="anvilcraft:shulker_terminal"/>
</row>

## Quick Open

- Set a custom keybind to quickly open a carried *terminal*

## Hover Window

- Can bind to a *storage station* for remote access.
- When carrying a *terminal*, use JEI's "+" button in any GUI to move items for quick crafting directly through the terminal.
- After binding a *storage station*, hovering over a *terminal* in another GUI displays a hover window:
  - With an empty hand, use the mouse wheel to select an item and Shift-left-click to withdraw it.
  - While holding an item, right-click to insert it.
  - Use Shift to scroll by row.
  - Use Ctrl to scroll by page.
  - Use Alt to zoom in.

## Smart Restocking

- Hold Alt to select a restocking mode: Smart (two-way), Restock Only, Store Only, or Off.
- Restock: when the held item is used up, attempts to withdraw the same item to replenish a full stack.
- Store: when picking up items, or by another non-active acquisition method such as an empty bucket after placing water, causes an item to exceed one stack, keeps one stack in the inventory and automatically stores the excess in the *storage station*. This excludes items actively taken from container GUIs.

<tip>
When using the <ref item="anvilcraft:building_rod"/> while carrying a *terminal*, items can be drawn automatically from a *storage station*
</tip>

## <ref item="anvilcraft:local_terminal"/>

<recipe id="anvilcraft:local_terminal"/>

- Automatically connects to the nearest <ref item="anvilcraft:large_crate"/> within 32 blocks as a *storage station*.

## <ref item="anvilcraft:shulker_terminal"/>

<recipe id="anvilcraft:shulker_terminal"/>

- Automatically connects to the first <ref item="anvilcraft:shulker_container"/> in the player's inventory.
- If the player is not carrying a <ref item="anvilcraft:shulker_container"/>, automatically connects to the nearest one within 64 blocks as a *storage station*.
