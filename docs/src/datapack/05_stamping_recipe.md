# 冲压配方 (Stamping Recipe)

冲压配方用于在冲压平台上将物品转换为其他物品。

## 基本结构

```json
{
  "type": "anvilcraft:stamping",
  "ingredients": [
    {
      "items": "minecraft:iron_ingot"
    }
  ],
  "results": [
    {
      "id": "anvilcraft:iron_plate",
      "count": 1
    }
  ]
}
```

## 字段说明

### type

固定值 `anvilcraft:stamping`，标识这是一个冲压配方。

### ingredients

配方所需的输入物品列表。每个元素包含：

- `items`: 物品ID（可以是单个物品ID字符串或物品ID数组）
- `count`: 物品数量（可选，默认为1）

### results

配方的输出物品列表。每个元素包含：

- `id`: 物品ID
- `count`: 物品数量

## 使用示例

将铁锭冲压成铁板：

```json
{
  "type": "anvilcraft:stamping",
  "ingredients": [
    {
      "items": "minecraft:iron_ingot"
    }
  ],
  "results": [
    {
      "id": "anvilcraft:iron_plate",
      "count": 1
    }
  ]
}
```