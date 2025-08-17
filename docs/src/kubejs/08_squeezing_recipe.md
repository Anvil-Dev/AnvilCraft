# 挤压配方 (Squeezing Recipe)

挤压配方使用炼药锅中的流体将方块挤压成其他方块。

## 基本结构

```js
ServerEvents.recipes(event => {
  event.custom({
    type: "anvilcraft:squeezing",
    ingredients: [
      {
        blocks: "minecraft:wet_sponge"
      }
    ],
    results: [
      {
        block: {
          name: "minecraft:sponge"
        },
        chance: 1.0
      }
    ],
    fluid: "minecraft:water",
    consume: -1
  })
})
```

## 字段说明

### type

固定值 `anvilcraft:squeezing`，标识这是一个挤压配方。

### ingredients (输入方块)

输入方块列表，每个元素包含：

- `blocks`: 方块ID（可以是单个方块ID字符串或方块ID数组）

### results (输出结果)

输出结果列表，每个元素包含：

- `block`: 方块状态对象，包含方块名称和其他属性
- `chance`: 结果出现的概率（0.0到1.0之间）

### fluid (流体)

流体类型，如 "minecraft:water" 或 "minecraft:lava"

### consume (流体消耗)

流体消耗量（可选）：

- 正数表示消耗流体
- 负数表示产生流体
- 0 表示不改变流体（默认值）

## 实用方法

```js
ServerEvents.recipes(event => {
  // 挤压配方 - 不同的构造函数参数组合
  event.recipes.anvilcraft.squeezing("anvilcraft:wet_sponge_to_sponge") // 仅ID
  
  event.recipes.anvilcraft.squeezing(
    "minecraft:wet_sponge",               // 输入
    [{                                   // 输出
      block: { name: "minecraft:sponge" },
      chance: 1.0
    }]
  )
  
  event.recipes.anvilcraft.squeezing(
    "minecraft:wet_sponge",               // 输入
    [{                                   // 输出
      block: { name: "minecraft:sponge" },
      chance: 1.0
    }],
    "minecraft:water"                    // 流体
  )
})
```

## KubeJS 风格构建器

```js
ServerEvents.recipes(event => {
  event.recipes.anvilcraft.squeezing()
    .input("minecraft:wet_sponge")       // 输入方块
    .result("minecraft:sponge")          // 输出方块
    .cauldron("minecraft:water")         // 需要的流体
    .produceFluid(true)                  // 产生流体 (负消耗)
})
```

## 使用示例

将湿海绵挤压成干海绵：

```js
ServerEvents.recipes(event => {
  event.recipes.anvilcraft.squeezing()
    .input("minecraft:wet_sponge")
    .result("minecraft:sponge")
    .cauldron("minecraft:water")
    .produceFluid(true)
})
```