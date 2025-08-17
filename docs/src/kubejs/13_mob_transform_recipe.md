# 生物转换配方 (Mob Transform Recipe)

生物转换配方允许你定义生物实体之间的转换规则，包括基于标签条件的复杂转换。

## 基本结构

```js
ServerEvents.recipes(event => {
  event.custom({
    type: "anvilcraft:mob_transform",
    input: "minecraft:cow",
    results: [
      {
        entity: "minecraft:pig",
        probability: 1.0
      }
    ],
    tagPredicates: [],
    tagModifications: [],
    transformOptions: []
  })
})
```

### 实用方法

```js
ServerEvents.recipes(event => {
  // 生物转换配方 - 不同的构造函数参数组合
  event.recipes.anvilcraft.mob_transform("anvilcraft:cow_to_pig") // 仅ID
  
  event.recipes.anvilcraft.mob_transform(
    "minecraft:cow",                       // 输入生物
    [{ entity: "minecraft:pig", probability: 1.0 }] // 转换结果
  )
  
  // 带谓词、修改和选项的完整版本
  event.recipes.anvilcraft.mob_transform(
    "anvilcraft:cow_to_pig_advanced",     // 配方ID
    "minecraft:cow",                       // 输入生物
    [{ entity: "minecraft:pig", probability: 1.0 }], // 转换结果
    [],                                   // 标签谓词
    [],                                   // 标签修改
    []                                    // 转换选项
  )
})
```

## KubeJS 风格构建器

```js
ServerEvents.recipes(event => {
  event.recipes.anvilcraft.mob_transform()
    .input("minecraft:cow")              // 输入生物
    .addResult("minecraft:pig", 1.0)     // 添加转换结果 (生物, 概率)
})
```

## 参数说明

### input (输入生物)

要转换的原始生物实体类型，如 "minecraft:cow"。

### results (转换结果)

转换可能产生的结果列表：

- `entity`: 目标生物实体类型
- `probability`: 转换概率 (0.0 到 1.0)

### tagPredicates (标签谓词)

转换触发的条件，基于实体的NBT标签值。

### tagModifications (标签修改)

转换后对实体NBT标签的修改。

### transformOptions (转换选项)

控制转换行为的特殊选项。

## 实际示例

### 基础转换

```js
ServerEvents.recipes(event => {
  // 将牛转换为猪
  event.recipes.anvilcraft.mob_transform()
    .input("minecraft:cow")
    .addResult("minecraft:pig")
})
```

### 带概率的转换

```js
ServerEvents.recipes(event => {
  // 将牛转换为猪(70%概率)或羊(30%概率)
  event.recipes.anvilcraft.mob_transform()
    .input("minecraft:cow")
    .addResult("minecraft:pig", 0.7)
    .addResult("minecraft:sheep", 0.3)
})
```

### 带条件的转换

```js
ServerEvents.recipes(event => {
  // 只有在实体有特定标签时才转换
  event.recipes.anvilcraft.mob_transform()
    .input("minecraft:zombie")
    .addResult("minecraft:skeleton")
    .predicate(p => p.numericTag("Health").greaterThan(10))  // 生命值大于10时
})
```

### 修改实体标签

```js
ServerEvents.recipes(event => {
  // 转换并修改实体标签
  event.recipes.anvilcraft.mob_transform()
    .input("minecraft:pig")
    .addResult("minecraft:cow")
    .modification(m => m.set("CustomName", '"Transformed Cow"'))  // 设置自定义名称
})
```

### 使用转换选项

```js
ServerEvents.recipes(event => {
  // 使用转换选项
  event.recipes.anvilcraft.mob_transform()
    .input("minecraft:chicken")
    .addResult("minecraft:bat")
    .transformOption("PRESERVE_EQUIPMENT")  // 保留装备
    .transformOption("COPY_POSITION")       // 复制位置
})
```

## 高级用法

### 复杂的标签条件

```js
ServerEvents.recipes(event => {
  event.recipes.anvilcraft.mob_transform()
    .input("minecraft:skeleton")
    .addResult("minecraft:wither_skeleton")
    .predicate(p => p.numericTag("Health").greaterThan(15))
    .predicate(p => p.distanceToNearestPlayer().lessThan(5))
    .modification(m => m.set("HandItems", "[{id: 'minecraft:stone_sword', Count: 1b}]"))
})
```

### 多个结果和复杂逻辑

```js
ServerEvents.recipes(event => {
  event.recipes.anvilcraft.mob_transform()
    .input("minecraft:villager")
    .addResult("minecraft:witch", 0.3)
    .addResult("minecraft:evoker", 0.2)
    .addResult("minecraft:vindicator", 0.5)
    .predicate(p => p.numericTag("Health").lessThan(10))
    .transformOption("PRESERVE_EQUIPMENT")
})
```

## 可用的谓词条件

- `numericTag(tagName)`: 访问数值型NBT标签
    - `.greaterThan(value)`: 大于指定值
    - `.lessThan(value)`: 小于指定值
    - `.equals(value)`: 等于指定值
    - `.between(min, max)`: 在指定范围内

- `distanceToNearestPlayer()`: 到最近玩家的距离
    - 支持与数值标签相同的比较方法

## 可用的标签修改

- `set(tagName, value)`: 设置NBT标签值
- `add(tagName, value)`: 增加数值标签值
- `multiply(tagName, value)`: 乘以数值标签值
- `remove(tagName)`: 移除标签

## 可用的转换选项

- `PRESERVE_EQUIPMENT`: 保留装备
- `COPY_POSITION`: 复制位置
- `COPY_NAME`: 复制名称
- `PRESERVE_EFFECTS`: 保留效果
