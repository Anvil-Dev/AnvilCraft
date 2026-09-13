package dev.dubhe.anvilcraft.porting;

import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import dev.anvilcraft.lib.v2.recipe.cache.BlockCache;
import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.api.block.BlockPlacementRules;
import dev.dubhe.anvilcraft.api.pointer.BlockPointer;
import dev.dubhe.anvilcraft.api.pointer.ITargetPointer;
import dev.dubhe.anvilcraft.block.cauldron.FireCauldronBlock;
import dev.dubhe.anvilcraft.block.cauldron.OilCauldronBlock;
import dev.dubhe.anvilcraft.block.placement.BlockPlacementRuleSet;
import dev.dubhe.anvilcraft.init.ModTargetPointers;
import dev.dubhe.anvilcraft.init.block.ModBlocks;
import dev.dubhe.anvilcraft.init.item.ModItems;
import dev.dubhe.anvilcraft.init.registry.ModRegistryKeys;
import dev.dubhe.anvilcraft.util.BlockPlacementUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.FunctionGameTestInstance;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.TestData;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BedPart;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.Half;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;
import net.neoforged.neoforge.registries.RegisterEvent;

import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

@EventBusSubscriber(modid = AnvilCraft.MOD_ID)
public final class PlacementPortGameTests {
    private static final BlockPos SOURCE = new BlockPos(2, 2, 2);
    private static final BlockPos TARGET = new BlockPos(6, 2, 2);
    private static final Map<String, Consumer<GameTestHelper>> TESTS = Map.of(
        "port_placement_rules", PlacementPortGameTests::rules,
        "port_placement_codec", PlacementPortGameTests::codec,
        "port_placement_inventory", PlacementPortGameTests::inventory,
        "port_placement_return_bucket", PlacementPortGameTests::returnBucket,
        "port_placement_move_chest", PlacementPortGameTests::moveChest,
        "port_placement_move_bed", PlacementPortGameTests::moveBed,
        "port_placement_neighbor_update", PlacementPortGameTests::neighborUpdate,
        "port_placement_blocked_bed", PlacementPortGameTests::blockedBed,
        "port_fire_cauldron", PlacementPortGameTests::fireCauldron,
        "port_placement_item_entity", PlacementPortGameTests::itemEntity
    );

    @SubscribeEvent
    public static void functions(RegisterEvent event) {
        event.register(Registries.TEST_FUNCTION, registry -> TESTS.forEach((name, test) -> registry.register(AnvilCraft.of(name), test)));
    }

    @SubscribeEvent
    public static void register(RegisterGameTestsEvent event) {
        var environment = event.registerEnvironment(AnvilCraft.of("port_placement"));
        TESTS.forEach((name, test) -> event.registerTest(AnvilCraft.of(name), new FunctionGameTestInstance(
            ResourceKey.create(Registries.TEST_FUNCTION, AnvilCraft.of(name)),
            new TestData<>(environment, AnvilCraft.of("port_empty"), 100, 0, true)
        )));
    }

    private static void rules(GameTestHelper helper) {
        var registries = helper.getLevel().registryAccess();
        helper.assertTrue(registries.lookup(ModRegistryKeys.BLOCK_PLACEMENT_RULES).isPresent(), "放置规则注册表必须加载");
        helper.assertTrue(BlockPlacementRules.getPlacementItemCount(registries, Blocks.STONE.defaultBlockState(),
            new ItemStack(Items.STONE)) == 1, "普通方块必须使用单个物品");
        helper.assertTrue(BlockPlacementRules.getPlacementItemCount(registries, Blocks.WHEAT.defaultBlockState(),
            new ItemStack(Items.WHEAT_SEEDS)) == 1, "小麦初始状态必须消耗种子");
        helper.assertTrue(BlockPlacementRules.getPlacementItemCount(registries,
            Blocks.WHEAT.defaultBlockState().setValue(BlockStateProperties.AGE_7, 7), new ItemStack(Items.WHEAT_SEEDS)) == -1,
            "成熟小麦不得作为初始放置状态");
        helper.assertTrue(BlockPlacementRules.getPlacementItemCount(registries,
            Blocks.SNOW.defaultBlockState().setValue(BlockStateProperties.LAYERS, 8), new ItemStack(Items.SNOW)) == 8,
            "雪层数量必须保持源规则");
        BlockState blueprint = Blocks.OAK_STAIRS.defaultBlockState().setValue(BlockStateProperties.HORIZONTAL_FACING, Direction.WEST)
            .setValue(BlockStateProperties.HALF, Half.TOP).setValue(BlockStateProperties.WATERLOGGED, true);
        BlockState placed = BlockPlacementRules.applyBlueprintStateRules(registries, Blocks.OAK_STAIRS.defaultBlockState(), blueprint);
        helper.assertTrue(placed == blueprint.setValue(BlockStateProperties.WATERLOGGED, false), "应继承蓝图并清除含水状态");
        BlockState fire = ModBlocks.FIRE_CAULDRON.getDefaultState().setValue(FireCauldronBlock.LEVEL, 3);
        BlockState oil = BlockPlacementRules.applyBlueprintStateRules(registries, fire, fire);
        helper.assertTrue(oil.is(ModBlocks.OIL_CAULDRON.get()) && oil.getValue(OilCauldronBlock.LEVEL) == 3, "火锅蓝图必须保留油量并熄火");
        helper.succeed();
    }

    private static void codec(GameTestHelper helper) {
        ItemStack stack = new ItemStack(Items.STONE, 2);
        stack.set(DataComponents.CUSTOM_NAME, Component.literal("placement codec"));
        BlockPlacementRuleSet original = new BlockPlacementRuleSet(
            List.of(new BlockPlacementRuleSet.StateRule(List.of("!age,!lit=true"), stack, new ItemStack(Items.BUCKET))), Map.of());
        var ops = helper.getLevel().registryAccess().createSerializationContext(JsonOps.INSTANCE);
        var encoded = BlockPlacementRuleSet.CODEC.encodeStart(ops, original).getOrThrow();
        var decoded = BlockPlacementRuleSet.CODEC.parse(ops, encoded).getOrThrow().getPlacementItems(Blocks.STONE.defaultBlockState());
        helper.assertTrue(decoded.size() == 1 && ItemStack.matches(decoded.getFirst().placeStack(), stack), "规则编码必须保留数量与组件");
        helper.assertTrue(decoded.getFirst().returnStack().is(Items.BUCKET), "规则编码必须保留返还物品");
        var forbidden = BlockPlacementRuleSet.CODEC.parse(ops,
            JsonParser.parseString("{\"rules\":[{\"properties\":\"\",\"item\":{}}]}")).getOrThrow();
        helper.assertTrue(forbidden.getPlacementItems(Blocks.STONE.defaultBlockState()).getFirst().isForbidden(), "空对象表示禁止放置");
        helper.succeed();
    }

    private static ChestBlockEntity chest(GameTestHelper helper) {
        helper.setBlock(SOURCE.below(), Blocks.STONE);
        helper.setBlock(TARGET.below(), Blocks.STONE);
        helper.setBlock(SOURCE, Blocks.CHEST);
        return helper.getBlockEntity(SOURCE, ChestBlockEntity.class);
    }

    private static void inventory(GameTestHelper helper) {
        ChestBlockEntity chest = chest(helper);
        chest.setItem(0, new ItemStack(Items.CANDLE, 1));
        chest.setItem(1, new ItemStack(Items.CANDLE, 1));
        BlockState required = Blocks.CANDLE.defaultBlockState().setValue(BlockStateProperties.CANDLES, 3);
        var type = ModTargetPointers.BLOCK_ITEM_HANDLER.get();
        var pointer = type.point(helper.getLevel(), helper.absolutePos(SOURCE), Direction.NORTH, required);
        helper.assertTrue(pointer == null && chest.getItem(0).getCount() == 1 && chest.getItem(1).getCount() == 1,
            "不足数量的模拟不能消耗容器物品");
        chest.setItem(1, new ItemStack(Items.CANDLE, 2));
        pointer = type.point(helper.getLevel(), helper.absolutePos(SOURCE), Direction.NORTH, required);
        helper.assertTrue(pointer != null, "应跨槽位找到足量材料");
        helper.assertTrue(pointer.applyToPos(helper.getLevel(), helper.absolutePos(TARGET), required), "容器指针放置失败");
        helper.assertTrue(chest.getItem(0).isEmpty() && chest.getItem(1).isEmpty(), "放置必须消耗三个蜡烛");
        helper.assertTrue(BlockPlacementUtil.applyBlueprintStates(helper.getLevel(), List.of(new BlockPlacementUtil.BlueprintPartSnapshot(
            helper.absolutePos(TARGET), required, Blocks.AIR.defaultBlockState()))), "蓝图状态应用失败");
        helper.assertTrue(helper.getBlockState(TARGET).getValue(BlockStateProperties.CANDLES) == 3, "蓝图必须恢复蜡烛数量");
        helper.succeed();
    }

    private static void returnBucket(GameTestHelper helper) {
        ChestBlockEntity chest = chest(helper);
        chest.setItem(0, new ItemStack(ModItems.OIL_BUCKET.get()));
        BlockState required = ModBlocks.FIRE_CAULDRON.get().fullFilled();
        var pointer = ModTargetPointers.BLOCK_ITEM_HANDLER.get().point(
            helper.getLevel(), helper.absolutePos(SOURCE), Direction.NORTH, required);
        helper.assertTrue(pointer != null && pointer.applyToPos(helper.getLevel(), helper.absolutePos(TARGET), required), "油桶放置火锅失败");
        helper.assertTrue(chest.getItem(0).is(Items.BUCKET) && chest.getItem(0).getCount() == 1, "空桶必须返还容器");
        helper.assertTrue(BlockPlacementUtil.applyBlueprintStates(helper.getLevel(), List.of(new BlockPlacementUtil.BlueprintPartSnapshot(
            helper.absolutePos(TARGET), required, Blocks.AIR.defaultBlockState()))), "油锅状态转换失败");
        helper.assertTrue(helper.getBlockState(TARGET).is(ModBlocks.OIL_CAULDRON.get()), "火锅蓝图最终应放置油锅");
        helper.succeed();
    }

    private static void moveChest(GameTestHelper helper) {
        ChestBlockEntity chest = chest(helper);
        chest.setItem(0, new ItemStack(Items.DIAMOND, 7));
        BlockPointer pointer = new BlockPointer(
            ModTargetPointers.BLOCK.get(), helper.absolutePos(SOURCE), helper.getBlockState(SOURCE), chest);
        var ops = helper.getLevel().registryAccess().createSerializationContext(JsonOps.INSTANCE);
        ITargetPointer restored = ITargetPointer.CODEC.parse(ops, ITargetPointer.CODEC.encodeStart(ops, pointer).getOrThrow()).getOrThrow();
        helper.assertTrue(restored.applyToPos(helper.getLevel(), helper.absolutePos(TARGET)), "序列化后的指针搬运箱子失败");
        helper.assertTrue(helper.getBlockState(SOURCE).isAir(), "搬运后来源应为空");
        ChestBlockEntity moved = helper.getBlockEntity(TARGET, ChestBlockEntity.class);
        helper.assertTrue(moved == chest && moved.getItem(0).getCount() == 7, "搬运必须保留方块实体与物品");
        helper.succeed();
    }

    private static void moveBed(GameTestHelper helper) {
        for (int x = 1; x <= 8; x++) {
            for (int z = 1; z <= 3; z++) helper.setBlock(new BlockPos(x, 1, z), Blocks.STONE);
        }
        BlockState foot = Blocks.RED_BED.defaultBlockState().setValue(BlockStateProperties.HORIZONTAL_FACING, Direction.NORTH)
            .setValue(BlockStateProperties.BED_PART, BedPart.FOOT);
        helper.setBlock(SOURCE, foot);
        helper.setBlock(SOURCE.north(), foot.setValue(BlockStateProperties.BED_PART, BedPart.HEAD));
        BlockPointer pointer = new BlockPointer(ModTargetPointers.BLOCK.get(), helper.absolutePos(SOURCE), foot);
        helper.assertTrue(pointer.isStillValid(helper.getLevel()), "搬运前的床状态无效");
        helper.assertTrue(BlockPlacementUtil.getPresentMultiblockParts(helper.getLevel(), helper.absolutePos(SOURCE), foot).size() == 2,
            "搬运前必须有完整床结构");
        BlockState target = foot.setValue(BlockStateProperties.HORIZONTAL_FACING, Direction.EAST);
        helper.assertTrue(pointer.applyToPos(helper.getLevel(), helper.absolutePos(TARGET), target), "整床旋转搬运失败");
        helper.assertTrue(helper.getBlockState(TARGET).getValue(BlockStateProperties.BED_PART) == BedPart.FOOT, "床尾缺失");
        helper.assertTrue(helper.getBlockState(TARGET.east()).getValue(BlockStateProperties.BED_PART) == BedPart.HEAD, "床头缺失");
        helper.assertTrue(helper.getBlockState(SOURCE).isAir() && helper.getBlockState(SOURCE.north()).isAir(), "旧床部件未清除");
        helper.succeed();
    }

    private static void itemEntity(GameTestHelper helper) {
        var level = helper.getLevel();
        var source = helper.absolutePos(SOURCE).getCenter();
        ItemEntity item = new ItemEntity(level, source.x, source.y, source.z, new ItemStack(Items.STONE, 2), 0, 0, 0);
        item.setNoGravity(true);
        level.addFreshEntity(item);
        helper.runAfterDelay(2, () -> {
            var pointer = ModTargetPointers.ITEM_ENTITY.get().point(level, helper.absolutePos(SOURCE), Direction.NORTH, null);
            helper.assertTrue(pointer != null && pointer.applyToPos(level, helper.absolutePos(TARGET)), "掉落物指针放置失败");
            helper.assertTrue(item.getItem().getCount() == 1, "掉落物应只消耗一个物品");
            helper.assertTrue(helper.getBlockState(TARGET).is(Blocks.STONE), "掉落物未放置到目标");
            helper.succeed();
        });
    }

    private static void neighborUpdate(GameTestHelper helper) {
        helper.setBlock(SOURCE, Blocks.STONE);
        helper.setBlock(SOURCE.above(), Blocks.TORCH);
        BlockPointer pointer = new BlockPointer(ModTargetPointers.BLOCK.get(), helper.absolutePos(SOURCE), helper.getBlockState(SOURCE));
        helper.assertTrue(pointer.applyToPos(helper.getLevel(), helper.absolutePos(TARGET)), "支撑方块搬运失败");
        helper.assertTrue(helper.getBlockState(SOURCE.above()).isAir(), "搬运完成后必须更新相邻方块的支撑状态");
        helper.succeed();
    }

    private static void blockedBed(GameTestHelper helper) {
        for (int x = 1; x <= 8; x++) {
            for (int z = 1; z <= 3; z++) helper.setBlock(new BlockPos(x, 1, z), Blocks.STONE);
        }
        BlockState foot = Blocks.RED_BED.defaultBlockState().setValue(BlockStateProperties.HORIZONTAL_FACING, Direction.NORTH)
            .setValue(BlockStateProperties.BED_PART, BedPart.FOOT);
        helper.setBlock(SOURCE, foot);
        helper.setBlock(SOURCE.north(), foot.setValue(BlockStateProperties.BED_PART, BedPart.HEAD));
        helper.setBlock(TARGET.east(), Blocks.STONE);
        BlockPointer pointer = new BlockPointer(ModTargetPointers.BLOCK.get(), helper.absolutePos(SOURCE), foot);
        helper.assertTrue(!pointer.applyToPos(helper.getLevel(), helper.absolutePos(TARGET),
            foot.setValue(BlockStateProperties.HORIZONTAL_FACING, Direction.EAST)), "目标床头被占据时必须拒绝整组搬运");
        helper.assertTrue(BlockPlacementUtil.getPresentMultiblockParts(helper.getLevel(), helper.absolutePos(SOURCE), foot).size() == 2,
            "拒绝搬运不得拆散来源床");
        helper.assertTrue(helper.getBlockState(TARGET).isAir() && helper.getBlockState(TARGET.east()).is(Blocks.STONE),
            "拒绝搬运不得改动目标");
        helper.succeed();
    }

    private static void fireCauldron(GameTestHelper helper) {
        for (int fill = 1; fill <= 4; fill++) {
            BlockState oil = ModBlocks.OIL_CAULDRON.getDefaultState().setValue(OilCauldronBlock.LEVEL, fill);
            helper.setBlock(SOURCE, oil);
            OilCauldronBlock.ignite(helper.getLevel(), helper.absolutePos(SOURCE), oil);
            BlockState fire = helper.getBlockState(SOURCE);
            helper.assertTrue(fire.is(ModBlocks.FIRE_CAULDRON.get()) && fire.getValue(FireCauldronBlock.LEVEL) == fill,
                "点燃必须转换为火锅并保留油量");
            BlockCache cache = new BlockCache(helper.getLevel());
            helper.assertTrue(ModBlocks.FIRE_CAULDRON.get().getFluidAmount(cache, helper.absolutePos(SOURCE)) == fill * 250,
                "火锅必须按层保留燃料量");
            helper.assertTrue(ModBlocks.FIRE_CAULDRON.get().consumeOnce(cache, helper.absolutePos(SOURCE), false).isPresent(),
                "火锅燃料消耗失败");
            BlockState remaining = cache.getBlockState(helper.absolutePos(SOURCE));
            helper.assertTrue(fill == 1 ? remaining.is(Blocks.CAULDRON) : remaining.getValue(FireCauldronBlock.LEVEL) == fill - 1,
                "火锅必须每次消耗一层并在用尽时返回空锅");
        }
        helper.succeed();
    }
}
