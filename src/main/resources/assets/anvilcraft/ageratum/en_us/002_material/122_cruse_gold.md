---
navigation:
  title: "§2Cursed Gold"
  icon: "anvilcraft:cursed_gold_ingot"
categories:
  - misc ingredients blocks
items:
  - anvilcraft:cursed_gold_block
  - anvilcraft:cursed_gold_ingot
  - anvilcraft:cursed_gold_nugget
---

# Cursed Gold

<row halign="center">
<item id="anvilcraft:cursed_gold_block"/>
<item id="anvilcraft:cursed_gold_ingot"/>
<item id="anvilcraft:cursed_gold_nugget"/>
</row>

<gradient start="#ff5544" end="#bbaa55">Cursed by forbidden knowledge</gradient>

# Acquisition

Use a Gold Ingot (Block) on a <ref item="anvilcraft:royal_grindstone"/> to remove curse enchantments or enchantment penalties to obtain it

# Functions

- Crafting <ref item="anvilcraft:corrupted_beacon"/>
- Piglins that pick up Cursed Gold will be zombified, and drop extra Cursed Gold upon death

<row halign="center" valign="center">
<entity id="minecraft:piglin" />
+ 
<item id="anvilcraft:cursed_gold_ingot"/>
=
<entity id="minecraft:zombified_piglin" />
</row>

<row halign="center" valign="center">

Kill
<entity id="minecraft:zombified_piglin" />
=
<item id="anvilcraft:cursed_gold_ingot"/>
+
<item id="anvilcraft:cursed_gold_nugget"/>
</row>

<tip>
If you're interested, you can consider automating Cursed Gold production this way
</tip>

# Properties

- When the player carries Cursed Gold series items in their inventory (whether nuggets, ingots, or blocks), they receive negative effects based on quantity (effects last for 10s after no longer holding the items)
  - 1 to 8: Weakness II
  - 9 to 64: Weakness II + Slowness II
  - 65 and above: Weakness II + Slowness II + Hunger II
