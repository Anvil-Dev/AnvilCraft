## 基本结构

anvil_processing_recipe.json

- ├─`type`: `anvilcraft:anvil_processing` 固定值，代表当前为铁砧处理配方
- ├─`anvil_recipe_type` : `AnvilRecipeType` 铁砧配方类型
- ├─`icon`: `Item` 配方图标
- ├─`predicates`:`[Predicate]` 配方判据
- └─`outcomes`: `[Outcome]` 配方输出

## `AnvilRecipeType`铁砧配方类型

可以为以下枚举值：

- `generic`: 通用
- `stamping`: 冲压
- `sieving`: 过筛
- `bulging`: 膨发
- `bulging_like`: 类膨发
- `fluid_handling`: 流体处理
- `crystallize`: 晶化
- `compaction`: 压实
- `compress`: 压缩
- `cooking`: 煎炒
- `item_inject`: 物品注入
- `block_smash`: 方块粉碎
- `item_smash`: 物品粉碎
- `squeeze`: 挤压
- `super_heating`: 超级加热
- `timewarp`: 时移
- `multiblock_crafting`: 大铁砧多方块配方

## `ItemStack` 物品堆栈

- ├─`item`:`Item` 物品ID，例如 `"item": "anvilcraft:amethyst_pickaxe"`
- └─`data`: `NBT` 物品NBT标签，例如 `"data": "{Damage:0, Enchantments:[{id:\"minecraft:fortune\",lvl:3s}]}"`

## `Predicate` 配方判据

- ├─`type`: `PredicateType` 配方判据类型
- ├─`offset`: `[Number]` 位置偏移，格式为 `[x, y, z]`，例如 `[1.0, -1.0, 2.0]`
- └─・・・ 根据不同判据类型决定

### `PredicateType` 配方判据类型

可以为以下枚举值：

#### `has_item`: 判定指定位置是否存在指定物品

#### `has_item_ingredient`: 判定指定位置是否存在指定物品，配方执行成功后将该物品扣除

#### `has_item_leaves`: 判定指定位置是否存在指定树叶物品，配方执行成功后将在指定位置概率生成该树叶物品的树苗

#### `has_block`: 判定指定位置是否存在指定方块

#### `not_has_block`: 判定指定位置是否不存在指定方块

#### `has_block_ingredient`: 判定指定位置是否存在指定方块，配方执行成功后将该方块置为空气

#### `has_fluid_cauldron`: 判定指定位置是否存在指定流体炼药锅

## `Outcome` 配方结果

- ├─`type`: `OutcomeType` 配方结果类型
- ├─`offset`: `[Number]` 位置偏移，格式为 `[x, y, z]`，例如 `[1.0, -1.0, 2.0]`
- ├─`chance`: `Number` 结果执行概率，范围为 `0` - `1`
- └─・・・ 根据不同结果类型决定

### `OutcomeType` 配方结果类型

可以为以下枚举值：

#### `damage_anvil`: 执行成功后强制损坏铁砧

#### `set_block`: 执行成功后在指定位置放置方块

#### `spawn_item`: 执行成功后在指定位置生成物品

#### `spawn_experience`: 执行成功后在指定位置生成经验

#### `run_command`: 执行成功后在指定位置运行命令

#### `select_one`:  执行成功后从多个结果中挑选一个执行，此时将结果的 chance 作为加权随机数
