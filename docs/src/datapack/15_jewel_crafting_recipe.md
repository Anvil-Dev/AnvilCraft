# 珠宝制作配方 (Jewel Crafting Recipe)

珠宝制作配方用于制作各种珠宝和装饰性物品。

## 基本结构

```json
{
  "type": "anvilcraft:jewel_crafting",
  "ingredients": [
    {
      "tag": "forge:gems/diamond"
    },
    {
      "tag": "forge:ingots/gold"
    },
    {
      "tag": "forge:ingots/gold"
    }
  ],
  "result": {
    "id": "anvilcraft:amber_block",
    "count": 1
  }
}
```

## 字段说明

### type

固定值 `anvilcraft:jewel_crafting`，标识这是一个珠宝制作配方。

### ingredients

输入材料列表，每个元素是一个标准的 Minecraft 配方材料：

- 可以是物品ID，如 `"minecraft:diamond"`
- 可以是标签，如 `"#forge:gems/diamond"`
- 可以是复杂的材料对象

### result

配方的输出物品：

- `id`: 物品ID
- `count`: 物品数量（可选，默认为1）

## 配方材料格式

### 物品材料

```json
{
  "item": "minecraft:diamond"
}
```

### 标签材料

```json
{
  "tag": "forge:gems/diamond"
}
```

### 带数量的材料

```json
{
  "item": "minecraft:gold_ingot",
  "count": 2
}
```

### 复杂材料

```json
{
  "type": "minecraft:item",
  "item": "minecraft:potion",
  "nbt": "{Potion:\"minecraft:water\"}"
}
```

## 使用示例

### 简单珠宝制作

```json
{
  "type": "anvilcraft:jewel_crafting",
  "ingredients": [
    {
      "item": "minecraft:diamond"
    },
    {
      "tag": "forge:ingots/gold"
    },
    {
      "tag": "forge:ingots/gold"
    }
  ],
  "result": {
    "id": "anvilcraft:amber_block",
    "count": 1
  }
}
```

### 复杂珠宝制作

```json
{
  "type": "anvilcraft:jewel_crafting",
  "ingredients": [
    {
      "tag": "forge:gems/emerald",
      "count": 3
    },
    {
      "tag": "forge:ingots/gold",
      "count": 5
    },
    {
      "item": "minecraft:nether_star"
    }
  ],
  "result": {
    "id": "anvilcraft:royal_crown",
    "count": 1
  }
}
```

### 带NBT数据的配方

```json
{
  "type": "anvilcraft:jewel_crafting",
  "ingredients": [
    {
      "item": "minecraft:potion",
      "nbt": "{Potion:\"minecraft:water\"}"
    },
    {
      "item": "minecraft:redstone"
    }
  ],
  "result": {
    "id": "minecraft:potion",
    "count": 1,
    "nbt": "{Potion:\"minecraft:awkward\"}"
  }
}
```

## 注意事项

1. 珠宝制作配方可以接受任意数量的输入材料
2. 材料的排列顺序不影响配方匹配
3. 可以使用物品标签来增加配方的灵活性
4. 输出物品只能有一个，不能像其他配方类型那样有多个输出
5. 最多支持4种不同的材料