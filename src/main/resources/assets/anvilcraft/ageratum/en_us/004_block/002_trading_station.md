---
navigation:
  title: "Trading Station"
  icon: "anvilcraft:trading_station"
items:
  - anvilcraft:trading_station
---

# <ref item="anvilcraft:trading_station"/>

<recipe id="anvilcraft:trading_station"/>

# Function

1. In the **GUI left trading panel**, place the resources you wish to trade away in the left slot, and the resources you want to acquire in the right slot
2. Use the **button below the trading panel** to set whether *other players* (completed) or *villagers* (incomplete) can use it
3. Use the **button to the right below the trading panel** to set whether auto input/output is allowed
4. **Other players** holding a sufficient amount of the item the **owner** wants can right-click to trade once

# Safety

- Only the owner can open the GUI
- When broken or when a block is placed nearby by another player, the owner receives an alert message
- If destroyed by a non-player, the message also includes the IDs of players online at the time and the nearest player

# Villager Trading (Incomplete)

After working, villagers will try to interact with trading stations that match their trade list, as long as the villager can **accept** the station's offer

<info>
It is known that villagers buy 10 clay balls for 1 emerald. If the trading station offers 10 or more clay balls and requests 1 or fewer emeralds, the trade can be completed
</info>
