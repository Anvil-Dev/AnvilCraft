---
navigation:
  title: "存储经验"
  icon: "anvilcraft:exp_gem"
items:
  - anvilcraft:exp_gem
  - anvilcraft:exp_gem_block
  - anvilcraft:exp_bucket
---

# 经验流体

> 将经验以物品或流体的形态存储

<info>
1000mB 经验流体 = 1 * <ref item="anvilcraft:exp_gem"/> = 50 玩家经验值
</info>

## 炼药锅交互

- 使用附魔之瓶向[锅](../001_feature/000_cauldron.md)倾倒，50%概率获得一层经验流体（是的，亏了）
- 使用空瓶舀[锅](../001_feature/000_cauldron.md)，获得一个附魔之瓶

## 玩家吞噬

- 玩家在锅内时，一次吸收 1B 液态经验，获得 50 点经验
- 玩家可以接触吸收世界中的流体源头，获得 50 点经验

## 榨取幽匿

- 在[锅](../001_feature/000_cauldron.md)上方的<ref item="minecraft:sculk"/>被砸时，有 10% 概率增加一层经验流体
- 若升级为<ref item="anvilcraft:frost_anvil"/>，则提升为 40% 概率

## 物品修复

- 在[锅](../001_feature/000_cauldron.md)内的经验流体可以修复带有经验修补附魔的物品
- 由于<ref item="minecraft:cauldron"/>一次修复消耗整锅流体，极易造成浪费

# <ref item="anvilcraft:exp_gem"/>

## 固液转换

<row>
<recipe id="anvilcraft:solid_liquid/exp_gem"/>
<recipe id="anvilcraft:solid_liquid/exp_fluid_cauldron"/>
</row>

## 特殊功能

- 右击使用<ref item="anvilcraft:exp_gem"/>获得50点经验值，shift右击使用手中所有<ref item="anvilcraft:exp_gem"/>
- 对村民使用一个<ref item="anvilcraft:exp_gem"/>，村民获得20点的交易经验值
- 对幼年村民使用一个<ref item="anvilcraft:exp_gem"/>，减少2分钟成长所需时间

## 存储方块

<row>
<recipe id="anvilcraft:exp_gem_block"/>
<recipe id="anvilcraft:exp_gem"/>
</row>