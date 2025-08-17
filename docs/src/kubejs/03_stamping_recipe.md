# 冲压配方 (Stamping Recipe)

冲压配方用于在冲压平台上将物品转换为其他物品。

## 基本结构

```js
ServerEvents.recipes(event => {
  event.custom({
    type: "anvilcraft:stamping",
    ingredients: [
      {
        items: "minecraft:iron_ingot"
      }
    ],
    results: [
      {
        id: "anvilcraft:iron_plate",
        count: 1
      }
    ]
  })
})
```

## 字段说明

### type

固定值 `anvilcraft:stamping`，标识这是一个冲压配方。

### ingredients (输入物品)

输入物品列表，每个元素包含：

- `items`: 物品ID（可以是单个物品ID字符串或物品ID数组）
- `count`: 物品数量（可选，默认为1）

### results (输出物品)

输出物品列表，每个元素包含：

- `id`: 物品ID
- `count`: 物品数量

## 实用方法

```js
ServerEvents.recipes(event => {
  // 冲压 - 不同的构造函数参数组合
  event.recipes.anvilcraft.stamping("anvilcraft:iron_ingot_to_plate") // 仅ID
  
  event.recipes.anvilcraft.stamping(
    "minecraft:iron_ingot",        // 输入
    ["anvilcraft:iron_plate"]     // 结果
  )
})
```

## KubeJS 风格构建器

```js
ServerEvents.recipes(event => {
  event.recipes.anvilcraft.stamping()
    .requires("minecraft:iron_ingot")  // 添加输入
    .result("anvilcraft:iron_plate")   // 添加输出结果
})
```

## 使用示例

将铁锭冲压成铁板：

```js
ServerEvents.recipes(event => {
  event.recipes.anvilcraft.stamping()
    .requires("minecraft:iron_ingot")
    .result("anvilcraft:iron_plate")
})
```