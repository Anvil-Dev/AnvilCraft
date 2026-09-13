package dev.dubhe.anvilcraft.porting;

import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.block.cfa.CelestialForgingAnvilAmplifierBlock;
import dev.dubhe.anvilcraft.block.cfa.CelestialForgingAnvilBlock;
import dev.dubhe.anvilcraft.block.state.Cube323PartHalf;
import dev.dubhe.anvilcraft.init.block.ModBlockTags;
import dev.dubhe.anvilcraft.init.block.ModBlocks;
import dev.dubhe.anvilcraft.item.block.ChuteBlockItem;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.FunctionGameTestInstance;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.TestData;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;
import net.neoforged.neoforge.registries.RegisterEvent;

import java.util.Map;
import java.util.function.Consumer;

@EventBusSubscriber(modid = AnvilCraft.MOD_ID)
public final class PlacementPreviewPortGameTests {
    private static final Map<String, Consumer<GameTestHelper>> TESTS = Map.of(
        "port_preview_amplifier", PlacementPreviewPortGameTests::amplifier,
        "port_preview_chute", PlacementPreviewPortGameTests::chute,
        "port_preview_tag", PlacementPreviewPortGameTests::tag
    );

    @SubscribeEvent
    public static void functions(RegisterEvent event) {
        event.register(Registries.TEST_FUNCTION, registry -> TESTS.forEach((name, test) -> registry.register(AnvilCraft.of(name), test)));
    }

    @SubscribeEvent
    public static void register(RegisterGameTestsEvent event) {
        var environment = event.registerEnvironment(AnvilCraft.of("port_preview"));
        TESTS.forEach((name, test) -> event.registerTest(AnvilCraft.of(name), new FunctionGameTestInstance(
            ResourceKey.create(Registries.TEST_FUNCTION, AnvilCraft.of(name)),
            new TestData<>(environment, AnvilCraft.of("port_empty"), 100, 0, true)
        )));
    }

    private static void amplifier(GameTestHelper helper) {
        BlockPos center = helper.absolutePos(new BlockPos(4, 1, 4));
        CelestialForgingAnvilBlock anvil = ModBlocks.CELESTIAL_FORGING_ANVIL.get();
        var state = anvil.defaultBlockState().setValue(CelestialForgingAnvilBlock.HALF, Cube323PartHalf.BOTTOM_CENTER);
        for (var part : anvil.getParts()) {
            helper.getLevel().setBlock(center.offset(anvil.offsetFrom(state, part)), anvil.placedState(part, state),
                Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE);
        }
        CelestialForgingAnvilAmplifierBlock amplifier = ModBlocks.CELESTIAL_FORGING_ANVIL_AMPLIFIER.get();
        var player = helper.makeMockPlayer(GameType.SURVIVAL);
        ItemStack stack = new ItemStack(amplifier.asItem(), 2);
        player.setItemInHand(InteractionHand.MAIN_HAND, stack);
        Direction[] facings = {Direction.NORTH, Direction.EAST, Direction.WEST, Direction.SOUTH};
        int index = 0;
        for (int z : new int[]{-2, 3}) {
            for (int x : new int[]{-2, 3}) {
                BlockPos main = center.offset(x, 0, z);
                for (int dx = -1; dx <= 0; dx++) {
                    for (int dz = -1; dz <= 0; dz++) {
                        BlockPos clicked = main.offset(dx, 0, dz);
                        helper.assertTrue(main.equals(amplifier.snapMainPos(helper.getLevel(), clicked)), "占地四格必须吸附到同一主块");
                    }
                }
                var context = new BlockPlaceContext(helper.getLevel(), player, InteractionHand.MAIN_HAND, stack,
                    new BlockHitResult(main.getCenter(), Direction.UP, main, false));
                var placed = amplifier.getStateForPlacement(context);
                helper.assertTrue(placed != null && placed.getValue(CelestialForgingAnvilAmplifierBlock.FACING) == facings[index++],
                    "四角必须使用对应朝向");
            }
        }
        BlockPos main = center.offset(-2, 0, -2);
        BlockPos clicked = main.offset(-1, 0, -1);
        var result = stack.useOn(new UseOnContext(helper.getLevel(), player, InteractionHand.MAIN_HAND, stack,
            new BlockHitResult(clicked.getCenter(), Direction.UP, clicked, false)));
        helper.assertTrue(result.consumesAction() && stack.getCount() == 1, "吸附后的实际放置必须成功且只消耗一个物品");
        var placed = helper.getLevel().getBlockState(main);
        for (var part : amplifier.getParts()) {
            helper.assertTrue(helper.getLevel().getBlockState(main.offset(amplifier.offsetFrom(placed, part)))
                == amplifier.placedState(part, placed), "吸附后全部部件必须完整");
        }
        helper.assertTrue(amplifier.snapMainPos(helper.getLevel(), clicked) == null, "占用后不能继续显示合法吸附位置");
        helper.succeed();
    }

    private static void chute(GameTestHelper helper) {
        BlockPos relative = new BlockPos(2, 2, 2);
        helper.setBlock(relative, ModBlocks.CREATIVE_CRATE.get());
        BlockPos pos = helper.absolutePos(relative);
        var player = helper.makeMockPlayer(GameType.SURVIVAL);
        var stack = new ItemStack(ModBlocks.CHUTE.asItem());
        player.setItemInHand(InteractionHand.MAIN_HAND, stack);
        for (Direction face : Direction.values()) {
            for (InteractionHand hand : InteractionHand.values()) {
                Vec3 hit = pos.getCenter().add(Vec3.atLowerCornerOf(face.getUnitVec3i()).scale(0.5));
                UseOnContext context = new UseOnContext(helper.getLevel(), player, hand, stack,
                    new BlockHitResult(hit, face, pos, false));
                helper.assertTrue(ChuteBlockItem.isStorageInteraction(context), "创造容器所有面和手别的中央都应保留交互");
                helper.assertTrue(stack.getItem().onItemUseFirst(stack, context) == InteractionResult.PASS,
                    "中央必须将操作交回容器");
                player.setShiftKeyDown(true);
                helper.assertTrue(!ChuteBlockItem.isStorageInteraction(context), "潜行应允许从中央放置");
                player.setShiftKeyDown(false);
            }
        }
        for (double coordinate : new double[]{3.0 / 16, 13.0 / 16}) {
            UseOnContext edge = new UseOnContext(helper.getLevel(), player, InteractionHand.MAIN_HAND, stack,
                new BlockHitResult(Vec3.atLowerCornerOf(pos).add(coordinate, 1, 0.5), Direction.UP, pos, false));
            helper.assertTrue(!ChuteBlockItem.isStorageInteraction(edge), "三个像素的边界应属于放置区域");
        }
        helper.succeed();
    }

    private static void tag(GameTestHelper helper) {
        for (Block block : new Block[]{ModBlocks.SMART_BLOCK_PLACER.get(), ModBlocks.PUMP.get(), ModBlocks.CHUTE.get(),
            ModBlocks.CRAB_TRAP.get(), ModBlocks.ADVANCED_COMPARATOR.get()}) {
            helper.assertTrue(block.defaultBlockState().is(ModBlockTags.PLACEMENT_PREVIEW), "单方块预览标签未加载");
        }
        helper.succeed();
    }
}
