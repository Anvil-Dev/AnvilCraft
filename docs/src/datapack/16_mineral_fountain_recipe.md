# 矿物涌泉配方 (Mineral Fountain Recipe)

矿物涌泉配方用于定义矿物涌泉的方块转换规则，包括基本转换和带概率的转换。

## 基本矿物涌泉配方

### 基本结构

```json
{
  "type": "anvilcraft:mineral_fountain",
  "need_block": "minecraft:stone",
  "from_block": "minecraft:cobblestone",
  "to_block": "minecraft:andesite"
}
```

## 字段说明

### type

固定值 `anvilcraft:mineral_fountain`，标识这是一个基本矿物涌泉配方。

### need_block

触发转换所需的方块。

### from_block

需要被转换的方块。

### to_block

转换后的方块。

## 使用示例

### 基本矿物涌泉

```json
{
  "type": "anvilcraft:mineral_fountain",
  "need_block": "minecraft:granite",
  "from_block": "minecraft:cobblestone",
  "to_block": "minecraft:diorite"
}
```

## 概率矿物涌泉配方

### 基本结构

```json
{
  "type": "anvilcraft:mineral_fountain_chance",
  "dimension": "minecraft:overworld",
  "from_block": "minecraft:stone",
  "to_block": "minecraft:diamond_ore",
  "chance": 0.05
}
```

## 字段说明

### type

固定值 `anvilcraft:mineral_fountain_chance`，标识这是一个概率矿物涌泉配方。

### dimension

配方有效的维度（使用资源位置格式，如 "minecraft:overworld"）。

### from_block

需要被转换的方块。

### to_block

转换后的方块。

### chance

转换概率，范围从 0.0 到 1.0。

## 使用示例

### 带概率的矿物涌泉

```json
{
  "type": "anvilcraft:mineral_fountain_chance",
  "dimension": "minecraft:the_nether",
  "from_block": "minecraft:netherrack",
  "to_block": "minecraft:ancient_debris",
  "chance": 0.1
}
```

### 多个矿物涌泉配方

```json
[
  {
    "type": "anvilcraft:mineral_fountain",
    "need_block": "minecraft:diamond_block",
    "from_block": "minecraft:stone",
    "to_block": "minecraft:diamond_ore"
  },
  {
    "type": "anvilcraft:mineral_fountain_chance",
    "dimension": "minecraft:overworld",
    "from_block": "minecraft:deepslate",
    "to_block": "minecraft:deepslate_diamond_ore",
    "chance": 0.02
  }
]
```

## 维度说明

可以使用以下维度资源位置：

- `minecraft:overworld` - 主世界
- `minecraft:the_nether` - 下界
- `minecraft:the_end` - 末地
- 或者其他模组添加的自定义维度

## 注意事项

1. 基本矿物涌泉配方需要附近有指定的 need_block 方块才能触发
2. 概率矿物涌泉配方只在指定维度内有效
3. 概率值必须在 0.0 到 1.0 之间，其中 0.0 表示 0% 概率，1.0 表示 100% 概率
4. 所有方块ID必须使用标准的 Minecraft 资源位置格式