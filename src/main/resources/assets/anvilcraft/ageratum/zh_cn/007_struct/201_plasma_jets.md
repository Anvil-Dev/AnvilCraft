---
navigation:
  title: "§6等离子喷流"
  icon: "anvilcraft:oil_bucket"
---

# 等离子喷流

<row halign="center">
<item id="anvilcraft:heater"/>
<item id="anvilcraft:magnet_block"/>
<item id="anvilcraft:tungsten_block"/>
<item id="anvilcraft:charge_collector"/>
<item id="anvilcraft:heat_collector"/>
</row>

## 结构

<structure id="../structures/plasma_jets.snbt"/>

- 需要[燃烧的炼药锅](../002_material/201_oil.md)
- 炼药锅底部有工作的[<translate key="block.anvilcraft.heater"/>](../004_block/100_heater.md)
- 炼药锅正上方 1x8x1 的空间没有方块
- 管壁最高可为四格

# 功能

- 将作为管壁的[常规可加热方块](../001_feature/101_heated_block.md)加热至<color=#cc5533>炽热</color>并增加0.1秒持续时间
- 若某层管壁符合对侧为[<translate key="block.anvilcraft.magnet_block"/>](../002_material/001_magnet.md)、另一对侧为[常规可加热方块](../001_feature/101_heated_block.md)时：
    - 将该层的[常规可加热方块](../001_feature/101_heated_block.md)加热至<color=#cc5533>炽热</color>并增加1秒持续时间
    - 在[常规可加热方块](../001_feature/101_heated_block.md)上产生256电荷

# 特性

- 喷流需要消耗锅中原油维持
    - 每层(250mb)原油为维持时间 +5min
    - 维持时间最多为 10min
- 在喷流中的实体会受到等同于熔岩中 4 倍的火焰类型伤害

---

## 相关

- [热能系统](../001_feature/101_heated_block.md)