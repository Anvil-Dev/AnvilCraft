# 数据包

数据包支持是 AnvilCraft 的核心功能之一，允许通过 JSON 文件自定义各种配方和机制。以下文档将详细介绍每种配方类型的结构、字段和使用方法。

## 配方

这些配方类型构成了 AnvilCraft 的基础工艺系统：

- [InWorld 配方](00_inworld_recipe.md) - 世界内配方系统，通过铁砧等机制触发
- [物品压缩配方](01_item_compress_recipe.md) - 将多个物品压缩成更高级的物品
- [方块压缩配方](02_block_compress_recipe.md) - 将多个方块压缩成更高级的方块
- [物品粉碎配方](03_item_crush_recipe.md) - 将物品粉碎成更小的物品
- [方块粉碎配方](04_block_crush_recipe.md) - 将方块粉碎成更小的方块或物品
- [冲压配方](05_stamping_recipe.md) - 在冲压平台上将物品转换为其他物品
- [膨发配方](06_bulging_recipe.md) - 使用炼药锅中的流体将物品膨发成其他物品
- [挤压配方](07_squeezing_recipe.md) - 使用炼药锅中的流体将方块挤压成其他方块
- [物品注入配方](08_item_inject_recipe.md) - 将流体注入物品中以创建新物品
- [烹饪配方](09_cooking_recipe.md) - 使用热源将物品烹饪成其他物品
- [超级加热配方](10_super_heating_recipe.md) - 使用高温流体将物品转换为其他物品
- [时移配方](11_timewarp_recipe.md) - 使用时间力量和流体将物品转换为其他物品
- [解包配方](12_unpack_recipe.md) - 将压缩物品解包为原始物品
- [多方块结构配方](13_multiblock_recipe.md) - 定义复杂的多方块结构配方
- [生物转换配方](14_mob_transform_recipe.md) - 定义生物实体之间的转换规则
- [珠宝制作配方](15_jewel_crafting_recipe.md) - 制作各种珠宝和装饰性物品
- [矿物涌泉配方](16_mineral_fountain_recipe.md) - 定义矿物涌泉的方块转换规则

## 使用说明

要使用这些配方，您需要在您的数据包中创建相应的 JSON 文件，并将其放置在正确的目录结构中。每种配方类型都有其特定的结构和字段要求，请参考相应的文档了解详细信息。
