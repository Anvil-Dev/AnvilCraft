package dev.dubhe.anvilcraft.porting;

import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.block.entity.BaseChuteBlockEntity;
import dev.dubhe.anvilcraft.block.entity.OverflowChuteBlockEntity;
import dev.dubhe.anvilcraft.block.logistics.chute.ChuteBlock;
import dev.dubhe.anvilcraft.block.logistics.chute.OverflowChuteBlock;
import dev.dubhe.anvilcraft.init.block.ModBlocks;
import dev.dubhe.anvilcraft.init.item.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.FunctionGameTestInstance;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.TestData;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BarrelBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;
import net.neoforged.neoforge.registries.RegisterEvent;
import net.neoforged.neoforge.transfer.item.ItemResource;

import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

@EventBusSubscriber(modid = AnvilCraft.MOD_ID)
public final class OverflowChutePortGameTests {
    private static final BlockPos POS = new BlockPos(5, 3, 3);
    private static final Map<String, Consumer<GameTestHelper>> TESTS = Map.of(
        "port_overflow_priority", OverflowChutePortGameTests::priority,
        "port_overflow_redstone", OverflowChutePortGameTests::redstone,
        "port_overflow_ports", OverflowChutePortGameTests::ports,
        "port_overflow_hammer", OverflowChutePortGameTests::hammer,
        "port_overflow_drop", OverflowChutePortGameTests::drop,
        "port_overflow_cooldown", OverflowChutePortGameTests::cooldown,
        "port_overflow_base", OverflowChutePortGameTests::base,
        "port_overflow_velocity", OverflowChutePortGameTests::velocity,
        "port_overflow_persistence", OverflowChutePortGameTests::persistence
    );

    @SubscribeEvent
    public static void functions(RegisterEvent event) {
        event.register(Registries.TEST_FUNCTION, registry -> TESTS.forEach((name, test) -> registry.register(AnvilCraft.of(name), test)));
    }

    @SubscribeEvent
    public static void register(RegisterGameTestsEvent event) {
        var environment = event.registerEnvironment(AnvilCraft.of("port_overflow"));
        TESTS.forEach((name, test) -> event.registerTest(AnvilCraft.of(name), new FunctionGameTestInstance(
            ResourceKey.create(Registries.TEST_FUNCTION, AnvilCraft.of(name)),
            new TestData<>(environment, AnvilCraft.of("port_logistics_empty"), 100, 0, true)
        )));
    }

    private static OverflowChuteBlockEntity setup(GameTestHelper helper, int count) {
        helper.setBlock(POS, ModBlocks.OVERFLOW_CHUTE.getDefaultState().setValue(OverflowChuteBlock.FACING, Direction.EAST)
            .setValue(OverflowChuteBlock.OVERFLOW_NORTH, true).setValue(OverflowChuteBlock.OVERFLOW_SOUTH, true));
        var be = helper.getBlockEntity(POS, OverflowChuteBlockEntity.class);
        be.getItemHandler().set(0, ItemResource.of(Items.IRON_INGOT), count);
        return be;
    }

    private static BarrelBlockEntity barrel(GameTestHelper helper, BlockPos pos) {
        helper.setBlock(pos, Blocks.BARREL);
        return helper.getBlockEntity(pos, BarrelBlockEntity.class);
    }

    private static int count(BarrelBlockEntity barrel) {
        int total = 0;
        for (int slot = 0; slot < barrel.getContainerSize(); slot++) total += barrel.getItem(slot).getCount();
        return total;
    }

    private static void priority(GameTestHelper helper) {
        var be = setup(helper, 14);
        final var main = barrel(helper, POS.east());
        final var north = barrel(helper, POS.north());
        final var south = barrel(helper, POS.south());
        be.tick();
        helper.assertTrue(count(main) == 14 && count(north) == 0 && count(south) == 0, "主出口有接收能力时不能同时溢出");
        for (int slot = 0; slot < main.getContainerSize(); slot++) main.setItem(slot, new ItemStack(Items.IRON_INGOT, 64));
        main.setItem(0, new ItemStack(Items.IRON_INGOT, 63));
        be.getItemHandler().set(0, ItemResource.of(Items.IRON_INGOT), 14);
        be.setCooldown(0);
        be.tick();
        helper.assertTrue(be.getItemHandler().getAmountAsInt(0) == 13 && count(north) == 0 && count(south) == 0,
            "主出口部分接收时本轮也不能溢出");
        be.setCooldown(0);
        be.tick();
        helper.assertTrue(count(north) == 6 && count(south) == 7 && be.isEmpty(), "出口堵塞后应尽量均分清空");
        helper.succeed();
    }

    private static void redstone(GameTestHelper helper) {
        var be = setup(helper, 0);
        final var main = barrel(helper, POS.east());
        final var north = barrel(helper, POS.north());
        final var south = barrel(helper, POS.south());
        barrel(helper, POS.west()).setItem(0, new ItemStack(Items.IRON_INGOT, 10));
        helper.setBlock(POS.above(), Blocks.REDSTONE_BLOCK);
        helper.assertTrue(!be.getBlockState().getValue(OverflowChuteBlock.ENABLED), "红石应关闭主出口");
        be.tick();
        helper.assertTrue(be.getItemHandler().getAmountAsInt(0) == 10, "红石不能阻止输入");
        be.setCooldown(0);
        be.tick();
        helper.assertTrue(count(main) == 0 && count(north) == 5 && count(south) == 5, "红石不能关闭溢流口");
        helper.succeed();
    }

    private static void ports(GameTestHelper helper) {
        for (Direction facing : Direction.values()) {
            for (int mask = 0; mask < 64; mask++) {
                BlockState state = ModBlocks.OVERFLOW_CHUTE.getDefaultState().setValue(OverflowChuteBlock.FACING, facing);
                for (Direction port : Direction.values()) {
                    state = state.setValue(OverflowChuteBlock.overflowProperty(port), (mask & (1 << port.ordinal())) != 0);
                }
                BlockState sanitized = OverflowChuteBlock.sanitizeOverflowPorts(state);
                for (Direction port : Direction.values()) {
                    boolean expected = port != facing && port != facing.getOpposite() && (mask & (1 << port.ordinal())) != 0;
                    helper.assertTrue(sanitized.getValue(OverflowChuteBlock.overflowProperty(port)) == expected,
                        "只应清除出入口方向的非法溢流口");
                }
            }
        }
        var be = setup(helper, 0);
        helper.setBlock(POS, be.getBlockState().setValue(OverflowChuteBlock.OVERFLOW_EAST, true));
        be.tick();
        helper.assertTrue(!be.getBlockState().getValue(OverflowChuteBlock.OVERFLOW_EAST), "运行时必须清理非法状态");
        helper.assertTrue(be.getItemHandler().size() == 1, "溢流溜槽只能有一个槽位");
        helper.succeed();
    }

    private static void hammer(GameTestHelper helper) {
        var be = setup(helper, 0);
        var player = helper.makeMockPlayer(GameType.SURVIVAL);
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModItems.ANVIL_HAMMER.get()));
        BlockPos pos = helper.absolutePos(POS);
        var block = ModBlocks.OVERFLOW_CHUTE.get();
        for (Direction face : new Direction[]{Direction.EAST, Direction.WEST}) {
            block.use(be.getBlockState(), helper.getLevel(), pos, player, InteractionHand.MAIN_HAND,
                new BlockHitResult(pos.getCenter(), face, pos, false));
            helper.assertTrue(!be.getBlockState().getValue(OverflowChuteBlock.overflowProperty(face)), "不能用锤开启出入口面");
        }
        block.use(be.getBlockState(), helper.getLevel(), pos, player, InteractionHand.MAIN_HAND,
            new BlockHitResult(pos.getCenter(), Direction.NORTH, pos, false));
        helper.assertTrue(!be.getBlockState().getValue(OverflowChuteBlock.OVERFLOW_NORTH), "单击锤应关闭已开启的侧口");
        helper.setBlock(POS.north(), ModBlocks.MAGNETIC_CHUTE.getDefaultState().setValue(OverflowChuteBlock.FACING, Direction.SOUTH));
        block.use(be.getBlockState(), helper.getLevel(), pos, player, InteractionHand.MAIN_HAND,
            new BlockHitResult(pos.getCenter(), Direction.NORTH, pos, false));
        helper.assertTrue(!be.getBlockState().getValue(OverflowChuteBlock.OVERFLOW_NORTH), "嘴对嘴时不得开启侧口");
        helper.setBlock(POS.north(), Blocks.AIR);
        block.use(be.getBlockState(), helper.getLevel(), pos, player, InteractionHand.MAIN_HAND,
            new BlockHitResult(pos.getCenter(), Direction.NORTH, pos, false));
        helper.assertTrue(ChuteBlock.outputsToward(be.getBlockState(), Direction.NORTH), "开启的侧口必须参与溜槽方向判定");
        helper.succeed();
    }

    private static List<ItemEntity> drops(GameTestHelper helper, BlockPos pos) {
        return helper.getLevel().getEntitiesOfClass(ItemEntity.class, new AABB(helper.absolutePos(pos)), item -> true);
    }

    private static void drop(GameTestHelper helper) {
        var be = setup(helper, 10);
        helper.setBlock(POS.east(), Blocks.STONE);
        helper.setBlock(POS.north(), Blocks.STONE);
        be.tick();
        var items = drops(helper, POS.south());
        helper.assertTrue(items.size() == 1 && items.getFirst().getItem().getCount() == 10 && be.isEmpty(),
            "某口受阻时后面的口应接收剩余份额");
        helper.assertTrue(items.getFirst().getDeltaMovement().lengthSqr() == 0, "溢流物品必须没有初速度");
        be.getItemHandler().set(0, ItemResource.of(Items.IRON_INGOT), 5);
        var south = barrel(helper, POS.south());
        for (int slot = 0; slot < south.getContainerSize(); slot++) south.setItem(slot, new ItemStack(Items.IRON_INGOT, 64));
        be.setCooldown(0);
        be.tick();
        helper.assertTrue(be.getItemHandler().getAmountAsInt(0) == 5, "有容器但拒收时不能改为抛出物品");
        helper.succeed();
    }

    private static void cooldown(GameTestHelper helper) {
        var be = setup(helper, 0);
        helper.setBlock(POS, be.getBlockState().setValue(OverflowChuteBlock.OVERFLOW_SOUTH, false));
        helper.setBlock(POS.east(), Blocks.STONE);
        helper.setBlock(POS.south(), Blocks.STONE);
        helper.setBlock(POS.north(), ModBlocks.OVERFLOW_CHUTE.getDefaultState().setValue(OverflowChuteBlock.FACING, Direction.NORTH));
        var target = helper.getBlockEntity(POS.north(), OverflowChuteBlockEntity.class);
        target.tick();
        be.getItemHandler().set(0, ItemResource.of(Items.IRON_INGOT), 8);
        be.tick();
        helper.assertTrue(target.getItemHandler().getAmountAsInt(0) == 8
            && target.getCooldown() == AnvilCraft.CONFIG.chuteMaxCooldown - 1, "送入本刻已更新的空溜槽时应补偿一刻冷却");
        helper.succeed();
    }

    private static void base(GameTestHelper helper) {
        helper.setBlock(POS, ModBlocks.CHUTE.getDefaultState());
        var be = (BaseChuteBlockEntity) helper.getLevel().getBlockEntity(helper.absolutePos(POS));
        var input = barrel(helper, POS.above());
        input.setItem(0, new ItemStack(Items.IRON_INGOT, 12));
        helper.setBlock(POS.east(), Blocks.REDSTONE_BLOCK);
        be.tick();
        helper.assertTrue(be.isEmpty() && count(input) == 12 && be.getItemHandler().size() == 9,
            "普通溜槽仍须保持九槽且红石关闭输入");
        helper.setBlock(POS.east(), Blocks.AIR);
        be.tick();
        helper.assertTrue(be.getItemHandler().getAmountAsInt(0) == 12, "普通溜槽取消红石后应恢复输入");
        helper.succeed();
    }

    private static void velocity(GameTestHelper helper) {
        for (Direction direction : Direction.values()) {
            helper.setBlock(POS, ModBlocks.OVERFLOW_CHUTE.getDefaultState().setValue(OverflowChuteBlock.FACING, direction));
            var be = helper.getBlockEntity(POS, OverflowChuteBlockEntity.class);
            be.getItemHandler().set(0, ItemResource.of(Items.IRON_INGOT), 1);
            be.setCooldown(0);
            be.tick();
            var items = drops(helper, POS.relative(direction));
            helper.assertTrue(items.size() == 1 && items.getFirst().getDeltaMovement().equals(
                new net.minecraft.world.phys.Vec3(direction.getStepX() * 0.25, direction.getStepY() * 0.25, direction.getStepZ() * 0.25)),
                "主出口必须在六个方向都保持磁性溜槽的初速度");
            items.getFirst().discard();
        }
        helper.succeed();
    }

    private static void persistence(GameTestHelper helper) {
        var be = setup(helper, 13);
        be.setCooldown(5);
        be.getItemHandler().setFilterEnabled(true);
        be.getItemHandler().setSlotLimit(0, 16);
        var tag = be.saveCustomOnly(helper.getLevel().registryAccess());
        be.getItemHandler().set(0, ItemResource.EMPTY, 0);
        be.setCooldown(0);
        be.loadCustomOnly(TagValueInput.create(ProblemReporter.DISCARDING, helper.getLevel().registryAccess(), tag));
        helper.assertTrue(be.getItemHandler().size() == 1 && be.getItemHandler().getAmountAsInt(0) == 13 && be.getCooldown() == 5,
            "保存重载必须保持单槽库存和冷却");
        helper.assertTrue(be.getItemHandler().isFilterEnabled() && be.getItemHandler().getFilter(0).is(Items.IRON_INGOT)
            && be.getItemHandler().getSlotLimit(0) == 16, "保存重载必须保持公共过滤数据");
        helper.getLevel().destroyBlock(helper.absolutePos(POS), false);
        helper.assertTrue(drops(helper, POS).stream().mapToInt(item -> item.getItem().getCount()).sum() == 13 && be.isEmpty(),
            "破坏应掉落并清空唯一槽位");
        helper.succeed();
    }
}
