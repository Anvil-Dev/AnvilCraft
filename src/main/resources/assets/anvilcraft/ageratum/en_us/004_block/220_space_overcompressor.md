---
navigation:
  title: "§6Space Overcompressor"
  icon: "anvilcraft:space_overcompressor"
items:
  - anvilcraft:nesting_shulker_box
  - anvilcraft:over_nesting_shulker_box
  - anvilcraft:supercritical_nesting_shulker_box
  - anvilcraft:space_overcompressor
---

# Space Overcompressor

<row halign="center">
<item id="anvilcraft:nesting_shulker_box"/>
<item id="anvilcraft:over_nesting_shulker_box"/>
<item id="anvilcraft:supercritical_nesting_shulker_box"/>
<item id="anvilcraft:space_overcompressor"/>
</row>

# Space Overcompressor

## Crafting

<row halign="center">
<recipe id="anvilcraft:item_inject/nesting_shulker_box"/>
<recipe id="anvilcraft:item_inject/over_nesting_shulker_box"/>
<recipe id="anvilcraft:item_inject/supercritical_nesting_shulker_box"/>
<recipe id="anvilcraft:block_compress/space_overcompressor"/>
</row>

## Function

- Produces <ref item="anvilcraft:neutronium_ingot"/>:
  - Place any metal block/ingot/nugget on top of <ref item="anvilcraft:space_overcompressor"/>
  - Strike with an anvil to convert it into mass value
  - When <ref item="anvilcraft:space_overcompressor"/> has accumulated enough mass value, it outputs one <ref item="anvilcraft:neutronium_ingot"/> below
- Participates in [multi-block crafting](210_giant_anvil.md#function)

---

# Nesting Shulker Boxes

## Properties

- Inherits shulker box properties; will be destroyed by pistons
- Right-clicking the three types of nesting shulker boxes emits 1, 2, and 3 opening/closing sounds respectively, each recognized by <ref item="minecraft:observer"/>
- Has a capacity of 27/54/108 item groups; cannot be manually stored, can only be interacted with by logistics blocks
- Can be unpacked back into <ref item="minecraft:shulker_box"/>

<row halign="center">
<recipe id="anvilcraft:stamping/shulker_box_from_nesting_shulker_box"/>
<recipe id="anvilcraft:stamping/shulker_box_from_over_nesting_shulker_box"/>
<recipe id="anvilcraft:stamping/shulker_box_from_supercritical_nesting_shulker_box"/>
</row>