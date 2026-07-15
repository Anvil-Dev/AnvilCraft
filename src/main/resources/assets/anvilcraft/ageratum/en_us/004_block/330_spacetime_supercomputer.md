---
navigation:
  title: "§5Spacetime Supercomputer"
  icon: "anvilcraft:spacetime_supercomputer"
items:
  - anvilcraft:spacetime_supercomputer
---

# <ref item="anvilcraft:spacetime_supercomputer"/>

<color=#886611> Manipulates time and space </color>

<recipe id="anvilcraft:procedural_process/spacetime_supercomputer_from_advanced_comparator"/>
<recipe id="anvilcraft:procedural_process/spacetime_supercomputer_from_advanced_comparator_2"/>

Crafting requires [Anvilon Irradiation](../002_material/321_anvilon.md#anvilon-irradiation)

1. Continuously consumes 512kW of power
2. Requires charging before operation, charges 1% every 3 seconds, each operation consumes at least 20% charge
3. Interferes with spacetime via commands; available commands can be selected on the left side of the GUI
4. After entering input, the three buttons at the bottom from left to right: Save & Execute, Save Only, Cancel
5. Executed commands are recorded on the right side of the GUI

## Additional Costs
- `/time add` command consumes an extra 1% charge per additional 1000gt
- `/tick sprint` command consumes an extra 1% charge per additional 200gt

<info>
The server administrator and the package integrator can disable a specific instruction through the configuration file. The disabled instruction will be highlighted in red and underlined in the list on the left side of the GUI
</info>
