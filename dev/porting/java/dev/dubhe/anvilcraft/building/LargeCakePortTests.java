package dev.dubhe.anvilcraft.building;

import com.mojang.authlib.GameProfile;
import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.block.cake.LargeCakeBlock;
import dev.dubhe.anvilcraft.block.state.Cube3x3PartHalf;
import dev.dubhe.anvilcraft.init.block.ModBlocks;
import dev.dubhe.anvilcraft.item.block.LargeCakeBlockItem;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.FunctionGameTestInstance;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.TestData;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;
import net.neoforged.neoforge.registries.RegisterEvent;

import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;

@EventBusSubscriber(modid = AnvilCraft.MOD_ID)
public final class LargeCakePortTests {
    private static final Map<String, Consumer<GameTestHelper>> TESTS = Map.of(
        "port_large_cake_item", LargeCakePortTests::placement,
        "port_large_cake_reject", LargeCakePortTests::reject,
        "port_large_cake_independent", LargeCakePortTests::independent,
        "port_large_cake_snapshot", LargeCakePortTests::snapshot
    );

    @SubscribeEvent
    public static void functions(RegisterEvent event) {
        event.register(Registries.TEST_FUNCTION, registry -> TESTS.forEach((name, test) -> registry.register(AnvilCraft.of(name), test)));
    }

    @SubscribeEvent
    public static void register(RegisterGameTestsEvent event) {
        var environment = event.registerEnvironment(AnvilCraft.of("port_large_cake"));
        TESTS.forEach((name, test) -> event.registerTest(AnvilCraft.of(name), new FunctionGameTestInstance(
            ResourceKey.create(Registries.TEST_FUNCTION, AnvilCraft.of(name)),
            new TestData<>(environment, AnvilCraft.of("port_logistics_empty"), 100, 0, true)
        )));
    }

    private static ServerPlayer player(GameTestHelper helper) {
        var player = FakePlayerFactory.get(helper.getLevel(), new GameProfile(UUID.randomUUID(), "PortCake"));
        player.setGameMode(GameType.SURVIVAL);
        player.setPos(Vec3.atCenterOf(helper.absolutePos(new BlockPos(0, 15, 0))));
        return player;
    }

    private static BlockPos prepare(GameTestHelper helper) {
        var origin = helper.absolutePos(new BlockPos(4, 15, 4));
        for (var pos : BlockPos.betweenClosed(origin.offset(-1, -1, -1), origin.offset(1, 2, 1))) {
            helper.getLevel().setBlock(pos, pos.getY() < origin.getY() ? Blocks.STONE.defaultBlockState()
                : Blocks.AIR.defaultBlockState(), Block.UPDATE_SKIP_ALL_SIDEEFFECTS);
        }
        return origin;
    }

    private static InteractionResult place(ServerPlayer player, BlockPos origin, ItemStack stack) {
        player.setItemInHand(InteractionHand.MAIN_HAND, stack);
        return stack.getItem().useOn(new UseOnContext(player.level(), player, InteractionHand.MAIN_HAND, stack,
            new BlockHitResult(Vec3.atBottomCenterOf(origin), Direction.UP, origin.below(), false)));
    }

    private static long count(GameTestHelper helper, BlockPos origin) {
        return BlockPos.betweenClosedStream(origin.offset(-1, 0, -1), origin.offset(1, 2, 1))
            .filter(pos -> helper.getLevel().getBlockState(pos).is(ModBlocks.LARGE_CAKE.get())).count();
    }

    private static void placement(GameTestHelper helper) {
        var origin = prepare(helper);
        var stack = new ItemStack(ModBlocks.LARGE_CAKE.get(), 2);
        helper.assertTrue(stack.getItem() instanceof LargeCakeBlockItem, "真实注册物品应使用完整蛋糕放置规则");
        helper.assertTrue(place(player(helper), origin, stack).consumesAction() && stack.getCount() == 1,
            "一次正常使用应消耗一个物品");
        for (var part : Cube3x3PartHalf.values()) {
            var state = helper.getLevel().getBlockState(origin.offset(part.getOffset()));
            helper.assertTrue(state.is(ModBlocks.LARGE_CAKE.get()) && state.getValue(LargeCakeBlock.HALF) == part,
                "每个部件必须位于对应格且保留朝向形状");
        }
        helper.assertTrue(count(helper, origin) == 27, "一次摆出 27 格完整蛋糕");
        helper.succeed();
    }

    private static void reject(GameTestHelper helper) {
        var origin = prepare(helper);
        var player = player(helper);
        var stack = new ItemStack(ModBlocks.LARGE_CAKE.get(), 2);
        var support = origin.offset(1, -1, 1);
        helper.getLevel().setBlockAndUpdate(support, Blocks.AIR.defaultBlockState());
        helper.assertTrue(!place(player, origin, stack).consumesAction() && count(helper, origin) == 0 && stack.getCount() == 2,
            "缺少任一底层支撑时不得放置部分蛋糕或扣料");
        helper.getLevel().setBlockAndUpdate(support, Blocks.STONE.defaultBlockState());
        var obstacle = origin.offset(-1, 2, -1);
        helper.getLevel().setBlockAndUpdate(obstacle, Blocks.GLASS.defaultBlockState());
        helper.assertTrue(!place(player, origin, stack).consumesAction() && count(helper, origin) == 0 && stack.getCount() == 2,
            "顶层阻挡不得覆盖方块或扣料");
        helper.assertTrue(helper.getLevel().getBlockState(obstacle).is(Blocks.GLASS), "保留原有障碍");
        helper.succeed();
    }

    private static void independent(GameTestHelper helper) {
        var origin = prepare(helper);
        var player = player(helper);
        place(player, origin, new ItemStack(ModBlocks.LARGE_CAKE.get()));
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.STICK));
        player.getFoodData().setFoodLevel(1);
        var clicked = origin.above();
        var hit = new BlockHitResult(Vec3.atCenterOf(clicked), Direction.NORTH, clicked, false);
        var result = helper.getLevel().getBlockState(clicked)
            .useItemOn(player.getMainHandItem(), helper.getLevel(), player, InteractionHand.MAIN_HAND, hit);
        helper.assertTrue(result.consumesAction() && player.getFoodData().getFoodLevel() == 16, "持物点击应增加 15 饥饿值");
        helper.assertTrue(helper.getLevel().getBlockState(clicked).isAir()
            && helper.getLevel().getBlockState(clicked.above()).isAir(), "吃掉点击的中层格，上方因失去支撑消失");
        helper.assertTrue(count(helper, origin) == 25 && helper.getLevel().getBlockState(origin).is(ModBlocks.LARGE_CAKE.get()),
            "相邻列和点击格下方仍保留，不能改吃顶层");
        var top = origin.offset(1, 2, 0);
        player.getFoodData().setFoodLevel(20);
        helper.assertTrue(!helper.getLevel().getBlockState(top).useWithoutItem(helper.getLevel(), player,
            new BlockHitResult(Vec3.atCenterOf(top), Direction.UP, top, false)).consumesAction(), "饱食时不吃蛋糕");
        helper.getLevel().removeBlock(origin.offset(1, -1, 1), false);
        helper.assertTrue(count(helper, origin) == 22, "失去地基仅移除所在三格列");
        helper.succeed();
    }

    private static void snapshot(GameTestHelper helper) {
        var origin = prepare(helper);
        var state = ModBlocks.LARGE_CAKE.getDefaultState();
        helper.getLevel().setBlockAndUpdate(origin, state);
        helper.assertTrue(count(helper, origin) == 1, "直接恢复主格不能自动生成整座蛋糕");
        var side = origin.offset(1, 0, 0);
        helper.getLevel().setBlockAndUpdate(side, state.setValue(LargeCakeBlock.HALF, Cube3x3PartHalf.BOTTOM_E));
        var area = new AABB(Vec3.atLowerCornerOf(origin.offset(-1, 0, -1)), Vec3.atLowerCornerOf(origin.offset(2, 3, 2)));
        var result = BlueprintCapture.capture(helper.getLevel(), area);
        var cakes = result.snapshot().blocks().stream()
            .filter(entry -> result.snapshot().stateOf(entry).is(ModBlocks.LARGE_CAKE.get())).toList();
        helper.assertTrue(cakes.size() == 2 && result.added() == 0, "捕获残缺蛋糕保留两格，不再自动补齐 27 格");
        for (var entry : cakes) {
            helper.assertTrue(BlueprintMultiblocks.core(entry.pos(), result.snapshot().stateOf(entry)).equals(entry.pos()),
                "蓝图里的蛋糕各格独立，不再归并为主格");
        }
        helper.succeed();
    }
}
