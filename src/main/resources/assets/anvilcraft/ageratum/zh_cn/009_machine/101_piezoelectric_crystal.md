---
navigation:
  title: "§2能量：压电晶体发电"
  icon: "anvilcraft:piezoelectric_crystal"
---

# 简易压电发电机

这大概是你的第一个发电机，它材料简单，前期即可搭建，即使发电量不多

发电量 4kW/个铁砧；共计 32kW

可以在此基础上继续展开

### 图示

<structure id="../../structures/machine/piezoelectric_crystal_0.snbt"/>

1. 脉冲发生器设置为循环模式：每间隔 5gt 输出信号 5gt，控制铁砧下落

- 所有 <ref item="minecraft:smooth_stone"/> 可替换为 任意完整不透明方块
- 所有 <ref item="minecraft:anvil"/> 可替换为 任意铁砧

## 进阶版

第一个发电机也可以用一些小技巧提高发电效率：

发电量 10kW/个铁砧；共计 80kW

### 图示

<structure id="../../structures/machine/piezoelectric_crystal_0.nbt"/>

1. 这里利用了砸船的小技巧和 0t 脉冲能让磁铁快速闪烁的小技巧

- 铁砧碰到船时，如果离地面 1格 以内，会瞬间落地，这里利用花盆控制船的高度正好在 1格 内
- 脉冲发生器每经过 4gt 输出 0gt 信号时，可以让磁铁最快闪烁

<tip>
你也可以在其它地方使用这些小技巧增加机器的效率
</tip>

# 皇家铁砧下弹压电发电机

- 随着进一步的发展，第一台发电机的电量很可能不够用了，是时候进行升级了
- 铁砧从高处落下的撞击会穿透多层压电晶体，产生更多电量。<ref item="anvilcraft:royal_anvil"/>从高处落下不会摔碎，正是合适的选择
- 虽然高处落下的铁砧发电量更多，但从高处落下也需要更长的时间。有什么办法可以让铁砧更快下落呢？答案是用 <ref item="minecraft:slime_block"/> 向下弹射

发电量 15kW/个铁砧；共计 150kW

<structure id="../../structures/machine/piezoelectric_crystal_1.snbt"/>

<tip>
树脂块也有粘液块一样的弹性，同时不会粘旁边的方块
</tip>

## 进阶版

可以用一些精妙的时序控制大幅提高发电效率，总之上面那个是老机器看看就行了，这个才是真实用（

发电量 75kW/个铁砧；共计 300kW

<structure id="../../structures/machine/piezoelectric_crystal_1.nbt"/>

<tip>
可以自行加长机器扩展发电量
</tip>