---
navigation:
  title: "§6潜影集装箱"
  icon: "anvilcraft:shulker_container"
items:
  - anvilcraft:shulker_container
  - anvilcraft:storage_port
  - anvilcraft:shulker_terminal
  - anvilcraft:storage_fluid_port
  - anvilcraft:storage_port_consolidator
---

# <ref item="anvilcraft:shulker_container"/>

## 制作

从上方将 1 个<ref item="anvilcraft:space_overcompressor"/>和 6 个<ref item="minecraft:netherite_block"/>砸入<ref item="anvilcraft:large_crate"/>，即可将其升级为<ref item="anvilcraft:shulker_container"/>

## 功能

- 可容纳 1024 种物品，每种物品的存储空间相互独立，均为 1024 组
- 破坏后，掉落的潜影集装箱会保留其中的物品
- 将<ref item="anvilcraft:space_overcompressor"/>压入<ref item="anvilcraft:shulker_container"/>，可将种类上限和存储容量翻倍，最多进行四次
- 自身无法通过<ref item="minecraft:hopper"/>等方块自动输入或输出物品，需要使用<ref item="anvilcraft:storage_port"/>

<info>
铁砧工艺提供的容器方块均拥有类似的 GUI，并提供[合成窗口](003_crate.md#合成窗口)等功能
</info>

<info>
潜影集装箱物品不能被存入潜影集装箱或仓储端口中
</info>

# 自动化存取

## <ref item="anvilcraft:storage_port"/>

<recipe id="anvilcraft:storage_port"/>

- 需要与<ref item="anvilcraft:shulker_container"/>或其它<ref item="anvilcraft:storage_port"/>相邻放置
- 可以通过<ref item="minecraft:hopper"/>等方块自动输入
- 标记物品后，会自动输入/输出该物品
- 标记物品后，玩家可以左击取出或右击塞入该物品
- 使用<ref item="anvilcraft:anvil_hammer"/>长按右键，可在轮盘中选择清除标记

## <ref item="anvilcraft:storage_fluid_port"/>

<recipe id="anvilcraft:storage_fluid_port"/>

- 与<ref item="anvilcraft:storage_port"/>类似，但用于流体交互
- 拥有 128 B（128,000 mB）的容量，可为*存储站*扩展流体存储能力，并能在*存储站*的 GUI 中查看和使用
- 会调整自身扬程以排出或吸入所储存的流体，使内部流体含量保持在 50%～75%
- 通过桶倒入流体时会自动标记该流体，使用<ref item="anvilcraft:anvil_hammer"/>长按右键可清除标记
- 在 GUI 内放入液体桶时，存储系统会自动将其拆分为流体和空桶，取出时则相反

<tip>
手持装满流体的桶右击端口，可直接存入流体
</tip>

## <ref item="anvilcraft:storage_port_consolidator"/>

- 类似*抽屉管理器*，会将所有相连的<ref item="anvilcraft:storage_port"/>和<ref item="anvilcraft:storage_fluid_port"/>中的内容汇总，并将汇总结果提供给外部物流
- *相连*判定：不能跨越潜影集装箱或*存储站*，只能沿相邻的*仓储端口*（包括*仓储流体端口*）延伸连接
- 直接贴附<ref item="anvilcraft:shulker_container"/>或<ref item="anvilcraft:hyperdimension_storage_station"/>时，外部物流可直接取出仓储中的物品，输入则先进入缓存再逐步存入仓储

# 配套终端

<recipe id="anvilcraft:shulker_terminal"/>

- 自动连接玩家身上的第一个<ref item="anvilcraft:shulker_container"/>
- 如果身上不携带<ref item="anvilcraft:shulker_container"/>，自动连接玩家 64 格以内最近的一个<ref item="anvilcraft:shulker_container"/>作为*存储站*
