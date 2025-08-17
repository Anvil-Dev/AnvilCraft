# KubeJS 集成

AnvilCraft 提供了完整的 KubeJS 集成，允许你使用 KubeJS 脚本自定义 AnvilCraft 的配方。

## 支持的配方类型

- [InWorld 配方](00_inworld_recipe.md) - 世界内配方系统
- [物品粉碎配方](01_item_crush_recipe.md)
- [物品压缩配方](02_item_compress_recipe.md)
- [冲压配方](03_stamping_recipe.md)
- [解包配方](04_unpack_recipe.md)
- [方块粉碎配方](05_block_crush_recipe.md)
- [方块压缩配方](06_block_compress_recipe.md)
- [膨发配方](07_bulging_recipe.md)
- [挤压配方](08_squeezing_recipe.md)
- [物品注入配方](09_item_inject_recipe.md)
- [超级加热配方](10_super_heating_recipe.md)
- [时移配方](11_time_warp_recipe.md)
- [多方块结构配方](12_multiblock_recipe.md) - 使用多方块结构的配方
- [生物转换配方](13_mob_transform_recipe.md) - 生物实体之间的转换配方
- [珠宝制作配方](14_jewel_crafting_recipe.md) - 珠宝制作配方
- [矿物涌泉配方](15_mineral_fountain_recipe.md) - 矿物涌泉相关配方

## 基础用法

要使用 AnvilCraft 的 KubeJS 集成，首先需要在 KubeJS 脚本中导入相关类：

```js
// 在你的 KubeJS 脚本中
ServerEvents.recipes(event => {
    // 你的配方代码
})
```

所有 AnvilCraft 配方都遵循 KubeJS 标准配方格式：

```js
event.custom({
    type: "anvilcraft:recipe_type",
    // 配方参数
})
```