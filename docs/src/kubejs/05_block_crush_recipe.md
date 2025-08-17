# 方块粉碎配方 (Block Crush Recipe)

方块粉碎配方用于将方块粉碎成更小的方块或物品。

## 基本结构

```js
ServerEvents.recipes(event => {
  event.custom({
    type: "anvilcraft:block_crush",
    inputs: [
      {
        blocks: "minecraft:cobblestone"
      }
    ],
    results: [
      {
        id: "minecraft:gravel",
        count: 1
      }
    ]
  })
})
```

## 字段说明

### type

固定值 `anvilcraft:block_crush`，标识这是一个方块粉碎配方。

### inputs (输入方块)

输入方块列表，每个元素包含：

- `blocks`: 方块ID（可以是单个方块ID字符串或方块ID数组）

### results (输出物品)

输出物品列表，每个元素包含：

- `id`: 物品ID
- `count`: 物品数量

## 实用方法

```js
ServerEvents.recipes(event => {
  // 方块粉碎 - 不同的构造函数参数组合
  event.recipes.anvilcraft.block_crush("anvilcraft:cobblestone_to_gravel") // 仅ID
  
  event.recipes.anvilcraft.block_crush(
    { blocks: "minecraft:cobblestone" },     // 输入
    { id: "minecraft:gravel", count: 1 }     // 输出
  )
})
```

## KubeJS 风格构建器

```js
ServerEvents.recipes(event => {
  event.recipes.anvilcraft.block_crush()
    .input("minecraft:cobblestone")      // 输入方块
    .result("minecraft:gravel")          // 输出方块
})
```

## 使用示例

将圆石粉碎成沙砾：

```js
ServerEvents.recipes(event => {
  event.recipes.anvilcraft.block_crush()
    .input("minecraft:cobblestone")
    .result("minecraft:gravel")
})
```