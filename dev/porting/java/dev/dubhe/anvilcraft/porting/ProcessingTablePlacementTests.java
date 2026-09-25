package dev.dubhe.anvilcraft.porting;

import dev.anvilcraft.lib.v2.recipe.event.InWorldRecipeEvent;
import dev.anvilcraft.lib.v2.recipe.util.InWorldRecipeContext;
import dev.anvilcraft.lib.v2.util.predicate.ChanceItemStack;
import dev.anvilcraft.lib.v2.util.predicate.ItemIngredientPredicate;
import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.api.block.BlockPlacementRules;
import dev.dubhe.anvilcraft.block.UseItemOnBlock;
import dev.dubhe.anvilcraft.block.entity.ProcessingTableBlockEntity;
import dev.dubhe.anvilcraft.block.entity.StampingPlatformBlockEntity;
import dev.dubhe.anvilcraft.block.placement.ProcessingTablePlacement;
import dev.dubhe.anvilcraft.building.BlockEntityContentAdapter;
import dev.dubhe.anvilcraft.init.ModTargetPointers;
import dev.dubhe.anvilcraft.init.block.ModBlockEntities;
import dev.dubhe.anvilcraft.init.block.ModBlocks;
import dev.dubhe.anvilcraft.init.item.ModItems;
import dev.dubhe.anvilcraft.recipe.anvil.wrap.AbstractProcessRecipe;
import dev.dubhe.anvilcraft.recipe.anvil.wrap.ItemCrushRecipe;
import dev.dubhe.anvilcraft.recipe.anvil.wrap.MeshRecipe;
import dev.dubhe.anvilcraft.recipe.anvil.wrap.StampingRecipe;
import dev.dubhe.anvilcraft.recipe.anvil.wrap.UnpackRecipe;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.FunctionGameTestInstance;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.TestData;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.item.FallingBlockEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.TrapDoorBlock;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.Half;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.phys.BlockHitResult;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;
import net.neoforged.neoforge.registries.RegisterEvent;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.item.ItemStacksResourceHandler;
import net.neoforged.neoforge.transfer.transaction.Transaction;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;

import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

@EventBusSubscriber(modid = AnvilCraft.MOD_ID)
public final class ProcessingTablePlacementTests {
    private static final Map<String, Consumer<GameTestHelper>> TESTS = Map.of(
        "port_processing_material_rules", ProcessingTablePlacementTests::rules,
        "port_processing_handler_assembly", ProcessingTablePlacementTests::handler,
        "port_processing_assembly_rollback", ProcessingTablePlacementTests::rollback,
        "port_processing_entity_assembly", ProcessingTablePlacementTests::entities,
        "port_processing_conversion", ProcessingTablePlacementTests::conversion,
        "port_processing_inventory", ProcessingTablePlacementTests::inventory,
        "port_processing_persistence", ProcessingTablePlacementTests::persistence,
        "port_processing_recipe_structures", ProcessingTablePlacementTests::recipes,
        "port_processing_recipe_execution", ProcessingTablePlacementTests::execution
    );

    @SubscribeEvent
    public static void functions(RegisterEvent event) {
        event.register(Registries.TEST_FUNCTION, registry -> TESTS.forEach((name, test) -> registry.register(AnvilCraft.of(name), test)));
    }

    @SubscribeEvent
    public static void register(RegisterGameTestsEvent event) {
        var environment = event.registerEnvironment(AnvilCraft.of("port_processing_placement"));
        TESTS.forEach((name, test) -> event.registerTest(AnvilCraft.of(name), new FunctionGameTestInstance(
            ResourceKey.create(Registries.TEST_FUNCTION, AnvilCraft.of(name)),
            new TestData<>(environment, AnvilCraft.of("port_logistics_empty"), 100, 0, true)
        )));
    }

    private static void rules(GameTestHelper helper) {
        var states = List.of(ModBlocks.CRUSHING_TABLE.getDefaultState(), ModBlocks.SIFTING_TABLE.getDefaultState(),
            ModBlocks.UNPACKING_TABLE.getDefaultState());
        var upgrades = List.of(Items.GRINDSTONE, Items.SCAFFOLDING, Items.IRON_TRAPDOOR);
        for (int i = 0; i < states.size(); i++) {
            var state = states.get(i);
            var items = BlockPlacementRules.getPlacementIngredients(helper.getLevel().registryAccess(), state);
            helper.assertTrue(items.size() == 2 && items.get(0).is(ModBlocks.STAMPING_PLATFORM.asItem())
                && items.get(1).is(upgrades.get(i)) && items.stream().allMatch(stack -> stack.getCount() == 1),
                "加工台数据规则必须同时列出冲压平台和升级材料");
            helper.assertTrue(ProcessingTablePlacement.baseMaterial(state).is(ModBlocks.STAMPING_PLATFORM.asItem()),
                "蓝图基础材料必须使用冲压平台");
        }
        helper.assertTrue(!ProcessingTablePlacement.isConverted(ModBlocks.STAMPING_PLATFORM.getDefaultState())
            && ProcessingTablePlacement.baseMaterial(Blocks.STONE.defaultBlockState()).is(Items.STONE), "普通方块不走升级组装");
        helper.succeed();
    }

    private static void handler(GameTestHelper helper) {
        var level = helper.getLevel();
        var source = helper.absolutePos(new BlockPos(2, 15, 2));
        level.setBlock(source, Blocks.CHEST.defaultBlockState(), Block.UPDATE_CLIENTS);
        var chest = (ChestBlockEntity) level.getBlockEntity(source);
        var states = List.of(ModBlocks.CRUSHING_TABLE.getDefaultState(), ModBlocks.SIFTING_TABLE.getDefaultState(),
            ModBlocks.UNPACKING_TABLE.getDefaultState());
        for (int i = 0; i < states.size(); i++) {
            var state = states.get(i);
            chest.setItem(0, ModBlocks.STAMPING_PLATFORM.asStack(3));
            chest.setItem(1, ItemStack.EMPTY);
            var type = ModTargetPointers.BLOCK_ITEM_HANDLER.get();
            helper.assertTrue(type.point(level, source, Direction.UP, state) == null, "缺少升级材料时不能选中冲压平台进行组装");
            chest.setItem(1, UseItemOnBlock.materialFor(state).copyWithCount(2));
            var pointer = type.point(level, source, Direction.UP, state);
            var target = helper.absolutePos(new BlockPos(2, 15 + i, 4));
            helper.assertTrue(pointer != null && pointer.applyToPos(level, target, state), "真实容器指针必须能放置对应加工台");
            helper.assertTrue(level.getBlockState(target).is(state.getBlock())
                && chest.getItem(0).getCount() == 2 && chest.getItem(1).getCount() == 1,
                "成功组装必须只扣除一份基础和一份升级材料");
        }
        helper.succeed();
    }

    private static void rollback(GameTestHelper helper) {
        var handler = new ItemStacksResourceHandler(2) {
            private int upgradeAttempts;

            @Override
            public int extract(int index, ItemResource resource, int amount, TransactionContext transaction) {
                if (index == 1 && ++this.upgradeAttempts > 1) return 0;
                return super.extract(index, resource, amount, transaction);
            }
        };
        handler.set(0, ItemResource.of(ModBlocks.STAMPING_PLATFORM.asItem()), 3);
        handler.set(1, ItemResource.of(Items.GRINDSTONE), 2);
        var target = helper.absolutePos(new BlockPos(2, 15, 4));
        boolean placed = ProcessingTablePlacement.placeFromHandler(helper.getLevel(), target, ModBlocks.CRUSHING_TABLE.getDefaultState(),
            handler, 0, ModBlocks.STAMPING_PLATFORM.asStack(), target.below(), Direction.NORTH);
        helper.assertTrue(!placed && handler.getAmountAsLong(0) == 3 && handler.getAmountAsLong(1) == 2
            && helper.getLevel().getBlockState(target).isAir(), "预检后升级材料失效，已提取的平台必须回滚，且不能先放出方块");
        helper.succeed();
    }

    private static void entities(GameTestHelper helper) {
        final var level = helper.getLevel();
        var source = helper.absolutePos(new BlockPos(2, 15, 2));
        final var target = helper.absolutePos(new BlockPos(2, 15, 4));
        var center = source.getCenter();
        var base = new ItemEntity(level, center.x, center.y, center.z, ModBlocks.STAMPING_PLATFORM.asStack(2));
        var upgrade = new ItemEntity(level, center.x, center.y, center.z, new ItemStack(Items.IRON_TRAPDOOR, 2));
        base.setNoGravity(true);
        upgrade.setNoGravity(true);
        base.setDeltaMovement(0, 0, 0);
        upgrade.setDeltaMovement(0, 0, 0);
        level.addFreshEntity(base);
        level.addFreshEntity(upgrade);
        helper.runAfterDelay(2, () -> {
            var state = ModBlocks.UNPACKING_TABLE.getDefaultState();
            var pointer = ModTargetPointers.ITEM_ENTITY.get().point(level, source, Direction.UP, state);
            helper.assertTrue(pointer != null && pointer.applyToPos(level, target, state), "地面指针必须查找同格升级材料并组装");
            helper.assertTrue(base.getItem().getCount() == 1 && upgrade.getItem().getCount() == 1
                && level.getBlockState(target).is(state.getBlock()), "地面组装必须扣除两种材料各一份");
            helper.assertTrue(!ProcessingTablePlacement.placeFromEntities(level, target, state, base, Direction.NORTH)
                && base.getItem().getCount() == 1 && upgrade.getItem().getCount() == 1, "目标被占用时不得扣除任何地面材料");
            helper.succeed();
        });
    }

    private static void conversion(GameTestHelper helper) {
        var level = helper.getLevel();
        var pos = helper.absolutePos(new BlockPos(2, 15, 4));
        var state = ModBlocks.STAMPING_PLATFORM.getDefaultState().setValue(BlockStateProperties.WATERLOGGED, true);
        level.setBlock(pos, state, Block.UPDATE_CLIENTS);
        var player = helper.makeMockPlayer(GameType.SURVIVAL);
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.GRINDSTONE, 2));
        state.useItemOn(player.getMainHandItem(), level, player, InteractionHand.MAIN_HAND,
            new BlockHitResult(pos.getCenter(), Direction.UP, pos, false));
        helper.assertTrue(level.getBlockState(pos).is(ModBlocks.STAMPING_PLATFORM.get()) && player.getMainHandItem().isEmpty()
            && ((ProcessingTableBlockEntity) level.getBlockEntity(pos)).getInput().getAmountAsLong(0) == 2,
            "加工台上表面应存入物品而不触发升级");
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.GRINDSTONE, 2));
        var hit = new BlockHitResult(pos.getCenter(), Direction.NORTH, pos, false);
        state.useItemOn(player.getMainHandItem(), level, player, InteractionHand.MAIN_HAND, hit);
        helper.assertTrue(level.getBlockState(pos).is(ModBlocks.CRUSHING_TABLE.get())
            && level.getBlockState(pos).getValue(BlockStateProperties.WATERLOGGED) && player.getMainHandItem().getCount() == 1,
            "侧面升级应消耗一件材料并保留含水状态");
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.SCAFFOLDING, 2));
        level.getBlockState(pos).useItemOn(player.getMainHandItem(), level, player, InteractionHand.MAIN_HAND, hit);
        helper.assertTrue(level.getBlockState(pos).is(ModBlocks.SIFTING_TABLE.get())
            && player.getInventory().countItem(Items.GRINDSTONE) == 1, "替换升级应返还之前的部件");
        player.setItemInHand(InteractionHand.MAIN_HAND, ModItems.ANVIL_HAMMER.asStack());
        level.getBlockState(pos).useItemOn(player.getMainHandItem(), level, player, InteractionHand.MAIN_HAND, hit);
        helper.assertTrue(level.getBlockState(pos).is(ModBlocks.STAMPING_PLATFORM.get())
            && player.getInventory().countItem(Items.SCAFFOLDING) == 1, "锤子应还原平台并返还升级部件");
        helper.succeed();
    }

    private static void inventory(GameTestHelper helper) {
        var level = helper.getLevel();
        var pos = helper.absolutePos(new BlockPos(2, 15, 4));
        level.setBlock(pos, ModBlocks.STAMPING_PLATFORM.getDefaultState(), Block.UPDATE_CLIENTS);
        var table = (ProcessingTableBlockEntity) level.getBlockEntity(pos);
        var external = level.getCapability(Capabilities.Item.BLOCK, pos, Direction.UP);
        var named = new ItemStack(Items.DIAMOND);
        named.set(DataComponents.CUSTOM_NAME, Component.literal("variant"));
        try (Transaction transaction = Transaction.openRoot()) {
            helper.assertTrue(external.insert(0, ItemResource.of(Items.DIAMOND), 60, transaction) == 60
                && external.insert(1, ItemResource.of(named), 12, transaction) == 4,
                "同一物品的不同组件也必须共享全台一组的上限");
            helper.assertTrue(external.extract(0, ItemResource.of(Items.DIAMOND), 1, transaction) == 0
                && external.extract(ItemResource.of(Items.DIAMOND), 1, transaction) == 0,
                "自动化的索引和无索引路径均不得抽走原料");
            transaction.commit();
        }
        var player = helper.makeMockPlayer(GameType.SURVIVAL);
        helper.assertTrue(!table.tryInteractItems(player, InteractionHand.OFF_HAND), "副手不能误触原料存取");
        helper.assertTrue(table.tryInteractItems(player, InteractionHand.MAIN_HAND)
            && player.getInventory().countItem(Items.DIAMOND) == 64 && table.getInput().getAmountAsLong(0) == 0,
            "主手空手应取回所有原料并清空台面");
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.EMERALD, 9));
        helper.assertTrue(table.tryInteractItems(player, InteractionHand.MAIN_HAND)
            && player.getMainHandItem().isEmpty() && table.getInput().getAmountAsLong(0) == 9, "主手应存入原料");
        table.onRecipeExecuted(20);
        helper.assertTrue(table.getDoorOpenProgress(0) == 0 && table.getSpinProgress(0) == 0, "加工动画应从闭门静止起点开始");
        level.destroyBlock(pos, false);
        int dropped = level.getEntitiesOfClass(ItemEntity.class, new net.minecraft.world.phys.AABB(pos).inflate(1)).stream()
            .filter(entity -> entity.getItem().is(Items.EMERALD)).mapToInt(entity -> entity.getItem().getCount()).sum();
        helper.assertTrue(dropped == 9, "拆除方块时原料必须只掉落一次");
        helper.succeed();
    }

    private static void persistence(GameTestHelper helper) {
        var registries = helper.getLevel().registryAccess();
        var state = ModBlocks.STAMPING_PLATFORM.getDefaultState();
        var original = new StampingPlatformBlockEntity(ModBlockEntities.STAMPING_PLATFORM.get(), BlockPos.ZERO, state);
        final var tag = original.saveWithFullMetadata(registries);
        var inputs = new CompoundTag();
        var items = new ListTag();
        var item = (CompoundTag) ItemStack.CODEC.encodeStart(registries.createSerializationContext(NbtOps.INSTANCE),
            new ItemStack(Items.DIAMOND, 5)).getOrThrow();
        item.putByte("Slot", (byte) 3);
        items.add(item);
        inputs.put("Items", items);
        inputs.putInt("Size", 8);
        tag.put("Inputs", inputs);
        original.loadWithComponents(TagValueInput.create(ProblemReporter.DISCARDING, registries, tag));
        helper.assertTrue(original.getInput().getAmountAsLong(3) == 5, "旧版原料槽格式必须能恢复");
        var saved = original.saveWithFullMetadata(registries);
        var extracted = BlockEntityContentAdapter.extract(state, saved, registries);
        helper.assertTrue(extracted.contents().size() == 1 && extracted.contents().getFirst().slot() == 3
            && !extracted.config().getCompoundOrEmpty("Inputs").contains("stacks"), "原生库存必须拆成供料且不残留在蓝图配置中");
        var restored = new StampingPlatformBlockEntity(ModBlockEntities.STAMPING_PLATFORM.get(), BlockPos.ZERO, state);
        restored.loadWithComponents(TagValueInput.create(ProblemReporter.DISCARDING, registries, extracted.config()));
        BlockEntityContentAdapter.insert(restored, extracted.contents(), registries);
        helper.assertTrue(restored.getInput().getAmountAsLong(3) == 5 && restored.getInput().getResource(3).is(Items.DIAMOND),
            "蓝图恢复必须绕过自动化的仅插入代理，准确还原原槽位");
        helper.succeed();
    }

    private static void recipes(GameTestHelper helper) {
        var mesh = new MeshRecipe(List.of(), List.of()).getFirstInputBlock().getStatesCache();
        helper.assertTrue(mesh.contains(Blocks.SCAFFOLDING.defaultBlockState())
            && mesh.contains(ModBlocks.SIFTING_TABLE.getDefaultState()), "过筛配方必须兼容脚手架与新过筛台");
        var unpack = new UnpackRecipe(List.of(), List.of()).getFirstInputBlock().getStatesCache();
        helper.assertTrue(unpack.contains(Blocks.IRON_TRAPDOOR.defaultBlockState().setValue(TrapDoorBlock.HALF, Half.TOP))
            && unpack.contains(ModBlocks.UNPACKING_TABLE.getDefaultState())
            && unpack.contains(ModBlocks.UNPACKING_TABLE.getDefaultState().setValue(BlockStateProperties.WATERLOGGED, true)),
            "拆包配方必须匹配关闭的上活板门及含水或干燥拆包台");
        helper.assertTrue(!unpack.contains(Blocks.IRON_TRAPDOOR.defaultBlockState().setValue(TrapDoorBlock.OPEN, true)),
            "新台面不能放宽原有活板门开合限制");
        helper.succeed();
    }

    private static void execution(GameTestHelper helper) {
        var inputs = List.of(ItemIngredientPredicate.of(Items.DIAMOND).build());
        var results = List.of(ChanceItemStack.of(Items.EMERALD, 1));
        List<AbstractProcessRecipe<?>> recipes = List.of(new StampingRecipe(inputs, results), new ItemCrushRecipe(inputs, results),
            new MeshRecipe(inputs, results), new UnpackRecipe(inputs, results));
        var states = List.of(ModBlocks.STAMPING_PLATFORM.getDefaultState(), ModBlocks.CRUSHING_TABLE.getDefaultState(),
            ModBlocks.SIFTING_TABLE.getDefaultState(), ModBlocks.UNPACKING_TABLE.getDefaultState());
        var level = helper.getLevel();
        for (int i = 0; i < states.size(); i++) {
            var pos = helper.absolutePos(new BlockPos(2, 15 + i * 3, 4));
            level.setBlock(pos, states.get(i), Block.UPDATE_CLIENTS);
            var table = (ProcessingTableBlockEntity) level.getBlockEntity(pos);
            try (Transaction transaction = Transaction.openRoot()) {
                table.getInput().insert(ItemResource.of(Items.DIAMOND), 1, transaction);
                transaction.commit();
            }
            var anvil = FallingBlockEntity.fall(level, pos.above(), Blocks.ANVIL.defaultBlockState());
            var context = new InWorldRecipeContext(level, pos.above().getBottomCenter(), anvil);
            var recipe = recipes.get(i);
            helper.assertTrue(recipe.matches(context, level), "配方必须从真实台面库存而非地面实体匹配原料");
            recipe.assemble(context);
            NeoForge.EVENT_BUS.post(new InWorldRecipeEvent(recipe.getType(), AnvilCraft.of("port_processing_execution"), recipe, context));
            context.accept();
            anvil.discard();
            helper.assertTrue(table.getInput().getResource(0).isEmpty(), "配方提交必须扣除台面原料");
            int produced = level.getEntitiesOfClass(ItemEntity.class, new net.minecraft.world.phys.AABB(pos).inflate(0.5)).stream()
                .filter(entity -> entity.getItem().is(Items.EMERALD)).mapToInt(entity -> entity.getItem().getCount()).sum();
            helper.assertTrue(produced == 1, "配方必须从台面底部生成结果而非留在空输出缓存");
            if (i < 2) helper.assertTrue(table.getDoorDurationTick() == (i == 0 ? 8 : 20), "冲压和粉碎应触发各自的动画周期");
        }
        helper.succeed();
    }
}
