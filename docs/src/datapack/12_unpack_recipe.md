# 解包配方 (Unpack Recipe)

解包配方用于将压缩物品解包为原始物品。

## 基本结构

```json
{
  "type": "anvilcraft:unpack",
  "ingredients": [
    {
      "items": "minecraft:iron_block"
    }
  ],
  "results": [
    {
      "id": "minecraft:iron_ingot",
      "count": 9
    }
  ]
}
```

## 字段说明

### type

固定值 `anvilcraft:unpack`，标识这是一个解包配方。

### ingredients

配方所需的输入物品列表。每个元素包含：

- `items`: 物品ID（可以是单个物品ID字符串或物品ID数组）
- `count`: 物品数量（可选，默认为1）

### results

配方的输出物品列表。每个元素包含：

- `id`: 物品ID
- `count`: 物品数量

## 使用示例

将铁块解包成铁锭：

```json
{
  "type": "anvilcraft:unpack",
  "ingredients": [
    {
      "items": "minecraft:iron_block"
    }
  ],
  "results": [
    {
      "id": "minecraft:iron_ingot",
      "count": 9
    }
  ]
}
```