---
navigation:
  title: "§2方块吞噬器"
  icon: "anvilcraft:block_devourer"
items:
  - anvilcraft:block_devourer
---

# 方块吞噬器

<recipe id="anvilcraft:block_devourer"/>

<info>
通过斩首III附魔砍末影龙，100%获得龙头
</info>

# 功能

- <ref item="anvilcraft:block_devourer"/>受到红石信号或被铁砧砸时，破坏前方一定范围内的方块
- 掉落物会尝试进入吞噬器后方的容器方块、实体库存内，无法进入则原地掉落
- 被铁砧砸时，属于[铁砧挖掘](../001_feature/000_anvil_destroy.md)的一种实现

| 激活方式         | 破坏范围 |
|--------------|------|
| 红石信号         | 3x3  |
| 从1格高处落下的铁砧   | 5x5  |
| 从2格高处落下的铁砧   | 7x7  |
| 从3格或更高处落下的铁砧 | 9x9  |

# 特性

- 可以被活塞推拉
- 世界基质方块如 <ref item="minecraft:stone"/> 、 <ref item="minecraft:netherrack"/> 等只有极少概率掉落
- 更廉价的方块破坏器可以使用[铁砧+切石机](../007_struct/000_anvil_processing.md)