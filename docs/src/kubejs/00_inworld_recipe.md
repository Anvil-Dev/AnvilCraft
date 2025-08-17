# InWorld 配方 (InWorld Recipe)

InWorld 配方系统是 AnvilCraft 中用于处理世界内配方的核心系统，它允许在特定条件下触发各种效果。

## 基本结构

```js
ServerEvents.recipes(event => {
  event.custom({
    type: "anvilcraft:in_world",
    icon: {
      item: "minecraft:anvil"
    },
    trigger: "anvilcraft:on_anvil_fall_on",
    conflicting: [],
    non_conflicting: [],
    outcomes: [],
    priority: 0,
    compatible: true
  })
})
```

## 字段说明

### type

固定值 `anvilcraft:in_world`，标识这是一个 InWorld 配方。

### icon (配方图标)

配方图标，用于在配方界面中显示：

- `item`: 物品ID，如 "minecraft:anvil"

### trigger (触发器)

触发器类型，决定配方何时被触发：

- 目前支持 `anvilcraft:on_anvil_fall_on` （当铁砧落下时触发）

### conflicting (冲突谓词)

冲突谓词列表，这些谓词之间相互冲突。

### non_conflicting (非冲突谓词)

非冲突谓词列表，这些谓词之间不冲突。

### outcomes (配方结果)

配方结果列表，当配方匹配时执行。

### priority (优先级)

配方优先级，数值越高优先级越高。

### compatible (兼容模式)

是否兼容模式，决定谓词匹配方式。

## 实用方法

```js
ServerEvents.recipes(event => {
  // InWorld配方 - 不同的构造函数参数组合
  event.recipes.anvilcraft.in_world("anvilcraft:iron_ingot_to_nugget") // 仅ID
  
  event.recipes.anvilcraft.in_world(
    { item: "minecraft:iron_nugget" },    // 图标
    "anvilcraft:on_anvil_fall_on",        // 触发器
    [],                                   // 冲突谓词
    [{                                   // 非冲突谓词
      type: "anvilcraft:has_item_ingredient",
      offset: [0, -1, 0],
      item: {
        items: "minecraft:iron_ingot"
      }
    }],
    [{                                   // 结果
      type: "anvilcraft:spawn_item",
      offset: [0, -1, 0],
      item: {
        id: "minecraft:iron_nugget"
      },
      count: 1.0
    }],
    0,                                   // 优先级
    true                                 // 兼容模式
  )
})
```

## KubeJS 风格构建器

```js
ServerEvents.recipes(event => {
  event.recipes.anvilcraft.in_world()
    .icon("minecraft:iron_nugget")                    // 设置图标
    .trigger("anvilcraft:on_anvil_fall_on")           // 设置触发器
    .hasItemIngredient("minecraft:iron_ingot")        // 添加物品谓词
    .below()                                          // 设置偏移位置在下方
    .spawnItem("minecraft:iron_nugget")               // 添加生成物品结果
    .priority(0)                                      // 设置优先级
    .compatible(true)                                 // 设置兼容模式
})
```

## 偏移量设置方法

### offset(Vec3 offset)

设置偏移量为指定向量。

### offset(double x, double y, double z)

设置偏移量为指定坐标。

### below(double below)

设置偏移量在当前位置下方指定距离。

### below()

设置偏移量在当前位置下方1格。

### above(double above)

设置偏移量在当前位置上方指定距离。

### above()

设置偏移量在当前位置上方1格。

## 谓词方法

### hasItem(...)

检查指定位置是否存在指定物品：

```js
ServerEvents.recipes(event => {
  event.recipes.anvilcraft.in_world()
    .hasItem("minecraft:iron_ingot")                 // 检查是否有铁锭
    .hasItem(0, -1, 0, "minecraft:iron_ingot")       // 在指定坐标检查是否有铁锭
})
```

### hasItemIngredient(...)

检查指定位置是否存在指定物品，如果配方匹配则消耗该物品：

```js
ServerEvents.recipes(event => {
  event.recipes.anvilcraft.in_world()
    .hasItemIngredient("minecraft:iron_ingot")       // 检查并消耗铁锭
})
```

### hasBlock(...)

检查指定位置是否存在指定方块：

```js
ServerEvents.recipes(event => {
  event.recipes.anvilcraft.in_world()
    .hasBlock("minecraft:iron_block")                // 检查是否有铁块
})
```

### hasBlockIngredient(...)

检查指定位置是否存在指定方块，如果配方匹配则清除该方块：

```js
ServerEvents.recipes(event => {
  event.recipes.anvilcraft.in_world()
    .hasBlockIngredient("minecraft:iron_block")      // 检查并清除铁块
})
```

### hasCauldron(...)

检查指定位置是否存在指定流体的炼药锅：

```js
ServerEvents.recipes(event => {
  event.recipes.anvilcraft.in_world()
    .hasCauldron("minecraft:water")                  // 检查是否有装水的炼药锅
    .hasCauldron("minecraft:water", 1)               // 检查并消耗1单位水
})
```

## 结果方法

### spawnItem(...)

在指定位置生成物品：

```js
ServerEvents.recipes(event => {
  event.recipes.anvilcraft.in_world()
    .spawnItem("minecraft:iron_nugget")              // 在偏移位置生成铁粒
    .spawnItem(0, -1, 0, "minecraft:iron_nugget")    // 在指定坐标生成铁粒
    .spawnItem(0, -1, 0, 0.5, "minecraft:iron_nugget") // 在指定坐标以50%概率生成铁粒
})
```

### setBlock(...)

在指定位置设置方块：

```js
ServerEvents.recipes(event => {
  event.recipes.anvilcraft.in_world()
    .setBlock("minecraft:diamond_block")             // 在偏移位置设置钻石块
})
```

### damageAnvil()

损坏铁砧：

```js
ServerEvents.recipes(event => {
  event.recipes.anvilcraft.in_world()
    .damageAnvil()                                   // 损坏铁砧
})
```

## 使用示例

### 基础示例

当铁砧砸在铁锭上时，生成铁粒并消耗铁锭：

```js
ServerEvents.recipes(event => {
  event.recipes.anvilcraft.in_world()
    .icon("minecraft:iron_nugget")
    .trigger("anvilcraft:on_anvil_fall_on")
    .hasItemIngredient("minecraft:iron_ingot")
    .below()
    .spawnItem("minecraft:iron_nugget")
    .below()
})
```

### 复杂示例

使用多方块结构制作钻石块：

```js
ServerEvents.recipes(event => {
  event.recipes.anvilcraft.in_world()
    .icon("minecraft:diamond_block")
    .trigger("anvilcraft:on_anvil_fall_on")
    .hasBlock("minecraft:diamond_block")
    .hasBlockIngredient("minecraft:iron_block")
    .above()
    .setBlock("minecraft:diamond_block")
    .damageAnvil()
    .priority(10)
})
```