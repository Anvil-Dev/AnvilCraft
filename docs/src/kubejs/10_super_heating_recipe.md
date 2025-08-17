# 超级加热配方 (Super Heating Recipe)

超级加热配方使用高温将物品转换为其他物品。

## 基本结构

```js
ServerEvents.recipes(event => {
  event.custom({
    type: "anvilcraft:super_heating",
    ingredients: [
      {
        items: "minecraft:sand"
      }
    ],
    results: [
      {
        id: "minecraft:glass",
        count: 1
      }
    ],
    fluid: "minecraft:lava"
  })
})
```

## 字段说明

### type

固定值 `anvilcraft:super_heating`，标识这是一个超级加热配方。

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
  // 超级加热配方 - 不同的构造函数参数组合
  event.recipes.anvilcraft.super_heating("anvilcraft:sand_to_glass") // 仅ID
  
  event.recipes.anvilcraft.super_heating(
    "minecraft:sand",                     // 输入
    [{ id: "minecraft:glass", count: 1 }]// 输出
  )
  
  event.recipes.anvilcraft.super_heating(
    "minecraft:sand",                     // 输入
    [{ id: "minecraft:glass", count: 1 }],// 输出
    "minecraft:lava"                     // 流体
  )
})
```

## KubeJS 风格构建器

```js
ServerEvents.recipes(event => {
  event.recipes.anvilcraft.super_heating()
    .requires("minecraft:sand")          // 输入物品
    .result("minecraft:glass")           // 输出物品
    .cauldron("minecraft:lava")          // 需要的流体
})
```

## 使用示例

将沙子超级加热成玻璃：

```js
ServerEvents.recipes(event => {
  event.recipes.anvilcraft.super_heating()
    .requires("minecraft:sand")
    .result("minecraft:glass")
    .cauldron("minecraft:lava")
})
```