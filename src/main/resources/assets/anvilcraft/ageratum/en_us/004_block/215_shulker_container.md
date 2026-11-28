---
navigation:
  title: "§6Shulker Container"
  icon: "anvilcraft:shulker_container"
items:
  - anvilcraft:shulker_container
  - anvilcraft:storage_port
  - anvilcraft:shulker_terminal
  - anvilcraft:storage_fluid_port
  - anvilcraft:storage_port_consolidator
---

# <ref item="anvilcraft:shulker_container"/>

## Crafting

Smash 1 <ref item="anvilcraft:space_overcompressor"/> and 6 <ref item="minecraft:netherite_block"/>s into a <ref item="anvilcraft:large_crate"/> from above to upgrade it to a <ref item="anvilcraft:shulker_container"/>.

## Function

- Holds up to 1024 item types, each with an independent storage space of 1024 stacks.
- When broken, the dropped Shulker Container preserves its contents.
- Smash a <ref item="anvilcraft:space_overcompressor"/> into a <ref item="anvilcraft:shulker_container"/> to double its type limit and storage capacity, up to four times.
- Items cannot be automatically inserted into or extracted from it by blocks such as <ref item="minecraft:hopper"/>s. Use a <ref item="anvilcraft:storage_port"/> instead.

<info>
Container blocks provided by AnvilCraft have similar GUIs, and offer features such as a [crafting window](003_crate.md#crafting-window)
</info>

<info>
A Shulker Container item cannot be stored in a Shulker Container or a storage port
</info>

# Automated Storage Access

## <ref item="anvilcraft:storage_port"/>

<recipe id="anvilcraft:storage_port"/>

- Must be placed adjacent to a <ref item="anvilcraft:shulker_container"/> or another <ref item="anvilcraft:storage_port"/>.
- Supports automatic input through blocks such as <ref item="minecraft:hopper"/>.
- Mark an item to automatically insert and extract it.
- With an item marked, players can left-click to withdraw it or right-click to insert it.
- Hold right-click with an <ref item="anvilcraft:anvil_hammer"/> to open a wheel menu and select the option to clear the mark.

## <ref item="anvilcraft:storage_fluid_port"/>

<recipe id="anvilcraft:storage_fluid_port"/>

- Similar to a <ref item="anvilcraft:storage_port"/>, but handles fluid interaction.
- Has a capacity of 128 B (128,000 mB), expanding the fluid storage of a *storage station*; its contents can be viewed and used in the *storage station* GUI.
- Adjusts its own *head* to drain or draw in the stored fluid, keeping its fill level between 50% and 75%.
- Pouring in fluid with a bucket automatically marks that fluid; hold right-click with an <ref item="anvilcraft:anvil_hammer"/> to clear the mark.
- When a fluid bucket is placed into the GUI, the storage system automatically splits it into fluid and an empty bucket, and does the reverse when extracting.

<tip>
Right-click the port with a filled bucket to store the fluid directly
</tip>

## <ref item="anvilcraft:storage_port_consolidator"/>

- Similar to a *Drawer Manager*, it combines the contents of all connected <ref item="anvilcraft:storage_port"/>s and <ref item="anvilcraft:storage_fluid_port"/>s and exposes the combined contents to external logistics.
- *Connection* rule: connections cannot pass through a Shulker Container or *storage station*; they can only extend through adjacent *storage ports* (including *storage fluid ports*).
- When placed directly against a <ref item="anvilcraft:shulker_container"/> or <ref item="anvilcraft:hyperdimension_storage_station"/>, external logistics can extract items from the storage directly; inserted items first enter a buffer and are then gradually stored.

# Terminal

<recipe id="anvilcraft:shulker_terminal"/>

- Automatically connects to the first <ref item="anvilcraft:shulker_container"/> in the player's inventory.
- If the player is not carrying a <ref item="anvilcraft:shulker_container"/>, automatically connects to the nearest one within 64 blocks as a *storage station*.
