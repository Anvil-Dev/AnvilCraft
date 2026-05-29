---
navigation:
  title: "§2Energy Conversion"
  icon: "anvilcraft:power_converter_big"
items:
  - anvilcraft:power_converter_small
  - anvilcraft:power_converter_middle
  - anvilcraft:power_converter_big
---

# Energy Conversion

<row halign="center">
<item id="anvilcraft:power_converter_small"/>
<item id="anvilcraft:power_converter_middle"/>
<item id="anvilcraft:power_converter_big"/>
</row>

# Energy Conversion

AnvilCraft's electrical power is fundamentally different from other mods' energy systems, so it can only be used by other mods after conversion

# Energy Converters

## Crafting

<row halign="center">
<recipe id="anvilcraft:power_converter_big"/>
<recipe id="anvilcraft:stonecutting/power_converter_middle"/>
<recipe id="anvilcraft:stonecutting/power_converter_small_from_big"/>
</row>
<row halign="center">
<recipe id="anvilcraft:power_converter_middle_from_small"/>
<recipe id="anvilcraft:power_converter_big_from_middle"/>
<recipe id="anvilcraft:power_converter_big_from_small"/>
</row>

## Function

- Converts AnvilCraft power unidirectionally to FE/RF/AE energy
- Conversion has a 10% loss
- Different sizes convert different amounts of energy, see the table below for details


|                                       Energy Converter                                       | Consumption (kW) | Equivalent Energy (FE/t) | Converted Energy After Loss (FE/t) |
|:-------------------------------------------------------------------------------------------:|:----------------:|:------------------------:|:----------------------------------:|
| <ref item="anvilcraft:power_converter_small"/>  |        1         |            80            |                 72                 |
| <ref item="anvilcraft:power_converter_middle"/> |        6         |           480            |                432                 |
|  <ref item="anvilcraft:power_converter_big"/>   |        36        |           2880           |                2592                |

> Under default configuration, 1kW = 80FE/t