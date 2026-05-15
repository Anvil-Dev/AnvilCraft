---
navigation:
  title: "§6激光"
  icon: "anvilcraft:ruby_laser"
items:
  - anvilcraft:ruby_laser
  - anvilcraft:ruby_prism
  - anvilcraft:laser_receiver
---

# 激光

<row halign="center">
<item id="anvilcraft:ruby_laser"/>
<item id="anvilcraft:ruby_prism"/>
<item id="anvilcraft:laser_receiver"/>
</row>

# 激光

<tip>
要建立本模组的激光系统，需准备大量的[<translate key="item.anvilcraft.ruby"/>](../002_material/000_gems.md)，[点我](../007_recipe/204_gem.md)查看方法
</tip>

- 定义：可被**不透明方块**阻挡的、笔直的光束
- 能力强度随**等级**决定
- 造成伤害 伤害[max:16] = 激光等级 - 4
- 加热[可加热方块](../001_feature/101_heated_block.md)
- 提取矿石，留下石头。矿产会从射出该激光的[<translate key="block.anvilcraft.ruby_prism"/>](201_basic_laser.md)的背部弹出或存入容器

> 如果[<translate key="block.anvilcraft.ruby_prism"/>](201_basic_laser.md)背部是也是**同方向**的[<translate key="block.anvilcraft.ruby_prism"/>](201_basic_laser.md)，
> 则从最后一个[<translate key="block.anvilcraft.ruby_prism"/>](201_basic_laser.md)的背部输出

|    等级    | 提取冷却(s) |    等级    |                加热能力                 |
|:--------:|:-------:|:--------:|:-----------------------------------:|
|  [4, 7]  |   24    |  [1, 3]  | <color=#661111>高温</color> |
| [8, 11]  |    6    | [4, 15]  | <color=#aa2222>红热</color> |
| [12, 15] |    3    | [16, 63] | <color=#cc5533>炽热</color> |
| [16, +∞) |    1    | [64,+∞)  | <color=#ee7744>白炽</color> |

# <translate key="block.anvilcraft.ruby_laser"/>

<recipe id="anvilcraft:ruby_laser"/>

- 耗电量16kW
- 受到红石信号停用
- 启用时发射激光[等级: 1, 射程: 128格]


# <translate key="block.anvilcraft.ruby_prism"/>

<recipe id="anvilcraft:ruby_prism"/>

- 不消耗电能
- 汇总其他5个方向的激光及其等级，向前发射[射程: 128格]


# <translate key="block.anvilcraft.laser_receiver"/>

<recipe id="anvilcraft:laser_receiver"/>

- 除底面外，可以接受激光并发电，同时根据激光等级发出红石信号
- 发电上限 = 激光等级 * 15kW
- 持续接收10s后，达到发电上限