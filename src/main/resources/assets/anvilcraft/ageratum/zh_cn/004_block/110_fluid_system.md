---
navigation:
  title: "§2流体系统"
  icon: "anvilcraft:fluid_tank"
items:
  - anvilcraft:fluid_tank
  - anvilcraft:pipe
  - anvilcraft:pump
  - anvilcraft:drain
  - anvilcraft:control_valve
  - anvilcraft:check_valve
  - anvilcraft:fluid_tank_minecart
---

# 蓄流于内

<recipe id="anvilcraft:fluid_tank"/>

- <ref item="anvilcraft:fluid_tank"/>可以存放 16B 流体
- 被破坏时，物品形式可以保存流体

# 顺流而下

<recipe id="anvilcraft:pipe"/>

- <ref item="anvilcraft:pipe"/>可以转移流体
- 受重力影响，会将流体从位置更高的容器转移到位置更低的容器
- 高度差越大，转移速度越快，每格提供50mB/gt的速度，最大速度为2000mB/gt

<structure id="../../structures/gravity_pipe.nbt"/>

## 玻璃管道

- 手持<ref item="minecraft:glass_pane"/>右击<ref item="anvilcraft:pipe"/>，可将其切换为<ref item="anvilcraft:glass_pipe"/>
- 手持<ref item="anvilcraft:anvil_hammer"/>右击<ref item="anvilcraft:glass_pipe"/>，可将其切换为<ref item="anvilcraft:pipe"/>

## <ref item="minecraft:cauldron"/>支持

- 管道支持<ref item="minecraft:cauldron"/>，但是<ref item="minecraft:cauldron"/>较为特殊，只能一次输入或输出一整桶流体（1000mB）

# 逆流而上

## 泵

<recipe id="anvilcraft:pump"/>

- <ref item="anvilcraft:pump"/>耗电 32kW
- 可被红石信号关闭
- 拥有 20 格高的*扬程*，可将流体泵送到更高处
- 可以将多个<ref item="anvilcraft:pump"/>串联，以叠加*扬程*

<structure id="../../structures/pump.nbt"/>

## 矿车

<recipe id="anvilcraft:unpack/fluid_tank_minecart"/>

如果没有电，也可以试试用<ref item="anvilcraft:fluid_tank_minecart"/>储存和运输流体。

# 释流于外

<recipe id="anvilcraft:drain"/>

- <ref item="anvilcraft:drain"/>可以将输入的流体排放到下方
- 也可以反过来使用，吸纳**上方**的流体
- 对于同层流体<ref item="anvilcraft:drain"/>不进行操作；但如果同层流体可以自身形成无限流体，排水口会不断被该流体填满

<structure id="../../structures/drain.nbt"/>

> 问：小明对一个100m³的游泳池以7L/s的速度排入水，同时以5L/s的速度排出水，经过多久可以装满泳池？

# 引流之主

<row>
<recipe id="anvilcraft:control_valve"/>
<recipe id="anvilcraft:check_valve"/>
</row>

- <ref item="anvilcraft:control_valve"/> 限制流体的*种类*和*流速*，接受红石信号时**断开**
- <ref item="anvilcraft:check_valve"/> 控制流体只能单向流动，接受红石信号时**反向**
