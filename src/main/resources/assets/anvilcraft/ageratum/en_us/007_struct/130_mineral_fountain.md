---
navigation:
  title: "Mineral Fountain"
  icon: "anvilcraft:mineral_fountain"
items:
  - anvilcraft:impact_pile
  - anvilcraft:mineral_fountain
  - anvilcraft:sturdy_deepslate
---

# Mineral Fountain

<row halign="center">
<item id="anvilcraft:impact_pile"/>
<item id="anvilcraft:mineral_fountain"/>
<item id="anvilcraft:sturdy_deepslate"/>
</row>

## Obtaining

<recipe id="anvilcraft:impact_pile"/>

1. Craft <ref item="anvilcraft:impact_pile"/>
2. Place <ref item="anvilcraft:impact_pile"/> on <ref item="minecraft:bedrock"/> or <ref item="minecraft:deepslate"/>, and ensure its position is **no higher than** 8 blocks above the world bottom
3. Drop a **fully intact** <ref item="minecraft:anvil"/> from a height of at least 20 blocks onto it
4. Eventually, both <ref item="anvilcraft:impact_pile"/> and <ref item="minecraft:anvil"/> will disappear, and a structure containing <ref item="anvilcraft:mineral_fountain"/>, <ref item="anvilcraft:sturdy_deepslate"/>, and lava will be generated

<info>
The height at which the <ref item="anvilcraft:mineral_fountain"/> is generated is fixed at world minimum height + 5
</info>

<warning>
The generated structure will replace blocks other than <ref item="minecraft:bedrock"/>
</warning>

### <ref item="anvilcraft:sturdy_deepslate"/>

Very hard stone, not very useful

## Properties

- Hard and blast-resistant
- Extremely difficult to mine
- No drops when broken

# Function

- The mineral fountain only works at positions **no higher than** 8 blocks above the world bottom (other structure blocks do not participate in its function and can be destroyed freely)

---

## Ore Generation

<structure id="../../structures/mineral_fountain/raw_mineral.snbt"/>

- If all four sides of the <ref item="anvilcraft:mineral_fountain"/> are **the same type of raw ore block**, it will convert the <ref item="minecraft:deepslate"/> above into the corresponding **deepslate ore**
- There is a chance to instead generate <ref item="anvilcraft:earth_core_shard_ore"/> or <ref item="anvilcraft:void_stone"/>

<info>
Raw ore blocks can be obtained through <ref item="anvilcraft:corrupted_beacon"/>
</info>

| World  | Chance of generating <ref item="anvilcraft:earth_core_shard_ore"/> | Chance of generating <ref item="anvilcraft:void_stone"/> | 
|:---:|:-------------------------------------------------:|:---------------------------------------:|
| Overworld |                        1%                         |                   1%                    |
| Nether  |                        10%                        |                    0                    |
| End  |                         0                         |                   10%                   |

---

## Lava Generation

<structure id="../../structures/mineral_fountain/lava.snbt"/>

- A <ref item="anvilcraft:mineral_fountain"/> surrounded on all four sides by **lava** can generate **lava**

---

## Heating

<structure id="../../structures/mineral_fountain/heat.snbt"/>

- A <ref item="anvilcraft:mineral_fountain"/> surrounded on all four sides by **lava** can heat [heatable blocks](../001_feature/101_heated_block.md) to <color=#aa2222>red-hot level</color>

---

## Cinerite Generation

<structure id="../../structures/mineral_fountain/cinerite.snbt"/>

- When no other structure conditions are met, the <ref item="anvilcraft:mineral_fountain"/> generates <ref item="anvilcraft:cinerite"/> above it, which can be used for [ore meshing](../008_recipe/001_basic_minerals.md)

