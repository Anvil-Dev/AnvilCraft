# InWorld 配方系统

InWorld 配方系统是 AnvilCraft 中用于处理世界内配方的核心系统，它允许在特定条件下触发各种效果。

## 基本结构

```json
{
  "type": "anvilcraft:in_world_recipe",
  "icon": {
    "item": "minecraft:anvil"
  },
  "trigger": "anvilcraft:on_anvil_fall_on",
  "conflicting": [],
  "non_conflicting": [],
  "outcomes": [],
  "priority": 0,
  "compatible": true
}
```

### 字段说明

- `type`: 固定值 `anvilcraft:in_world_recipe`，标识这是一个 InWorld 配方
- `icon`: 配方图标，用于在配方界面中显示
- `trigger`: 触发器类型，决定配方何时被触发
- `conflicting`: 冲突谓词列表，这些谓词之间相互冲突
- `non_conflicting`: 非冲突谓词列表，这些谓词之间不冲突
- `outcomes`: 配方结果列表，当配方匹配时执行
- `priority`: 配方优先级，数值越高优先级越高
- `compatible`: 是否兼容模式，决定谓词匹配方式

## 触发器 (Trigger)

触发器决定了配方何时被激活。目前支持的触发器:

### `anvilcraft:on_anvil_fall_on`

当铁砧落下时触发

## 谓词 (Predicate)

谓词用于判断配方是否可以执行，分为冲突和非冲突两类。

### `has_item`

检查指定位置是否存在指定物品

```json
{
  "type": "anvilcraft:has_item",
  "offset": [0, -1, 0],
  "item": {
    "items": "minecraft:iron_ingot"
  }
}
```

### `has_item_ingredient`

检查指定位置是否存在指定物品，如果配方匹配则消耗该物品

```json
{
  "type": "anvilcraft:has_item_ingredient",
  "offset": [0, -1, 0],
  "item": {
    "items": "minecraft:iron_ingot"
  }
}
```

### `has_block`

检查指定位置是否存在指定方块

```json
{
  "type": "anvilcraft:has_block",
  "offset": [0, -1, 0],
  "block": {
    "blocks": "minecraft:iron_block"
  }
}
```

### `has_block_ingredient`

检查指定位置是否存在指定方块，如果配方匹配则清除该方块

```json
{
  "type": "anvilcraft:has_block_ingredient",
  "offset": [0, -1, 0],
  "block": {
    "blocks": "minecraft:iron_block"
  }
}
```

### `has_fluid_cauldron`

检查指定位置是否存在指定流体的炼药锅

```json
{
  "type": "anvilcraft:has_fluid_cauldron",
  "offset": [0, -1, 0],
  "fluid": "minecraft:water"
}
```

## 结果 (Outcome)

结果定义了配方匹配后执行的操作。

### `spawn_item`

在指定位置生成物品

```json
{
  "type": "anvilcraft:spawn_item",
  "offset": [0, -1, 0],
  "item": {
    "id": "minecraft:iron_nugget"
  },
  "count": 1.0
}
```

### `set_block`

在指定位置设置方块

```json
{
  "type": "anvilcraft:set_block",
  "offset": [0, -1, 0],
  "block": {
    "Name": "minecraft:diamond_block"
  }
}
```

### `damage_anvil`

损坏铁砧

```json
{
  "type": "anvilcraft:damage_anvil"
}
```

## 使用示例

以下是一个完整的示例，当铁砧砸在铁锭上时，生成铁粒并消耗铁锭:

```json
{
  "type": "anvilcraft:in_world_recipe",
  "icon": {
    "item": "minecraft:iron_nugget"
  },
  "trigger": "anvilcraft:on_anvil_fall_on",
  "conflicting": [],
  "non_conflicting": [
    {
      "type": "anvilcraft:has_item_ingredient",
      "offset": [0, -1, 0],
      "item": {
        "items": "minecraft:iron_ingot"
      }
    }
  ],
  "outcomes": [
    {
      "type": "anvilcraft:spawn_item",
      "offset": [0, -1, 0],
      "item": {
        "id": "minecraft:iron_nugget"
      },
      "count": 1.0
    }
  ],
  "priority": 0,
  "compatible": true
}
```