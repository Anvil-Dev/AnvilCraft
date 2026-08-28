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
---

# <ref item="anvilcraft:large_crate"/>

> super~big~crate~

<item id="anvilcraft:large_crate"/>

- Obtained through [multi-block conversion](210_giant_anvil.md#function).

## Function

- Holds 1024 stacks of items.
- Partial stacks can share the same stack space, similar to a bundle.

# <ref item="anvilcraft:shulker_container"/>

## Crafting

Smash 1 <ref item="anvilcraft:space_overcompressor"/> and 6 <ref item="minecraft:netherite_block"/>s into a <ref item="anvilcraft:large_crate"/> from above to upgrade it.

## Function

- Holds 1024 stacks of items.
- Breaking it preserves its contents.
- Smash <ref item="anvilcraft:space_overcompressor"/> into a <ref item="anvilcraft:shulker_container"/> to double its capacity, up to four times.
- It cannot be automatically inserted into or extracted from by blocks such as <ref item="minecraft:hopper"/>. Use a <ref item="anvilcraft:storage_port"/> instead.

# <ref item="anvilcraft:storage_port"/>

<recipe id="anvilcraft:storage_port"/>

- Must be placed adjacent to a <ref item="anvilcraft:shulker_container"/> or another <ref item="anvilcraft:storage_port"/>.
- Supports automatic input through blocks such as <ref item="minecraft:hopper"/>.
- Mark an item to automatically input and output that item.
- With an item marked, players can left-click to withdraw it or right-click to insert it.
- Hold right-click with an <ref item="anvilcraft:anvil_hammer"/> to remove the item mark.

# Terminals

<row halign="center">
<item id="anvilcraft:local_terminal"/>
<item id="anvilcraft:shulker_terminal"/>
</row>

## Hover Window

- Can bind to a *storage station* for remote access.
- When carrying a *terminal*, use JEI's "+" button in any GUI to move items for quick crafting directly through the terminal.
- After binding a *storage station*, hovering over a *terminal* in another GUI displays a hover window:
  - With an empty hand, use the mouse wheel to select an item and left-click to withdraw it.
  - While holding an item, right-click to insert it.

## Smart Restocking

- Hold Alt to select a restocking mode: Smart (two-way), Restock Only, Store Only, or Off.
- Restock: when the held item is used up, attempts to withdraw the same item to replenish a full stack.
- Store: when picking up items, or by another non-active acquisition method such as an empty bucket after placing water, causes an item to exceed one stack, keeps one stack in the inventory and automatically stores the excess in the *storage station*. This excludes items actively taken from container GUIs.

## <ref item="anvilcraft:local_terminal"/>

<recipe id="anvilcraft:local_terminal"/>

- Automatically connects to the nearest <ref item="anvilcraft:large_crate"/> within 32 blocks as a *storage station*.

## <ref item="anvilcraft:shulker_terminal"/>

<recipe id="anvilcraft:shulker_terminal"/>

- Automatically connects to the first <ref item="anvilcraft:shulker_container"/> in the player's inventory.
- If the player is not carrying a <ref item="anvilcraft:shulker_container"/>, automatically connects to the nearest one within 64 blocks as a *storage station*.
