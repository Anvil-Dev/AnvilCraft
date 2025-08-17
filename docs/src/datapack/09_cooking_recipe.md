# 烹饪配方 (Cooking Recipe)

烹饪配方用于使用热源将物品烹饪成其他物品。

## 基本结构

```json
{
  "type": "anvilcraft:cooking",
  "ingredients": [
    {
      "items": "minecraft:beef"
    }
  ],
  "results": [
    {
      "id": "minecraft:cooked_beef",
      "count": 1
    }
  ]
}
```

## 字段说明

### type

固定值 `anvilcraft:cooking`，标识这是一个烹饪配方。

### ingredients

配方所需的输入物品列表。每个元素包含：

- `items`: 物品ID（可以是单个物品ID字符串或物品ID数组）
- `count`: 物品数量（可选，默认为1）

### results

配方的输出物品列表。每个元素包含：

- `id`: 物品ID
- `count`: 物品数量

## 使用示例

将生牛肉烹饪成熟牛肉：

```json
{
  "type": "anvilcraft:cooking",
  "ingredients": [
    {
      "items": "minecraft:beef"
    }
  ],
  "results": [
    {
      "id": "minecraft:cooked_beef",
      "count": 1
    }
  ]
}
```