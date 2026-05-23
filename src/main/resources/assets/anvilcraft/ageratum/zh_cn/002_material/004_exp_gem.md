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

<info>
1000mb 经验流体 = 1 * <ref item="anvilcraft:exp_gem"/> = 50 玩家经验值
</info>

## 炼药锅交互

- 使用附魔之瓶向炼药锅倾倒，50%概率获得一层经验流体（是的，亏了）
- 使用空瓶舀经验锅，获得一个附魔之瓶

## 玩家吞噬

- 玩家可以接触吸收满的经验锅，获得50点经验
- 玩家可以接触吸收世界中的流体源头，获得50点经验

## 榨取幽匿

- 幽匿块在炼药锅上被砸，有10%概率增加一层经验流体
- 如果获得并使用[浮霜铁砧](../004_block/233_frost_anvil.md)，则该概率变为40%

# <ref item="anvilcraft:exp_gem"/>

## 固液转换

<row>
<recipe id="anvilcraft:bulging/exp_gem"/>
<recipe id="anvilcraft:bulging/exp_fluid_cauldron"/>
</row>

- 一满锅经验流体被铁砧砸，消耗所有流体产生一个经验宝石
- 一个经验宝石在装满水的炼药锅中被砸，消耗经验宝石，获得一锅经验流体

## 特殊功能

- 右键消耗经验宝石获得50点经验值，同时shift右键消耗手中所有经验宝石
- 对村民消耗一个经验宝石，村民获得20点的交易经验值
- 对幼年村民消耗一个经验宝石，减少2分钟成长所需时间

## 存储方块

<row>
<recipe id="anvilcraft:exp_gem_block"/>
<recipe id="anvilcraft:exp_gem"/>
</row>