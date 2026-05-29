---
navigation:
  title: "Magnet"
  icon: "anvilcraft:magnet_ingot"
items:
  - anvilcraft:magnet_ingot
  - anvilcraft:magnet_block
  - anvilcraft:hollow_magnet_block
  - anvilcraft:ferrite_core_magnet_block
---

# Magnet
<row halign="center">
<item id="anvilcraft:magnet_ingot"/>
<item id="anvilcraft:magnet_block"/>
<item id="anvilcraft:hollow_magnet_block"/>
<item id="anvilcraft:ferrite_core_magnet_block"/>
</row>

# First Acquisition
You need to use a <ref item="minecraft:lightning_rod"/> to attract lightning,
converting an <ref item="minecraft:iron_block"/> via lightning strike into <ref item="anvilcraft:hollow_magnet_block"/>


<tip>
Right-click a <ref item="minecraft:lightning_rod"/> with <ref item="anvilcraft:topaz"/> to consume the <ref item="anvilcraft:topaz"/> and immediately generate a lightning bolt
</tip>

The default lightning-to-magnet block conversion range is as follows:

<structure id="../structures/lightning_convert_magnets.snbt"/>

<row halign="center">
<recipe id="anvilcraft:magnet_ingot_from_hollow_block"/>
<recipe id="anvilcraft:hollow_magnet_block"/>
</row>

# Magnetization
- Right-click <ref item="anvilcraft:hollow_magnet_block"/> with <ref item="minecraft:iron_ingot"/>, or craft, to obtain <ref item="anvilcraft:ferrite_core_magnet_block"/>
- Place <ref item="anvilcraft:ferrite_core_magnet_block"/> in the world; it will slowly transform into <ref item="anvilcraft:magnet_block"/>, converting the <ref item="minecraft:iron_ingot"/> into <ref item="anvilcraft:magnet_ingot"/>
- You can craft, or right-click with an empty hand to extract <ref item="anvilcraft:magnet_ingot"/>

<row halign="center">
<recipe id="anvilcraft:ferrite_core_magnet_block"/>
<recipe id="anvilcraft:magnet_ingot_from_block"/>
<recipe id="anvilcraft:magnet_block"/>
</row>

# Magnetic Effect
- <ref item="anvilcraft:magnet_block"/>, <ref item="anvilcraft:ferrite_core_magnet_block"/>, and <ref item="anvilcraft:hollow_magnet_block"/> can attract various anvils within 5 blocks directly below
- Demagnetizes when receiving a redstone signal
- Repeatedly activating and deactivating can lift and drop the anvil repeatedly

# Electromagnetic Effect
Once you have power generation capability, use <ref item="anvilcraft:charger"/> to charge <ref item="minecraft:iron_ingot"/> into <ref item="anvilcraft:magnet_ingot"/>

<recipe id="anvilcraft:charger_charging/magnet_ingot"/>
