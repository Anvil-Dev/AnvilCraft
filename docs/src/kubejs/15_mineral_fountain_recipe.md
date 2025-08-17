# 矿物涌泉配方 (Mineral Fountain Recipe)

矿物涌泉配方用于定义矿物涌泉的方块转换规则，包括基本转换和带概率的转换。

## 基本矿物涌泉配方 (mineral_fountain)

### 基本结构

```js
ServerEvents.recipes(event => {
  event.custom({
    type: "anvilcraft:mineral_fountain",
    need_block: "minecraft:stone",
    from_block: "minecraft:cobblestone",
    to_block: "minecraft:andesite"
  })
})
```

### 实用方法

```js
ServerEvents.recipes(event => {
  // 基本矿物涌泉配方 - 不同的构造函数参数组合
  event.recipes.anvilcraft.mineral_fountain("anvilcraft:cobblestone_to_andesite") // 仅ID
  
  event.recipes.anvilcraft.mineral_fountain(
    "minecraft:stone",                      // 需要的方块
    "minecraft:cobblestone",                // 被转换的方块
    "minecraft:andesite"                    // 转换后的方块
  )
})
```

## KubeJS 风格构建器

```js
ServerEvents.recipes(event => {
  event.recipes.anvilcraft.mineral_fountain()
    .needBlock("minecraft:stone")        // 需要的方块
    .fromBlock("minecraft:cobblestone")  // 被转换的方块
    .toBlock("minecraft:andesite")       // 转换后的方块
})
```

## 概率矿物涌泉配方 (mineral_fountain_chance)

### 基本结构

```js
ServerEvents.recipes(event => {
  event.custom({
    type: "anvilcraft:mineral_fountain_chance",
    dimension: "minecraft:overworld",
    from_block: "minecraft:stone",
    to_block: "minecraft:diamond_ore",
    chance: 0.05
  })
})
```

### 实用方法

```js
ServerEvents.recipes(event => {
  // 概率矿物涌泉配方 - 不同的构造函数参数组合
  event.recipes.anvilcraft.mineral_fountain_chance("anvilcraft:stone_to_diamond_ore") // 仅ID
  
  event.recipes.anvilcraft.mineral_fountain_chance(
    "minecraft:overworld",                  // 维度
    "minecraft:stone",                      // 被转换的方块
    "minecraft:diamond_ore",                // 转换后的方块
    0.05                                    // 转换概率
  )
})
```

### KubeJS 风格构建器

```js
ServerEvents.recipes(event => {
  event.recipes.anvilcraft.mineral_fountain_chance()
    .dimension("minecraft:overworld")   // 维度
    .fromBlock("minecraft:stone")       // 被转换的方块
    .toBlock("minecraft:diamond_ore")   // 转换后的方块
    .chance(0.05)                       // 转换概率 (5%)
})
```

## 参数说明

### mineral_fountain 配方参数

- `need_block`: 触发转换所需的方块
- `from_block`: 需要被转换的方块
- `to_block`: 转换后的方块

### mineral_fountain_chance 配方参数

- `dimension`: 配方有效的维度（使用资源位置格式，如 "minecraft:overworld"）
- `from_block`: 需要被转换的方块
- `to_block`: 转换后的方块
- `chance`: 转换概率，范围从 0.0 到 1.0

## 实际示例

### 基本矿物涌泉

```js
ServerEvents.recipes(event => {
  // 当矿物涌泉附近有花岗岩时，将圆石转换为闪长岩
  event.recipes.anvilcraft.mineral_fountain()
    .needBlock("minecraft:granite")
    .fromBlock("minecraft:cobblestone")
    .toBlock("minecraft:diorite")
})
```

### 带概率的矿物涌泉

```js
ServerEvents.recipes(event => {
  // 在下界中，将下界岩以10%概率转换为远古残骸
  event.recipes.anvilcraft.mineral_fountain_chance()
    .dimension("minecraft:the_nether")
    .fromBlock("minecraft:netherrack")
    .toBlock("minecraft:ancient_debris")
    .chance(0.1)
})
```

### 多个矿物涌泉配方

```js
ServerEvents.recipes(event => {
  // 在主世界中不同条件下的多种转换
  event.recipes.anvilcraft.mineral_fountain()
    .needBlock("minecraft:diamond_block")
    .fromBlock("minecraft:stone")
    .toBlock("minecraft:diamond_ore")
    
  event.recipes.anvilcraft.mineral_fountain_chance()
    .dimension("minecraft:overworld")
    .fromBlock("minecraft:deepslate")
    .toBlock("minecraft:deepslate_diamond_ore")
    .chance(0.02)
})
```

## 维度说明

可以使用以下维度资源位置：

- `minecraft:overworld` - 主世界
- `minecraft:the_nether` - 下界
- `minecraft:the_end` - 末地
- 或者其他模组添加的自定义维度

## 注意事项

1. [mineral_fountain](file:///D:/Projects/repos/AnvilCraft/src/main/java/dev/dubhe/anvilcraft/integration/kubejs/recipe/mineral/MineralFountainRecipeSchema.java#L37)
   配方需要附近有指定的 [need_block](file:///D:/Projects/repos/AnvilCraft/src/main/java/dev/dubhe/anvilcraft/integration/kubejs/recipe/mineral/MineralFountainRecipeSchema.java#L34)
   方块才能触发
2. [mineral_fountain_chance](file:///D:/Projects/repos/AnvilCraft/src/main/java/dev/dubhe/anvilcraft/integration/kubejs/recipe/mineral/MineralFountainChanceRecipeSchema.java#L47)
   配方只在指定维度内有效
3. 概率值必须在 0.0 到 1.0 之间，其中 0.0 表示 0% 概率，1.0 表示 100% 概率
4. 所有方块ID必须使用标准的 Minecraft 资源位置格式
