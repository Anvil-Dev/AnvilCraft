# 物品压缩配方 (Item Compress Recipe)

物品压缩配方用于将多个相同物品压缩成一个更高级的物品。

## 基本结构

```json
{
  "type": "anvilcraft:item_compress",
  "ingredients": [
    {
      "items": "minecraft:iron_nugget",
      "count": 9
    }
  ],
  "results": [
    {
      "id": "minecraft:iron_ingot",
      "count": 1
    }
  ]
}
```

## 字段说明

### type

固定值 `anvilcraft:item_compress`，标识这是一个物品压缩配方。

### ingredients

配方所需的输入物品列表。每个元素包含：

- `items`: 物品ID（可以是单个物品ID字符串或物品ID数组）
- `count`: 物品数量

### results

配方的输出物品列表。每个元素包含：

- `id`: 物品ID
- `count`: 物品数量

## 使用示例

将9个铁粒压缩成1个铁锭：

```json
{
  "type": "anvilcraft:item_compress",
  "ingredients": [
    {
      "items": "minecraft:iron_nugget",
      "count": 9
    }
  ],
  "results": [
    {
      "id": "minecraft:iron_ingot",
      "count": 1
    }
  ]
}
```