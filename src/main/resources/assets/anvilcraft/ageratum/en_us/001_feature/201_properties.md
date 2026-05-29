---
navigation:
  title: "§6Properties"
  icon: "minecraft:enchanted_book"
---

# Properties

This page showcases all currently available special tool properties and their effects


# Reforging

- Comes from [Ember Metal Tools](../002_material/211_ember_metal.md)  
- Tools can recover durability in high temperatures  

| Block (Fluid) | Durability restored/gt |
|--------|----------|
| Fire      | 2        |
| Soul Fire    | 5        |
| Lava     | 10       |
| Lava Cauldron  | 10       |

---

# Ruthless

- Comes from [Frost Metal Tools](../002_material/202_frost_metal.md)  
- Weapons and tools with **Ruthless** will have all enchantments disabled except *Durability*, *Mending*, *Riptide*, and *Loyalty*
- The levels of disabled enchantments are converted into attack damage and mining speed  
  - Attack Damage: 2*√n + n/3  
  - Mining Speed: n
---

# Multiphase

Tools with this property will have two "phases"  
Each phase stores its own name and enchantments, which do not conflict  
You can switch between phases by pressing <Key id="key.anvilcraft.switch_phase" />

---

# Eternal

- Comes from [Transcendium Tools](../002_material/312_transcendium.md)
Tools with the **Eternal** property will:  
  - No longer consume durability, always remaining at full durability (also gains the vanilla Unbreakable tag as a safety measure)  
  - Be immune to fire, explosions, and cactus damage; will not fall into the void; slowly floats upward in areas where y < Ymin+5  
  - Never despawn over time  

---

# Fortune's Favor

- Comes from [Transcendium Tools](../002_material/312_transcendium.md)
- When a tool with **Fortune's Favor** triggers a whitelisted enchantment, there is a 25% chance to trigger it again, and a 5% chance to trigger it twice more (dropping three heads at once or catching three fish at once is possible)  
  - Currently supported enchantments: Fortune, Looting, Beheading, Thorns, Luck of the Sea
