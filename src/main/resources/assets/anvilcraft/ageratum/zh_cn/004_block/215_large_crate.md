---
navigation:
  title: "§6大型板条箱"
  icon: "anvilcraft:large_crate"
items:
  - anvilcraft:large_crate
  - anvilcraft:local_terminal
---

# <ref item="anvilcraft:large_crate"/>

> super~big~crate~

<item id="anvilcraft:large_crate"/>

- 可通过[多方块转换](210_giant_anvil.md#功能)获得

## 功能

- 可容纳 1024 组物品
- 不满一组的物品可以混合占用同一组空间，类似收纳袋

<info>
铁砧工艺提供的容器方块均拥有类似的 GUI，并提供[合成窗口](003_crate.md#合成窗口)等功能
</info>

<tip>
手持<ref item="anvilcraft:large_crate"/>按 Shift+右击，可替换 3x3x3 范围内的<ref item="anvilcraft:crate"/>
</tip>

> 可升级为<ref item="anvilcraft:shulker_container"/>

# 终端

<row halign="center">
<item id="anvilcraft:local_terminal"/>
</row>

## 快捷打开

- 可自定义快捷键，快速打开携带的*终端*

## 悬浮窗

- 可以绑定*存储站*进行远距离访问
- 携带*终端*时，在任何 GUI 内使用 JEI 的“+”号进行快速合成，可直接通过*终端*调用物品
- 绑定*存储站*后，在其它 GUI 内，将鼠标悬停在*终端*物品上，会显示一个浮窗：
  - 空手时使用滚轮选择物品，按 Shift 左击取出
  - 手持物品时，右击塞入
  - 按住 Shift 滚动，可按行滚动
  - 按住 Ctrl 滚动，可按页滚动
  - 按住 Alt 放大

## 智能补货

- 按住 Alt 选择补货模式：智能（双向）、仅补货、仅存入、关
- 补货：手持的物品使用完毕时，会尝试取出相同物品补充满一组
- 存入：当捡起物品（或其他非主动获取物品的方式，例如水桶放置后变成空桶；不包括从容器 GUI 中主动拿取）使某物品超过一组时，只保留一组在身上，多余的物品自动存入*存储站*

<tip>
手持*终端*使用<ref item="anvilcraft:building_rod"/>时，可自动调用*存储站*中的物品
</tip>

## <ref item="anvilcraft:local_terminal"/>

<recipe id="anvilcraft:local_terminal"/>

- 自动连接玩家 32 格以内最近的一个<ref item="anvilcraft:large_crate"/>作为*存储站*
