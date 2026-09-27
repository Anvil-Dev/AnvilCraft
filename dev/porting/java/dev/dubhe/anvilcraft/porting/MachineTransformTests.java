package dev.dubhe.anvilcraft.porting;

import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.block.fluid.ControlValveBlock;
import dev.dubhe.anvilcraft.block.fluid.PumpBlock;
import dev.dubhe.anvilcraft.block.state.FacingWithAxis;
import dev.dubhe.anvilcraft.block.state.Orientation;
import dev.dubhe.anvilcraft.block.utility.redstone.BlockComparatorBlock;
import dev.dubhe.anvilcraft.init.block.ModBlocks;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.FunctionGameTestInstance;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.TestData;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;
import net.neoforged.neoforge.registries.RegisterEvent;

import java.util.List;

@EventBusSubscriber(modid = AnvilCraft.MOD_ID)
public final class MachineTransformTests {
    @SubscribeEvent
    public static void functions(RegisterEvent event) {
        event.register(Registries.TEST_FUNCTION, registry -> registry.register(
            AnvilCraft.of("port_machine_transform_matrix"), MachineTransformTests::matrix));
    }

    @SubscribeEvent
    public static void register(RegisterGameTestsEvent event) {
        var id = AnvilCraft.of("port_machine_transform_matrix");
        var environment = event.registerEnvironment(id);
        event.registerTest(id, new FunctionGameTestInstance(ResourceKey.create(Registries.TEST_FUNCTION, id),
            new TestData<>(environment, AnvilCraft.of("port_logistics_empty"), 100, 0, true)));
    }

    private static Direction transform(Direction direction, Rotation rotation, Mirror mirror) {
        int x = direction.getStepX();
        int z = direction.getStepZ();
        if (mirror == Mirror.FRONT_BACK) x = -x;
        if (mirror == Mirror.LEFT_RIGHT) z = -z;
        int turns = switch (rotation) {
            case NONE -> 0;
            case CLOCKWISE_90 -> 1;
            case CLOCKWISE_180 -> 2;
            case COUNTERCLOCKWISE_90 -> 3;
        };
        for (int turn = 0; turn < turns; turn++) {
            int previousX = x;
            x = -z;
            z = previousX;
        }
        for (var candidate : Direction.values()) {
            if (candidate.getStepX() == x && candidate.getStepY() == direction.getStepY() && candidate.getStepZ() == z) return candidate;
        }
        throw new IllegalStateException("Invalid transformed unit vector");
    }

    private static BlockState expected(BlockState state, Rotation rotation, Mirror mirror) {
        if (state.hasProperty(BlockComparatorBlock.FACING_WITH_AXIS)) {
            var original = state.getValue(BlockComparatorBlock.FACING_WITH_AXIS);
            var facing = transform(original.getFacing(), rotation, mirror);
            var axisDirection = Direction.fromAxisAndDirection(original.getAxis(), Direction.AxisDirection.POSITIVE);
            var axis = transform(axisDirection, rotation, mirror).getAxis();
            var value = FacingWithAxis.valueOf(facing.name() + "_" + axis.name());
            return state.setValue(BlockComparatorBlock.FACING_WITH_AXIS, value);
        }
        if (state.hasProperty(PumpBlock.ORIENTATION)) {
            var parts = state.getValue(PumpBlock.ORIENTATION).name().split("_");
            var front = transform(Direction.valueOf(parts[0]), rotation, mirror);
            var top = transform(Direction.valueOf(parts[1]), rotation, mirror);
            return state.setValue(PumpBlock.ORIENTATION, Orientation.valueOf(front.name() + "_" + top.name()));
        }
        if (state.hasProperty(ControlValveBlock.AXIS)) {
            var direction = Direction.fromAxisAndDirection(state.getValue(ControlValveBlock.AXIS), Direction.AxisDirection.POSITIVE);
            return state.setValue(ControlValveBlock.AXIS, transform(direction, rotation, mirror).getAxis());
        }
        var facing = state.getValue(HorizontalDirectionalBlock.FACING);
        return state.setValue(HorizontalDirectionalBlock.FACING, transform(facing, rotation, mirror));
    }

    private static void matrix(GameTestHelper helper) {
        int checked = 0;
        for (var block : List.of(ModBlocks.BLOCK_COMPARATOR.get(), ModBlocks.SMART_BLOCK_PLACER.get(),
            ModBlocks.STRUCTURE_SCANNER.get(), ModBlocks.CONTROL_VALVE.get(), ModBlocks.PUMP.get())) {
            for (var state : block.getStateDefinition().getPossibleStates()) {
                for (var mirror : Mirror.values()) {
                    helper.assertTrue(state.mirror(mirror).mirror(mirror).equals(state), "Double mirror changed state " + state);
                    for (var rotation : Rotation.values()) {
                        helper.assertTrue(state.mirror(mirror).rotate(rotation).equals(expected(state, rotation, mirror)),
                            "Machine frame or unrelated property changed: " + state + " / " + mirror + " / " + rotation);
                        checked++;
                    }
                }
                var restored = state;
                for (int turn = 0; turn < 4; turn++) restored = restored.rotate(Rotation.CLOCKWISE_90);
                helper.assertTrue(restored.equals(state), "Four quarter-turns changed state " + state);
            }
        }
        AnvilCraft.LOGGER.info("PORT_MACHINE_TRANSFORM_MATRIX_PASSED: {}", checked);
        helper.succeed();
    }
}
