---
navigation:
  title: "§2龙杖"
  icon: "anvilcraft:dragon_rod"
categories:
  - tools
items:
  - anvilcraft:dragon_rod
---

# 龙杖

<row halign="center">
<item id="anvilcraft:dragon_rod"/>
<item id="anvilcraft:royal_dragon_rod"/>
<item id="anvilcraft:ember_dragon_rod"/>
<item id="anvilcraft:transcendence_dragon_rod"/>
</row>

## 龙杖

- **龙杖**将 <gradient start="#ff00ff" end="#00e3ff"> "放下方块吞噬器→铁砧锤敲击→收回方块吞噬器" </gradient> 的流程简化至一个工具内  
- 所有龙杖的功能是相同的，只是耐久和属性不同

## 合成

龙杖可以在工作台中合成

<recipe id="anvilcraft:dragon_rod"/>

此外，你还能合成皇家钢、余烬金属版本和超限金属版本的龙杖，这些龙杖有着对应金属工具的属性

<row halign="center">
<recipe id="anvilcraft:smithing/royal_dragon_rod"/>
<recipe id="anvilcraft:smithing/ember_dragon_rod"/>
<recipe id="anvilcraft:smithing/transcendence_dragon_rod"/>
</row>
<row halign="center">
<recipe id="anvilcraft:royal_dragon_rod"/>
<recipe id="anvilcraft:ember_dragon_rod"/>
<recipe id="anvilcraft:transcendence_dragon_rod"/>
</row>

## 使用

- 龙杖的操作十分简单
  1. 左键破坏一定范围内的方块
  2. 右键切换范围大小，有3x3、5x5、7x7、9x9四个范围
  3. 当手持龙杖准星指向方块时会显示范围框。
- 3x3范围不消耗耐久，往后依次消耗1、2、4点耐久。
- 当龙杖耐久消耗殆尽时不会完全损坏，而是失去所有功能，类似于 **鞘翅**

## 破坏时

龙杖遵循方块吞噬器的规则，当挖掘世界基底方块（**石头**、**下界岩**、**末地石**）时，只有5%的概率掉落。但是它无法连锁顶部的可下落方块   
龙杖在挖掘一次后会有一段冷却时间，默认为1秒。这段冷却时长只受*急迫*效果和*挖掘疲劳*效果影响，每级急迫会减少4tick，每级挖掘疲劳会增加1秒

# 相关

- [皇家钢工具](../002_material/110_royal_steel.md)
- [余烬金属工具](../002_material/211_ember_metal.md)
- [超限金属工具](../002_material/312_transcendium.md)