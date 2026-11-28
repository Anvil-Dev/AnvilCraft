---
navigation:
  title: "§2Heliostats"
  icon: "anvilcraft:heliostats"
items:
  - anvilcraft:heliostats
---

# Heliostats
<row halign="center">
<recipe id="anvilcraft:heliostats_biological"/>
<recipe id="anvilcraft:heliostats_electrical"/>
</row>

# Function

- Right-click a block with <ref item="anvilcraft:heliostats"/> in hand to bind it, then place the heliostat. Uses sunlight to heat the bound block
- Right-click a placed <ref item="anvilcraft:heliostats"/> that has an illumination target with an unconfigured <ref item="anvilcraft:heliostats"/> item to copy the target to the item
- Right-click a placed <ref item="anvilcraft:heliostats"/> without an illumination target with a configured <ref item="anvilcraft:heliostats"/> item to set the target on the placed block

<info>
The block illuminated by a heliostat and the blocks directly above and below it are heated to the same temperature level; blocks one step farther above or below are heated to one temperature level lower
</info>

<info>
If 64 <ref item="anvilcraft:heliostats"/> are used, the heat generated can be converted to 896kW of power
</info>

- Different numbers of <ref item="anvilcraft:heliostats"/> concentrated together provide different heating capabilities

| Count |                  4                  |                 12                  |                 32                  |                 64                  |
|:-----:|:-----------------------------------:|:-----------------------------------:|:-----------------------------------:|:-----------------------------------:|
| Level | <color=#661111>Hot</color> | <color=#aa2222>Red-hot</color> | <color=#cc5533>Blazing</color> | <color=#ee7744>Incandescent</color> |

<info>
Enable "Heliostats Beam" in the client graphics settings to render the beams
</info>

---

## Related

- [Heat System](../001_feature/101_heated_block.md)