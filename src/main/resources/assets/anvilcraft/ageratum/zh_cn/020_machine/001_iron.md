---
navigation:
  title: "资源：刷铁机"
  icon: "minecraft:iron_ingot"
  parent: machine.md
---

# 资源：刷铁机

# 前言

<translate key="item.minecraft.iron_ingot"/>十分重要，因此需要制作一台刷铁机。

利用[铁砧抢夺](../001_feature/000_anvil_loot.md)配合[发射器修补铁傀儡](../001_feature/000_dispenser.md)的特性，自动生产大量<translate key="item.minecraft.iron_ingot"/>

1. 足够高的<translate key="item.minecraft.anvil"/>砸到铁傀儡时会掉落<translate key="item.minecraft.iron_ingot"/>
2. 通过<translate key="item.minecraft.dispenser"/>将一部分铁来修复受伤的铁傀儡
3. 在一只铁傀儡身上得到无限的<translate key="item.minecraft.iron_ingot"/>

# 图示

<row halign="center">
<GameScene zoom="3" background="#888888" interactive={true}>
    <IsometricCamera yaw="210" pitch="80" />
    <structure id="../structures/machine/iron.snbt"/>
    <DiamondAnnotation pos="1.5 1.5 3.5" color="#ffff00">
        在此安置铁傀儡，玻璃板会限制其移动
    </DiamondAnnotation>
    <BlockAnnotation x="2" y="3" z="1" color="#ff0000">
        中继器调为3档，否则，频率过高会使得发射器发射出的多余铁锭来不及被溜槽收回
    </BlockAnnotation>
    <BlockAnnotation x="0" y="1" z="2" color="#ffffff">
        设置过滤：铁锭
    </BlockAnnotation>
    <BlockAnnotation x="1" y="2" z="1" color="#ffffff">
        设置过滤：铁砧
    </BlockAnnotation>
</GameScene>

<Column>

- 所有 <translate key="item.minecraft.glass"/> 可替换为 任意完整方块
- 所有 <translate key="item.minecraft.smooth_stone"/> 可替换为 任意完整不透明方块
- 所有 <translate key="item.minecraft.smooth_stone_slab"/> 可替换为 任意台阶
- 做完后记得放入一个铁砧
- 高度别做错了，仔细数

> 右键调整位置
> 
> 左键调整角度

</Column>
</row>