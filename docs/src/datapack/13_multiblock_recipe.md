# 多方块结构配方 (Multiblock Recipe)

多方块结构配方用于定义复杂的多方块结构，并在其中心产出物品。

## 基本结构

```json
{
  "type": "anvilcraft:multiblock",
  "pattern": {
    "layers": [
      ["   ", " B ", "   "],
      ["BAB", "A A", "BAB"],
      ["BBB", "BBB", "BBB"]
    ],
    "symbols": {
      "A": {
        "predicate": {
          "blocks": "minecraft:iron_block"
        }
      },
      "B": {
        "predicate": {
          "blocks": "minecraft:gold_block"
        }
      }
    }
  },
  "result": {
    "id": "minecraft:diamond",
    "count": 1
  }
}
```

## 字段说明

### type

固定值 `anvilcraft:multiblock`，标识这是一个多方块结构配方。

### pattern

定义多方块结构的形状和组成：

- `layers`: 由字符串数组组成的层数，每一层都是一个二维的字符网格
- `symbols`: 字符与方块谓词的映射关系

#### layers

每一层由相同长度的字符串数组组成，每个字符串代表一行，每个字符代表一个方块位置：

- 空格字符表示该位置不需要任何方块
- 其他字符需要在 symbols 中定义对应的方块谓词

#### symbols

字符与方块谓词的映射：

- 键为在 layers 中使用的字符
- 值为方块谓词，定义该位置允许的方块类型

### result

配方的输出物品：

- `id`: 物品ID
- `count`: 物品数量（可选，默认为1）

## 方块谓词

方块谓词用于定义某位置允许的方块类型：

### 基本方块谓词

```json
{
  "predicate": {
    "blocks": "minecraft:iron_block"
  }
}
```

### 带属性的方块谓词

```json
{
  "predicate": {
    "blocks": "minecraft:hopper",
    "properties": {
      "facing": "down"
    }
  }
}
```

### 方块标签谓词

```json
{
  "predicate": {
    "tag": "minecraft:logs"
  }
}
```

## 使用示例

### 简单的3x3x3结构

```json
{
  "type": "anvilcraft:multiblock",
  "pattern": {
    "layers": [
      ["AAA", "AAA", "AAA"],
      ["A A", "A A", "A A"],
      ["AAA", "AAA", "AAA"]
    ],
    "symbols": {
      "A": {
        "predicate": {
          "blocks": "minecraft:iron_block"
        }
      }
    }
  },
  "result": {
    "id": "minecraft:chest",
    "count": 1
  }
}
```

### 复杂结构示例

```json
{
  "type": "anvilcraft:multiblock",
  "pattern": {
    "layers": [
      ["RAR", "A A", "RAR"],
      ["RGR", "G G", "RGR"],
      ["RRR", "RRR", "RRR"]
    ],
    "symbols": {
      "R": {
        "predicate": {
          "blocks": "minecraft:redstone_block"
        }
      },
      "A": {
        "predicate": {
          "blocks": "minecraft:iron_block"
        }
      },
      "G": {
        "predicate": {
          "blocks": "minecraft:gold_block"
        }
      }
    }
  },
  "result": {
    "id": "minecraft:diamond_block",
    "count": 1
  }
}
```

## 注意事项

1. 多方块结构的中心点位于结构的正中心
2. 结构必须完整匹配才能触发配方
3. 空格字符表示该位置不需要任何方块
4. 符号定义必须覆盖所有在层中使用的字符
5. 结构大小没有严格的限制，但建议保持在合理的范围内以保证性能