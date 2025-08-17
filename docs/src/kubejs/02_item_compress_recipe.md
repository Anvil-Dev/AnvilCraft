# 物品压缩配方 (Item Compress Recipe)

物品压缩配方用于将多个相同物品压缩成一个更高级的物品。

## 基本结构

```js
ServerEvents.recipes(event => {
  event.custom({
    type: "anvilcraft:item_compress",
    ingredients: [
      {
        items: "minecraft:iron_nugget",
        count: 9
      }
    ],
    results: [
      {
        id: "minecraft:iron_ingot",
        count: 1
      }
    ]
  })
})
```

## 字段说明

### type

固定值 `anvilcraft:item_compress`，标识这是一个物品压缩配方。

### ingredients (输入物品)

输入物品列表，每个元素包含：

- `items`: 物品ID（可以是单个物品ID字符串或物品ID数组）
- `count`: 物品数量

### results (输出物品)

输出物品列表，每个元素包含：

- `id`: 物品ID
- `count`: 物品数量

## 实用方法

```js
ServerEvents.recipes(event => {
  // 物品压缩 - 不同的构造函数参数组合
  event.recipes.anvilcraft.item_compress("anvilcraft:iron_nugget_to_ingot") // 仅ID
  
  event.recipes.anvilcraft.item_compress(
    "minecraft:iron_nugget 9",     // 输入 (9个铁粒)
    ["minecraft:iron_ingot"]      // 结果
  )
})
```

## KubeJS 风格构建器

```js
ServerEvents.recipes(event => {
  event.recipes.anvilcraft.item_compress()
    .requires("minecraft:iron_nugget", 9)  // 添加输入
    .result("minecraft:iron_ingot")        // 添加输出结果
})
```

## 使用示例

将9个铁粒压缩成1个铁锭：

```js
ServerEvents.recipes(event => {
  event.recipes.anvilcraft.item_compress()
    .requires("minecraft:iron_nugget", 9)
    .result("minecraft:iron_ingot")
})
```