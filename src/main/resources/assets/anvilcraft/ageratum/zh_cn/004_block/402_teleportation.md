---
navigation:
  title: "§5锻星砧：超距传送"
  icon: "anvilcraft:black_hole"
items:
  - anvilcraft:celestial_forging_anvil_portal
---

# 无限传输

1. 首先，制造若干拥有*虫洞*的[全同黑洞](401_celestial_forging_anvil.md#全同天体)
2. 为这些*黑洞*安装[虫洞稳定器](402_mega_structure.md#虫洞稳定器)，将这些黑洞所属的<ref item="anvilcraft:celestial_forging_anvil"/>连接起来
3. 然后，可以通过在<ref item="anvilcraft:celestial_forging_anvil"/>的同一位置摆上接口，接口之间共享存储，拥有无线传输的功能
4. 还可以放置<ref item="anvilcraft:celestial_forging_anvil_portal"/>传送生物，但在同一组*全同黑洞*中，最多只能放置两个（但可以在四个方向各放一组，实现一一对应）

<recipe id="anvilcraft:celestial_forging_anvil_portal"/>  

<structure id="../../structures/teleportation.nbt"/>

> 不同颜色代表不同分组

<structure id="../../structures/teleportation_2.nbt"/>

# 加载区块

- 被连接的<ref item="anvilcraft:celestial_forging_anvil"/>只要有一个被加载，则都会被加载
- 每个<ref item="anvilcraft:celestial_forging_anvil"/>本体底部中心块为中心3x3区块都会被强加载
