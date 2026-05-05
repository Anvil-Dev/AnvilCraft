---
navigation:
  title: "§2飘升机背包"
  icon: "anvilcraft:ionocraft_backpack"
  parent: anvilcraft_guideme:tool.md
categories:
  - tools
items:
  - anvilcraft:ionocraft_backpack
---

# 飘升机背包

<row halign="center">
<item id="anvilcraft:ionocraft_backpack"/>
</row>

# 合成

<recipe id="anvilcraft:ionocraft_backpack"/>

# 功能

- 可以穿在胸甲栏位
- 装备着时，拥有创造飞行能力，并消耗电力
- 在不供电时，持续飞行最长20min

<info>
安装饰品栏模组时，可以放在饰品栏
</info>

<color=#00e3ff> 小小鸟 飞飞飞 </color>

# 充电

## 电网供电

- 拥有不同档位的充电效率：64 128 256 512
- 电网剩余功率≥ 128kW (即充电功率的2倍)时，充电功率为64kW，以此类推
- 如电网中有多个玩家佩戴了飘升机背包，则先将电网剩余功率除以玩家数量再比较

<info>
简而言之，在保证电网不过载的前提下，会吸收尽可能多的能量
</info>

## 电容供电

- 使用背包内的[<translate key="item.anvilcraft.capacitor"/>](../002_material/101_capacitor.md)补充4MJ（飞行时间+80秒）
- 使用背包内的[<translate key="item.anvilcraft.supercapacitor"/>](../002_material/101_capacitor.md)直接充满（飞行时间变为1200秒）

# 相关

- [飘升机](../004_prop/101_ionocraft.md)