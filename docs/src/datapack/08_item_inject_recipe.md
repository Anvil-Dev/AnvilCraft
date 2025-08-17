# 物品注入配方 (Item Inject Recipe)

物品注入配方用于将流体注入物品中以创建新物品。

## 基本结构

```json
{
  "type": "anvilcraft:item_inject",
  "ingredients": [
    {
      "items": "minecraft:glass_bottle"
    }
  ],
  "results": [
    {
      "id": "minecraft:potion",
      "count": 1
    }
  ],
  "cauldron": {
    "fluid": "minecraft:water"
  }
}
```

## 字段说明

### type

固定值 `anvilcraft:item_inject`，标识这是一个物品注入配方。

### ingredients

配方所需的输入物品列表。每个元素包含：

- `items`: 物品ID（可以是单个物品ID字符串或物品ID数组）
- `count`: 物品数量（可选，默认为1）

### results

配方的输出物品列表。每个元素包含：

- `id`: 物品ID
- `count`: 物品数量

### cauldron

炼药锅相关设置：

- `fluid`: 流体类型
- `consume`: 消耗量（可选，负数表示产生流体，正数表示消耗流体）

## 使用示例

将玻璃瓶注入水制作成药水：

```json
{
  "type": "anvilcraft:item_inject",
  "ingredients": [
    {
      "items": "minecraft:glass_bottle"
    }
  ],
  "results": [
    {
      "id": "minecraft:potion",
      "count": 1
    }
  ],
  "cauldron": {
    "fluid": "minecraft:water"
  }
}
```