# 多方块结构配方 (Multiblock Recipe)

多方块结构配方允许你定义复杂的多方块结构，并在其中心产出物品。

## 基本结构

```js
ServerEvents.recipes(event => {
  event.custom({
    type: "anvilcraft:multiblock",
    pattern: {
      layers: [
        ["   ", " B ", "   "],
        ["BAB", "A A", "BAB"],
        ["BBB", "BBB", "BBB"]
      ],
      symbols: {
        "A": {
          predicate: {
            blocks: "minecraft:iron_block"
          }
        },
        "B": {
          predicate: {
            blocks: "minecraft:gold_block"
          }
        }
      }
    },
    result: {
      id: "minecraft:diamond",
      count: 1
    }
  })
})
```

### 实用方法

```js
ServerEvents.recipes(event => {
  // 多方块结构配方 - 不同的构造函数参数组合
  event.recipes.anvilcraft.multiblock("anvilcraft:diamond_multiblock") // 仅ID
  
  event.recipes.anvilcraft.multiblock(
    {                                       // 结构模式
      layers: [
        ["   ", " B ", "   "],
        ["BAB", "A A", "BAB"],
        ["BBB", "BBB", "BBB"]
      ],
      symbols: {
        "A": { blocks: "minecraft:iron_block" },
        "B": { blocks: "minecraft:gold_block" }
      }
    },
    { id: "minecraft:diamond", count: 1 }   // 输出
  )
})
```

## KubeJS 风格构建器

```js
ServerEvents.recipes(event => {
  event.recipes.anvilcraft.multiblock()
    .layer("   ", " B ", "   ")          // 第一层
    .layer("BAB", "A A", "BAB")          // 第二层
    .layer("BBB", "BBB", "BBB")          // 第三层
    .symbol('A', { blocks: "minecraft:iron_block" })  // 定义符号A
    .symbol('B', { blocks: "minecraft:gold_block" })  // 定义符号B
    .result("minecraft:diamond")         // 输出物品
})
```

## 参数说明

### pattern (结构模式)

定义多方块结构的形状和组成：

- `layers`: 由字符串数组组成的层数，每一层都是一个二维的字符网格
- `symbols`: 字符与方块谓词的映射关系

### result (输出)

配方的输出物品：

- `id`: 物品ID
- `count`: 物品数量

## 实际示例

### 简单的3x3x3结构

```js
ServerEvents.recipes(event => {
  event.recipes.anvilcraft.multiblock()
    .layer("AAA", "AAA", "AAA")
    .layer("A A", "A A", "A A")
    .layer("AAA", "AAA", "AAA")
    .symbol('A', { blocks: "minecraft:iron_block" })
    .result("minecraft:chest")
})
```

### 复杂结构示例

```js
ServerEvents.recipes(event => {
  event.recipes.anvilcraft.multiblock()
    .layer("RAR", "A A", "RAR")
    .layer("RGR", "G G", "RGR")
    .layer("RRR", "RRR", "RRR")
    .symbol('R', { blocks: "minecraft:redstone_block" })
    .symbol('A', { blocks: "minecraft:iron_block" })
    .symbol('G', { blocks: "minecraft:gold_block" })
    .result("minecraft:diamond_block")
})
```

## 使用方块状态

你也可以指定更详细的方块状态：

```js
ServerEvents.recipes(event => {
  event.recipes.anvilcraft.multiblock()
    .layer("HHH", "H H", "HHH")
    .layer("H H", "H H", "H H")
    .layer("HHH", "HHH", "HHH")
    .symbol('H', {
      predicate: {
        blocks: "minecraft:hopper",
        properties: {
          facing: "down"
        }
      }
    })
    .result("anvilcraft:magnet_block")
})
```

## 使用方块标签

```js
ServerEvents.recipes(event => {
  event.recipes.anvilcraft.multiblock()
    .layer("LLL", "L L", "LLL")
    .layer("L L", "L L", "L L")
    .layer("LLL", "LLL", "LLL")
    .symbol('L', { tag: "minecraft:logs" })
    .result("minecraft:campfire")
})
```

## 注意事项

1. 多方块结构的中心点位于结构的正中心
2. 结构必须完整匹配才能触发配方
3. 空格字符表示该位置不需要任何方块
4. 符号定义必须覆盖所有在层中使用的字符
5. 结构大小没有严格的限制，但建议保持在合理的范围内以保证性能
