---
navigation:
  title: "§5Overheated State"
  icon: "anvilcraft:overheated_ember_metal_block"
items:
  - anvilcraft:overheated_ember_metal_block
---

# Overheated State

<row halign="center">
<item id="anvilcraft:ember_metal_block"/>
<item id="anvilcraft:overheated_ember_metal_block"/>
</row>

---

# Prerequisites:

- [Thermal System](../001_feature/101_heated_block.md)
- [Anvil Collision Crafting](../004_block/215_large_electromagnet.md#铁砧撞击合成)

<warning>
If you skip the prerequisites, you won't understand this chapter
</warning>

---

# <ref item="anvilcraft:ember_metal_block"/>

- <ref item="anvilcraft:ember_metal_block"/> also counts as a *heatable block* and has 2 temperature levels:
  - <color=#666666>Normal</color>
  - <color=#6688cc>Overheated</color>
- Cannot be heated by conventional methods, but can be heated by the methods described below
- Very unstable, typically only maintaining its state for a short time. After cooling, there is a 5% chance of turning into <ref item="minecraft:netherite_block"/>
- Can provide 1024kW of energy to <ref item="anvilcraft:heat_collector"/>
- Burns mobs standing on it, evaporates water within a certain distance

# Overheating

<row halign="center">
<recipe id="anvilcraft:anvil_collision/anvil_tier_2_and_uranium_block_256"/>
<recipe id="anvilcraft:anvil_collision/anvil_tier_2_and_plutonium_block_256"/>
</row>

- Consume <ref item="anvilcraft:uranium_block"/> in a collision, heating up to 16 Ember Metal Blocks, lasting up to 20s
- Consume <ref item="anvilcraft:plutonium_block"/> in a collision, heating up to 16 Ember Metal Blocks, lasting up to 60s
