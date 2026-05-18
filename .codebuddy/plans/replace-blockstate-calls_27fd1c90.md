---
name: replace-blockstate-calls
overview: 将 ModBlocks.java 中使用 `.blockstate(DataGenUtil::simpleBlock)` 和 `.blockstate(DataGenUtil::horizontalFacingBlock)` 且在 models/block/ 中有对应模型文件的方块，替换为 `.blockstate(DataGenUtil::noExtraModelOrState)`。共16处替换。
todos:
  - id: replace-simpleblock
    content: 使用 [mcp:idea] replace_text_in_file 将 .blockstate(DataGenUtil::simpleBlock) 替换为 .blockstate(DataGenUtil::noExtraModelOrState)（15处）
    status: completed
  - id: replace-horizontalfacing
    content: 使用 [mcp:idea] replace_text_in_file 将 .blockstate(DataGenUtil::horizontalFacingBlock) 替换为 .blockstate(DataGenUtil::noExtraModelOrState)（1处）
    status: completed
    dependencies:
      - replace-simpleblock
  - id: verify-result
    content: 使用 [mcp:idea] search_in_files_by_text 验证文件中不再存在 simpleBlock 和 horizontalFacingBlock 调用
    status: completed
    dependencies:
      - replace-horizontalfacing
---

## 用户需求

在 ModBlocks.java 文件中，查找所有 `.blockstate()` 调用（如 `.blockstate(DataGenUtil::simpleBlock)`、`.blockstate(DataGenUtil::horizontalFacingBlock)` 等），当对应方块在 `models/block/` 文件夹中存在模型文件时，将其替换为 `.blockstate(DataGenUtil::noExtraModelOrState)`。

## 已确认需替换的项目（共16处）

### `.blockstate(DataGenUtil::simpleBlock)` → `.blockstate(DataGenUtil::noExtraModelOrState)`（15处）

1. 行982: impact_pile
2. 行1094: mineral_fountain
3. 行1178: sliding_rail_stop
4. 行1471: frost_metal_block
5. 行1496: cut_frost_metal_block
6. 行1596: ember_metal_block
7. 行1612: cut_ember_metal_block
8. 行1692: transcendium_block
9. 行1703: heavy_iron_block
10. 行3241: confined_space_anvilon
11. 行3254: confined_mass_anvilon
12. 行3267: confined_energy_anvilon
13. 行3280: confined_neutronium_ingot
14. 行3294: confinement_chamber
15. 行3304: singularity_crystal

### `.blockstate(DataGenUtil::horizontalFacingBlock)` → `.blockstate(DataGenUtil::noExtraModelOrState)`（1处）

16. 行310: stamping_platform

### 不替换的类型

- `DataGenUtil::transparentBlock`（3处）- 只生成 blockstate 不生成模型
- `DataGenUtil::onlyState`（8处）- 只生成 blockstate 不生成模型
- `ModelProviderUtil::liquid`（4处）- 液体模型
- 各种 lambda 表达式 - 自定义 slab/stairs/wall 逻辑

## 技术方案

使用 IDEA MCP 的 `replace_text_in_file` 工具逐个替换，不使用任何脚本。

### 替换策略

1. 先替换 `.blockstate(DataGenUtil::simpleBlock)` → `.blockstate(DataGenUtil::noExtraModelOrState)`（共15处，使用 replaceAll 一次性完成，因为所有15处均已确认有对应模型文件）
2. 再替换 `.blockstate(DataGenUtil::horizontalFacingBlock)` → `.blockstate(DataGenUtil::noExtraModelOrState)`（1处）
3. 替换完成后验证结果

### 注意事项

- 不能替换已经是 `noExtraModelOrState` 的条目
- 不能替换 `transparentBlock`、`onlyState`、`liquid` 及 lambda 表达式
- 所有替换均通过 IDEA MCP 工具完成，不使用脚本

# Agent Extensions

- **idea**: 使用 `replace_text_in_file` 工具执行文本替换，使用 `search_in_files_by_text` 验证替换结果