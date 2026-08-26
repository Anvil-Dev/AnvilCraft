---
navigation:
  title: "§2龙杖"
  icon: "anvilcraft:dragon_rod"
items:
  - anvilcraft:dragon_rod
  - anvilcraft:royal_dragon_rod
  - anvilcraft:frost_dragon_rod
  - anvilcraft:ember_dragon_rod
  - anvilcraft:transcendence_dragon_rod
---

# 龙杖

<recipe id="anvilcraft:dragon_rod"/>

## 功能

手持<ref item="anvilcraft:dragon_rod"/>，左键破坏一定范围内的方块，右键切换范围大小

| 破坏范围 | 耐久度消耗 |
|------|-------|
| 3x3  | 0     |
| 5x5  | 1     |
| 7x7  | 2     |
| 9x9  | 4     |

<info>
当<ref item="anvilcraft:dragon_rod"/>耐久消耗殆尽时不会完全损坏，而是失去所有功能，类似于<ref item="minecraft:elytra"/>
</info>

### 超限龙杖

- 第一次吞噬会进入 10 个 tick 的启动冷却；在 15 个 tick 内再次执行吞噬即可预热。预热后，只要持续按住攻击键，服务端就会在每个玩家 tick 吞噬准星下的方块
- 松开攻击键、切换出龙杖或进入冷却会停止连续吞噬；目标方块无效时仅跳过该 tick
- 按住[左 Alt]可以打开此龙杖的双选项轮盘：保护容器不被吞噬，或允许吞噬容器。该设置保存在每根龙杖上

### 破坏时

- <ref item="anvilcraft:dragon_rod"/>遵循<ref item="anvilcraft:block_devourer"/>的规则，当挖掘世界基底方块（**石头**、**下界岩**、**末地石**）时，只有5%的概率掉落。但是它无法连锁顶部的可下落方块   
- <ref item="anvilcraft:dragon_rod"/>在挖掘一次后会有一段冷却时间，默认为1秒。这段冷却时长只受*急迫*效果和*挖掘疲劳*效果影响，每级急迫会减少4tick，每级挖掘疲劳会增加1秒

# 相关

<row halign="center">
<item id="anvilcraft:royal_dragon_rod"/>
<item id="anvilcraft:frost_dragon_rod"/>
<item id="anvilcraft:ember_dragon_rod"/>
<item id="anvilcraft:transcendence_dragon_rod"/>
</row>

<ref item="anvilcraft:dragon_rod"/>可以用更高级的材料升级

- [皇家钢工具](../002_material/110_royal_steel.md)
- [浮霜金属工具](../002_material/202_frost_metal.md)
- [余烬金属工具](../002_material/211_ember_metal.md)
- [超限金属工具](../002_material/312_transcendium.md)