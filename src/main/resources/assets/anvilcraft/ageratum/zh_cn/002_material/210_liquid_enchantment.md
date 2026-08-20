---
navigation:
  title: "§6液态魔咒"
  icon: "anvilcraft:frost_metal_ingot"
items:
  - anvilcraft:frost_metal_block
---

# 液态魔咒

> 借助[液态经验](004_exp_gem.md#经验流体)，将魔咒转为流体形态进行存储，实现自动化生产与复制；液态魔咒还可用于制作<ref item="anvilcraft:enchanted_gold_ingot"/>。

# 空白液态魔咒

在<ref item="anvilcraft:large_cauldron"/>内制造空白液态魔咒：

2000mB 经验流体 + 3 <ref item="minecraft:lapis_lazuli"/> → 1mB 空白液态魔咒

# 定向附魔

消耗特定材料，即可获得对应的魔咒：

| 所需空白液态魔咒 | 所需材料                                        | 产出液态魔咒类型与数量              |
|----------|---------------------------------------------|--------------------------|
| 1mB      | <ref item="anvilcraft:royal_steel_ingot"/>  | 精准采集 1mB                 |
| 1mB      | <ref item="anvilcraft:frost_metal_ingot"/>  | 崩解 1mB                   |
| 16mB     | <ref item="anvilcraft:ember_metal_ingot"/>  | 熔炼 16mB                  |
| 128mB    | <ref item="anvilcraft:transcendium_ingot"/> | 时运 64mB + 抢夺 64mB        |
| 1mB      | <ref item="minecraft:emerald"/>             | 经验修补 1mB                 |
| 8mB      | <ref item="anvilcraft:ruby"/>               | 火焰保护 8mB                 |
| 2mB      | <ref item="anvilcraft:sapphire"/>           | 冰霜行者 2mB                 |
| 1mB      | <ref item="anvilcraft:topaz"/>              | 引雷 1mB                   |
| 12mB     | <ref item="minecraft:amethyst_block"/>      | 伐木 4mB + 收割 4mB + 斩首 4mB |

# 使用液态魔咒

通过<ref item="anvilcraft:auto_enchanting_table"/>附魔到物品上

1级附魔对应1mB液态魔咒、2级对应2mB，3级4mB、4级8mB、5级16mB...以此类推

# 魔咒复制

在<ref item="anvilcraft:large_cauldron"/>中，空白液态魔咒可以被*液态魔咒*同化：

[加热器加热] 1mB 空白液态魔咒 + 8mB 液态魔咒 + 1 <ref item="minecraft:lapis_lazuli"/> → 9mB 液态魔咒

# 魔咒清洗

- 8mB 液态魔咒 + <ref item="anvilcraft:silver_nugget"/> → 8mB 空白液态魔咒
- 1mB 诅咒液态魔咒 + 16 <ref item="minecraft:gold_ingot"/> → 16 <ref item="anvilcraft:cursed_gold_ingot"/>
- 9mB 诅咒液态魔咒 + 16 <ref item="minecraft:gold_ingot"/> → 16 <ref item="anvilcraft:cursed_gold_block"/>

# 运输与过滤

- 液态魔咒不能排放到世界中，也不能用桶抓取，只能用<ref item="anvilcraft:pipe"/>运输
- 由于各种液态魔咒都是同一种液体，因此过滤方式特殊：使用附魔书在<ref item="anvilcraft:control_valve"/>中标记特定附魔（创造模式下配置<ref item="anvilcraft:creative_fluid_tank"/>同理）
