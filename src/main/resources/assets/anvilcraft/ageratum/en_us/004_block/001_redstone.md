---
navigation:
  title: "Additional Redstone Components"
  icon: "minecraft:redstone"
items:
  - anvilcraft:block_comparator
  - anvilcraft:pulse_generator
  - anvilcraft:item_detector
  - anvilcraft:advanced_comparator
---

# Additional Redstone Components

<row halign="center">
<item id="anvilcraft:block_comparator"/>
<item id="anvilcraft:pulse_generator"/>
<item id="anvilcraft:item_detector"/>
<item id="anvilcraft:advanced_comparator"/>
</row>

# <ref item="anvilcraft:block_comparator"/>

<recipe id="anvilcraft:block_comparator"/>

- When the blocks on both sides are the same, emits a redstone signal forward
- Defaults to **Normal Mode**. Right-click to switch to **Precision Mode**
- In <color=#999922>Normal Mode</color>, only checks if the blocks are the same
- In <color=#999922>Precision Mode</color>, checks if the block states are completely identical

# <ref item="anvilcraft:pulse_generator"/>

<recipe id="anvilcraft:pulse_generator"/>

- Based on the signal behind it, emits a configured redstone pulse forward

## GUI

In the GUI you can set the **condition**, **delay**, and **duration** of the emitted pulse

- The <color=#999922>top-left</color> button controls three modes: work on receiving redstone signal / work when signal disappears / loop when not receiving signal
- In the <color=#999922>middle</color>, set the delay before emitting the signal
- On the <color=#999922>right</color>, set the duration of the emitted signal
- The <color=#999922>bottom-left</color> button controls whether the output signal is in normal mode / inverted mode

# <ref item="anvilcraft:item_detector"/>

<recipe id="anvilcraft:item_detector"/>

- Can detect **dropped items** up to 8 blocks behind

## GUI

In the GUI you can set range and filters

- When <color=#999922>no filter is set</color>, the redstone signal strength varies **linearly** with the number of dropped items. At 64 items, it outputs full signal strength of 15
- When <color=#999922>a filter is set</color>, if none of the filtered dropped items exist, no signal is output. Otherwise, for **each**
  filter slot detecting an item, the redstone signal is calculated based on the corresponding item count (full signal strength reached when count = [64 * filter count]). The final output is the **minimum** of all redstone signals

# <ref item="anvilcraft:advanced_comparator"/>

<recipe id="anvilcraft:advanced_comparator"/>

- Based on the signal detected from a container placed behind or the input signal, determines whether to output full signal
- By default, no signal when below the lower threshold; starts outputting when above the upper threshold

## GUI

In the GUI you can set thresholds and other modes

- The <color=#999922>first button</color> from top to bottom enables window mode: outputs signal when the signal is within a certain range
- The <color=#999922>second button</color> from top to bottom enables inverted mode: outputs signal when it normally wouldn't, and vice versa
- When the <color=#999922>third button</color> from top to bottom is enabled, uses the redstone signals received on both sides as thresholds (regardless of left/right, the higher one is the upper threshold)





