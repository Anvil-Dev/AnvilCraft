# 超级加热配方 (Super Heating Recipe)

超级加热配方用于使用高温将物品转换为其他物品。

## 基本结构

```json
{
  "type": "anvilcraft:super_heating",
  "ingredients": [
    {
      "items": "minecraft:sand"
    }
  ],
  "results": [
    {
      "id": "minecraft:glass",
      "count": 1
    }
  ]
}
```

## 字段说明

### type

固定值 `anvilcraft:super_heating`，标识这是一个超级加热配方。

### ingredients

配方所需的输入物品列表。每个元素包含：

- `items`: 物品ID（可以是单个物品ID字符串或物品ID数组）
- `count`: 物品数量（可选，默认为1）

### results

配方的输出物品列表。每个元素包含：

- `id`: 物品ID
- `count`: 物品数量

## 使用示例

将沙子超级加热成玻璃：

```json
{
  "type": "anvilcraft:super_heating",
  "ingredients": [
    {
      "items": "minecraft:sand"
    }
  ],
  "results": [
    {
      "id": "minecraft:glass",
      "count": 1
    }
  ]
}
```