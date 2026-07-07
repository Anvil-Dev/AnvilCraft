---
navigation:
  title: "资源：三模沙机"
  icon: "minecraft:sand"
---

# 资源：三模沙机

制造沙砾和沙子的机器，结构和刷石机类似

接下来这台三模式砸沙机，可以控制拉杆切换产出圆石、沙砾还是沙子

<structure id="../../structures/machine/sand.snbt"/>

<tip>
左键调整位置；右键调整角度；PgUP/PgDN调整显示高度
</tip>

1. 活塞往右2格，是*1号开关*：机器总开关
2. *1号开关*下面是*2号开关*：开启则生产圆石；关闭则启用*3号开关*
3. *1号开关*左边是*3号开关*：开启则生产沙子；关闭则生产沙砾
4. *1号开关*后面的脉冲发生器设置为循环模式：每间隔 10gt 发出长度 5gt 的信号
5. 脉冲发生器另一个设置为上升沿模式：延迟 6gt 发出长度 17gt 的信号
6. 放一个漏斗矿车

<warning>
关闭*总开关*后，再调整模式
</warning>

- 所有 <ref item="minecraft:smooth_stone"/> 可替换为 任意完整不透明方块
- 所有 <ref item="minecraft:glass"/> 可替换为 任意透明方块