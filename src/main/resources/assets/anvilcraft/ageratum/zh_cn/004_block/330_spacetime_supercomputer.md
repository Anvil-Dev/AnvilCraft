---
navigation:
  title: "§5时空超算"
  icon: "anvilcraft:spacetime_supercomputer"
items:
  - anvilcraft:spacetime_supercomputer
---

# <ref item="anvilcraft:spacetime_supercomputer"/>

<color=#886611> 操纵时间与空间 </color>

1. 持续耗能512kW，工作前需要充能
2. 每 3s 充能 1%
3. 通过指令的方式干涉时空，每次消耗 20% 充能进度
4. GUI左侧可以选择可调用指令
5. 输入完成后，下方三个按钮从左往右功能依次为：保存并执行、仅保存、取消
6. 执行过的指令会在GUI右侧记录

## 额外消耗
- `/time add` 指令每增加 1000gt 额外消耗1%充能
- `/tick sprint` 指令每增加 200gt 额外消耗1%充能进度

<info>
服务器管理员和整合包作者可以通过配置文件禁用单独的某条指令，被禁用的指令在gui左侧列表中标红并画横线
</info>