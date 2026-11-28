---
navigation:
  title: "§6Energy Weapon Platform"
  icon: "anvilcraft:energy_weapon_platform"
items:
  - anvilcraft:energy_weapon_platform
  - anvilcraft:spectral_weapon_launcher
  - anvilcraft:anvil_railgun
  - anvilcraft:corrupted_beacon_activator
  - anvilcraft:tesla_gun
  - anvilcraft:laser_gun
---

<row halign="center">
<item id="anvilcraft:energy_weapon_platform"/>
<item id="anvilcraft:spectral_weapon_launcher"/>
<item id="anvilcraft:anvil_railgun"/>
<item id="anvilcraft:corrupted_beacon_activator"/>
<item id="anvilcraft:tesla_gun"/>
<item id="anvilcraft:laser_gun"/>
</row>

# <ref item="anvilcraft:energy_weapon_platform"/>

<recipe id="anvilcraft:energy_weapon_platform"/>

- Can store 320MJ of energy (=640MFE)
- Open it to insert materials for modification, transforming it into various powerful *energy weapons*

# Energy Weapons

- *Energy weapons* can be charged in <ref item="anvilcraft:charger"/>, or consume <ref item="anvilcraft:capacitor"/> and <ref item="anvilcraft:supercapacitor"/> from your inventory
- Most **ranged weapon enchantments** and **weapon enchantments** work on *energy weapons*, providing powerful boosts

<info>
If the first item in the recipe has enchantments, the finished product inherits them
</info>

# <ref item="anvilcraft:spectral_weapon_launcher"/>

<recipe id="anvilcraft:energy_weapon_make/spectral_weapon_launcher"/>

- Functions identically to <ref item="anvilcraft:spectral_slingshot"/>, but deals 100% damage (<ref item="anvilcraft:spectral_slingshot"/> deals 50%)

# <ref item="anvilcraft:anvil_railgun"/>

<recipe id="anvilcraft:energy_weapon_make/anvil_railgun"/>

1. Used like a <ref item="minecraft:bow"/>, but fires anvils as ammo
2. The Quick Charge enchantment shortens the charging time

<info>
Against large mobs, it may deal both impact damage and anvil landing damage consecutively
</info>

# <ref item="anvilcraft:corrupted_beacon_activator"/>

<recipe id="anvilcraft:energy_weapon_make/corrupted_beacon_activator"/>

- Hold right-click to continuously fire a corrupted beam up to 64 blocks, dealing *Time Warp damage* to all mobs it contacts and granting up to Wither V
- Quick Charge enchantment increases damage frequency

# <ref item="anvilcraft:tesla_gun"/>

<recipe id="anvilcraft:energy_weapon_make/tesla_gun"/>

- Right-click to fire an arc, dealing 40 points of *lightning damage* to mobs
- Can chain to nearby mobs, but reduces damage by 10 per bounce
- Has a 4s attack cooldown, which can be reduced by Quick Charge enchantment (5gt per level, up to 60gt)

# <ref item="anvilcraft:laser_gun"/>

<recipe id="anvilcraft:energy_weapon_make/laser_gun"/>

## Mining Mode

- Hold right-click to beam ores, mining entire ore veins
- Collected minerals are deposited directly into your inventory
- All mining enchantments apply

## Attack Mode

- Keep the beam on the same mob to build charge. The attack bar fills over 20 seconds, and each quarter of the bar raises the damage and the beam strength by one level.
- While you hold right-click and aim away, the attack bar loses one quarter every second. Aim at the same mob to resume from the remaining charge. Switching to another mob or releasing right-click resets the charge.
- When you mine ore with attack charge remaining, the upper bar shows attack charge and the lower bar shows mining progress.
- Loot from killed mobs is deposited directly into your inventory

| Duration   | DPS | Power Consumption | Extra Effects            |
|--------|-----------|--------|-----------------|
| Up to 5s | 12        | 200kW  |                 |
| 6-10s  | 28        | 400kW  |                 |
| 11-15s | 60        | 800kW  |                 |
| 16-20s | 124       | 1600kW | Starts overheating, inflicting fire damage to the user |
| 21s and beyond | 252       | 3200kW | Starts overheating, inflicting lava damage to the user |

<warning>
Even if damage resets, the fire or lava damage to the user persists for an additional 5s
</warning>
