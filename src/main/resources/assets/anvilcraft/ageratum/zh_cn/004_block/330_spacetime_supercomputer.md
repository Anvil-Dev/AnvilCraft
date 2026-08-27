---
navigation:
  title: "§5时空超算"
  icon: "anvilcraft:spacetime_supercomputer"
items:
  - anvilcraft:spacetime_supercomputer
---

# <ref item="anvilcraft:spacetime_supercomputer"/>

<color=#886611> 操纵时间与空间 </color>

<recipe id="anvilcraft:procedural_process/spacetime_supercomputer_from_advanced_comparator"/>
<recipe id="anvilcraft:procedural_process/spacetime_supercomputer_from_advanced_comparator_2"/>

合成需要[砧子辐照](../002_material/321_anvilon.md#砧子辐照)

1. 持续耗能512kW
2. 工作前需要充能，每 3s 充能 1%，每次工作至少消耗 20% 充能
3. 通过指令的方式干涉时空，GUI左侧可以选择可调用指令
4. 输入完成后，下方三个按钮从左往右功能依次为：保存并执行、仅保存、取消
5. 执行过的指令会在GUI右侧记录

## 额外消耗
- `/time add` 指令每增加 1000gt 额外消耗1%充能
- `/tick sprint` 指令每增加 200gt 额外消耗1%充能进度

<info>
服务器管理员和整合包作者可以通过配置文件禁用单独的某条指令，被禁用的指令在gui左侧列表中标红并画横线
</info>

# 四维多方块合成

执行[多方块合成](210_giant_anvil.md#2多方块合成)时，如果<ref item="minecraft:crafting_table"/>阵列中心的方块是<ref item="anvilcraft:spacetime_supercomputer"/>（无需通电），则合成方式变为四维多方块合成：

- n×n的合成面积同时对应n次砸击来合成一个物品，例如3×3的合成面积对应3次砸击
- 每次砸击消耗方块，最后一次砸击完成后输出产物

<info>
可以用来生产<ref item="anvilcraft:hypercube"/>
</info>