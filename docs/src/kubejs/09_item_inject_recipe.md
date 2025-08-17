# 物品注入配方 (Item Inject Recipe)

物品注入配方将流体注入物品中以创建新物品。

## 基本结构

```js
ServerEvents.recipes(event => {
  event.custom({
    type: "anvilcraft:item_inject",
    ingredients: [
      {
        items: "minecraft:glass_bottle"
      }
    ],
    results: [
      {
        id: "minecraft:potion",
        count: 1
      }
    ],
    input_block: {
      blocks: "minecraft:water"
    },
    result_block: {
      block: {
        name: "minecraft:air"
      },
      chance: 1.0
    }
  })
})
```

## 字段说明

### type

固定值 `anvilcraft:item_inject`，标识这是一个物品注入配方。

### ingredients (输入物品)

输入物品列表，每个元素包含：

- `items`: 物品ID（可以是单个物品ID字符串或物品ID数组）
- `count`: 物品数量（可选，默认为1）

### results (输出物品)

输出物品列表，每个元素包含：

- `id`: 物品ID
- `count`: 物品数量

### input_block (输入方块)

输入方块，包含：

- `blocks`: 方块ID（可以是单个方块ID字符串或方块ID数组）

### result_block (输出方块)

输出方块，包含：

- `block`: 方块状态对象，包含方块名称和其他属性
- `chance`: 结果出现的概率（0.0到1.0之间）

## 实用方法

```js
ServerEvents.recipes(event => {
  // 物品注入配方 - 不同的构造函数参数组合
  event.recipes.anvilcraft.item_inject("anvilcraft:glass_bottle_to_potion") // 仅ID
  
  event.recipes.anvilcraft.item_inject(
    "minecraft:glass_bottle",             // 输入物品
    [{ id: "minecraft:potion", count: 1 }],// 输出物品
    { blocks: "minecraft:water" },        // 输入方块
    {                                    // 输出方块
      block: { name: "minecraft:air" },
      chance: 1.0
    }
  )
  
  // 简化版本（不包含输出物品）
  event.recipes.anvilcraft.item_inject(
    "minecraft:glass_bottle",             // 输入物品
    { blocks: "minecraft:water" },        // 输入方块
    {                                    // 输出方块
      block: { name: "minecraft:air" },
      chance: 1.0
    }
  )
})
```

## KubeJS 风格构建器

```js
ServerEvents.recipes(event => {
  event.recipes.anvilcraft.item_inject()
    .requires("minecraft:glass_bottle")  // 输入物品
    .result("minecraft:potion")          // 输出物品
    .inputBlock("minecraft:water")       // 输入方块
    .resultBlock("minecraft:air")        // 输出方块
})
```

## 使用示例

将玻璃瓶注入水制作成药水：

```js
ServerEvents.recipes(event => {
  event.recipes.anvilcraft.item_inject()
    .requires("minecraft:glass_bottle")
    .result("minecraft:potion")
    .inputBlock("minecraft:water")
    .resultBlock("minecraft:air")
})
```