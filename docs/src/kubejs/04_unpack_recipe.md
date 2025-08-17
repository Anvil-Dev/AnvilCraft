# 解包配方 (Unpack Recipe)

解包配方用于将压缩物品解包为原始物品。

## 基本结构

```js
ServerEvents.recipes(event => {
  event.custom({
    type: "anvilcraft:unpack",
    ingredients: [
      {
        items: "minecraft:iron_block"
      }
    ],
    results: [
      {
        id: "minecraft:iron_ingot",
        count: 9
      }
    ]
  })
})
```

## 字段说明

### type

固定值 `anvilcraft:unpack`，标识这是一个解包配方。

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
  // 解包 - 不同的构造函数参数组合
  event.recipes.anvilcraft.unpack("anvilcraft:ingot_to_nuggets") // 仅ID
  
  event.recipes.anvilcraft.unpack(
    "minecraft:iron_ingot",        // 输入
    ["minecraft:iron_nugget 9"]   // 结果
  )
})
```

## KubeJS 风格构建器

```js
ServerEvents.recipes(event => {
  event.recipes.anvilcraft.unpack()
    .requires("minecraft:iron_block")   // 添加输入
    .result("minecraft:iron_ingot", 9)  // 添加输出结果
})
```

## 使用示例

将铁块解包成铁锭：

```js
ServerEvents.recipes(event => {
  event.recipes.anvilcraft.unpack()
    .requires("minecraft:iron_block")
    .result("minecraft:iron_ingot", 9)
})
```