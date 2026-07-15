---
navigation:
  title: "额外红石元件"
  icon: "minecraft:redstone"
items:
  - anvilcraft:block_comparator
  - anvilcraft:pulse_generator
  - anvilcraft:item_detector
  - anvilcraft:advanced_comparator
  - anvilcraft:redstone_wire
---

# 额外红石元件

<row halign="center">
<item id="anvilcraft:block_comparator"/>
<item id="anvilcraft:pulse_generator"/>
<item id="anvilcraft:item_detector"/>
<item id="anvilcraft:advanced_comparator"/>
</row>

# <ref item="anvilcraft:block_comparator"/>

<recipe id="anvilcraft:block_comparator"/>

- 两侧的方块相同时，向前方发出红石信号
- 默认为**普通模式**右键切换为**精准模式**
- <color=#999922>普通模式</color>下，仅检查方块是否相同
- <color=#999922>精准模式</color>下，会检查方块的状态是否完全相同

# <ref item="anvilcraft:pulse_generator"/>

<recipe id="anvilcraft:pulse_generator"/>

- 根据背后的信号，向前发出设定的红石脉冲

## GUI

在GUI中可以设置发出脉冲的**条件**、**延迟**和**持续时长**

- <color=#999922>左上角</color>的按钮控制三个模式：收到红石信号时工作/信号消失时工作/不受到信号时循环工作
- 在<color=#999922>中间</color>设置延时多久发出信号
- 在<color=#999922>右边</color>设置发出信号的持续时间
- <color=#999922>左下角</color>的按钮控制输出信号为正常模式/反转模式

# <ref item="anvilcraft:item_detector"/>

<recipe id="anvilcraft:item_detector"/>

- 可检测身后最多8格的**掉落物**

## GUI

在GUI中可以设置范围和过滤

- <color=#999922>不设置过滤</color>时，输出红石信号强度随掉落物的数量**线性变化**。数量达到64个时输出满信号强度15
- <color=#999922>设置过滤</color>时，若过滤的掉落物都不存在，则不输出信号。否则，对**每个**检测到物品的过滤格，分别根据对应掉落物数量计算红石信号(数量达到[64*过滤数量]时计算满信号强度)。最终输出选择所有红石信号的**最小值**输出

# <ref item="anvilcraft:advanced_comparator"/>

<recipe id="anvilcraft:advanced_comparator"/>

- 根据从背部放置容器检测到的信号或输入的信号，决定是否输出满信号
- 默认在信号低于下阈值后无信号；在信号高于上阈值后开始输出信号

## GUI

在GUI中可以设置阈值与其他模式

- 从上往下<color=#999922>第一个按钮</color>可启用窗口模式：在信号位于一定区间时输出信号
- 从上往下<color=#999922>第二个按钮</color>可启用反转模式：在原来不输出信号的时候输出信号，反之亦然
- 从上往下<color=#999922>第三个按钮</color>启用后，将两侧收到的红石信号作为阈值(不分左右，高者为上阈值)

# <ref item="anvilcraft:redstone_computer"/>

<recipe id="anvilcraft:redstone_computer"/>

- 输出三个输入端的信号强度之和，最高不超过15

# <ref item="anvilcraft:redstone_wire"/>

<recipe id="anvilcraft:redstone_wire"/>

## 放置

- 可以沿着墙壁或天花板放置

### 连线机制

- 一格内只能存在一根<ref item="anvilcraft:redstone_wire"/>，存在唯一附着方向
- 与斜上方贴在附着方向相同的<ref item="anvilcraft:redstone_wire"/>爬墙连接
- 会与正上方垂直于自己的<ref item="anvilcraft:redstone_wire"/>爬墙连接
- 会与同方块附着的四个相邻面的<ref item="anvilcraft:redstone_wire"/>连接

## 接收

- 底座绝缘，不接受红石信号，只在断口输入输出

## 传输

- 传输红石信号不衰减
- 如果受到多个红石信号，取最高值

## 输出

- 红石粉输入的信号不会输出给红石粉
- 输出只激活不充能
