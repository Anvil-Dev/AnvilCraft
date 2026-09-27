package dev.dubhe.anvilcraft.porting;

import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.block.entity.fluid.GlassPipeBlockEntity;
import dev.dubhe.anvilcraft.block.fluid.PipeBlock;
import dev.dubhe.anvilcraft.block.fluid.PipeCornerBlock;
import dev.dubhe.anvilcraft.block.fluid.PipeStraightBlock;
import dev.dubhe.anvilcraft.init.block.ModBlockEntities;
import dev.dubhe.anvilcraft.init.block.ModBlocks;
import dev.dubhe.anvilcraft.init.item.ModItems;
import dev.dubhe.anvilcraft.util.BlockPlacementUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.FunctionGameTestInstance;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.TestData;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.phys.BlockHitResult;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.registries.RegisterEvent;

import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

@EventBusSubscriber(modid = AnvilCraft.MOD_ID)
public final class GlassPipeStateTests {
    private static final Map<String, Consumer<GameTestHelper>> TESTS = Map.of(
        "port_glass_pipe_conversion", GlassPipeStateTests::conversion,
        "port_glass_pipe_display", GlassPipeStateTests::display,
        "port_glass_pipe_components", GlassPipeStateTests::components,
        "port_glass_pipe_transforms", GlassPipeStateTests::transforms
    );

    @SubscribeEvent
    public static void functions(RegisterEvent event) {
        event.register(Registries.TEST_FUNCTION, registry -> TESTS.forEach((name, test) -> registry.register(AnvilCraft.of(name), test)));
    }

    @SubscribeEvent
    public static void register(RegisterGameTestsEvent event) {
        var environment = event.registerEnvironment(AnvilCraft.of("port_glass_pipe_state"));
        TESTS.forEach((name, test) -> event.registerTest(AnvilCraft.of(name), new FunctionGameTestInstance(
            ResourceKey.create(Registries.TEST_FUNCTION, AnvilCraft.of(name)),
            new TestData<>(environment, AnvilCraft.of("port_logistics_empty"), 100, 0, true)
        )));
    }

    private static int connection(BlockState state, Direction direction) {
        if (state.getBlock() instanceof PipeStraightBlock) {
            if (direction.getAxis() != state.getValue(PipeBlock.AXIS)) return -1;
            return state.getValue(direction.getAxisDirection() == Direction.AxisDirection.NEGATIVE
                ? PipeBlock.HAS_END_START : PipeBlock.HAS_END_END) ? 1 : 0;
        }
        if (state.getBlock() instanceof PipeCornerBlock) {
            var corner = state.getValue(PipeBlock.CORNER_ENDED);
            if (direction == corner.getFirstDirection()) return state.getValue(PipeBlock.HAS_END_START) ? 1 : 0;
            if (direction == corner.getSecondDirection()) return state.getValue(PipeBlock.HAS_END_END) ? 1 : 0;
            return -1;
        }
        return switch (state.getValue(PipeBlock.getPropertyForDirection(direction))) {
            case NONE -> -1;
            case PIPE -> 0;
            case END -> 1;
        };
    }

    private static void transforms(GameTestHelper helper) {
        int checked = 0;
        for (var block : java.util.List.of(ModBlocks.PIPE_STRAIGHT.get(), ModBlocks.PIPE_CORNER.get(), ModBlocks.PIPE_NODE.get(),
            ModBlocks.GLASS_PIPE_STRAIGHT.get(), ModBlocks.GLASS_PIPE_CORNER.get(), ModBlocks.GLASS_PIPE_NODE.get())) {
            for (var state : block.getStateDefinition().getPossibleStates()) {
                for (var rotation : Rotation.values()) {
                    var transformed = state.rotate(rotation);
                    for (var direction : Direction.values()) {
                        helper.assertTrue(connection(state, direction) == connection(transformed, rotation.rotate(direction)),
                            "Rotation changed a pipe connection or end cap");
                    }
                    checked++;
                }
                for (var mirror : Mirror.values()) {
                    var transformed = state.mirror(mirror);
                    for (var direction : Direction.values()) {
                        helper.assertTrue(connection(state, direction) == connection(transformed, mirror.mirror(direction)),
                            "Mirror changed a pipe connection or end cap");
                    }
                    checked++;
                }
            }
        }
        AnvilCraft.LOGGER.info("PORT_GLASS_TRANSFORM_MATRIX_PASSED: {}", checked);
        helper.succeed();
    }

    private static void conversion(GameTestHelper helper) {
        var level = helper.getLevel();
        var pos = helper.absolutePos(new BlockPos(4, 6, 4));
        var state = ModBlocks.PIPE_STRAIGHT.getDefaultState().setValue(PipeBlock.HAS_CHECK_VALVE, true);
        level.setBlockAndUpdate(pos, state);
        var original = PipeBlock.getCheckValve(level, pos);
        if (original == null) throw new IllegalStateException("Missing native pipe block entity");
        original.setValve(Direction.WEST, Direction.EAST);
        original.setPowered(true);
        var player = helper.makeMockPlayer(GameType.SURVIVAL);
        var panes = new ItemStack(Items.GLASS_PANE, 2);
        player.setItemInHand(InteractionHand.MAIN_HAND, panes);
        var hit = new BlockHitResult(pos.getCenter(), Direction.UP, pos, false);
        helper.assertTrue(state.useItemOn(panes, level, player, InteractionHand.MAIN_HAND, hit).consumesAction(),
            "Glass pane conversion was not accepted");
        helper.assertTrue(level.getBlockState(pos).is(ModBlocks.GLASS_PIPE_STRAIGHT)
            && panes.getCount() == 2 && level.getBlockEntity(pos) instanceof GlassPipeBlockEntity,
            "Glass conversion must retain the block entity and leave the pane unconsumed");
        var glass = (GlassPipeBlockEntity) level.getBlockEntity(pos);
        helper.assertTrue(glass.getBaseFlow(Direction.WEST) == Direction.EAST, "Glass conversion lost the check valve");
        helper.assertTrue(ModBlocks.GLASS_PIPE_STRAIGHT.get().change(player, pos, level, ModItems.ANVIL_HAMMER.asStack())
            && level.getBlockState(pos).is(ModBlocks.PIPE_STRAIGHT)
            && PipeBlock.getCheckValve(level, pos).getBaseFlow(Direction.WEST) == Direction.EAST,
            "Hammer conversion lost the ordinary pipe or valve");
        helper.succeed();
    }

    private static final class CountingPipe extends GlassPipeBlockEntity {
        private int updates;

        private CountingPipe(BlockPos pos, BlockState state) {
            super(ModBlockEntities.GLASS_PIPE.get(), pos, state);
        }

        @Override
        public void sendUpdate() {
            this.updates++;
            super.sendUpdate();
        }
    }

    private static void display(GameTestHelper helper) {
        var level = helper.getLevel();
        var pos = helper.absolutePos(new BlockPos(4, 6, 4));
        level.setBlockAndUpdate(pos, ModBlocks.GLASS_PIPE_STRAIGHT.getDefaultState());
        var pipe = new CountingPipe(pos, level.getBlockState(pos));
        level.setBlockEntity(pipe);
        pipe.showFluid(new FluidStack(Fluids.WATER, 1000), Set.of(Direction.WEST));
        pipe.showFluid(new FluidStack(Fluids.WATER, 500), Set.of(Direction.WEST));
        helper.assertTrue(pipe.updates == 1 && pipe.getDisplayFluid().getAmount() == 1,
            "Continuous flow sent duplicate updates or stored transported volume");
        pipe.showFluid(new FluidStack(Fluids.WATER, 500), Set.of(Direction.EAST));
        helper.assertTrue(pipe.updates == 2 && pipe.getDisplayDirections().equals(Set.of(Direction.WEST, Direction.EAST)),
            "Same-tick directions were not merged");
        pipe.setGasDisplay(new FluidStack(Fluids.LAVA, 1), Set.of(Direction.UP), 0.4F);
        helper.assertTrue(pipe.isShowingGas() && pipe.getDisplayFluid().is(Fluids.LAVA)
            && pipe.getGasAlpha() == 0.4F, "Persistent gas display did not take priority");
        int before = pipe.updates;
        pipe.setGasDisplay(new FluidStack(Fluids.LAVA, 1), Set.of(Direction.UP), 0.4F);
        helper.assertTrue(pipe.updates == before, "Unchanged gas display sent an update");
        helper.runAfterDelay(3, () -> {
            helper.assertTrue(pipe.checkDisplayExpiry() && pipe.isShowingGas(), "Liquid expiry cleared persistent gas");
            pipe.clearGasDisplay();
            helper.assertTrue(pipe.getDisplayFluid().isEmpty() && pipe.getDisplayDirections().isEmpty(), "Expired flow stayed visible");
            helper.succeed();
        });
    }

    private static void components(GameTestHelper helper) {
        var level = helper.getLevel();
        var pos = helper.absolutePos(new BlockPos(4, 6, 4));
        var state = ModBlocks.GLASS_PIPE_CORNER.getDefaultState();
        var remaining = BlockPlacementUtil.placeBlock(level, pos, ModItems.PIPE.asStack(), state);
        helper.assertTrue(remaining.isEmpty() && level.getBlockState(pos).is(ModBlocks.GLASS_PIPE_CORNER),
            "Blueprint glass variant did not consume the ordinary pipe material");
        var pipe = (GlassPipeBlockEntity) level.getBlockEntity(pos);
        pipe.setValve(Direction.NORTH, Direction.SOUTH);
        pipe.showFluid(new FluidStack(Fluids.WATER, 1), Set.of(Direction.NORTH, Direction.UP));
        pipe.setGasDisplay(new FluidStack(Fluids.LAVA, 1), Set.of(Direction.EAST), 0.25F);
        var copied = new GlassPipeBlockEntity(ModBlockEntities.GLASS_PIPE.get(), pos, state);
        copied.setLevel(level);
        copied.loadCustomOnly(TagValueInput.create(ProblemReporter.DISCARDING, level.registryAccess(),
            pipe.getUpdateTag(level.registryAccess())));
        helper.assertTrue(copied.getBaseFlow(Direction.NORTH) == Direction.SOUTH && copied.isShowingGas()
            && copied.getDisplayFluid().is(Fluids.LAVA) && copied.getGasAlpha() == 0.25F
            && copied.getDisplayDirections().equals(Set.of(Direction.EAST)), "Display packet fields failed to round-trip");
        helper.succeed();
    }
}
