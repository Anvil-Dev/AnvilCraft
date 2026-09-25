package dev.dubhe.anvilcraft.porting;

import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.block.cake.LargeCakeBlock;
import dev.dubhe.anvilcraft.block.multipart.AbstractMultiPartBlock;
import dev.dubhe.anvilcraft.block.state.Cube3x3PartHalf;
import dev.dubhe.anvilcraft.building.StructureSnapshot;
import dev.dubhe.anvilcraft.building.StructureSnapshotCodec;
import dev.dubhe.anvilcraft.init.block.ModBlocks;
import dev.dubhe.anvilcraft.init.item.ModComponents;
import dev.dubhe.anvilcraft.init.item.ModItems;
import dev.dubhe.anvilcraft.item.block.LargeCakeBlockItem;
import dev.dubhe.anvilcraft.item.property.component.StructureDiskData;
import dev.dubhe.anvilcraft.util.BlockPlacementUtil;
import dev.dubhe.anvilcraft.util.StructureLoadUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.FunctionGameTestInstance;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.TestData;
import net.minecraft.nbt.NbtIo;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.Half;
import net.minecraft.world.level.storage.LevelResource;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;
import net.neoforged.neoforge.registries.RegisterEvent;

import java.nio.file.Files;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Consumer;

@EventBusSubscriber(modid = AnvilCraft.MOD_ID)
public final class BlueprintPlacementParityTests {
    private static final Map<String, Consumer<GameTestHelper>> TESTS = Map.of(
        "port_blueprint_layout_matrix", BlueprintPlacementParityTests::matrix,
        "port_blueprint_disk_frames", BlueprintPlacementParityTests::disk,
        "port_blueprint_giant_origin", helper -> multipart(helper, ModBlocks.GIANT_ANVIL.get()),
        "port_blueprint_cauldron_origin", helper -> multipart(helper, ModBlocks.LARGE_CAULDRON.get()),
        "port_blueprint_tank_origin", helper -> multipart(helper, ModBlocks.LARGE_FLUID_TANK.get()),
        "port_blueprint_pipe_states", BlueprintPlacementParityTests::pipes,
        "port_blueprint_cake_parts", BlueprintPlacementParityTests::cake
    );

    @SubscribeEvent
    public static void functions(RegisterEvent event) {
        event.register(Registries.TEST_FUNCTION, registry -> TESTS.forEach((name, test) -> registry.register(AnvilCraft.of(name), test)));
    }

    @SubscribeEvent
    public static void register(RegisterGameTestsEvent event) {
        var environment = event.registerEnvironment(AnvilCraft.of("port_blueprint_placement_parity"));
        TESTS.forEach((name, test) -> event.registerTest(AnvilCraft.of(name), new FunctionGameTestInstance(
            ResourceKey.create(Registries.TEST_FUNCTION, AnvilCraft.of(name)),
            new TestData<>(environment, AnvilCraft.of("port_logistics_empty"), 100, 0, true)
        )));
    }

    private static int storageIndex(BlockPlacementUtil.BlueprintLayout layout, BlockPos pos) {
        try {
            var method = BlockPlacementUtil.BlueprintLayout.class.getDeclaredMethod("getStorageIndex", BlockPos.class);
            method.setAccessible(true);
            return (int) method.invoke(layout, pos);
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException(exception);
        }
    }

    private static void matrix(GameTestHelper helper) {
        var origin = helper.absolutePos(new BlockPos(5, 5, 5));
        int checked = 0;
        for (var target : Direction.Plane.HORIZONTAL) {
            for (var scanner : Direction.Plane.HORIZONTAL) {
                for (boolean auto : new boolean[]{false, true}) {
                    for (boolean upside : new boolean[]{false, true}) {
                        BlockState[] states = new BlockState[125];
                        Arrays.fill(states, Blocks.OAK_STAIRS.defaultBlockState()
                            .setValue(BlockStateProperties.HORIZONTAL_FACING, scanner));
                        var layout = new BlockPlacementUtil.BlueprintLayout(
                            helper.getLevel(), origin, target, scanner, auto, upside, 5, 4, states);
                        for (int index = 0; index < 125; index++) {
                            var pos = layout.getPosition(index);
                            helper.assertTrue(storageIndex(layout, pos) == index, "Layout inverse lost a position");
                            var facing = auto ? target.getOpposite() : scanner;
                            helper.assertTrue(layout.getState(index).getValue(BlockStateProperties.HORIZONTAL_FACING) == facing,
                                "Auto-rotation changed the wrong heading");
                            helper.assertTrue(layout.getState(index).getValue(BlockStateProperties.HALF)
                                == (upside ? Half.TOP : Half.BOTTOM),
                                "Upside-down placement lost block properties");
                            helper.assertTrue(pos.getY() == origin.getY() + index / 25 - (upside ? 4 : 0), "Layer height changed");
                            checked++;
                        }
                    }
                }
            }
        }
        helper.assertTrue(checked == 8000, "Orientation matrix coverage changed");
        AnvilCraft.LOGGER.info("PORT_BLUEPRINT_LAYOUT_MATRIX_PASSED: {}", checked);
        helper.succeed();
    }

    private static void disk(GameTestHelper helper) {
        var id = UUID.randomUUID();
        var path = helper.getLevel().getServer().getWorldPath(LevelResource.ROOT).resolve("anvilcraft/structures/port_" + id + ".nbt");
        var snapshot = new StructureSnapshot(new Vec3i(3, 2, 1),
            List.of(Blocks.STONE.defaultBlockState(), Blocks.GOLD_BLOCK.defaultBlockState()),
            List.of(new StructureSnapshot.BlockEntry(BlockPos.ZERO, 0, Optional.empty()),
                new StructureSnapshot.BlockEntry(new BlockPos(2, 1, 0), 1, Optional.empty())), List.of());
        try {
            Files.createDirectories(path.getParent());
            NbtIo.writeCompressed(StructureSnapshotCodec.write(snapshot), path);
            for (var direction : Direction.Plane.HORIZONTAL) {
                var stack = ModItems.STRUCTURE_DISK.asStack();
                stack.set(ModComponents.STRUCTURE_DISK_DATA,
                    new StructureDiskData(path.getFileName().toString(), "Parity", id, direction, 3, 2, 1, false, true));
                var data = StructureLoadUtil.loadStructureFromDisk(helper.getLevel(), stack);
                int width = direction.getAxis() == Direction.Axis.X ? 1 : 3;
                int depth = direction.getAxis() == Direction.Axis.X ? 3 : 1;
                helper.assertTrue(data != null && data.width == width && data.depth == depth && data.blocks.size() == 2,
                    "Non-square disk frame has wrong dimensions");
                for (var block : data.blocks) {
                    helper.assertTrue(block.x() >= 0 && block.x() < width && block.z() >= 0 && block.z() < depth,
                        "Transformed disk block lies outside its frame");
                }
                var preview = StructureLoadUtil.loadStructureFromDiskForPreview(helper.getLevel(), stack);
                helper.assertTrue(preview != null && preview.diskData.direction() == Direction.NORTH
                    && preview.width == 3 && preview.depth == 1 && preview.blocks.getLast().x() == 2,
                    "Canonical preview received a second frame rotation");
            }
        } catch (Exception exception) {
            throw new IllegalStateException(exception);
        } finally {
            try {
                Files.deleteIfExists(path);
            } catch (java.io.IOException exception) {
                throw new IllegalStateException(exception);
            }
        }
        helper.succeed();
    }

    private static <P extends Enum<P>> void multipart(GameTestHelper helper, AbstractMultiPartBlock<P> block) {
        var level = helper.getLevel();
        var pos = helper.absolutePos(new BlockPos(6, 7, 6));
        var state = block.defaultBlockState();
        for (P part : block.getParts()) {
            var candidate = block.placedState(part, state);
            if (block.isMainPart(candidate)) {
                state = candidate;
                break;
            }
        }
        var expected = BlockPlacementUtil.getExpectedMultiblockParts(pos, state);
        var positions = expected.stream().map(BlockPlacementUtil.MultiblockPart::pos).toList();
        for (var part : expected) {
            if (!positions.contains(part.pos().below())) level.setBlockAndUpdate(part.pos().below(), Blocks.STONE.defaultBlockState());
        }
        var remaining = BlockPlacementUtil.placeBlock(level, pos, block.asItem().getDefaultInstance(), state);
        helper.assertTrue(remaining.isEmpty(), "Multipart placement did not consume its item");
        helper.runAfterDelay(10, () -> {
            for (var part : expected) {
                helper.assertTrue(level.getBlockState(part.pos()).is(block)
                    && level.getBlockState(part.pos()).getValue(block.getPart()) == part.state().getValue(block.getPart()),
                    "Multipart core offset or delayed survival differs from blueprint");
            }
            helper.succeed();
        });
    }

    private static void cake(GameTestHelper helper) {
        var level = helper.getLevel();
        var pos = helper.absolutePos(new BlockPos(6, 7, 6));
        var state = ModBlocks.LARGE_CAKE.getDefaultState().setValue(LargeCakeBlock.HALF, Cube3x3PartHalf.BOTTOM_CENTER);
        BlockState[] states = new BlockState[125];
        Arrays.fill(states, Blocks.AIR.defaultBlockState());
        var layout = new BlockPlacementUtil.BlueprintLayout(level, pos.south(4), Direction.NORTH, Direction.SOUTH,
            true, false, 5, 4, states);
        LargeCakeBlockItem.forEachPlacedBlock(pos, state, (part, partState) -> states[storageIndex(layout, part)] = partState);
        helper.assertTrue(Arrays.stream(states).filter(value -> value.is(ModBlocks.LARGE_CAKE)
            && !BlockPlacementUtil.isSecondaryBlueprintPart(value)).count() == 1, "Cake material counted more than once");
        for (int x = -1; x <= 1; x++) {
            for (int z = -1; z <= 1; z++) level.setBlockAndUpdate(pos.offset(x, -1, z), Blocks.STONE.defaultBlockState());
        }
        var snapshots = layout.capturePartSnapshots(level, pos, state);
        helper.assertTrue(snapshots.size() == Cube3x3PartHalf.values().length, "Cake blueprint snapshot missed secondary parts");
        helper.assertTrue(BlockPlacementUtil.placeBlock(level, pos, ModBlocks.LARGE_CAKE.asStack(), state).isEmpty()
            && BlockPlacementUtil.applyBlueprintStates(level, snapshots), "Cake placement did not apply its complete blueprint");
        helper.runAfterDelay(10, () -> {
            LargeCakeBlockItem.forEachPlacedBlock(pos, state, (part, expected) ->
                helper.assertTrue(level.getBlockState(part) == expected, "Cake structure lost a part after placement"));
            helper.succeed();
        });
    }

    private static void pipes(GameTestHelper helper) {
        var level = helper.getLevel();
        int index = 0;
        for (var block : List.of(ModBlocks.PIPE_STRAIGHT.get(), ModBlocks.PIPE_CORNER.get(), ModBlocks.PIPE_NODE.get())) {
            var pos = helper.absolutePos(new BlockPos(3 + index++ * 3, 8, 4));
            var state = block.defaultBlockState();
            var remaining = BlockPlacementUtil.placeBlock(level, pos, ModItems.PIPE.asStack(), state);
            helper.assertTrue(remaining.isEmpty() && level.getBlockState(pos).is(block), "Pipe blueprint used its default straight shape");
        }
        helper.succeed();
    }
}
