package dev.dubhe.anvilcraft.building;

import dev.dubhe.anvilcraft.AnvilCraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.FunctionGameTestInstance;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.TestData;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.entity.decoration.LeashFenceKnotEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.entity.FurnaceBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.ChestType;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;
import net.neoforged.neoforge.registries.RegisterEvent;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;

@EventBusSubscriber(modid = AnvilCraft.MOD_ID)
public final class BlueprintCaptureTests {
    private static final Map<String, Consumer<GameTestHelper>> TESTS = Map.of(
        "port_blueprint_complete_parts", BlueprintCaptureTests::parts,
        "port_blueprint_overlap_rejection", BlueprintCaptureTests::overlap,
        "port_blueprint_fluid_reach", BlueprintCaptureTests::fluids,
        "port_blueprint_chest_mapping", BlueprintCaptureTests::chests,
        "port_blueprint_scanner_coordinates", BlueprintCaptureTests::scanner,
        "port_blueprint_live_capture", BlueprintCaptureTests::capture,
        "port_blueprint_runtime_fields", BlueprintCaptureTests::runtime,
        "port_blueprint_clock_rebase", BlueprintCaptureTests::clock,
        "port_blueprint_scanner_supports", BlueprintCaptureTests::supports
    );

    @SubscribeEvent
    public static void functions(RegisterEvent event) {
        event.register(Registries.TEST_FUNCTION, registry -> TESTS.forEach((name, test) -> registry.register(AnvilCraft.of(name), test)));
    }

    @SubscribeEvent
    public static void register(RegisterGameTestsEvent event) {
        var environment = event.registerEnvironment(AnvilCraft.of("port_blueprint_capture"));
        TESTS.forEach((name, test) -> event.registerTest(AnvilCraft.of(name), new FunctionGameTestInstance(
            ResourceKey.create(Registries.TEST_FUNCTION, AnvilCraft.of(name)),
            new TestData<>(environment, AnvilCraft.of("port_logistics_empty"), 100, 0, true)
        )));
    }

    private static StructureSnapshot snapshot(BlockState state) {
        return new StructureSnapshot(new Vec3i(1, 1, 1), List.of(state), List.of(
            new StructureSnapshot.BlockEntry(BlockPos.ZERO, 0, Optional.empty())), List.of());
    }

    private static void parts(GameTestHelper helper) {
        var door = BlueprintNormalizer.normalize(snapshot(Blocks.OAK_DOOR.defaultBlockState()));
        helper.assertTrue(door.added() == 1 && door.snapshot().size().equals(new Vec3i(1, 2, 1))
            && door.snapshot().stateOf(door.snapshot().blocks().get(1))
                .getValue(BlockStateProperties.DOUBLE_BLOCK_HALF) == DoubleBlockHalf.UPPER, "缺少的门上半必须完整补齐");
        var state = Blocks.RED_BED.defaultBlockState().setValue(BlockStateProperties.HORIZONTAL_FACING, Direction.WEST);
        var original = snapshot(state);
        var tick = new BlueprintTicks.Entry(BlockPos.ZERO, Identifier.parse("minecraft:red_bed"), 20, 0, false);
        original = new StructureSnapshot(original.size(), original.palette(), original.blocks(),
            List.of(new StructureSnapshot.EntityEntry(new Vec3(0.5, 0.5, 0.5), BlockPos.ZERO, new CompoundTag())), List.of(tick), 100);
        var bed = BlueprintNormalizer.normalize(original);
        helper.assertTrue(bed.offset().equals(new BlockPos(1, 0, 0)) && bed.snapshot().size().equals(new Vec3i(2, 1, 1))
            && bed.snapshot().ticks().getFirst().pos().equals(new BlockPos(1, 0, 0))
            && bed.snapshot().entities().getFirst().pos().equals(new Vec3(1.5, 0.5, 0.5)), "补齐负坐标床头时实体和 tick 必须一起平移");
        var orphan = BlueprintNormalizer.normalize(snapshot(Blocks.OAK_DOOR.defaultBlockState()
            .setValue(BlockStateProperties.DOUBLE_BLOCK_HALF, DoubleBlockHalf.UPPER)));
        helper.assertTrue(orphan.removed() == 1 && orphan.snapshot().blocks().isEmpty(), "孤立依附部件不能作为完整结构放置");
        var piston = BlueprintNormalizer.normalize(snapshot(Blocks.STICKY_PISTON.defaultBlockState()
            .setValue(BlockStateProperties.FACING, Direction.EAST).setValue(BlockStateProperties.EXTENDED, true)));
        helper.assertTrue(piston.added() == 1 && piston.snapshot().blocks().size() == 2, "伸出的活塞必须补齐头部");
        helper.succeed();
    }

    private static void overlap(GameTestHelper helper) {
        var invalid = new StructureSnapshot(new Vec3i(1, 2, 1),
            List.of(Blocks.OAK_DOOR.defaultBlockState(), Blocks.STONE.defaultBlockState()), List.of(
                new StructureSnapshot.BlockEntry(BlockPos.ZERO, 0, Optional.empty()),
                new StructureSnapshot.BlockEntry(new BlockPos(0, 1, 0), 1, Optional.empty())), List.of());
        try {
            BlueprintNormalizer.normalize(invalid);
            helper.fail("部件生成覆盖独立方块时必须拒绝");
        } catch (IllegalArgumentException expected) {
            helper.assertTrue(expected.getMessage().contains("Overlapping"), "必须报告重叠原因");
        }
        helper.succeed();
    }

    private static void fluids(GameTestHelper helper) {
        var water = Blocks.WATER.defaultBlockState();
        var flow = water.setValue(LiquidBlock.LEVEL, 1);
        var origin = BlockPos.ZERO;
        var states = Map.of(origin, water, origin.east(), flow, origin.east().below(), flow,
            origin.above(), flow, origin.west(), Blocks.LAVA.defaultBlockState().setValue(LiquidBlock.LEVEL, 1),
            origin.east(4), flow);
        var reachable = BlueprintFluids.reachable(states);
        helper.assertTrue(reachable.equals(Set.of(origin, origin.east(), origin.east().below())),
            "液体只应沿相同类型向水平和下方连通，不能向上或跨空隙传播");
        var slab = Blocks.OAK_SLAB.defaultBlockState().setValue(BlockStateProperties.WATERLOGGED, true);
        helper.assertTrue(!BlueprintFluids.reachable(Map.of(origin, slab, origin.below(), flow)).contains(origin.below()),
            "含水方块的实心碰撞面必须阻止连通");
        var isolated = BlueprintNormalizer.normalize(snapshot(flow));
        helper.assertTrue(isolated.removed() == 1, "缺少可达液体源的流动方块必须清理");
        helper.succeed();
    }

    private static void chests(GameTestHelper helper) {
        var left = Blocks.CHEST.defaultBlockState().setValue(BlockStateProperties.HORIZONTAL_FACING, Direction.NORTH)
            .setValue(BlockStateProperties.CHEST_TYPE, ChestType.LEFT);
        var right = left.setValue(BlockStateProperties.CHEST_TYPE, ChestType.RIGHT);
        var declared = Map.of(BlockPos.ZERO.asLong(), left, BlockPos.ZERO.east().asLong(), right);
        helper.assertTrue(OrdinaryBlockAdapter.isPairedChestAttached(BlockPos.ZERO, left, declared)
            && OrdinaryBlockAdapter.isPairedChestCore(BlockPos.ZERO.east(), right, declared)
            && OrdinaryBlockAdapter.pairedChestMaterial(right).getCount() == 2, "成对箱子应只在核心一次分配两份材料");
        helper.assertTrue(OrdinaryBlockAdapter.projectionState(left, BlockPos.ZERO, Map.of())
            .getValue(BlockStateProperties.CHEST_TYPE) == ChestType.SINGLE
            && OrdinaryBlockAdapter.commitState(left, BlockPos.ZERO, Set.of())
                .getValue(BlockStateProperties.CHEST_TYPE) == ChestType.SINGLE,
            "不完整大箱子的预览和提交都必须退化为单箱");
        helper.assertTrue(OrdinaryBlockAdapter.material(Blocks.OAK_DOOR.defaultBlockState()
            .setValue(BlockStateProperties.DOUBLE_BLOCK_HALF, DoubleBlockHalf.UPPER)).isEmpty(), "依附格不能重复扣材料");
        helper.succeed();
    }

    private static void scanner(GameTestHelper helper) {
        var size = new Vec3i(2, 3, 4);
        var pos = new BlockPos(1, 2, 3);
        var point = new Vec3(1.25, 2.5, 3.75);
        var tick = new BlueprintTicks.Entry(pos, Identifier.parse("minecraft:stone"), 12, 0, false);
        for (var facing : Direction.Plane.HORIZONTAL) {
            for (boolean upsideDown : new boolean[]{false, true}) {
                var raw = new StructureSnapshot(size, List.of(Blocks.STONE.defaultBlockState()), List.of(
                    new StructureSnapshot.BlockEntry(pos, 0, Optional.empty())), List.of(
                    new StructureSnapshot.EntityEntry(point, pos, new CompoundTag())), List.of(tick), 80);
                var result = ScannerDiskNormalizer.normalize(raw, facing, upsideDown);
                BlockPos expected = switch (facing) {
                    case SOUTH -> new BlockPos(0, upsideDown ? 0 : 2, 0);
                    case WEST -> new BlockPos(3, upsideDown ? 0 : 2, 0);
                    case EAST -> new BlockPos(0, upsideDown ? 0 : 2, 1);
                    default -> new BlockPos(1, upsideDown ? 0 : 2, 3);
                };
                helper.assertTrue(result.blocks().getFirst().pos().equals(expected)
                    && result.ticks().getFirst().pos().equals(expected) && result.entities().getFirst().blockPos().equals(expected),
                    "扫描朝向与倒置必须同步还原方块、实体锚点和 tick");
            }
        }
        helper.succeed();
    }

    private static void capture(GameTestHelper helper) {
        var level = helper.getLevel();
        var origin = helper.absolutePos(new BlockPos(2, 16, 4));
        level.setBlock(origin, Blocks.OAK_DOOR.defaultBlockState(), Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE);
        level.setBlock(origin.above(), Blocks.OAK_DOOR.defaultBlockState()
            .setValue(BlockStateProperties.DOUBLE_BLOCK_HALF, DoubleBlockHalf.UPPER), Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE);
        level.scheduleTick(origin, Blocks.OAK_DOOR, 25);
        var item = new ItemEntity(level, origin.getX() + 0.5, origin.getY() + 0.5, origin.getZ() + 0.5, new ItemStack(Items.DIAMOND));
        level.addFreshEntity(item);
        var captured = BlueprintCapture.capture(level, new AABB(origin)).snapshot();
        helper.assertTrue(captured.size().equals(new Vec3i(1, 2, 1)) && captured.blocks().size() == 2,
            "实际扫描门下半也必须捕获范围外的完整部件");
        helper.assertTrue(captured.entities().size() == 1 && captured.ticks().stream().anyMatch(tick -> tick.delay() == 25)
            && captured.capturedAt() == level.getGameTime(), "实际捕获必须保留实体、调度和当前时钟");
        item.discard();
        var knot = new LeashFenceKnotEntity(level, origin);
        var knotData = BlueprintCapture.captureEntity(knot);
        helper.assertTrue(knotData != null && knotData.getStringOr("id", "").equals("minecraft:leash_knot"),
            "默认不序列化的栓绳结也必须进入快照");
        helper.succeed();
    }

    private static void runtime(GameTestHelper helper) {
        final var furnace = new FurnaceBlockEntity(BlockPos.ZERO, Blocks.FURNACE.defaultBlockState());
        var tag = new CompoundTag();
        tag.putInt("lit_time_remaining", 80);
        tag.putInt("lit_total_time", 200);
        tag.putInt("cooking_time_spent", 30);
        tag.putInt("cooking_total_time", 100);
        furnace.loadCustomOnly(TagValueInput.create(ProblemReporter.DISCARDING, helper.getLevel().registryAccess(), tag));
        var saved = furnace.saveWithFullMetadata(helper.getLevel().registryAccess());
        saved.putString("OwnerSetting", "keep");
        var state = BlueprintRuntimeData.take(furnace, saved);
        helper.assertTrue(state.getIntOr("lit_time_remaining", 0) == 80 && state.getIntOr("cooking_time_spent", 0) == 30
            && !saved.contains("lit_time_remaining") && saved.getStringOr("OwnerSetting", "").equals("keep"),
            "新版熔炉运行字段必须提取，普通配置留在原数据里");
        helper.succeed();
    }

    private static void clock(GameTestHelper helper) {
        var config = new CompoundTag();
        var extra = new CompoundTag();
        extra.putLong("PhaseStartGameTime", 490);
        extra.putInt("PhaseDuration", 40);
        config.put("ExtraData", extra);
        config.putLong("RollStart", 480);
        BlueprintRuntimeData.restoreClock(config, new StructureSnapshot(new Vec3i(1, 1, 1), List.of(), List.of(), List.of(),
            List.of(), 500), BlockPos.ZERO, 1000);
        helper.assertTrue(extra.getLongOr("PhaseStartGameTime", -1) == 990 && config.getLongOr("RollStart", -1) == 980,
            "时间戳必须按捕获与恢复时钟差重基，保持已运行时长");
        var legacy = new CompoundTag();
        var legacyExtra = new CompoundTag();
        legacyExtra.putInt("PhaseDuration", 40);
        legacy.put("ExtraData", legacyExtra);
        var tick = new BlueprintTicks.Entry(BlockPos.ZERO, Identifier.parse("minecraft:stone"), 12, 0, false);
        BlueprintRuntimeData.restoreClock(legacy, new StructureSnapshot(new Vec3i(1, 1, 1), List.of(), List.of(), List.of(),
            List.of(tick), 0), BlockPos.ZERO, 3);
        helper.assertTrue(legacyExtra.getLongOr("PhaseStartGameTime", -1) == 0 && legacyExtra.getIntOr("PhaseDuration", 0) == 12,
            "无捕获时间的旧数据应从剩余 tick 恢复，不能产生负起始时间");
        helper.succeed();
    }

    private static void supports(GameTestHelper helper) {
        for (var facing : Direction.Plane.HORIZONTAL) {
            for (boolean upsideDown : new boolean[]{false, true}) {
                final var rawSize = facing.getAxis() == Direction.Axis.X ? new Vec3i(4, 3, 2) : new Vec3i(2, 3, 4);
                final var support = rawBlock(new BlockPos(1, 0, 2), facing, upsideDown);
                var node = new CompoundTag();
                node.putString("id", "anvilcraft:magnetized_node");
                node.put("block_state", NbtUtils.writeBlockState(Blocks.OAK_SLAB.defaultBlockState()));
                var outlet = new CompoundTag();
                outlet.putString("id", "anvilcraft:cauldron_outlet");
                outlet.putInt("attached_direction", Direction.EAST.get3DDataValue());
                var raw = new StructureSnapshot(rawSize, List.of(Blocks.OAK_SLAB.defaultBlockState()), List.of(
                    new StructureSnapshot.BlockEntry(support, 0, Optional.empty())), List.of(
                    new StructureSnapshot.EntityEntry(rawPoint(new Vec3(1.5, 0.5, 2.5), facing, upsideDown), support, node),
                    new StructureSnapshot.EntityEntry(rawPoint(new Vec3(2.01, 0.5, 2.5), facing, upsideDown), support, outlet)));
                var normalized = ScannerDiskNormalizer.normalize(raw, facing, upsideDown);
                for (var entity : normalized.entities()) {
                    helper.assertTrue(entity.blockPos().equals(new BlockPos(1, 0, 2)),
                        "方向和倒置还原后，模组实体必须锚定真实支撑格：" + facing + "/" + upsideDown + "/" + entity.blockPos());
                }
            }
        }
        helper.succeed();
    }

    private static BlockPos rawBlock(BlockPos pos, Direction facing, boolean upsideDown) {
        int y = upsideDown ? 2 - pos.getY() : pos.getY();
        return switch (facing) {
            case SOUTH -> new BlockPos(1 - pos.getX(), y, 3 - pos.getZ());
            case WEST -> new BlockPos(3 - pos.getZ(), y, pos.getX());
            case EAST -> new BlockPos(pos.getZ(), y, 1 - pos.getX());
            default -> new BlockPos(pos.getX(), y, pos.getZ());
        };
    }

    private static Vec3 rawPoint(Vec3 pos, Direction facing, boolean upsideDown) {
        double y = upsideDown ? 3 - pos.y : pos.y;
        return switch (facing) {
            case SOUTH -> new Vec3(2 - pos.x, y, 4 - pos.z);
            case WEST -> new Vec3(4 - pos.z, y, pos.x);
            case EAST -> new Vec3(pos.z, y, 2 - pos.x);
            default -> new Vec3(pos.x, y, pos.z);
        };
    }
}
