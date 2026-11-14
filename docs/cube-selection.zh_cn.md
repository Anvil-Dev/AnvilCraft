# 模型选取与描边配置

客户端通过 `assets/anvilcraft/anvillib/cube_selection.json` 设置模型选取与描边；需要 AnvilLib `2.0.0+snapshot.686` 或更新版本。

- `namespaces` 设置命名空间默认模式，`blocks` 设置单个方块的模式，方块规则优先。
- `model` 使用模型精确选取和描边，`outline` 保留方块原有选取并绘制模型轮廓，`vanilla` 保留方块原有选取和描边。
- 高优先级资源包逐项覆盖同名配置项，未填写项保留；同一资源包的不同配置文件按资源 ID 字典序合并。
- 整合包可在 `kubejs/assets/example/anvillib/cube_selection.json` 中配置 `kubejs` 或其他命名空间；使用 `F3+T` 重载。
- 多方块整机轮廓和方块实体动态部件继续由 AnvilCraft 提供，描边事件遵守 Cube 的资源规则及运行时排除规则。

例如，在资源包中为交易台恢复精确选取：

```json
{
  "blocks": {
    "anvilcraft:trading_station": "model"
  }
}
```

`assets/anvilcraft/model_selection_denylist.json` 仅保留 `disable_ber_selection`，控制哪些方块不加入方块实体渲染部件。
原有 `disable_precise_picking` 配置改用 `outline`，原有 `disable_model_outline` 配置改用 `vanilla`；两者均包含的方块使用 `vanilla`。
选取模式不改变服务端碰撞、实体物理或配方行为，放置预览所需的静态几何仍保留。
