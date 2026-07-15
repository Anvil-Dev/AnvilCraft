---
navigation:
  title: "§6能量武器平台"
  icon: "anvilcraft:energy_weapon_platform"
items:
  - anvilcraft:energy_weapon_platform
  - anvilcraft:spectral_weapon_launcher
  - anvilcraft:anvil_railgun
  - anvilcraft:corrupted_beacon_activator
  - anvilcraft:tesla_gun
  - anvilcraft:laser_gun
---

<row halign="center">
<item id="anvilcraft:energy_weapon_platform"/>
<item id="anvilcraft:spectral_weapon_launcher"/>
<item id="anvilcraft:anvil_railgun"/>
<item id="anvilcraft:corrupted_beacon_activator"/>
<item id="anvilcraft:tesla_gun"/>
<item id="anvilcraft:laser_gun"/>
</row>

# <ref item="anvilcraft:energy_weapon_platform"/>

<recipe id="anvilcraft:energy_weapon_platform"/>

- 可以存储320MJ能量(=640MFE)
- 打开后可以放入材料进行改造，变为各种强大的*能量武器*

# 能量武器

- *能量武器*可以放在<ref item="anvilcraft:charger"/>中充电，也能消耗背包中的<ref item="anvilcraft:capacitor"/>和<ref item="anvilcraft:supercapacitor"/>中的电力
- 大部分**远程武器附魔**和**武器附魔**均可在*能量武器*中生效，提供强大拐力

<info>
配方中的第一个物品如果有附魔，成品继承附魔
</info>

# <ref item="anvilcraft:spectral_weapon_launcher"/>

<recipe id="anvilcraft:energy_weapon_make/spectral_weapon_launcher"/>

- 功能和<ref item="anvilcraft:spectral_slingshot"/>一致，但造成100%伤害(<ref item="anvilcraft:spectral_slingshot"/>为50%)

# <ref item="anvilcraft:anvil_railgun"/>

<recipe id="anvilcraft:energy_weapon_make/anvil_railgun"/>

1. 攻击需要铁砧作为弹药，将除了<ref item="anvilcraft:spectral_anvil"/>外的*铁砧*放在副手，长按右键后松开，最多装填16个铁砧作为弹药
2. 射击需要长按右键进行最多 5s 蓄力，蓄力期间可随时发射 （需要至少蓄力10%的时间）
3. 蓄力越久，速度越快，伤害越高，耗电越多

<info>
对于大型生物，可能连续造成撞击伤害和铁砧落地伤害
</info>

# <ref item="anvilcraft:corrupted_beacon_activator"/>

<recipe id="anvilcraft:energy_weapon_make/corrupted_beacon_activator"/>

- 长按右键，持续发射最远 64 格的腐化光束，对接触到的所有生物造成*时移伤害*，并使其获得最高 5级 的凋零效果
- 快速装填附魔提升伤害频率

# <ref item="anvilcraft:tesla_gun"/>

<recipe id="anvilcraft:energy_weapon_make/tesla_gun"/>

- 右键发射电弧，对生物造成40点*雷击伤害*
- 可以连锁攻击附近的生物，但每次弹射降低10点伤害
- 拥有4s攻击冷却，可以被快速装填附魔降低（每级降低5gt，最多降低60gt），

# <ref item="anvilcraft:laser_gun"/>

<recipe id="anvilcraft:energy_weapon_make/laser_gun"/>

## 采矿模式

- 长按右键照射矿石，可以采集整片矿石堆
- 采集到的矿物直接塞入背包
- 挖掘类附魔均可生效

## 攻击模式

- 持续攻击单个生物，造成越来越高的伤害，停止攻击或转换目标重置伤害
- 击杀生物获得的战利品直接塞入背包

| 持续时间   | DPS（每秒伤害） | 耗电功率   | 额外特性            |
|--------|-----------|--------|-----------------|
| 5s及以内  | 12        | 200kW  |                 |
| 6-10s  | 28        | 400kW  |                 |
| 11-15s | 60        | 800kW  |                 |
| 16-20s | 124       | 1600kW | 开始过热，使持有者受到火焰伤害 |
| 21s及以后 | 252       | 3200kW | 开始过热，使持有者受到熔岩伤害 |

<warning>
即使伤害重置，持有者受到的火焰伤害或熔岩伤害还会额外持续5s
</warning>
