# 生物转换配方 (Mob Transform Recipe)

生物转换配方允许你定义生物实体之间的转换规则，包括基于标签条件的复杂转换。

## 基本结构

```json
{
  "type": "anvilcraft:mob_transform",
  "input": "minecraft:cow",
  "results": [
    {
      "entity": "minecraft:pig",
      "probability": 1.0
    }
  ],
  "tagPredicates": [],
  "tagModifications": [],
  "transformOptions": []
}
```

## 字段说明

### type

固定值 `anvilcraft:mob_transform`，标识这是一个生物转换配方。

### input

要转换的原始生物实体类型，如 "minecraft:cow"。

### results

转换可能产生的结果列表：

- `entity`: 目标生物实体类型
- `probability`: 转换概率 (0.0 到 1.0)

### tagPredicates

转换触发的条件，基于实体的NBT标签值。

### tagModifications

转换后对实体NBT标签的修改。

### transformOptions

控制转换行为的特殊选项。

## 标签谓词 (tagPredicates)

标签谓词用于定义转换触发的条件，基于实体的NBT标签值。

### 数值标签谓词

```json
{
  "tagPath": "Health",
  "comparison": ">",
  "value": 10.0
}
```

支持的比较操作符：

- `>`: 大于
- `<`: 小于
- `>=`: 大于等于
- `<=`: 小于等于
- `==`: 等于
- `!=`: 不等于

### 距离谓词

```json
{
  "type": "distance_to_nearest_player",
  "comparison": "<",
  "value": 5.0
}
```

## 标签修改 (tagModifications)

标签修改用于在转换后修改实体的NBT标签。

### 设置标签值

```json
{
  "operation": "SET",
  "tagPath": "CustomName",
  "value": "{\"text\":\"Transformed Cow\"}"
}
```

### 增加数值标签

```json
{
  "operation": "ADD",
  "tagPath": "Health",
  "value": 10.0
}
```

### 乘以数值标签

```json
{
  "operation": "MULTIPLY",
  "tagPath": "Health",
  "value": 2.0
}
```

### 移除标签

```json
{
  "operation": "REMOVE",
  "tagPath": "CustomName"
}
```

## 转换选项 (transformOptions)

转换选项控制转换行为的特殊选项：

- `PRESERVE_EQUIPMENT`: 保留装备
- `COPY_POSITION`: 复制位置
- `COPY_NAME`: 复制名称
- `PRESERVE_EFFECTS`: 保留效果

## 使用示例

### 基础转换

```json
{
  "type": "anvilcraft:mob_transform",
  "input": "minecraft:cow",
  "results": [
    {
      "entity": "minecraft:pig",
      "probability": 1.0
    }
  ]
}
```

### 带概率的转换

```json
{
  "type": "anvilcraft:mob_transform",
  "input": "minecraft:cow",
  "results": [
    {
      "entity": "minecraft:pig",
      "probability": 0.7
    },
    {
      "entity": "minecraft:sheep",
      "probability": 0.3
    }
  ]
}
```

### 带条件的转换

```json
{
  "type": "anvilcraft:mob_transform",
  "input": "minecraft:zombie",
  "results": [
    {
      "entity": "minecraft:skeleton",
      "probability": 1.0
    }
  ],
  "tagPredicates": [
    {
      "tagPath": "Health",
      "comparison": ">",
      "value": 10.0
    }
  ]
}
```

### 修改实体标签

```json
{
  "type": "anvilcraft:mob_transform",
  "input": "minecraft:pig",
  "results": [
    {
      "entity": "minecraft:cow",
      "probability": 1.0
    }
  ],
  "tagModifications": [
    {
      "operation": "SET",
      "tagPath": "CustomName",
      "value": "{\"text\":\"Transformed Cow\"}"
    }
  ]
}
```

### 使用转换选项

```json
{
  "type": "anvilcraft:mob_transform",
  "input": "minecraft:chicken",
  "results": [
    {
      "entity": "minecraft:bat",
      "probability": 1.0
    }
  ],
  "transformOptions": [
    "PRESERVE_EQUIPMENT",
    "COPY_POSITION"
  ]
}
```

### 复杂的标签条件

```json
{
  "type": "anvilcraft:mob_transform",
  "input": "minecraft:skeleton",
  "results": [
    {
      "entity": "minecraft:wither_skeleton",
      "probability": 1.0
    }
  ],
  "tagPredicates": [
    {
      "tagPath": "Health",
      "comparison": ">",
      "value": 15.0
    },
    {
      "type": "distance_to_nearest_player",
      "comparison": "<",
      "value": 5.0
    }
  ],
  "tagModifications": [
    {
      "operation": "SET",
      "tagPath": "HandItems",
      "value": "[{id: \"minecraft:stone_sword\", Count: 1b}]"
    }
  ]
}
```