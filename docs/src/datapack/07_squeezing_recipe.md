# 挤压配方 (Squeezing Recipe)

挤压配方用于使用炼药锅中的流体将方块转换为其他方块。

## 基本结构

```json
{
  "type": "anvilcraft:squeezing",
  "ingredients": [
    {
      "blocks": "minecraft:wet_sponge"
    }
  ],
  "results": [
    {
      "block": {
        "Name": "minecraft:sponge"
      },
      "chance": 1.0
    }
  ],
  "cauldron": {
    "fluid": "minecraft:water",
    "consume": -1
  }
}
```

## 字段说明

### type

固定值 `anvilcraft:squeezing`，标识这是一个挤压配方。

### ingredients

配方所需的输入方块列表。每个元素包含：

- `blocks`: 方块ID（可以是单个方块ID字符串或方块ID数组）

### results

配方的输出结果列表。每个元素包含：

- `block`: 方块状态对象，包含方块名称和其他属性
- `chance`: 结果出现的概率（0.0到1.0之间）

### cauldron

炼药锅相关设置：

- `fluid`: 流体类型
- `consume`: 消耗量（可选，负数表示产生流体，正数表示消耗流体）

## 使用示例

将湿海绵挤压成干海绵：

```json
{
  "type": "anvilcraft:squeezing",
  "ingredients": [
    {
      "blocks": "minecraft:wet_sponge"
    }
  ],
  "results": [
    {
      "block": {
        "Name": "minecraft:sponge"
      },
      "chance": 1.0
    }
  ],
  "cauldron": {
    "fluid": "minecraft:water",
    "consume": -1
  }
}
```