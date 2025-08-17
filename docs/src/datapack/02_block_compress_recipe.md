# 方块压缩配方 (Block Compress Recipe)

方块压缩配方用于将多个相同方块压缩成一个更高级的方块。

## 基本结构

```json
{
  "type": "anvilcraft:block_compress",
  "inputs": [
    {
      "blocks": "minecraft:iron_block"
    }
  ],
  "results": [
    {
      "block": {
        "name": "anvilcraft:compressed_iron_block"
      },
      "chance": 1.0
    }
  ]
}
```

## 字段说明

### type

固定值 `anvilcraft:block_compress`，标识这是一个方块压缩配方。

### inputs

配方所需的输入方块列表。每个元素包含：

- `blocks`: 方块ID（可以是单个方块ID字符串或方块ID数组）

### results

配方的输出结果列表。每个元素包含：

- `block`: 方块状态对象，包含方块名称和其他属性
- `chance`: 结果出现的概率（0.0到1.0之间）

## 使用示例

将铁块压缩成压缩铁块：

```json
{
  "type": "anvilcraft:block_compress",
  "inputs": [
    {
      "blocks": "minecraft:iron_block"
    }
  ],
  "results": [
    {
      "block": {
        "name": "anvilcraft:compressed_iron_block"
      },
      "chance": 1.0
    }
  ]
}
```