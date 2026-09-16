package dev.dubhe.anvilcraft.porting;

import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.init.block.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.FunctionGameTestInstance;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.TestData;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.BlockHitResult;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;
import net.neoforged.neoforge.registries.RegisterEvent;

@EventBusSubscriber(modid = AnvilCraft.MOD_ID)
public final class UpstreamRedstonePlacementTests {
    @SubscribeEvent
    public static void function(RegisterEvent event) {
        event.register(Registries.TEST_FUNCTION, registry -> registry.register(AnvilCraft.of("port_upstream_redstone_placement"),
            UpstreamRedstonePlacementTests::placement));
    }

    @SubscribeEvent
    public static void register(RegisterGameTestsEvent event) {
        var environment = event.registerEnvironment(AnvilCraft.of("port_upstream_redstone"));
        event.registerTest(AnvilCraft.of("port_upstream_redstone_placement"), new FunctionGameTestInstance(
            ResourceKey.create(Registries.TEST_FUNCTION, AnvilCraft.of("port_upstream_redstone_placement")),
            new TestData<>(environment, AnvilCraft.of("port_empty"), 100, 0, true)));
    }

    private static void placement(GameTestHelper helper) {
        var player = helper.makeMockPlayer(GameType.SURVIVAL);
        BlockPos pos = helper.absolutePos(new BlockPos(2, 2, 2));
        for (Direction facing : Direction.Plane.HORIZONTAL) {
            player.setYRot(facing.toYRot());
            for (boolean sneaking : new boolean[]{false, true}) {
                player.setShiftKeyDown(sneaking);
                for (Block block : new Block[]{ModBlocks.ADVANCED_COMPARATOR.get(), ModBlocks.PULSE_GENERATOR.get(),
                    ModBlocks.ITEM_DETECTOR.get()}) {
                    var context = new BlockPlaceContext(helper.getLevel(), player, InteractionHand.MAIN_HAND, new ItemStack(block),
                        new BlockHitResult(pos.getCenter(), Direction.UP, pos, false));
                    var placed = block.getStateForPlacement(context);
                    Direction expected = block == ModBlocks.ITEM_DETECTOR.get() ? facing : facing.getOpposite();
                    helper.assertTrue(placed != null && placed.getValue(BlockStateProperties.HORIZONTAL_FACING) == expected,
                        "三个红石元件的最新朝向必须在普通和潜行放置时保持一致");
                }
            }
        }
        helper.succeed();
    }
}
