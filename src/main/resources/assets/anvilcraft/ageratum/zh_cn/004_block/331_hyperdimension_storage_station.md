---
navigation:
  title: "§5超维存储站"
  icon: "anvilcraft:hyperdimension_storage_station"
items:
  - anvilcraft:hyperdimension_storage_station
  - anvilcraft:hyperdimension_terminal
  - anvilcraft:hyperdimension_uploader
---

# <ref item="anvilcraft:hyperdimension_storage_station"/>

## 制作

将 1x <ref item="anvilcraft:singularity_crystal"/>，16x <ref item="anvilcraft:hypercube"/> 从顶部砸入<ref item="anvilcraft:shulker_container"/>将其升级

## 功能

- 可以容纳**无限**物品
- 挖掘掉落可以保存其中的物品
- 自身无法通过<ref item="minecraft:hopper"/>等方块自动输入输出，需使用<ref item="anvilcraft:storage_port"/>

# <ref item="anvilcraft:hyperdimension_terminal"/>

<recipe id="anvilcraft:hyperdimension_terminal"/>

1. 右击绑定<ref item="anvilcraft:hyperdimension_storage_station"/>（绑定后不可更改，不可解绑）
2. 右击打开对应的<ref item="anvilcraft:hyperdimension_storage_station"/>的gui，即便该<ref item="anvilcraft:hyperdimension_storage_station"/>在三维世界被拆除

<tip>
多个玩家可以绑定同一个，通过绑定后拆除可以防止别人绑定
</tip>

# 超距输入

<item id="anvilcraft:hyperdimension_uploader"/>

## 制作

放置<ref item="anvilcraft:singularity_crystal"/>，手持一个绑定了<ref item="anvilcraft:hyperdimension_storage_station"/>的<ref item="anvilcraft:hyperdimension_terminal"/>右键<ref item="anvilcraft:singularity_crystal"/>，<ref item="anvilcraft:singularity_crystal"/>变为绑定对应<ref item="anvilcraft:hyperdimension_storage_station"/>的<ref item="anvilcraft:hyperdimension_uploader"/>

## 功能

- 有16格子，只能容纳16组相同物品，可以通过<ref item="minecraft:hopper"/>等方块输入输出
- 内部的物品都尝试向绑定的<ref item="anvilcraft:hyperdimension_storage_station"/>内存入

<warning>
自身不加载区块
</warning>
