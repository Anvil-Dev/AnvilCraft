---
navigation:
  title: "§6大型板条箱"
  icon: "anvilcraft:large_crate"
items:
  - anvilcraft:large_crate
  - anvilcraft:shulker_container
  - anvilcraft:storage_port
  - anvilcraft:local_terminal
  - anvilcraft:shulker_terminal
---

# <ref item="anvilcraft:large_crate"/>

> super~big~crate~

<item id="anvilcraft:large_crate"/>

- 通过[多方块转换](210_giant_anvil.md#功能)获得

## 功能

- 可以容纳1024组物品
- 不满一组的物品可以混合占用同一组空间，类似收纳袋

# <ref item="anvilcraft:shulker_container"/>

## 制作
将 1x <ref item="anvilcraft:space_overcompressor"/>，6x <ref item="minecraft:netherite_block"/> 从顶部砸入<ref item="anvilcraft:large_crate"/>将其升级

## 功能

- 可以容纳约6.5万种物品，每种物品存储空间相互独立，均为约6.5万个
- 挖掘掉落可以保存其中的物品
- 将<ref item="anvilcraft:space_overcompressor"/>压入<ref item="anvilcraft:shulker_container"/>将容量翻倍，最多进行四次
- 自身无法通过<ref item="minecraft:hopper"/>等方块自动输入输出，需使用<ref item="anvilcraft:storage_port"/>

# <ref item="anvilcraft:storage_port"/>

<recipe id="anvilcraft:storage_port"/>

- 作需要与<ref item="anvilcraft:shulker_container"/>或其它<ref item="anvilcraft:storage_port"/>相邻放置
- 可以通过<ref item="minecraft:hopper"/>等方块自动输入
- 使用物品标记，自动输入/输出该物品
- 使用物品标记，玩家可以手动左击取出物品或右击塞入物品
- 使用<ref item="anvilcraft:anvil_hammer"/>长按右键去除物品标记

# 终端

<row halign="center">
<item id="anvilcraft:local_terminal"/>
<item id="anvilcraft:shulker_terminal"/>
</row>

## 悬浮窗

- 可以绑定*存储站*进行远距离访问
- 携带有*终端*时，在任何gui内尝试用jei的“+”号移动物品快速合成时，可直接通过*终端*调用物品
- 绑定*存储站*后，在其它GUI内，将鼠标悬浮于*终端*物品上，会显示一个浮窗：
  - 空手时使用滚轮选择物品，左击取出
  - 手持物品时，右击塞入

## 智能补货

- 按住alt选择补货模式：智能（双向），仅补货，仅存入，关
- 补货：手持的物品使用完毕时会尝试取出相同物品补充满一组
- 存入：捡起物品（或其他非主动获得物品的方式，例如水桶放置后变桶等；不包括从容器gui中主动拿取）使得某物品超过一组时，只保留一组在身上，多余的物品自动存入*存储站*

## <ref item="anvilcraft:local_terminal"/>

<recipe id="anvilcraft:local_terminal"/>

- 自动连接玩家 32 格以内的一个最近的<ref item="anvilcraft:large_crate"/>作为*存储站*

## <ref item="anvilcraft:shulker_terminal"/>

<recipe id="anvilcraft:shulker_terminal"/>

- 自动连接玩家身上的第一个<ref item="anvilcraft:shulker_container"/>
- 如果身上不携带<ref item="anvilcraft:shulker_container"/>，自动连接玩家 64 格以内的一个最近的<ref item="anvilcraft:shulker_container"/>作为*存储站*
