---
navigation:
  title: "§6大型炼药锅"
  icon: "anvilcraft:large_cauldron"
items:
  - anvilcraft:large_cauldron
---

# <ref item="anvilcraft:large_cauldron"/>

> super~big~cauldron~

<item id="anvilcraft:large_cauldron"/>

- 通过[多方块转换](210_giant_anvil.md#功能)获得

# 容量

- 可以容纳 8 种*输入*物品，每种 9 组
- 可以容纳 32 种*输出*物品，每种 9 组

- 可以容纳 8 种流体，每种 64B

# 物流

## 物品

- 直接将物品以掉落物形式从上方丢入锅内
- 手持物品，右击**顶面**或**侧面最上面一层**的位置放入物品；空手右击锅口，可取出所指位置输入格的全部物品
- 通过*物流方块*从**顶面**或**侧面最上面两层**的位置输入物品；从**底面**或**侧面最下面一层**的位置输出物品

## 流体

- 手持流体桶从侧边倒入流体（在顶面右击会把桶塞进锅内）；手持空桶从侧边瞄准，可以取出所指的流体
- 通过*流体管道*从**侧面**和**顶面**排放流体到最上层（如果锅中不存在该流体）或抽取最上层的流体
- 通过*流体管道*从**底面**排放流体到最下层（如果锅中不存在该流体）或抽取最下层的流体

# 兼容常规锅加工

- 只有通过<ref item="anvilcraft:giant_anvil"/>砸击<ref item="anvilcraft:large_cauldron"/>才能执行各种配方
- 加工效率相当于 9 个<ref item="minecraft:cauldron"/>在**加工物品**或**方块压榨**
- 可以在底部放置工作的<ref item="anvilcraft:heater"/>和<ref item="anvilcraft:corrupted_beacon"/>等方块，使得<ref item="anvilcraft:giant_anvil"/>同时具备执行*高温熔炼*和*时移*的能力

<info>

[原油](../002_material/201_oil.md)只有位于顶层时才能燃烧

</info>

# 复杂流体反应

得益于超大容量，<ref item="anvilcraft:large_cauldron"/>可以完成*复杂流体反应*：
- 输入流体种类大于等于 2 种
- 输入流体需求大于 1000mB

## 液态魔咒

在<ref item="anvilcraft:large_cauldron"/>中完成[液态魔咒](../002_material/210_liquid_enchantment.md)相关的配方
