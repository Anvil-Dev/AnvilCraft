# 物品粉碎配方 (Item Crush Recipe)

物品粉碎配方用于将物品粉碎成更小的物品。

## 基本结构

```js
ServerEvents.recipes(event => {
    event.custom({
        type: "anvilcraft:item_crush",
        ingredients: [
            {
                items: "minecraft:iron_ingot"
            }
        ],
        results: [
            {
                id: "minecraft:iron_nugget",
                count: 3
            }
        ]
    })
})
```

## 字段说明

### type

固定值 `anvilcraft:item_crush`，标识这是一个物品粉碎配方。

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
    // 物品粉碎 - 不同的构造函数参数组合
    event.recipes.anvilcraft.item_crush("anvilcraft:iron_ingot_to_nuggets") // 仅ID

    event.recipes.anvilcraft.item_crush(
        "minecraft:iron_ingot",              // 输入
        ["minecraft:iron_nugget 3"]         // 结果
    )
})
```

## KubeJS 风格构建器

```js
ServerEvents.recipes(event => {
    event.recipes.anvilcraft.item_crush()
        .requires("minecraft:iron_ingot")  // 添加输入
        .result("minecraft:iron_nugget", 3) // 添加输出结果
})
```

## 使用示例

将铁锭粉碎成铁粒：

```js
ServerEvents.recipes(event => {
    event.recipes.anvilcraft.item_crush()
        .requires("minecraft:iron_ingot")
        .result("minecraft:iron_nugget", 3)
})
```