# 膨发配方 (Bulging Recipe)

膨发配方使用炼药锅中的流体将物品膨发成其他物品。

## 基本结构

```js
ServerEvents.recipes(event => {
  event.custom({
    type: "anvilcraft:bulging",
    ingredients: [
      {
        items: "minecraft:dirt",
        count: 1
      }
    ],
    results: [
      {
        id: "minecraft:clay",
        count: 1
      }
    ],
    fluid: "minecraft:water"
  })
})
```

## 字段说明

### type

固定值 `anvilcraft:bulging`，标识这是一个膨发配方。

### ingredients (输入物品)

输入物品列表，每个元素包含：

- `items`: 物品ID（可以是单个物品ID字符串或物品ID数组）
- `count`: 物品数量

### results (输出物品)

输出物品列表，每个元素包含：

- `id`: 物品ID
- `count`: 物品数量

### fluid (流体)

流体类型，如 "minecraft:water" 或 "minecraft:lava"

## 实用方法

```js
ServerEvents.recipes(event => {
  // 膨发配方 - 不同的构造函数参数组合
  event.recipes.anvilcraft.bulging("anvilcraft:dirt_to_clay") // 仅ID
  
  event.recipes.anvilcraft.bulging(
    "minecraft:dirt",                     // 输入
    [{ id: "minecraft:clay", count: 1 }] // 输出
  )
  
  event.recipes.anvilcraft.bulging(
    "minecraft:dirt",                     // 输入
    [{ id: "minecraft:clay", count: 1 }],// 输出
    "minecraft:water"                    // 流体
  )
})
```

## KubeJS 风格构建器

```js
ServerEvents.recipes(event => {
  event.recipes.anvilcraft.bulging()
    .requires("minecraft:dirt")          // 输入物品
    .result("minecraft:clay")            // 输出物品
    .cauldron("minecraft:water")         // 需要的流体
})
```

## 使用示例

将泥土膨发成黏土：

```js
ServerEvents.recipes(event => {
  event.recipes.anvilcraft.bulging()
    .requires("minecraft:dirt")
    .result("minecraft:clay")
    .cauldron("minecraft:water")
})
```