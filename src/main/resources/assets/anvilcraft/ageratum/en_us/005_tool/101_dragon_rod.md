---
navigation:
  title: "§2Dragon Rod"
  icon: "anvilcraft:dragon_rod"
items:
  - anvilcraft:dragon_rod
  - anvilcraft:royal_dragon_rod
  - anvilcraft:frost_dragon_rod
  - anvilcraft:ember_dragon_rod
  - anvilcraft:transcendence_dragon_rod
---

# Dragon Rod

<recipe id="anvilcraft:dragon_rod"/>

## Functions

Hold a <ref item="anvilcraft:dragon_rod"/> and left-click to break blocks within a certain range; right-click to toggle the range size.

| Range | Durability Cost |
|------|-------|
| 3x3  | 0     |
| 5x5  | 1     |
| 7x7  | 2     |
| 9x9  | 4     |

<info>
When the <ref item="anvilcraft:dragon_rod"/>'s durability is depleted, it does not break completely but loses all functionality, similar to <ref item="minecraft:elytra"/>
</info>

### Transcendence Dragon Rod

- The first devour starts a 10-tick startup cooldown. Repeat the devour action within 15 ticks to warm it up; after that, while the attack key remains held, the server continuously devours the block under the crosshair each player tick
- Release the attack key, switch away from the rod, or enter a cooldown to stop continuous devouring; an invalid target simply skips that tick
- Hold [Left Alt] to open a two-option wheel for this rod: protect containers from devouring or allow containers to be devoured. This setting is stored on each rod

### When Breaking

- The <ref item="anvilcraft:dragon_rod"/> follows <ref item="anvilcraft:block_devourer"/> rules: when mining world base blocks (**Stone**, **Netherrack**, **End Stone**), there is only a 5% chance of dropping. However, it cannot chain-gather falling blocks from the top
- After mining once, the Dragon Rod has a cooldown period, defaulting to 1 second. This cooldown is only affected by the *Haste* and *Mining Fatigue* effects: each level of Haste reduces it by 4 ticks, each level of Mining Fatigue increases it by 1 second

# Related

<row halign="center">
<item id="anvilcraft:royal_dragon_rod"/>
<item id="anvilcraft:frost_dragon_rod"/>
<item id="anvilcraft:ember_dragon_rod"/>
<item id="anvilcraft:transcendence_dragon_rod"/>
</row>

<ref item="anvilcraft:dragon_rod"/> can be upgraded with higher-tier materials

- [Royal Steel Tools](../002_material/110_royal_steel.md)
- [Frost Metal Tools](../002_material/202_frost_metal.md)
- [Ember Metal Tools](../002_material/211_ember_metal.md)
- [Transcendium Tools](../002_material/312_transcendium.md)