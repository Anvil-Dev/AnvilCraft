# 方块压缩配方 (Block Compress Recipe)

方块压缩配方用于将多个相同方块压缩成一个更高级的方块。

## 基本结构

```js
ServerEvents.recipes(event => {
  event.custom({
    type: "anvilcraft:block_compress",
    inputs: [
      {
        blocks: "minecraft:iron_block"
      }
    ],
    results: [
      {
        block: {
          name: "anvilcraft:compressed_iron_block"
        },
        chance: 1.0
      }
    ]
  })
})
```

## 字段说明

### type

固定值 `anvilcraft:block_compress`，标识这是一个方块压缩配方。

### inputs (输入方块)

输入方块列表，每个元素包含：

- `blocks`: 方块ID（可以是单个方块ID字符串或方块ID数组）

### results (输出结果)

输出结果列表，每个元素包含：

- `block`: 方块状态对象，包含方块名称和其他属性
- `chance`: 结果出现的概率（0.0到1.0之间）

## 实用方法

```js
ServerEvents.recipes(event => {
  // 方块压缩 - 不同的构造函数参数组合
  event.recipes.anvilcraft.block_compress("anvilcraft:iron_block_to_compressed") // 仅ID
  
  event.recipes.anvilcraft.block_compress(
    [{ blocks: "minecraft:iron_block" }],    // 输入列表
    [{                                      // 输出列表
      block: { name: "anvilcraft:compressed_iron_block" },
      chance: 1.0
    }]
  )
})
```

## KubeJS 风格构建器

```js
ServerEvents.recipes(event => {
  event.recipes.anvilcraft.block_compress()
    .input("minecraft:iron_block")                // 输入方块
    .result("anvilcraft:compressed_iron_block")   // 输出方块
})
```

## 使用示例

将铁块压缩成压缩铁块：

```js
ServerEvents.recipes(event => {
  event.recipes.anvilcraft.block_compress()
    .input("minecraft:iron_block")
    .result("anvilcraft:compressed_iron_block")
})
```