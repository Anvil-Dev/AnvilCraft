# 时移配方 (Time Warp Recipe)

时移配方使用时间力量将物品转换为其他物品。

## 基本结构

```js
ServerEvents.recipes(event => {
  event.custom({
    type: "anvilcraft:time_warp",
    ingredients: [
      {
        items: "minecraft:rotten_flesh"
      }
    ],
    results: [
      {
        id: "minecraft:leather",
        count: 1
      }
    ],
    fluid: "minecraft:water"
  })
})
```

## 字段说明

### type

固定值 `anvilcraft:time_warp`，标识这是一个时移配方。

### ingredients (输入物品)

输入物品列表，每个元素包含：

- `items`: 物品ID（可以是单个物品ID字符串或物品ID数组）
- `count`: 物品数量（可选，默认为1）

### results (输出物品)

输出物品列表，每个元素包含：

- `id`: 物品ID
- `count`: 物品数量

### fluid (流体)

流体类型，如 "minecraft:water" 或 "minecraft:lava"

## 实用方法

```js
ServerEvents.recipes(event => {
    // 时移配方 - 不同的构造函数参数组合
    event.recipes.anvilcraft.time_warp("anvilcraft:rotten_flesh_to_leather") // 仅ID

    event.recipes.anvilcraft.time_warp(
        "minecraft:rotten_flesh",             // 输入
        [{id: "minecraft:leather", count: 1}]// 输出
    )

    event.recipes.anvilcraft.time_warp(
        "minecraft:rotten_flesh",             // 输入
        [{id: "minecraft:leather", count: 1}],// 输出
        "minecraft:water"                    // 流体
    )
})
```

## KubeJS 风格构建器

```js
ServerEvents.recipes(event => {
    event.recipes.anvilcraft.time_warp()
        .requires("minecraft:rotten_flesh")  // 输入物品
        .result("minecraft:leather")         // 输出物品
        .cauldron("minecraft:water")         // 需要的流体
})
```

## 使用示例

将腐肉时移成皮革：

```js
ServerEvents.recipes(event => {
    event.recipes.anvilcraft.time_warp()
        .requires("minecraft:rotten_flesh")
        .result("minecraft:leather")
        .cauldron("minecraft:water")
})
```