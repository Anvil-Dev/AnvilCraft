## 注册方块

在 AnvilCraft 附属开发中，注册方块与注册物品类似，但涉及更多的属性和模型设置。

### 打开 `init.AddonBlocks.java` ，你将看到如下语句：

```java
public static final BlockEntry<Block> EXAMPLE_BLOCK = REGISTRATE
    .block("example_block", Block::new)
    .simpleItem()
    .register();
```

该语句即为注册方块的示例，其中 `example_block` 为你即将注册的方块的ID，`Block::new` 为你方块类构造方法的引用。

### 本章节内容将详细介绍 `REGISTRATE.block()` 的使用方法

使用 `REGISTRATE.block()` 方法后，你将拿到一个 `BlockBuilder` ，该对象拥有一个 `.register()` 方法，调用后返回一个
`BlockEntry` ，其对应的方块将在合适的时机自动注册。

#### `BlockBuilder.initialProperties()`

该方法用于设置方块的初始属性，通常基于一个现有的方块属性：

```java
public static final BlockEntry<Block> EXAMPLE_BLOCK = REGISTRATE
    .block("example_block", Block::new)
    .initialProperties(() -> Blocks.IRON_BLOCK)
    .simpleItem()
    .register();
```

该示例展示了如何为注册的方块设置初始属性，继承了铁块的属性。

#### `BlockBuilder.properties()`

该方法用于修改方块的特定属性：

```java
public static final BlockEntry<Block> EXAMPLE_BLOCK = REGISTRATE
    .block("example_block", Block::new)
    .initialProperties(() -> Blocks.STONE)
    .properties(p -> p.lightLevel(state -> 5).noOcclusion())
    .simpleItem()
    .register();
```

该示例展示了如何设置方块的亮度等级和无遮挡属性。

#### `BlockBuilder.tag()`

该方法用于设置方块的标签：

```java
public static final BlockEntry<Block> EXAMPLE_BLOCK = REGISTRATE
    .block("example_block", Block::new)
    .initialProperties(() -> Blocks.STONE)
    .tag(BlockTags.MINEABLE_WITH_PICKAXE, BlockTags.NEEDS_STONE_TOOL)
    .simpleItem()
    .register();
```

该示例展示了如何将方块添加到"可使用镐挖掘"和"需要石镐"标签中。

#### `BlockBuilder.blockstate()`

该方法用于设置方块的状态和模型：

```java
public static final BlockEntry<Block> EXAMPLE_BLOCK = REGISTRATE
    .block("example_block", Block::new)
    .initialProperties(() -> Blocks.STONE)
    .blockstate((ctx, provider) -> provider.simpleBlock(ctx.get()))
    .simpleItem()
    .register();
```

该示例展示了如何为方块设置简单的方块模型。

#### `BlockBuilder.item()`

该方法用于为方块注册对应的物品：

```java
public static final BlockEntry<Block> EXAMPLE_BLOCK = REGISTRATE
    .block("example_block", Block::new)
    .initialProperties(() -> Blocks.STONE)
    .blockstate((ctx, provider) -> provider.simpleBlock(ctx.get()))
    .item()
    .initialProperties(() -> new Item.Properties())
    .build()
    .register();
```

该示例展示了如何为方块注册物品，并设置物品属性。

#### `BlockBuilder.simpleItem()`

这是一个便捷方法，用于快速为方块注册基础物品：

```java
public static final BlockEntry<Block> EXAMPLE_BLOCK = REGISTRATE
    .block("example_block", Block::new)
    .initialProperties(() -> Blocks.STONE)
    .blockstate((ctx, provider) -> provider.simpleBlock(ctx.get()))
    .simpleItem()
    .register();
```

该示例展示了如何使用 `simpleItem()` 快速为方块注册物品。

#### `BlockBuilder.recipe()`

该方法用于设置方块的配方：

```java
public static final BlockEntry<Block> EXAMPLE_BLOCK = REGISTRATE
    .block("example_block", Block::new)
    .initialProperties(() -> Blocks.STONE)
    .blockstate((ctx, provider) -> provider.simpleBlock(ctx.get()))
    .simpleItem()
    .recipe((ctx, provider) -> ShapedRecipeBuilder.shaped(RecipeCategory.BUILDING_BLOCKS, ctx.get(), 4)
        .pattern("XX")
        .pattern("XX")
        .define('X', Items.STONE)
        .unlockedBy(AnvilCraftDatagen.hasItem(Items.STONE), RegistrateRecipeProvider.has(Items.STONE))
        .save(provider))
    .register();
```

该示例展示了如何为方块添加一个有序合成配方。

### 自定义方块类

除了使用原版 Block 类，你还可以创建自定义的方块类：

```java
public class CustomBlock extends Block {
    public CustomBlock(Properties properties) {
        super(properties);
    }

    @Override
    public void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean isMoving) {
        // 添加自定义逻辑
        super.onPlace(state, level, pos, oldState, isMoving);
    }
}
```

然后在注册时使用：

```java
public static final BlockEntry<CustomBlock> CUSTOM_BLOCK = REGISTRATE
    .block("custom_block", CustomBlock::new)
    .initialProperties(() -> Blocks.STONE)
    .simpleItem()
    .register();
```

### 方块注册最佳实践

1. **命名规范**
    * 使用小写字母和下划线命名方块ID
    * 保持命名的一致性和描述性

2. **属性设置**
    * 始终设置合适的初始属性
    * 根据方块的功能添加适当的标签

3. **模型和渲染**
    * 为方块提供合适的模型
    * 考虑方块的光照和遮挡属性

4. **配方设计**
    * 为方块提供合理的合成配方
    * 确保配方平衡，不破坏游戏体验

5. **及时注册**
    * 确保在 mod 主类的构造函数中调用 `register()` 方法
    * 例如：`AddonBlocks.register();`

### 完整示例

以下是一个完整的自定义方块注册示例：

```java
public static final BlockEntry<Block> RUBY_BLOCK = REGISTRATE
    .block("ruby_block", Block::new)
    .initialProperties(() -> Blocks.IRON_BLOCK)
    .properties(p -> p.lightLevel(state -> 3))
    .tag(BlockTags.MINEABLE_WITH_PICKAXE, BlockTags.BEACON_BASE_BLOCKS)
    .blockstate((ctx, provider) -> provider.simpleBlock(ctx.get()))
    .simpleItem()
    .recipe((ctx, provider) -> ShapedRecipeBuilder.shaped(RecipeCategory.BUILDING_BLOCKS, ctx.get())
        .pattern("XXX")
        .pattern("XXX")
        .pattern("XXX")
        .define('X', ModItems.RUBY)
        .unlockedBy(AnvilCraftDatagen.hasItem(ModItems.RUBY), RegistrateRecipeProvider.has(ModItems.RUBY))
        .save(provider))
    .register();
```

这个示例展示了如何：

- 创建一个基于铁块属性的方块
- 设置方块的亮度等级
- 添加适当的标签
- 设置简单的方块模型
- 为方块注册物品
- 添加合成配方
