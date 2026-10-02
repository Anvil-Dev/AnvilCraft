---
navigation:
  title: "§6Tesla Tower"
  icon: "anvilcraft:tesla_tower"
items:
  - anvilcraft:tesla_tower
---

# Tesla Tower

<recipe id="anvilcraft:tesla_tower"/>

- Consumes 128 kW of power when working; stops working when power is insufficient
- Can be disabled by redstone signal

# Function

- Every 4 seconds, zaps the nearest mob or <ref item="minecraft:lightning_rod"/> within a radius of 8 blocks
  - When attacking mobs, it can chain to up to 4 mobs (including chaining beyond the electric shock radius), dealing 40 / 30 / 20 / 10 damage in sequence
- An allowlist can be set in the GUI