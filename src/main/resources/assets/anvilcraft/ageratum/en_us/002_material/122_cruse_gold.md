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
  - anvilcraft:cursed_golden_apple
---

# Cursed Gold

> Metal cursed by forbidden knowledge; can be used to craft <ref item="anvilcraft:corrupted_beacon"/>.

<row halign="center">
<item id="anvilcraft:cursed_gold_block"/>
<item id="anvilcraft:cursed_gold_ingot"/>
<item id="anvilcraft:cursed_gold_nugget"/>
</row>

# Acquisition

Use a Gold Ingot (or Gold Block) on a <ref item="anvilcraft:royal_grindstone"/> to remove its curse enchantments or enchantment penalties and obtain Cursed Gold.

# Piglin Curse

> Piglins that pick up Cursed Gold become Zombified Piglins and drop extra Cursed Gold when they die

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

# Player Curse

- When the player's inventory contains any *Cursed Gold* items, using a <ref item="anvilcraft:royal_anvil"/> causes them to be struck by lightning.
- When the player's inventory contains any *Cursed Gold* items, they receive negative effects based on the quantity; the effects disappear 10s after they no longer carry any of these items.
  - 1 to 8: Weakness II
  - 9 to 64: Weakness II + Slowness II
  - 65 or more: Weakness II + Slowness II + Hunger II

# <ref item="anvilcraft:cursed_golden_apple"/>

<recipe id="anvilcraft:cursed_golden_apple"/>

- Eating it in the Overworld ~~goes to hell~~ teleports you to the Nether; eating it in the Nether returns you to the Overworld, similar to a Nether portal.
- Eating it in the End returns you to your respawn point, similar to the End fountain.
