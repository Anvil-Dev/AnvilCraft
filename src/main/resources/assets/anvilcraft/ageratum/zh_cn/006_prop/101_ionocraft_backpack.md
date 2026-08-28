---
navigation:
  title: "§2飘升机背包"
  icon: "anvilcraft:ionocraft_backpack"
items:
  - anvilcraft:ionocraft
  - anvilcraft:ionocraft_backpack
---

# 飘升机

<recipe id="anvilcraft:ionocraft"/>

## 功能

- 右键放置在地面上生
- 电网范围中，消耗 16kW 快速上升
- 不在电网范围内会缓慢下降
- 实体可以踩在上面

# 飘升机背包

<recipe id="anvilcraft:ionocraft_backpack"/>

## 功能

- 可以穿在胸甲栏位
- 装备着时，拥有创造飞行能力，并消耗电力

<info>
安装饰品栏模组时，可以放在饰品栏
</info>

## 充电

### 电网供电

- 拥有不同档位的充电效率：64 128 256 512
- 电网剩余功率≥ 128kW (即充电功率的2倍)时，充电功率为64kW，以此类推
- 如电网中有多个玩家佩戴了飘升机背包，则先将电网剩余功率除以玩家数量再比较

<info>
简而言之，玩家在电网中时，<ref item="anvilcraft:ionocraft_backpack"/>在保证电网不过载的前提下，会吸收尽可能多的能量
</info>

### 电容供电

- 自动使用背包内的<ref item="anvilcraft:capacitor"/>补充能量
- 由于<ref item="anvilcraft:supercapacitor"/>电量过多，为避免浪费，不会自动使用；需要[手动充能](../002_material/101_capacitor.md#充能物品)

