---
navigation:
  title: "§6Large Crate"
  icon: "anvilcraft:large_crate"
items:
  - anvilcraft:large_crate
  - anvilcraft:local_terminal
---

# <ref item="anvilcraft:large_crate"/>

> super~big~crate~

<item id="anvilcraft:large_crate"/>

- Obtained through [multi-block conversion](210_giant_anvil.md#function).

## Function

- Holds 1024 stacks of items.
- Partial stacks can share the same stack space, similar to a bundle.

<info>
Container blocks provided by AnvilCraft have similar GUIs, and offer features such as a [crafting window](003_crate.md#crafting-window)
</info>

<tip>
Hold a <ref item="anvilcraft:large_crate"/> and Shift-right-click to replace a 3x3x3 area of <ref item="anvilcraft:crate"/>s
</tip>

> Can be upgraded to a <ref item="anvilcraft:shulker_container"/>

# Terminal

<row halign="center">
<item id="anvilcraft:local_terminal"/>
</row>

## Quick Open

- Set a custom keybind to quickly open a carried *terminal*

## Hover Window

- Can bind to a *storage station* for remote access.
- When carrying a *terminal*, use JEI's "+" button in any GUI to move items for quick crafting directly through the terminal.
- After binding a *storage station*, hovering over a *terminal* in another GUI displays a hover window:
  - With an empty hand, use the mouse wheel to select an item and Shift-left-click to withdraw it.
  - While holding an item, right-click to insert it.
  - Hold Shift while scrolling to scroll by row.
  - Hold Ctrl while scrolling to scroll by page.
  - Hold Alt to zoom in.

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
