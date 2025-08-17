# 方块粉碎配方 (Block Crush Recipe)

方块粉碎配方用于将方块粉碎成更小的方块或物品。

## 基本结构

```json
{
  "type": "anvilcraft:block_crush",
  "inputs": [
    {
      "blocks": "minecraft:cobblestone"
    }
  ],
  "results": [
    {
      "id": "minecraft:gravel",
      "count": 1
    }
  ]
}
```

## 字段说明

### type

固定值 `anvilcraft:block_crush`，标识这是一个方块粉碎配方。

### inputs

配方所需的输入方块列表。每个元素包含：

- `blocks`: 方块ID（可以是单个方块ID字符串或方块ID数组）

### results

配方的输出物品列表。每个元素包含：

- `id`: 物品ID
- `count`: 物品数量

## 使用示例

将圆石粉碎成沙砾：

```json
{
  "type": "anvilcraft:block_crush",
  "inputs": [
    {
      "blocks": "minecraft:cobblestone"
    }
  ],
  "results": [
    {
      "id": "minecraft:gravel",
      "count": 1
    }
  ]
}
```