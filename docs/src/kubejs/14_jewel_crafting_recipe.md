# 珠宝制作配方 (Jewel Crafting Recipe)

珠宝制作配方用于制作各种珠宝和装饰性物品。

## 基本结构

```js
ServerEvents.recipes(event => {
  event.custom({
    type: "anvilcraft:jewel_crafting",
    ingredients: [
      {
        tag: "forge:gems/diamond"
      },
      {
        tag: "forge:ingots/gold"
      },
      {
        tag: "forge:ingots/gold"
      }
    ],
    result: {
      id: "anvilcraft:amber_block",
      count: 1
    }
  })
})
```

### 实用方法

```js
ServerEvents.recipes(event => {
  // 珠宝制作配方 - 不同的构造函数参数组合
  event.recipes.anvilcraft.jewel_crafting("anvilcraft:diamond_to_amber") // 仅ID
  
  event.recipes.anvilcraft.jewel_crafting(
    [                                      // 输入材料列表
      { tag: "forge:gems/diamond" },
      { tag: "forge:ingots/gold" },
      { tag: "forge:ingots/gold" }
    ],
    { id: "anvilcraft:amber_block", count: 1 } // 输出
  )
})
```

## KubeJS 风格构建器

```js
ServerEvents.recipes(event => {
  event.recipes.anvilcraft.jewel_crafting()
    .requires("#forge:gems/diamond")     // 添加材料
    .requires("#forge:ingots/gold", 2)   // 添加2个金锭
    .result("anvilcraft:amber_block")    // 设置结果
})
```

## 参数说明

### ingredients (输入材料)

输入材料列表，每个元素是一个标准的 Minecraft 配方材料：

- 可以是物品ID，如 `"minecraft:diamond"`
- 可以是标签，如 `"#forge:gems/diamond"`
- 可以是复杂的材料对象

### result (输出物品)

配方的输出物品：

- `id`: 物品ID
- `count`: 物品数量

## 实际示例

### 简单珠宝制作

```js
ServerEvents.recipes(event => {
  // 用钻石和2个金锭制作琥珀块
  event.recipes.anvilcraft.jewel_crafting()
    .requires("minecraft:diamond")
    .requires("#forge:ingots/gold", 2)
    .result("anvilcraft:amber_block")
})
```

### 复杂珠宝制作

```js
ServerEvents.recipes(event => {
  // 用多个材料制作皇家头冠
  event.recipes.anvilcraft.jewel_crafting()
    .requires("#forge:gems/emerald", 3)
    .requires("#forge:ingots/gold", 5)
    .requires("minecraft:nether_star")
    .result("anvilcraft:royal_crown")
})
```

## 使用原版配方材料

你也可以使用标准的 Minecraft 配方材料格式：

```js
ServerEvents.recipes(event => {
  event.recipes.anvilcraft.jewel_crafting()
    .requires({
      type: "minecraft:item",
      item: "minecraft:diamond"
    })
    .requires({
      tag: "forge:ingots/gold"
    }, 2)
    .result("anvilcraft:amber_block")
})
```

## 注意事项

1. 珠宝制作配方可以接受任意数量的输入材料
2. 材料的排列顺序不影响配方匹配
3. 可以使用物品标签来增加配方的灵活性
4. 输出物品只能有一个，不能像其他配方类型那样有多个输出
