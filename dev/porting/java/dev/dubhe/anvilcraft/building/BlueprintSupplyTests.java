package dev.dubhe.anvilcraft.building;

import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.block.cauldron.Layered4LevelCauldronBlock;
import dev.dubhe.anvilcraft.block.entity.FluidTankBlockEntity;
import dev.dubhe.anvilcraft.init.block.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.FunctionGameTestInstance;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.TestData;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.EndPortalFrameBlock;
import net.minecraft.world.level.block.LayeredCauldronBlock;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.NetherPortalBlock;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.piston.PistonMovingBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.registries.RegisterEvent;
import net.neoforged.neoforge.transfer.access.ItemAccess;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.item.ItemStacksResourceHandler;
import net.neoforged.neoforge.transfer.transaction.Transaction;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

@EventBusSubscriber(modid = AnvilCraft.MOD_ID)
public final class BlueprintSupplyTests {
    private static final Map<String, Consumer<GameTestHelper>> TESTS = Map.of(
        "port_blueprint_special_materials", BlueprintSupplyTests::materials,
        "port_blueprint_ignition_regions", BlueprintSupplyTests::ignition,
        "port_blueprint_block_entity_creation", BlueprintSupplyTests::blockEntities,
        "port_blueprint_fluid_mapping", BlueprintSupplyTests::fluids,
        "port_blueprint_tank_supply", BlueprintSupplyTests::tanks,
        "port_blueprint_exact_fluid_transaction", BlueprintSupplyTests::transactions
    );

    @SubscribeEvent
    public static void functions(RegisterEvent event) {
        event.register(Registries.TEST_FUNCTION, registry -> TESTS.forEach((name, test) -> registry.register(AnvilCraft.of(name), test)));
    }

    @SubscribeEvent
    public static void register(RegisterGameTestsEvent event) {
        var environment = event.registerEnvironment(AnvilCraft.of("port_blueprint_supply"));
        TESTS.forEach((name, test) -> event.registerTest(AnvilCraft.of(name), new FunctionGameTestInstance(
            ResourceKey.create(Registries.TEST_FUNCTION, AnvilCraft.of(name)),
            new TestData<>(environment, AnvilCraft.of("port_logistics_empty"), 100, 0, true)
        )));
    }

    private static void materials(GameTestHelper helper) {
        Map<Block, Item> base = Map.ofEntries(
            Map.entry(Blocks.POTTED_DANDELION, Items.FLOWER_POT),
            Map.entry(Blocks.RED_CANDLE_CAKE, Items.CAKE),
            Map.entry(Blocks.ATTACHED_PUMPKIN_STEM, Items.PUMPKIN_SEEDS),
            Map.entry(Blocks.ATTACHED_MELON_STEM, Items.MELON_SEEDS),
            Map.entry(Blocks.TALL_SEAGRASS, Items.SEAGRASS),
            Map.entry(Blocks.KELP_PLANT, Items.KELP),
            Map.entry(Blocks.BAMBOO_SAPLING, Items.BAMBOO),
            Map.entry(Blocks.WEEPING_VINES_PLANT, Items.WEEPING_VINES),
            Map.entry(Blocks.TWISTING_VINES_PLANT, Items.TWISTING_VINES),
            Map.entry(Blocks.CAVE_VINES_PLANT, Items.GLOW_BERRIES),
            Map.entry(Blocks.FROSTED_ICE, Items.ICE),
            Map.entry(ModBlocks.SIMPLE_MAGNETIC_CHUTE.get(), ModBlocks.MAGNETIC_CHUTE.asItem())
        );
        base.forEach((block, item) -> helper.assertTrue(BlueprintSpecialBlocks.material(block.defaultBlockState()).is(item),
            "特殊方块必须按可供料的基础物品计数"));
        Map<BlockState, Item> extras = Map.of(
            Blocks.POTTED_DANDELION.defaultBlockState(), Items.DANDELION,
            Blocks.RED_CANDLE_CAKE.defaultBlockState(), Items.RED_CANDLE,
            Blocks.TALL_SEAGRASS.defaultBlockState(), Items.BONE_MEAL,
            Blocks.END_PORTAL_FRAME.defaultBlockState().setValue(EndPortalFrameBlock.HAS_EYE, true), Items.ENDER_EYE
        );
        extras.forEach((state, item) -> {
            var extra = BlueprintSpecialBlocks.extra(state);
            helper.assertTrue(extra.size() == 1 && extra.getFirst().is(item) && extra.getFirst().getCount() == 1,
                "装饰或升级材料不能因使用基础物品而丢失");
        });
        helper.assertTrue(BlueprintSpecialBlocks.material(Blocks.STONE.defaultBlockState()).isEmpty()
            && BlueprintSpecialBlocks.extra(Blocks.FLOWER_POT.defaultBlockState()).isEmpty(), "普通方块与空花盆不能凭空增加材料");
        helper.succeed();
    }

    private static void ignition(GameTestHelper helper) {
        var blocks = new LinkedHashMap<BlockPos, BlockState>();
        BlockState x = Blocks.NETHER_PORTAL.defaultBlockState().setValue(NetherPortalBlock.AXIS, Direction.Axis.X);
        BlockState z = x.setValue(NetherPortalBlock.AXIS, Direction.Axis.Z);
        blocks.put(BlockPos.ZERO, x);
        blocks.put(new BlockPos(1, 0, 0), x);
        blocks.put(new BlockPos(1, 1, 0), x);
        blocks.put(new BlockPos(1, 1, 1), x);
        blocks.put(new BlockPos(2, 1, 0), z);
        blocks.put(new BlockPos(2, 2, 0), z);
        blocks.put(new BlockPos(2, 2, 1), z);
        var cores = BlueprintIgnition.portalCores(blocks);
        helper.assertTrue(cores.size() == 7 && cores.values().stream().distinct().count() == 3
            && cores.get(new BlockPos(1, 1, 0)).equals(BlockPos.ZERO)
            && cores.get(new BlockPos(2, 2, 1)).equals(new BlockPos(2, 1, 0)),
            "传送门只沿同轴平面归并，垂直相连、相邻不同轴和不同平面应正确区分");
        helper.assertTrue(BlueprintIgnition.isIgnition(x) && BlueprintIgnition.isIgnition(Blocks.SOUL_FIRE.defaultBlockState())
            && !BlueprintIgnition.isIgnition(Blocks.CAMPFIRE.defaultBlockState()), "营火不能错误归并为火焰点火操作");
        helper.succeed();
    }

    private static void blockEntities(GameTestHelper helper) {
        var original = new ChestBlockEntity(BlockPos.ZERO, Blocks.CHEST.defaultBlockState());
        original.setItem(7, new ItemStack(Items.EMERALD, 4));
        var data = original.saveWithFullMetadata(helper.getLevel().registryAccess());
        var pos = helper.absolutePos(new BlockPos(2, 15, 4));
        var loaded = BlueprintBlockEntities.create(helper.getLevel(), pos, Blocks.CHEST.defaultBlockState(), data);
        helper.assertTrue(loaded instanceof ChestBlockEntity chest && chest.getItem(7).is(Items.EMERALD)
            && chest.getItem(7).getCount() == 4 && loaded.getBlockPos().equals(pos) && loaded.getLevel() == helper.getLevel(),
            "蓝图方块实体应加载真实数据并关联当前世界与目标位置");
        helper.assertTrue(BlueprintBlockEntities.create(helper.getLevel(), pos, Blocks.MOVING_PISTON.defaultBlockState(), null)
            instanceof PistonMovingBlockEntity, "移动活塞应使用其专用方块实体");
        helper.assertTrue(BlueprintBlockEntities.create(helper.getLevel(), pos, Blocks.STONE.defaultBlockState(), null) == null,
            "普通方块不能生成方块实体");
        helper.succeed();
    }

    private static void fluids(GameTestHelper helper) {
        helper.assertTrue(FluidBuildAdapter.liquidOf(Blocks.WATER.defaultBlockState()).getAmount() == 1000
            && FluidBuildAdapter.liquidOf(Blocks.WATER.defaultBlockState().setValue(LiquidBlock.LEVEL, 3)).isEmpty(),
            "只有源液体可以计为一桶材料");
        for (int layer = 1; layer <= 4; layer++) {
            var state = ModBlocks.HONEY_CAULDRON.getDefaultState().setValue(Layered4LevelCauldronBlock.LEVEL, layer);
            helper.assertTrue(FluidBuildAdapter.cauldronFluidOf(state).getAmount() == layer * 250,
                "模组四层锅必须保留每层 250 mB");
        }
        int[] expected = {0, 333, 666, 1000};
        for (int layer = 1; layer <= 3; layer++) {
            var state = Blocks.WATER_CAULDRON.defaultBlockState().setValue(LayeredCauldronBlock.LEVEL, layer);
            helper.assertTrue(FluidBuildAdapter.isFilledCauldron(state)
                && FluidBuildAdapter.cauldronFluidOf(state).getAmount() == expected[layer]
                && FluidBuildAdapter.cauldronItem(state).is(Items.CAULDRON), "分层炼药锅必须按源版精确 mB 计料");
        }
        helper.assertTrue(!FluidBuildAdapter.isFilledCauldron(Blocks.CAULDRON.defaultBlockState())
            && FluidBuildAdapter.bucketOf(new FluidStack(Fluids.WATER, 1000)).is(Items.WATER_BUCKET)
            && FluidBuildAdapter.bucketOf(new FluidStack(Fluids.WATER, 333)).isEmpty(), "非整桶流体不能映射为完整桶");
        helper.succeed();
    }

    private static void tanks(GameTestHelper helper) {
        final var level = helper.getLevel();
        var state = ModBlocks.FLUID_TANK.getDefaultState();
        var tank = (FluidTankBlockEntity) ModBlocks.FLUID_TANK.get().newBlockEntity(BlockPos.ZERO, state);
        var handler = tank.getFluidHandler();
        try (Transaction transaction = Transaction.openRoot()) {
            handler.insert(FluidResource.of(Fluids.WATER), 1234, transaction);
            transaction.commit();
        }
        var data = tank.saveWithFullMetadata(level.registryAccess());
        var before = data.copy();
        var extracted = FluidBuildAdapter.extractTanks(state, data, level.registryAccess());
        helper.assertTrue(!extracted.unmapped() && extracted.tanks().size() == 1
            && extracted.tanks().getFirst().fluid().getAmount() == 1234 && data.equals(before)
            && handler.getAmountAsLong(0) == 1234, "储罐规划必须保留精确数量且不改变原始数据或实际储罐");
        var target = (FluidTankBlockEntity) ModBlocks.FLUID_TANK.get().newBlockEntity(BlockPos.ZERO, state);
        FluidBuildAdapter.insert(target, extracted.tanks());
        helper.assertTrue(target.getFluidHandler().getAmountAsLong(0) == 1234, "储罐内容必须按供料恢复");
        boolean rejected = false;
        try {
            FluidBuildAdapter.insert(target, List.of(
                new FluidBuildAdapter.TankFluid(0, new FluidStack(Fluids.WATER, 100)),
                new FluidBuildAdapter.TankFluid(0, new FluidStack(Fluids.LAVA, 100))
            ));
        } catch (IllegalArgumentException expected) {
            rejected = true;
        }
        helper.assertTrue(rejected && target.getFluidHandler().getAmountAsLong(0) == 1234,
            "后续流体无法插入时必须回滚之前插入的全部数量");
        helper.succeed();
    }

    private static void transactions(GameTestHelper helper) {
        var bucket = new ItemStack(Items.WATER_BUCKET);
        var water = new FluidStack(Fluids.WATER, 1000);
        helper.assertTrue(FluidBuildAdapter.canProvideExactFluid(bucket, water) && bucket.is(Items.WATER_BUCKET)
            && !FluidBuildAdapter.canProvideExactFluid(bucket, new FluidStack(Fluids.WATER, 333)),
            "预检应接受整桶而拒绝不可拆分的部分桶，且不得改变原物品");
        var items = new ItemStacksResourceHandler(1);
        items.set(0, ItemResource.of(bucket), 1);
        var access = ItemAccess.forHandlerIndexStrict(items, 0);
        helper.assertTrue(!FluidBuildAdapter.takeExactFluid(access, new FluidStack(Fluids.WATER, 1001))
            && items.getResource(0).is(Items.WATER_BUCKET), "不足量事务必须完整回滚满桶替换");
        helper.assertTrue(FluidBuildAdapter.takeExactFluid(access, water) && items.getResource(0).is(Items.BUCKET)
            && items.getAmountAsLong(0) == 1, "精确扣除应通过原生物品访问器返回空桶");
        helper.succeed();
    }
}
