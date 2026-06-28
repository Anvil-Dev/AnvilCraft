---
navigation:
  title: "§2流体系统"
  icon: "anvilcraft:fluid_tank"
items:
  - anvilcraft:fluid_tank
  - anvilcraft:pipe
  - anvilcraft:pump
---

# <ref item="anvilcraft:fluid_tank"/>

- 可以存放 16B 液体

# <ref item="anvilcraft:pipe"/>

<recipe id="anvilcraft:pipe"/>

- 可以转移液体
- 受重力影响，会将液体从位置更高的容器转移到位置更低的容器
- 高度差越大，转移速度越快，每格提供50mB/gt的速度，最大速度为2000mB/gt

<structure id="../../structures/gravity_pipe.nbt"/>

## 管道节点

<block id="anvilcraft:pipe_node"/>

- 一条管道有3个或更多方向被连接时，自身会变为*节点*
- 节点在管道系统中被视为容器，可以自然接受高处的流体，向低处排放流体，不能向同层传输流体

<structure id="../../structures/pipe_node.nbt"/>

# <ref item="anvilcraft:pump"/>

<recipe id="anvilcraft:pump"/>

- 耗电 32kW 
- 可被红石信号关闭
- 工作时对液体施加10格高的*扬程*
- 可以将多个<ref item="anvilcraft:pump"/>串联以叠加*扬程*

<structure id="../../structures/pump.nbt"/>

# <ref item="minecraft:cauldron"/>支持

- 管道支持<ref item="minecraft:cauldron"/>
- 但是<ref item="minecraft:cauldron"/>较为特殊，因为它是分层(250mB)或只有一整锅(1000mB)的形态，需要管道在1gt输入足量的液体才能成功注入
