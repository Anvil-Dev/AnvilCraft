# 时移配方 (Time Warp Recipe)

时移配方用于使用时间力量将物品转换为其他物品。

## 基本结构

```json
{
  "type": "anvilcraft:timewarp",
  "ingredients": [
    {
      "items": "minecraft:rotten_flesh"
    }
  ],
  "results": [
    {
      "id": "minecraft:leather",
      "count": 1
    }
  ]
}
```

## 字段说明

### type

固定值 `anvilcraft:timewarp`，标识这是一个时移配方。

### ingredients

配方所需的输入物品列表。每个元素包含：

- `items`: 物品ID（可以是单个物品ID字符串或物品ID数组）
- `count`: 物品数量（可选，默认为1）

### results

配方的输出物品列表。每个元素包含：

- `id`: 物品ID
- `count`: 物品数量

## 使用示例

将腐肉时移成皮革：

```json
{
  "type": "anvilcraft:timewarp",
  "ingredients": [
    {
      "items": "minecraft:rotten_flesh"
    }
  ],
  "results": [
    {
      "id": "minecraft:leather",
      "count": 1
    }
  ]
}
```