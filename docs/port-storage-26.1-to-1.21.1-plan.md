# 移植计划：26.1 仓储系统（最终结果）移植到 1.21.1

> 目标分支：`port/1.21/1.6`（HEAD `2a9c262bf` "temp"）
> 源分支：`origin/dev/26.1/1.6`
> 移植依据：以下 5 个 commit 的**最终结果**（等价于 `13f573207` 状态），忽略中间演进
> 更新日期：2026-08-11（含 P0 执行结果）

## 一、目标与范围

将 26.1 仓储系统（Storage System）完整移植到 1.21.1：

| Commit | PR | 主题 |
|--------|----|------|
| `618a5fd8a` | #3683 | 存储方块 + 基础代码框架 |
| `a962a8345` | #3795 | 存储方块功能 |
| `b4a3442bc` | #4190 | 修复存储行为（RPC 化） |
| `62bbc1c80` | #4209 | 修复问题 + 升级行为 |
| `13f573207` | #4306 | JEI 集成与收尾 |

**策略：只取最终结果。** 移植范围 = `git diff 618a5fd8a^ 13f573207` 的净变化：
- **新增 124 个文件**（Java 55 + 资源/生成数据 69）
- **修改 114 个文件**（Java 57 + 资源/生成数据 57）
- 中间产物（如已删除的 `inventory/StorageMenu`、`network/multiple/StoragePackets`、旧 `block/container/CrateBlock`、内嵌 `UnlimitedItemStack` 等）**一律不移植**。

## 二、P0 基线核查结果（已完成）

1. **工作树干净**：`git status --short` 仅 `?? docs/`；`git diff --check` 无输出。
2. **anvillib 2.0.0 本地发布确认**：`C:\Users\29569\.m2\settings.xml` 将 localRepository 指向 `D:/Maven/`；`D:\Maven\dev\anvilcraft\lib\` 下存在 1.21.1 全套模块 2.0.0（rpc、sync、sync-processor、rendering、space-select、collision、explosion、font、test 等），且 `anvillib-rpc-neoforge-1.21.1-2.0.0-sources.jar` 可作源码参考。
3. **远程 Maven 不可达**：Cjsah Maven（`server.cjsah.net:1002`）连接超时，**不得依赖远程获取 anvillib**；一切 anvillib 解析走本地 `D:\Maven`（`mavenLocal()`）。
4. **基线编译通过**：`./gradlew.bat compileJava --console=plain` → `BUILD SUCCESSFUL in 1m 24s`（anvillib 2.0.0 本地解析成功，未访问远程）。
5. **RPC 依赖问题已修复（用户）**：`anvillib-neoforge-1.21.1-2.0.0` 主 jar 已于 2026-08-11 16:55 重新发布，现在与 26.1 布局一致：jarjar 内嵌全部 18 个模块（含 `anvillib_rpc`、`anvillib_sync`、`anvillib_rendering`、`anvillib_space_select`、`anvillib_collision`、`anvillib_explosion`、`anvillib_font`），且 `.module` 已声明全部模块依赖（含 `anvillib-rpc-neoforge-1.21.1`）。因此编译期与运行期均可由既有 `api(libs.anvillib)` + `jarJar(libs.anvillib)` 直接获得，**无需新增任何 anvillib-rpc 依赖或 mods.toml 声明**。
6. **1.21 分支已含仓储美术资源**：commit `a5bcf5d88`（#3569 存储方块资源）、`8680e89c7`（#3581 存储站资源）、`aa6ae274a`（潜影集装箱方块）、`b3240e855`（粒子优化）已把 main-resources 模型/纹理合入 1.21 分支；**Java 代码与生成数据缺失**，且部分 main-resources 与最终状态（`13f573207`）存在差异需合并。
7. **移植面已核算**：见第六节完整清单（NEW 124 / MOD 114）。

## 三、依赖变更（P1）

**结论：RPC 已随主 jar 内嵌，无需任何额外依赖声明。**

1. `gradle/libs.versions.toml`：保持 `anvillib = "2.0.0"`，无需新增条目。
2. `dependencies.gradle`：保持既有 `api(libs.anvillib)` 与 `jarJar(libs.anvillib)` 不变；rpc/sync/rendering 等模块随主 jar 的嵌套 jarjar 一并打包。
3. `src/main/resources/META-INF/neoforge.mods.toml`：无需新增 `anvillib_rpc` 依赖声明（与 26.1 分支一致，仅声明 `anvillib`）。
4. 注意：Gradle 对 `mavenLocal()` 为原地引用、不写入 modules-2 缓存，主 jar 重新发布后**下次构建自动生效**；若在别的机器/CI 上发现仍解析到旧 jar，检查本地仓库副本或执行 `--refresh-dependencies`。
5. 启动时在日志确认 `anvillib_rpc` 随主 jar 加载（NeoForge jarjar 按版本合并内嵌的 `anvillib-network`，与 26.1 行为一致）。

## 四、26.1 → 1.21.1 适配要点（已核实）

| 26.1 | 1.21.1 | 说明 / 证据 |
|------|--------|-------------|
| `net.minecraft.resources.Identifier` | `net.minecraft.resources.ResourceLocation` | 出现在 `rpc/StorageServerStub`、`saved/storage/Storages`、全部 category、`PlayerSettings` 等 |
| `net.minecraft.world.item.ItemInstance` | `net.minecraft.world.item.ItemStack` | `api/itemhandler/unlimited/SpaceSize/TypeLimitItemStacksResourceHandler` |
| `net.minecraft.world.level.storage.ValueInput/ValueOutput` | `CompoundTag` + `HolderLookup.Provider` | `StorageBlockEntity`、unlimited handlers 的保存/加载；1.21.1 无此类（分支代码从未引用） |
| `net.minecraft.world.level.saveddata.SavedDataType` | `SavedData.Factory<T>` | `saved/storage/Storages` |
| `net.neoforged.neoforge.transfer.*`（`StacksResourceHandler`/`ItemResource`/`Transaction`/`TransferPreconditions`） | `IItemHandler` + `ItemStack`（流体 `IFluidHandler`+`FluidStack`） | unlimited handlers 基类、`anvil/Upgrade2ShulkerContainerBehavior`、`rpc/StorageServerStub`；1.21.1（NeoForge 21.1）无 transfer API，分支无任何引用 |
| `dev.anvilcraft.lib.v2.util.UnlimitedItemStack` | `dev.anvilcraft.lib.v2.util.stack.UnlimitedItemStack` | 1.21.1 anvillib 的类位于 `util.stack` 包；分支 `OverLimitItemHandler` 已是该 import |
| `dev.anvilcraft.lib.v2.codec.CodecUtil.mapCodec(...)`（26.1） | 1.21.1 `CodecUtil` 无 `mapCodec` | 分类 CODEC 改用 `RecordCodecBuilder.mapCodec`（已核实 anvillib-codec 1.21.1 源码） |
| `ComponentSerialization.flatRestrictedCodec(int)`（26.1） | 1.21.1 为 `flatCodec(int)`/`FLAT_CODEC` | 分类 `name` 字段改用 `flatCodec(Integer.MAX_VALUE)` |
| `RegistryOps.RegistryInfo`（26.1） | 1.21.1 `RegistryOps` 无 `RegistryInfo` | `ICategory.CODEC` 改用 `registryOps.getter(key)` + `HolderLookup.RegistryLookup` 匹配 |
| `ItemStackTemplate`（26.1 新类） | 1.21.1 不存在 | 分类 icon 用 `ItemStack`（`ItemStack.CODEC`/`STREAM_CODEC`） |
| `CreativeModeTab.contains(ItemStack)`（26.1） | 1.21.1 NeoForge 已补 `contains` | `BuiltInRegistries.CREATIVE_MODE_TAB.getOrThrow(key).contains(stack)`；注意 1.21.1 `Registry.getOrThrow` 直接返回 T（无 `.value()`） |
| `ItemStack.getCustomName()`（26.1） | 1.21.1 无 | 改用 `stack.get(DataComponents.CUSTOM_NAME)` |
| `DataComponentPredicate.Type<?>`/`AnyValueType`/`DataComponentPredicates`（26.1 `core.component.predicates`） | 1.21.1 不存在（原版为精确值匹配的 `net.minecraft.core.component.DataComponentPredicate`） | `HasComponentCategory` 改为 `List<DataComponentPredicate>` + `MatchType`；`FOOD_AND_DRINK`/`ENCHANTED` 延至 P3（依赖 `ModDataComponentPredicates`/`ExtraEnchantmentsPredicate`） |
| `RecipeBookCategory`/`RecipeDisplay`/`ContextMap`（26.1） | 1.21.1 不存在（仅客户端 `RecipeBookCategories`） | `RecipeBookCategoryCategory` 不移植；`ModCategoryTypes` 注册 8 个类型 |
| `net.minecraft.core.component.predicates.EnchantmentsPredicate`（26.1） | 不存在；1.21.1 为 `net.minecraft.advancements.critereon.ItemEnchantmentsPredicate` | 分支 `util/DataGenUtil.java` 已导入 1.21.1 类 → `mixin/EnchantmentsPredicateMixin` 与 `ExtraEnchantmentsPredicate` 需改写或删除 |
| `ComponentSerialization`（26.1 `bootstrap` 注入） | 1.21.1 存在 `ComponentSerialization`（分支已导入） | `mixin/ComponentSerializationMixin` 移植时核对 `bootstrap(ExtraCodecs.LateBoundIdMapper...)` 签名；`api/component/ModNameContents` 依赖它注册 `anvilcraft:mod_name` |
| `TranslatableContents`/`TranslatableFormatException`（26.1 自定义） | 1.21.1 已有原版 `net.minecraft.network.chat.contents.TranslatableContents` | `api/component/*` 三件套逐文件评估：能复用原版则删除，否则按 1.21.1 原版 API 重写 |
| 生成 `items/*.json`（26.1 新物品模型格式） | 1.21.1 生成到 `models/item/*.json` | `src/generated/resources/assets/anvilcraft/items/*.json` 由 1.21.1 datagen 重新生成，不直接搬运 |
| JEI `IGlobalGuiHandler`/`IClickableIngredientFactory` | JEI 19.32.0.358（1.21.1） | `AnvilCraftJeiPlugin` 移植时核对接口存在性；`addItemStackInfo` 的删除与仓储无关，保留 1.21 侧既有注册 |
| `META-INF/accesstransformer.cfg` | 同款类 | C1 新增 `public net.minecraft.core.NonNullList <init>(Ljava/util/List;Ljava/lang/Object;)V`，1.21.1 直接可移植 |
| `mixin/invoker/BaseMappedRegistryInvoker` | NeoForge 21.1 存在 `BaseMappedRegistry` | 用于 `setSync`，核对后移植 |

## 五、分阶段实施

> **阶段验证约定**：每个阶段完成后，若当前模型自带多模态能力，可尝试自动启动测试客户端并截图查看界面/渲染结果；否则在阶段完成后即停止，并向用户列出本阶段所有应测试项，由用户确认后再进入下一阶段。

### P1 依赖与基础（构建 + mixin/AT）
- 内容：第三节全部依赖变更；`anvilcraft.mixins.json` 新增 `ComponentSerializationMixin`、`EnchantmentsPredicateMixin`（评估后）、`invoker/BaseMappedRegistryInvoker`；`accesstransformer.cfg` 追加 NonNullList 构造。
- 验证：`./gradlew.bat compileJava --console=plain` 通过。

### P2 注册表与分类系统（地基）——已完成（2026-08-11）
- **注册表拆分**：新增 `init/registry/ModRegistries.java` + `ModRegistryKeys.java`。同步注册表 `AMULET_TYPE/AMULET_DEF_TYPE/MODIFIER_TYPE/CUSTOM_DATA_TYPE/NUMBER_PROVIDER_TYPE/CATEGORY_TYPE/TARGET_POINTER_TYPE`（`.sync(true).maxId(512)`，`NewRegistryEvent` 注册）；数据包注册表 `AMULET_DEF/CATEGORY/BLOCK_PLACEMENT_RULES`（`DataPackRegistryEvent` 注册）。删除 1.21 旧 `init/ModRegistries.java`；**保留 1.21 特有** `TARGET_POINTER_TYPE` 与 `BLOCK_PLACEMENT_RULES`（26.1 目标无，因 1.21 有未移植的智能方块放置器/pointer 系统）。
- **引用迁移（20 文件）**：`api/amulet/AmuletManager`、`api/amulet/def/IAmuletDefinition`、`api/block/BlockPlacementRules`、`api/pointer/BlockItemHandlerPointer/BlockPointer/ITargetPointer/ItemEntityPointer`、`api/recipe/data/ICustomDataComponent`、`api/recipe/number/INumberProvider`、`api/recipe/result/modifier/IResultModifier`、`data/AnvilCraftDatagen`、`data/provider/ModBlockPlacementRuleProvider`、`init/ModTargetPointers`、`init/item/ModAmuletDefinitions/ModAmuletDefinitionTypes/ModAmuletTypes`、`init/item/ModCustomDataComponents`、`init/recipe/ModNumberProviderTypes/ModResultModifierTypes`、`item/property/component/amulet/IAmulet`。迁移模式：`*_REGISTRY` → 裸名（`AMULET_TYPE` 等）、`*_KEY` → `ModRegistryKeys.*`（`MODIFIER_KEY→MODIFIER`、`AMULET_DEF_KEY→AMULET_DEF`、`BLOCK_PLACEMENT_RULES_KEY→BLOCK_PLACEMENT_RULES`）。
- **分类系统新增（15 文件）**：`saved/storage/category/*`（`ICategory` + 8 个分类实现 + `store/CategoryEntry` + `store/CategoryMode` + package-info×2）、`init/storage/ModCategoryTypes.java`（8 个类型）、`init/storage/ModCategories.java`（5 个内置分类）。`AnvilCraft.java` 构造函数新增 `ModCategoryTypes.register(modEventBus)`；`AnvilCraftDatagen` 新增 `genInit.add(ModRegistryKeys.CATEGORY, ModCategories::bootstrap)`。
- **P2 范围 1.21.1 适配**：
  - `ItemStackTemplate` → `ItemStack`（icon 字段及 CODEC/STREAM_CODEC）。
  - `CodecUtil.mapCodec`（1.21.1 anvillib 无此方法）→ `RecordCodecBuilder.mapCodec`。
  - `ComponentSerialization.flatRestrictedCodec` → `flatCodec(Integer.MAX_VALUE)`。
  - `RegistryOps.RegistryInfo`（26.1）→ `RegistryOps.getter(key)` + `HolderLookup.RegistryLookup` 匹配。
  - `Identifier` → `ResourceLocation`（`location()`/`withDefaultNamespace`/`withSuffix` 均存在）。
  - `CreativeModeTab.contains`：1.21.1 NeoForge 已有（基于延迟构建的 `displayItemsSearchTab`，服务端默认空集合不 NPE）。
  - `Registry.getOrThrow(key)` 1.21.1 直接返回 T，无 `.value()`。
  - `ItemStack.getCustomName()` → `get(DataComponents.CUSTOM_NAME)`。
  - `HasComponentCategory`：26.1 的 `DataComponentPredicate.Type<?>/AnyValueType`（1.21.1 不存在）改为 `List<DataComponentPredicate>` + `MatchType`，保留 `single/and/or/nand/nor` 工厂与测试逻辑。
  - `FilterContent`：补 26.1 最终态实例方法 `filter(ItemStack)`（静态方法改为委托），供 `FilterCategory` 使用。
  - `NamespaceCategory` 名称暂用纯字符串参数（`api/component/ModNameContents` 属组件三件套，P3/P4 处理）。
  - **`RecipeBookCategoryCategory` 不移植**（1.21.1 无 `RecipeBookCategory/RecipeDisplay/ContextMap`）；`ModCategoryTypes` 注册 8 个类型，`REDSTONE` 仅保留 `CreativeModeTabCategory` 分支。
  - **`FOOD_AND_DRINK`/`ENCHANTED` 暂缓注册**：依赖 26.1 的 `DataComponentPredicates/EnchantmentsPredicate/PotionsPredicate` 与 P3 的 `init/item/ModDataComponentPredicates`/`ExtraEnchantmentsPredicate`，P3 就绪后加回（`HasComponentCategory` 框架已就绪）。
- **验证结果**：
  - `./gradlew.bat compileJava --console=plain` → `BUILD SUCCESSFUL`。
  - `./gradlew.bat runData --console=plain` → `BUILD SUCCESSFUL`；生成 `src/generated/resources/data/anvilcraft/anvilcraft/category/{minecraft,block,unstackable,anvilcraft,redstone}.json`，结构/字段与 26.1 一致（namespace 分类 name 暂为纯字符串参数）。
  - `git diff --check` 干净；`runData` 未产生其他无关生成文件差异。
- **P2 应测试项（需用户确认）**：
  1. 启动客户端+专用服务器：`category_type` 注册表含 8 个类型；`category` 数据包注册表加载 5 个分类，日志无 codec 报错。
  2. `/reload` 后确认 `data/anvilcraft/anvilcraft/category/*.json` 正常解析（含嵌套 `creative_mode_tab` 的 `redstone.json`）。
  3. `REDSTONE`（creative_mode_tab）：客户端打开创造栏后过滤匹配；服务端默认不匹配（`displayItemsSearchTab` 由客户端构建）——如需服务端匹配后续评估。
  4. 旧存档加载：amulet/配方注册表迁移后无 ID/引用异常。

### P3 方块、方块实体与升级行为（服务端内容）
- 新增 `block/container/storage/*`（`CrateBlock`、`LargeCrateBlock`、`HyperdimensionStorageStationBlock`、`ShulkerContainerBlock`——由 1.21 既有 `block/ShulkerContainerBlock` 迁移改造）。
- 新增 `block/entity/storage/*`（`StorageBlockEntity` + 4 个具体实体；`ValueInput/ValueOutput` 序列化改为 `CompoundTag`+`HolderLookup`）。
- 新增 `item/block/ShulkerContainerBlockItem.java`（1.21 既有 `block/item/ShulkerContainerBlockItem` 迁移改造）、`item/property/component/StorageRef.java`、`api/energy/IEnergyHandlerHolder.java`、`api/tooltip/impl/*`（LargeCrate/ShulkerContainer TooltipProvider）。
- 注册：`init/block/ModBlocks.java`、`ModBlockEntities.java`；升级行为 `anvil/UpgradeShulkerContainerBehavior`、`anvil/Upgrade2ShulkerContainerBehavior`（`ItemResource/Transaction` 改写为 1.21.1 物品实体/方块实体操作）→ `init/ModAnvilBehaviors.java`。
- 关联小改：`block/entity/FeCollector/PowerConverter/OverseerBlockEntity`、`api/itemhandler/OverLimitItemHandler`、`api/tooltip/HudTooltipManager`、`item/property/component/FilterContent`、`OverLimitItemContainerContents`、`inventory/container/FilterContainer`、`recipe/transform/MobTransformWithItemRecipe`、`item/utility/EnergyWeaponPlatformItem`、`init/item/ModDataComponentPredicates`、`item/property/predicate/ExtraEnchantmentsPredicate`（改写）。
- 验证：`compileJava`；进游戏放置 4 类方块、铁砧升级链路冒烟。

### P4 存储核心（数据 + 无限处理器 + RPC）
- 新增 `saved/storage/*`（`Storages`——`SavedData.Factory` 改造、`BaseStorage`、`StorageType`、`CrateStorage`、`LargeCrateStorage`、`ShulkerContainerStorage`、`HyperdimensionStorage`）。
- 新增 `saved/setting/*`（`PlayerSetting`、`PlayerSettings`、`StorageSetting`、`mode/*` 4 个枚举）。
- 新增 `api/itemhandler/unlimited/*`（`UnlimitedItemStacksResourceHandler`、`SpaceSizeItemStacksResourceHandler`、`TypeLimitItemStacksResourceHandler`）——基类由 `StacksResourceHandler` 改为 1.21.1 `IItemHandler` 实现，保持公开 API（CODEC/STREAM_CODEC/`typeLimit`/`spaceSize`）不变。
- 新增 RPC：`rpc/SettingServerStub`、`rpc/StorageServerStub`、`rpc/StorageInput`、`network/PlayerSettingsSyncPacket`（`ItemResource/Transaction` 改写为 `IItemHandler` 操作）。
- 新增 `init/ModCapabilities.java`、`data/advancement/ModAdvancementsHandler.java`、`event/TooltipEventListener.java`、`util/registrater/DataGenUtil.java`。
- 验证：`compileJava`；专用服务器下存储交互（存取/克隆/丢弃）无 RPC 崩溃。

### P5 客户端界面
- 新增 `client/gui/screen/StorageScreen.java`、`CategorySettingsScreen.java`、`client/gui/component/category/CategoryButton/CategoryList`、`client/gui/component/RenderableWidgetAdder.java`、`client/support/GuiRenderSupport.java`、`client/rpc/SettingClientStub.java`、`client/rpc/StorageClientStub.java`。
- 关联小改：`client/gui/component/SwitchableButton`、`client/event/ClientEventListener`、`client/gui/screen/ActiveSilencerScreen/EmberGrindstoneScreen/TeslaTowerScreen`、`constant/SharedTextures`、`config/AnvilCraftClientConfig/AnvilCraftServerConfig`、`init/ModMenuTypes`、`event/PlayerTickEventHandler`、`event/ServerLifecycleEventListener`。
- 验证：`compileJava`；客户端打开 4 类 GUI，排序/搜索/分类设置/NBT 折叠冒烟。

### P6 数据生成与资源
- 移植数据生成器改动：`data/AnvilCraftDatagen`、`data/lang/CategoryLang`（新增）、`LangHandler`、`OtherLang`、`ScreenLang`、`ToolPropertyLang`、`data/recipe/MultiBlockConversionRecipeLoader`（三方合并）、`MultiBlockRecipeLoader`、`RegistrumBlockRecipeLoader`、`data/tags/BlockTagLoader`、`recipe/multiblock/BlockPredicateWithState`、`MultiblockConversionRecipe`、`MultiblockRecipe`、`util/FormattingUtil`。
- 资源合并：1.21 已存在的 main-resources（crate/storage_station/shulker_container 模型纹理）与 `13f573207` 差异合并；新增 `blockstates/hyperdimension_storage_station.json`、`large_crate.json`、`font/small.json`+`textures/font/small.png`、`gui/sprites/category_settings_selected_*`；**忽略** 26.1 专用且与仓储无关的模型改动（如 celestial_forging_anvil 系列、`items/*.json` 新格式）。
- `./gradlew.bat runData --console=plain`，审查生成 diff（lang、loot、recipe、advancement、tags、blockstates、models）。
- 验证：`runData` 无异常；`git diff --check` 干净。

### P7 集成与全面验证
- JEI：`integration/jei/AnvilCraftJeiPlugin` 移植 StorageScreen 相关注册（核对 19.32 API），保留 1.21 既有 `addItemStackInfo`。
- `AnvilCraft.java` 等杂项小改收尾；`mods.toml` 依赖声明复核。
- 验证：
  1. `./gradlew.bat compileJava --console=plain`
  2. `./gradlew.bat runData --console=plain`（如有生成变化）
  3. `git diff --check`、`git status --short`
  4. 客户端 + 专用服务器冒烟：4 类方块存取/克隆/丢弃、搜索/排序/分类、玩家设置同步、铁砧升级链路、崩溃恢复（`RecoverStation`）
  5. 存档兼容性：新旧存档加载（`StorageBlockEntity` 序列化改造）

## 六、完整文件清单

### 6.1 新增 — Java（55）
- `anvil`
  - `anvil/Upgrade2ShulkerContainerBehavior.java`
  - `anvil/UpgradeShulkerContainerBehavior.java`
- `api/component`
  - `api/component/ModNameContents.java`
  - `api/component/package-info.java`
  - `api/component/TranslatableContents.java`
  - `api/component/TranslatableFormatException.java`
- `api/energy`
  - `api/energy/IEnergyHandlerHolder.java`
- `api/holderset`
  - `api/holderset/package-info.java`
- `api/itemhandler/unlimited`
  - `api/itemhandler/unlimited/package-info.java`
  - `api/itemhandler/unlimited/SpaceSizeItemStacksResourceHandler.java`
  - `api/itemhandler/unlimited/TypeLimitItemStacksResourceHandler.java`
  - `api/itemhandler/unlimited/UnlimitedItemStacksResourceHandler.java`
- `api/tooltip/impl`
  - `api/tooltip/impl/LargeCrateTooltipProvider.java`
  - `api/tooltip/impl/ShulkerContainerTooltipProvider.java`
- `block/container/storage`
  - `block/container/storage/CrateBlock.java`
  - `block/container/storage/HyperdimensionStorageStationBlock.java`
  - `block/container/storage/LargeCrateBlock.java`
  - `block/container/storage/package-info.java`
  - `block/container/storage/ShulkerContainerBlock.java`
- `block/entity/storage`
  - `block/entity/storage/CrateBlockEntity.java`
  - `block/entity/storage/HyperdimensionStorageStationBlockEntity.java`
  - `block/entity/storage/LargeCrateBlockEntity.java`
  - `block/entity/storage/package-info.java`
  - `block/entity/storage/ShulkerContainerBlockEntity.java`
  - `block/entity/storage/StorageBlockEntity.java`
- `client/gui/component`
  - `client/gui/component/RenderableWidgetAdder.java`
- `client/gui/component/category`
  - `client/gui/component/category/CategoryButton.java`
  - `client/gui/component/category/CategoryList.java`
  - `client/gui/component/category/package-info.java`
- `client/gui/screen`
  - `client/gui/screen/CategorySettingsScreen.java`
  - `client/gui/screen/StorageScreen.java`
- `client/rpc`
  - `client/rpc/SettingClientStub.java`
  - `client/rpc/StorageClientStub.java`
- `client/support`
  - `client/support/GuiRenderSupport.java`
- `data/advancement`
  - `data/advancement/ModAdvancementsHandler.java`
- `data/lang`
  - `data/lang/CategoryLang.java`
- `event`
  - `event/TooltipEventListener.java`
- `init`
  - `init/ModCapabilities.java`
- `init/item`
  - `init/item/ModDataComponentPredicates.java`
- `init/registry`
  - `init/registry/ModRegistries.java`
  - `init/registry/ModRegistryKeys.java`
- `init/storage`
  - `init/storage/ModCategories.java`
  - `init/storage/ModCategoryTypes.java`
- `item/block`
  - `item/block/ShulkerContainerBlockItem.java`
- `item/property/component`
  - `item/property/component/StorageRef.java`
- `item/property/predicate`
  - `item/property/predicate/ExtraEnchantmentsPredicate.java`
- `item/utility`
  - `item/utility/EnergyWeaponPlatformItem.java`
- `mixin`
  - `mixin/ComponentSerializationMixin.java`
  - `mixin/EnchantmentsPredicateMixin.java`
- `mixin/invoker`
  - `mixin/invoker/BaseMappedRegistryInvoker.java`
- `network`
  - `network/PlayerSettingsSyncPacket.java`
- `rpc`
  - `rpc/package-info.java`
  - `rpc/SettingServerStub.java`
  - `rpc/StorageInput.java`
  - `rpc/StorageServerStub.java`
- `saved/setting`
  - `saved/setting/package-info.java`
  - `saved/setting/PlayerSetting.java`
  - `saved/setting/PlayerSettings.java`
  - `saved/setting/StorageSetting.java`
- `saved/setting/mode`
  - `saved/setting/mode/NbtDisplayMode.java`
  - `saved/setting/mode/OrderMode.java`
  - `saved/setting/mode/package-info.java`
  - `saved/setting/mode/SearchMode.java`
  - `saved/setting/mode/SortMode.java`
- `saved/storage`
  - `saved/storage/BaseStorage.java`
  - `saved/storage/CrateStorage.java`
  - `saved/storage/HyperdimensionStorage.java`
  - `saved/storage/LargeCrateStorage.java`
  - `saved/storage/package-info.java`
  - `saved/storage/ShulkerContainerStorage.java`
  - `saved/storage/Storages.java`
  - `saved/storage/StorageType.java`
- `saved/storage/category`
  - `saved/storage/category/AndCategory.java`
  - `saved/storage/category/BlockCategory.java`
  - `saved/storage/category/CreativeModeTabCategory.java`
  - `saved/storage/category/FilterCategory.java`
  - `saved/storage/category/HasComponentCategory.java`
  - `saved/storage/category/ICategory.java`
  - `saved/storage/category/NamespaceCategory.java`
  - `saved/storage/category/OrCategory.java`
  - `saved/storage/category/package-info.java`
  - `saved/storage/category/RecipeBookCategoryCategory.java`
  - `saved/storage/category/UnstackableCategory.java`
- `saved/storage/category/store`
  - `saved/storage/category/store/CategoryEntry.java`
  - `saved/storage/category/store/CategoryMode.java`
  - `saved/storage/category/store/package-info.java`
- `util/registrater`
  - `util/registrater/DataGenUtil.java`

### 6.2 修改 — Java（57）
- `(root)`
  - `AnvilCraft.java`
- `api/amulet`
  - `api/amulet/AmuletManager.java`
- `api/amulet/def`
  - `api/amulet/def/IAmuletDefinition.java`
- `api/itemhandler`
  - `api/itemhandler/OverLimitItemHandler.java`
- `api/recipe/data`
  - `api/recipe/data/ICustomDataComponent.java`
- `api/recipe/number`
  - `api/recipe/number/INumberProvider.java`
- `api/recipe/result/modifier`
  - `api/recipe/result/modifier/IResultModifier.java`
- `api/tooltip`
  - `api/tooltip/HudTooltipManager.java`
  - `api/tooltip/ItemTooltipManager.java`
- `block/entity`
  - `block/entity/FeCollectorBlockEntity.java`
  - `block/entity/OverseerBlockEntity.java`
  - `block/entity/PowerConverterBlockEntity.java`
- `client/event`
  - `client/event/ClientEventListener.java`
- `client/gui/component`
  - `client/gui/component/SwitchableButton.java`
- `client/gui/screen`
  - `client/gui/screen/ActiveSilencerScreen.java`
  - `client/gui/screen/EmberGrindstoneScreen.java`
  - `client/gui/screen/TeslaTowerScreen.java`
- `config`
  - `config/AnvilCraftClientConfig.java`
  - `config/AnvilCraftServerConfig.java`
- `constant`
  - `constant/SharedTextures.java`
- `data`
  - `data/AnvilCraftDatagen.java`
- `data/lang`
  - `data/lang/LangHandler.java`
  - `data/lang/OtherLang.java`
  - `data/lang/ScreenLang.java`
  - `data/lang/ToolPropertyLang.java`
- `data/recipe`
  - `data/recipe/MultiBlockConversionRecipeLoader.java`
  - `data/recipe/MultiBlockRecipeLoader.java`
  - `data/recipe/RegistrumBlockRecipeLoader.java`
- `data/tags`
  - `data/tags/BlockTagLoader.java`
- `event`
  - `event/PlayerTickEventHandler.java`
  - `event/ServerLifecycleEventListener.java`
- `init`
  - `init/ModAnvilBehaviors.java`
  - `init/ModMenuTypes.java`
- `init/block`
  - `init/block/ModBlockEntities.java`
  - `init/block/ModBlocks.java`
- `init/item`
  - `init/item/ModAmuletDefinitions.java`
  - `init/item/ModAmuletDefinitionTypes.java`
  - `init/item/ModAmuletTypes.java`
  - `init/item/ModComponents.java`
  - `init/item/ModCustomDataComponents.java`
- `init/recipe`
  - `init/recipe/ModNumberProviderTypes.java`
  - `init/recipe/ModResultModifierTypes.java`
- `integration/jei`
  - `integration/jei/AnvilCraftJeiPlugin.java`
- `inventory/container`
  - `inventory/container/FilterContainer.java`
- `item/property/component`
  - `item/property/component/FilterContent.java`
  - `item/property/component/OverLimitItemContainerContents.java`
- `item/property/component/amulet`
  - `item/property/component/amulet/IAmulet.java`
- `recipe/multiblock`
  - `recipe/multiblock/BlockPredicateWithState.java`
  - `recipe/multiblock/MultiblockConversionRecipe.java`
  - `recipe/multiblock/MultiblockRecipe.java`
- `recipe/transform`
  - `recipe/transform/MobTransformWithItemRecipe.java`
- `util`
  - `util/FormattingUtil.java`

### 6.3 新增 — 资源 / 生成数据（69）
- `src/generated/resources/assets/anvilcraft/blockstates/crate.json`
- `src/generated/resources/assets/anvilcraft/items/acceleration_ring.json`
- `src/generated/resources/assets/anvilcraft/items/celestial_forging_anvil.json`
- `src/generated/resources/assets/anvilcraft/items/celestial_forging_anvil_amplifier.json`
- `src/generated/resources/assets/anvilcraft/items/crate.json`
- `src/generated/resources/assets/anvilcraft/items/deflection_ring.json`
- `src/generated/resources/assets/anvilcraft/items/giant_anvil.json`
- `src/generated/resources/assets/anvilcraft/items/hyperdimension_storage_station.json`
- `src/generated/resources/assets/anvilcraft/items/large_crate.json`
- `src/generated/resources/assets/anvilcraft/items/large_fluid_tank.json`
- `src/generated/resources/assets/anvilcraft/items/shulker_container.json`
- `src/generated/resources/data/anvilcraft/advancement/recipes/multiblock/large_crate.json`
- `src/generated/resources/data/anvilcraft/advancement/recipes/multiblock_conversion/large_crate.json`
- `src/generated/resources/data/anvilcraft/advancement/recipes/transportation/crate.json`
- `src/generated/resources/data/anvilcraft/anvilcraft/category/anvilcraft.json`
- `src/generated/resources/data/anvilcraft/anvilcraft/category/block.json`
- `src/generated/resources/data/anvilcraft/anvilcraft/category/enchanted.json`
- `src/generated/resources/data/anvilcraft/anvilcraft/category/food_and_drink.json`
- `src/generated/resources/data/anvilcraft/anvilcraft/category/minecraft.json`
- `src/generated/resources/data/anvilcraft/anvilcraft/category/redstone.json`
- `src/generated/resources/data/anvilcraft/anvilcraft/category/unstackable.json`
- `src/generated/resources/data/anvilcraft/loot_table/blocks/crate.json`
- `src/generated/resources/data/anvilcraft/loot_table/blocks/hyperdimension_storage_station.json`
- `src/generated/resources/data/anvilcraft/loot_table/blocks/large_crate.json`
- `src/generated/resources/data/anvilcraft/recipe/crate.json`
- `src/generated/resources/data/anvilcraft/recipe/multiblock/large_crate.json`
- `src/generated/resources/data/anvilcraft/recipe/multiblock_conversion/large_crate.json`
- `src/main/resources/assets/anvilcraft/blockstates/hyperdimension_storage_station.json`
- `src/main/resources/assets/anvilcraft/blockstates/large_crate.json`
- `src/main/resources/assets/anvilcraft/font/small.json`
- `src/main/resources/assets/anvilcraft/models/block/hyperdimension_storage_station_part.json`
- `src/main/resources/assets/anvilcraft/models/block/large_crate_part.json`
- `src/main/resources/assets/anvilcraft/textures/font/small.png`
- `src/main/resources/assets/anvilcraft/textures/gui/sprites/category_settings_selected_back.png`
- `src/main/resources/assets/anvilcraft/textures/gui/sprites/category_settings_selected_back.png.mcmeta`
- `src/main/resources/assets/anvilcraft/textures/gui/sprites/category_settings_selected_front.png`
- `src/main/resources/assets/anvilcraft/textures/gui/sprites/category_settings_selected_front.png.mcmeta`

### 6.4 修改 — 资源 / 生成数据（57）
- `gradle/libs.versions.toml`
- `src/generated/resources/assets/anvilcraft/lang/en_ud.json`
- `src/generated/resources/assets/anvilcraft/lang/en_us.json`
- `src/generated/resources/data/anvilcraft/tags/block/needs_ember_tool.json`
- `src/generated/resources/data/anvilcraft/tags/item/explosion_proof.json`
- `src/generated/resources/data/minecraft/tags/block/mineable/axe.json`
- `src/generated/resources/data/minecraft/tags/block/mineable/pickaxe.json`
- `src/main/resources/anvilcraft.mixins.json`
- `src/main/resources/assets/anvilcraft/blockstates/shulker_container.json`
- `src/main/resources/assets/anvilcraft/models/block/acceleration_ring.json`
- `src/main/resources/assets/anvilcraft/models/block/celestial_forging_anvil.json`
- `src/main/resources/assets/anvilcraft/models/block/celestial_forging_anvil_amplifier.json`
- `src/main/resources/assets/anvilcraft/models/block/celestial_forging_anvil_fluid_interface.json`
- `src/main/resources/assets/anvilcraft/models/block/celestial_forging_anvil_interface.json`
- `src/main/resources/assets/anvilcraft/models/block/celestial_forging_anvil_laser_interface.json`
- `src/main/resources/assets/anvilcraft/models/block/celestial_forging_anvil_logistics_interface.json`
- `src/main/resources/assets/anvilcraft/models/block/crate.json`
- `src/main/resources/assets/anvilcraft/models/block/deflection_ring.json`
- `src/main/resources/assets/anvilcraft/models/block/giant_anvil.json`
- `src/main/resources/assets/anvilcraft/models/block/hyperdimension_storage_station.json`
- `src/main/resources/assets/anvilcraft/models/block/large_cake.json`
- `src/main/resources/assets/anvilcraft/models/block/large_cauldron.json`
- `src/main/resources/assets/anvilcraft/models/block/large_crate.json`
- `src/main/resources/assets/anvilcraft/models/block/large_fluid_tank.json`
- `src/main/resources/assets/anvilcraft/models/block/shulker_container.json`
- `src/main/resources/assets/anvilcraft/models/block/shulker_container_open.json`
- `src/main/resources/assets/anvilcraft/models/block/shulker_container_part.json`
- `src/main/resources/assets/anvilcraft/models/block/template_large_block.json`
- `src/main/resources/assets/anvilcraft/textures/block/crate_bottom.png`
- `src/main/resources/assets/anvilcraft/textures/block/crate_side.png`
- `src/main/resources/assets/anvilcraft/textures/block/crate_top.png`
- `src/main/resources/assets/anvilcraft/textures/block/hyperdimension_storage_station.png`
- `src/main/resources/assets/anvilcraft/textures/block/hyperspace_storage_station.png`
- `src/main/resources/assets/anvilcraft/textures/block/large_crate_bottom.png`
- `src/main/resources/assets/anvilcraft/textures/block/large_crate_side.png`
- `src/main/resources/assets/anvilcraft/textures/block/large_crate_top.png`
- `src/main/resources/assets/anvilcraft/textures/block/shulker_container.png`
- `src/main/resources/assets/anvilcraft/textures/block/shulker_container_open.png`
- `src/main/resources/assets/anvilcraft/textures/gui/misc/background/storage_station.png`
- `src/main/resources/assets/anvilcraft/textures/gui/misc/background/storage_station_category_setting.png`
- `src/main/resources/assets/anvilcraft/textures/gui/misc/storage_station/cancel.png`
- `src/main/resources/assets/anvilcraft/textures/gui/misc/storage_station/capacity.png`
- `src/main/resources/assets/anvilcraft/textures/gui/misc/storage_station/category.png`
- `src/main/resources/assets/anvilcraft/textures/gui/misc/storage_station/category_add.png`
- `src/main/resources/assets/anvilcraft/textures/gui/misc/storage_station/category_setting.png`
- `src/main/resources/assets/anvilcraft/textures/gui/misc/storage_station/confirm.png`
- `src/main/resources/assets/anvilcraft/textures/gui/misc/storage_station/nbt_fold.png`
- `src/main/resources/assets/anvilcraft/textures/gui/misc/storage_station/nbt_unfold.png`
- `src/main/resources/assets/anvilcraft/textures/gui/misc/storage_station/put.png`
- `src/main/resources/assets/anvilcraft/textures/gui/misc/storage_station/reverse_order.png`
- `src/main/resources/assets/anvilcraft/textures/gui/misc/storage_station/search_clear.png`
- `src/main/resources/assets/anvilcraft/textures/gui/misc/storage_station/search_retention.png`
- `src/main/resources/assets/anvilcraft/textures/gui/misc/storage_station/sequential_order.png`
- `src/main/resources/assets/anvilcraft/textures/gui/misc/storage_station/slider_big.png`
- `src/main/resources/assets/anvilcraft/textures/gui/misc/storage_station/slider_small.png`
- `src/main/resources/assets/anvilcraft/textures/gui/misc/storage_station/sort_by_mod.png`
- `src/main/resources/assets/anvilcraft/textures/gui/misc/storage_station/sort_by_name.png`
- `src/main/resources/assets/anvilcraft/textures/gui/misc/storage_station/sort_by_name_reverse.png`
- `src/main/resources/assets/anvilcraft/textures/gui/misc/storage_station/sort_by_number.png`
- `src/main/resources/assets/anvilcraft/textures/gui/misc/storage_station/sort_by_number_reverse.png`
- `src/main/resources/assets/anvilcraft/textures/gui/misc/storage_station/take.png`
- `src/main/resources/META-INF/accesstransformer.cfg`


## 七、风险与待确认

| 风险/问题 | 影响 | 缓解 |
|-----------|------|------|
| 主 jar 内嵌 `anvillib_rpc` 的加载（嵌套 jarjar 合并 `anvillib-network`） | 运行时 mod 加载 | 与 26.1 布局一致，启动日志确认 `anvillib_rpc` 加载且 `anvillib_network` 无重复/版本冲突；如出现冲突再单独 `jarJar(libs.anvillibRpc)` |
| `EnchantmentsPredicateMixin` / `ExtraEnchantmentsPredicate` 目标类在 1.21.1 不存在 | mixin 崩溃 / 编译失败 | 按 1.21.1 `ItemEnchantmentsPredicate` 改写或删除；只保留仓储需要的语义 |
| `ComponentSerialization.bootstrap` 签名差异 | mixin 注入失败 | 移植时比对 1.21.1 反编译源码；签名不符则改用 `@Mod.EventBusSubscriber` 等替代注册 |
| `api/component` 三件套冗余 | 序列化/显示异常 | 优先复用 1.21.1 原版 `TranslatableContents`；仅保留 `ModNameContents` 必需部分 |
| `ValueInput/ValueOutput` 改写 | 方块实体存档损坏 | 用 `CompoundTag`+`HolderLookup.Provider` 重写并做存档往返测试 |
| 多方块配方加载器/`BlockPredicateWithState` 三方合并 | 冲突集中、回归风险 | 用 `git merge-file` 三方合并；跑既有多方块配方（CFA/大蛋糕/大熔炉等）回归 |
| JEI 19.32 接口差异 | JEI 集成失败 | 先核对 `IGlobalGuiHandler`/`IClickableIngredientFactory` 是否存在；不存在则用 `IGuiContainerHandler` 等价实现 |
| 远程 Maven 不可达 | 任何远程 anvillib 解析都会失败 | 一律走 `D:\Maven` 本地仓库；CI/他人环境需先发布 anvillib 2.0.0 或复制本地仓库 |

## 八、参考

- 源 commit：`618a5fd8a`、`a962a8345`、`b4a3442bc`、`62bbc1c80`、`13f573207`（`origin/dev/26.1/1.6`）
- 系列总 diff：`git diff 618a5fd8a^ 13f573207 --stat`
- 本地 anvillib：`D:\Maven\dev\anvilcraft\lib\`（1.21.1 2.0.0 全套；主 jar 已于 2026-08-11 16:55 重新发布，内嵌 rpc 等 18 个模块；`anvillib-rpc-neoforge-1.21.1-2.0.0-sources.jar` 可作源码参考）
- 环境：MC 1.21.1 / NeoForge 21.1.219 / JEI 19.32.0.358 / anvillib 2.0.0（本地发布）
