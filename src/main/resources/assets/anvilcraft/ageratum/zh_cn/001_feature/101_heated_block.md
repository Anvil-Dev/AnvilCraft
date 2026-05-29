---
navigation:
  title: "§2热能系统"
  icon: "anvilcraft:redhot_tungsten_block"
items:
  - anvilcraft:heated_tungsten_block
  - anvilcraft:redhot_tungsten_block
  - anvilcraft:glowing_tungsten_block
  - anvilcraft:incandescent_tungsten_block
  - anvilcraft:heated_netherite_block
  - anvilcraft:redhot_netherite_block
  - anvilcraft:glowing_netherite_block
  - anvilcraft:incandescent_netherite_block
---

# 热能系统

<row halign="center">
<item id="anvilcraft:tungsten_block"/>
<item id="anvilcraft:heated_tungsten_block"/>
<item id="anvilcraft:redhot_tungsten_block"/>
<item id="anvilcraft:glowing_tungsten_block"/>
<item id="anvilcraft:incandescent_tungsten_block"/>
</row>

---

<row halign="center">
<item id="minecraft:netherite_block"/>
<item id="anvilcraft:heated_netherite_block"/>
<item id="anvilcraft:redhot_netherite_block"/>
<item id="anvilcraft:glowing_netherite_block"/>
<item id="anvilcraft:incandescent_netherite_block"/>
</row>

# 可加热方块

- 定义：有若干**温度等级**和**持续时间**的方块

## 温度等级

- 作为<color=#cc44cc>抽象概念</color>，反映方块的温度
- 可通过外部加热提升并维持**温度等级**
- <ref item="minecraft:netherite_block"/>、<ref item="anvilcraft:tungsten_block"/>具有5个温度等级：
  - <color=#666666>普通</color>
  - <color=#661111>高温</color>
  - <color=#aa2222>红热</color>
  - <color=#cc5533>炽热</color>
  - <color=#ee7744>白炽</color>

## 持续时间

- 作为<color=#cc44cc>抽象概念</color>，反映方块因为散热，将在多久后降低**温度等级**
- 自动减少，若归零，则**温度等级**下降，**持续时间**重置为10s
- 收到外部加热时提升，最大值: 20min
- <ref item="minecraft:comparator"/>可以检测持续时间，输出红石信号

# 高温方块

- 定义：不是<color=#666666>普通</color>等级的可加热方块

## 特性

- 可以为<ref item="anvilcraft:heat_collector"/>提供给热量以发电
- 挖掘时，如果工具没有*精准采集/熔炼*附魔，则**温度等级**降低一级
- 放置时，**温度等级**降低一级
- <color=#aa2222>红热</color>及以上等级的方块，烫伤踩在上面的生物
- <color=#aa2222>红热</color>及以上等级的方块，每过一段时间，和海绵一样蒸发一定范围内的水

# 加热与加热能力

- 加热能力：决定最终将方块加热到哪个**温度等级**
- 不同的加热方式具有不同的**加热能力**(决定最终将方块加热哪个温度等级)
- 加热时：
  - 温度等级 < 加热能力: 方块提升至对应**温度等级**
  - 温度等级 = 加热能力: **持续时间**增加
  - 温度等级 > 加热能力: 方块不受影响

# 加热方式

|                     加热方式                      |                加热能力                 | 提升时间(s) |
|:---------------------------------------------:|:-----------------------------------:|:--------|
|      <ref item="anvilcraft:heater"/>      | <color=#661111>高温</color> | 0.1     |
| <ref item="anvilcraft:mineral_fountain"/> | <color=#aa2222>红热</color> | 1       |
|    <ref item="anvilcraft:heliostats"/>    |                 可变                  | 4       |
|    <ref item="anvilcraft:ruby_laser"/>    |                 可变                  | 0.1     |
|       [等离子喷流](../007_struct/201_plasma_jets.md)       | <color=#cc5533>炽热</color> | 0.1 / 1 |