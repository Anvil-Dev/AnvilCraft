---
navigation:
  title: "§2Energy Conversion"
  icon: "anvilcraft:power_converter_big"
items:
  - anvilcraft:power_converter_small
  - anvilcraft:power_converter_middle
  - anvilcraft:power_converter_big
  - anvilcraft:fe_collector
---

# Energy Conversion

AnvilCraft's electrical power is fundamentally different from other mods' energy systems, so it requires specialized conversion blocks

# Energy Converters

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

- Converts AnvilCraft power unidirectionally to FE/RF/AE energy
- Conversion has loss
- Different sizes convert different amounts of energy, see the table below for details

|                Energy Converter                 | Consumption (kW) | Equivalent Energy (FE/t) | Converted Energy After Loss (FE/t) |
|:-----------------------------------------------:|:----------------:|:------------------------:|:----------------------------------:|
| <ref item="anvilcraft:power_converter_small"/>  |        1         |           100            |                 90                 |
| <ref item="anvilcraft:power_converter_middle"/> |        16        |           1600           |                1440                |
|  <ref item="anvilcraft:power_converter_big"/>   |       256        |          25600           |               23040                |

<info>
Under default configuration: 1kW = 100FE/t, conversion loss rate 10%; configurable via config
</info>

# <ref item="anvilcraft:fe_collector"/>

<recipe id="anvilcraft:fe_collector"/>

- Receives FE energy from other mods
- When stored FE reaches 40% of the capacity, starts converting; stops when below 2%
- When stored FE reaches 50% of the capacity, starts outputting FE to adjacent blocks where possible

|             FE Collector              | Consumption (FE/t) | Equivalent Energy (kW) | Converted Energy After Loss (kW) |
|:-------------------------------------:|:------------------:|:----------------------:|:--------------------------------:|
| <ref item="anvilcraft:fe_collector"/> |       10,000       |          100           |                90                |

<tip>
Can be placed in series to work together
</tip>
