# 物品粉碎配方 (Item Crush Recipe)

物品粉碎配方用于将物品粉碎成更小的物品。

## 基本结构

```json
{
  "type": "anvilcraft:item_crush",
  "ingredients": [
    {
      "items": "minecraft:iron_ingot"
    }
  ],
  "results": [
    {
      "id": "minecraft:iron_nugget",
      "count": 3
    }
  ]
}
```

## 字段说明

### type

固定值 `anvilcraft:item_crush`，标识这是一个物品粉碎配方。

### ingredients

配方所需的输入物品列表。每个元素包含：

- `items`: 物品ID（可以是单个物品ID字符串或物品ID数组）
- `count`: 物品数量（可选，默认为1）

### results

配方的输出物品列表。每个元素包含：

- `id`: 物品ID
- `count`: 物品数量

## 使用示例

将铁锭粉碎成铁粒：

```json
{
  "type": "anvilcraft:item_crush",
  "ingredients": [
    {
      "items": "minecraft:iron_ingot"
    }
  ],
  "results": [
    {
      "id": "minecraft:iron_nugget",
      "count": 3
    }
  ]
}
```