package dev.dubhe.anvilcraft.porting;

import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.building.BlueprintPlacement;
import dev.dubhe.anvilcraft.building.BuildingEntityTransform;
import dev.dubhe.anvilcraft.building.StructureSnapshot;
import dev.dubhe.anvilcraft.entity.CauldronOutletEntity;
import dev.dubhe.anvilcraft.entity.MagnetizedNodeEntity;
import dev.dubhe.anvilcraft.init.entity.ModEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.UUIDUtil;
import net.minecraft.core.Vec3i;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.FunctionGameTestInstance;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.TestData;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.FloatTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.decoration.ItemFrame;
import net.minecraft.world.entity.item.FallingBlockEntity;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.level.storage.TagValueOutput;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;
import net.neoforged.neoforge.registries.RegisterEvent;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Consumer;

@EventBusSubscriber(modid = AnvilCraft.MOD_ID)
public final class BlueprintTransformTests {
    private static final BlockPos ANCHOR = new BlockPos(20, 30, 40);
    private static final Map<String, Consumer<GameTestHelper>> TESTS = Map.of(
        "port_blueprint_transform_grid", BlueprintTransformTests::grid,
        "port_blueprint_tile", BlueprintTransformTests::tile,
        "port_blueprint_tile_limits", BlueprintTransformTests::limits,
        "port_blueprint_state_facing", BlueprintTransformTests::facing,
        "port_blueprint_motion", BlueprintTransformTests::motion,
        "port_blueprint_hanging_entity", BlueprintTransformTests::hanging,
        "port_blueprint_mod_entity", BlueprintTransformTests::modEntities,
        "port_blueprint_dynamic_parts", BlueprintTransformTests::dynamic,
        "port_blueprint_animated_endpoints", BlueprintTransformTests::animated
    );

    @SubscribeEvent
    public static void functions(RegisterEvent event) {
        event.register(Registries.TEST_FUNCTION, registry -> TESTS.forEach((name, test) -> registry.register(AnvilCraft.of(name), test)));
    }

    @SubscribeEvent
    public static void register(RegisterGameTestsEvent event) {
        var environment = event.registerEnvironment(AnvilCraft.of("port_blueprint_transform"));
        TESTS.forEach((name, test) -> event.registerTest(AnvilCraft.of(name), new FunctionGameTestInstance(
            ResourceKey.create(Registries.TEST_FUNCTION, AnvilCraft.of(name)),
            new TestData<>(environment, AnvilCraft.of("port_logistics_empty"), 100, 0, true)
        )));
    }

    private static Vec3 linear(Vec3 value, Rotation rotation, Mirror mirror) {
        double x = mirror == Mirror.FRONT_BACK ? -value.x : value.x;
        double z = mirror == Mirror.LEFT_RIGHT ? -value.z : value.z;
        return switch (rotation) {
            case NONE -> new Vec3(x, value.y, z);
            case CLOCKWISE_90 -> new Vec3(-z, value.y, x);
            case CLOCKWISE_180 -> new Vec3(-x, value.y, -z);
            case COUNTERCLOCKWISE_90 -> new Vec3(z, value.y, -x);
        };
    }

    private static void grid(GameTestHelper helper) {
        for (var size : List.of(new Vec3i(1, 1, 1), new Vec3i(1, 4, 9), new Vec3i(8, 1, 3), new Vec3i(3, 5, 2))) {
            for (var rotation : Rotation.values()) {
                for (var mirror : Mirror.values()) {
                    var placement = new BlueprintPlacement(ANCHOR, rotation, mirror);
                    var bounds = placement.bounds(size);
                    var cells = new HashSet<BlockPos>();
                    for (BlockPos local : BlockPos.betweenClosed(BlockPos.ZERO,
                        new BlockPos(size.getX() - 1, size.getY() - 1, size.getZ() - 1))) {
                        var vector = linear(Vec3.atLowerCornerOf(local), rotation, mirror);
                        var expected = BlockPos.containing(vector).offset(ANCHOR);
                        helper.assertTrue(placement.worldOf(local).equals(expected) && bounds.isInside(expected),
                            "所有薄片、长条和非立方尺寸必须使用同一整数坐标变换");
                        cells.add(expected);
                        var point = Vec3.atLowerCornerOf(local).add(0.25, 0.75, 0.625);
                        var transformed = placement.localOf(point, local);
                        var expectedPoint = linear(point.add(-0.5, 0, -0.5), rotation, mirror).add(0.5, 0, 0.5);
                        helper.assertTrue(transformed.distanceTo(expectedPoint) < 1.0E-8
                            && BlockPos.containing(transformed).equals(placement.localOf(local)), "格内实体不能被甩到相邻格子");
                    }
                    helper.assertTrue(cells.size() == size.getX() * size.getY() * size.getZ()
                        && bounds.getXSpan() * bounds.getYSpan() * bounds.getZSpan() == cells.size(), "变换后的包围盒必须精确覆盖全部方块");
                }
            }
        }
        helper.succeed();
    }

    private static StructureSnapshot solid(Vec3i size) {
        var entries = new ArrayList<StructureSnapshot.BlockEntry>();
        for (var pos : BlockPos.betweenClosed(BlockPos.ZERO, new BlockPos(size.getX() - 1, size.getY() - 1, size.getZ() - 1))) {
            entries.add(new StructureSnapshot.BlockEntry(pos.immutable(), 0, Optional.empty()));
        }
        return new StructureSnapshot(size, List.of(Blocks.STONE.defaultBlockState()), entries, List.of());
    }

    private static void tile(GameTestHelper helper) {
        var snapshot = solid(new Vec3i(2, 1, 3));
        var placement = new BlueprintPlacement(new BlockPos(10, 5, 20), Rotation.CLOCKWISE_90, Mirror.NONE);
        var copies = placement.tile(snapshot, new BlockPos(4, 3, 16));
        helper.assertTrue(copies.size() == 27, "负向三轴拖拽应按旋转后的 3x1x2 间隔平铺");
        var occupied = new HashSet<BlockPos>();
        for (var copy : copies) {
            for (var entry : snapshot.blocks()) helper.assertTrue(occupied.add(copy.worldOf(entry.pos())), "各份蓝图不能重叠");
        }
        helper.assertTrue(occupied.size() == 162 && occupied.contains(new BlockPos(2, 3, 16))
            && occupied.contains(new BlockPos(10, 5, 21)), "批量覆盖范围必须完整且无空隙");
        helper.succeed();
    }

    private static void limits(GameTestHelper helper) {
        var placement = new BlueprintPlacement(BlockPos.ZERO, Rotation.NONE, Mirror.NONE);
        var unit = solid(new Vec3i(1, 1, 1));
        helper.assertTrue(placement.tile(unit, new BlockPos(3999, 0, 0)).size() == 4000, "4000 份单块蓝图应在边界内");
        for (int last : new int[]{4000, Integer.MAX_VALUE, Integer.MIN_VALUE}) {
            helper.assertTrue(placement.tile(unit, new BlockPos(last, 0, 0)).isEmpty(), "超限与整数极值必须在分配前拒绝");
        }
        var dense = solid(new Vec3i(16, 16, 16));
        helper.assertTrue(placement.tile(dense, BlockPos.ZERO).size() == 1
            && placement.tile(dense, new BlockPos(16, 0, 0)).isEmpty(), "单份保留原服务校验，批量必须按真实条目数量计费限额");
        var entities = new StructureSnapshot(unit.size(), List.of(), List.of(), List.of(
            new StructureSnapshot.EntityEntry(Vec3.ZERO, BlockPos.ZERO, new CompoundTag()),
            new StructureSnapshot.EntityEntry(Vec3.ZERO, BlockPos.ZERO, new CompoundTag())));
        helper.assertTrue(placement.tile(entities, new BlockPos(2000, 0, 0)).isEmpty(), "实体也必须计入平铺上限");
        helper.succeed();
    }

    private static void facing(GameTestHelper helper) {
        for (var source : Direction.values()) {
            for (var player : Direction.Plane.HORIZONTAL) {
                var rotation = BlueprintPlacement.facingPlayer(source, player);
                helper.assertTrue(rotation.rotate(source.getAxis().isHorizontal() ? source : Direction.NORTH) == player.getOpposite(),
                    "默认朝向必须面向玩家");
            }
        }
        var state = Blocks.OAK_STAIRS.defaultBlockState().setValue(BlockStateProperties.HORIZONTAL_FACING, Direction.NORTH);
        var placement = new BlueprintPlacement(ANCHOR, Rotation.CLOCKWISE_90, Mirror.NONE);
        helper.assertTrue(placement.stateOf(state).getValue(BlockStateProperties.HORIZONTAL_FACING) == Direction.EAST,
            "方块状态必须跟坐标同步旋转");
        helper.succeed();
    }

    private static void motion(GameTestHelper helper) {
        var data = new CompoundTag();
        data.putString("id", "minecraft:armor_stand");
        data.store("UUID", UUIDUtil.CODEC, UUID.randomUUID());
        var angles = new ListTag();
        angles.add(FloatTag.valueOf(30));
        angles.add(FloatTag.valueOf(15));
        data.put("Rotation", angles);
        var velocity = new Vec3(0.2, -0.4, 0.6);
        data.store("Motion", Vec3.CODEC, velocity);
        data.put("Passengers", new ListTag());
        for (var rotation : Rotation.values()) {
            for (var mirror : Mirror.values()) {
                var placement = new BlueprintPlacement(ANCHOR, rotation, mirror);
                var result = BuildingEntityTransform.transform(new StructureSnapshot.EntityEntry(new Vec3(1.25, 2.5, 3.75),
                    new BlockPos(1, 2, 3), data), placement);
                helper.assertTrue(result.read("Motion", Vec3.CODEC).orElseThrow().distanceTo(linear(velocity, rotation, mirror)) < 1.0E-8,
                    "速度应只做线性变换，不混入锚点或半格平移");
                float expectedYaw = (mirror == Mirror.FRONT_BACK ? -30 : mirror == Mirror.LEFT_RIGHT ? 150 : 30)
                    + switch (rotation) {
                    case NONE -> 0;
                    case CLOCKWISE_90 -> 90;
                    case CLOCKWISE_180 -> -180;
                    case COUNTERCLOCKWISE_90 -> -90;
                };
                helper.assertTrue(result.getListOrEmpty("Rotation").getFloatOr(0, 0) == expectedYaw
                    && result.getListOrEmpty("Rotation").getFloatOr(1, 0) == 15, "偏航必须变换，俯仰保持原值");
                helper.assertTrue(!result.contains("UUID") && !result.contains("Passengers") && data.contains("UUID"),
                    "变换应移除旧实体身份和内嵌乘客，不修改输入");
            }
        }
        helper.succeed();
    }

    private static CompoundTag save(GameTestHelper helper, Entity entity) {
        var output = TagValueOutput.createWithContext(ProblemReporter.DISCARDING, helper.getLevel().registryAccess());
        entity.saveAsPassenger(output);
        return output.buildResult();
    }

    private static void load(GameTestHelper helper, Entity entity, CompoundTag tag) {
        entity.load(TagValueInput.create(ProblemReporter.DISCARDING, helper.getLevel().registryAccess(), tag));
    }

    private static void hanging(GameTestHelper helper) {
        var original = new ItemFrame(helper.getLevel(), new BlockPos(2, 3, 4), Direction.NORTH);
        var data = save(helper, original);
        var placement = new BlueprintPlacement(ANCHOR, Rotation.CLOCKWISE_90, Mirror.NONE);
        var transformed = BuildingEntityTransform.transform(
            new StructureSnapshot.EntityEntry(original.position(), original.getPos(), data), placement);
        var restored = new ItemFrame(helper.getLevel(), BlockPos.ZERO, Direction.NORTH);
        load(helper, restored, transformed);
        helper.assertTrue(restored.getPos().equals(new BlockPos(16, 33, 42)) && restored.getDirection() == Direction.EAST,
            "26.1 原生悬挂实体必须实际读入变换后的 block_pos 和 Facing");
        helper.succeed();
    }

    private static void modEntities(GameTestHelper helper) {
        var placement = new BlueprintPlacement(ANCHOR, Rotation.CLOCKWISE_90, Mirror.NONE);
        for (boolean legacy : new boolean[]{false, true}) {
            var data = new CompoundTag();
            data.putString("id", "anvilcraft:magnetized_node");
            data.store(legacy ? "BlockPos" : "block_pos", BlockPos.CODEC, new BlockPos(1, 2, 3));
            data.put(legacy ? "BlockState" : "block_state", NbtUtils.writeBlockState(Blocks.OAK_SLAB.defaultBlockState()));
            var transformed = BuildingEntityTransform.transform(new StructureSnapshot.EntityEntry(new Vec3(1.5, 2.5, 3.5),
                new BlockPos(1, 2, 3), data), placement);
            var node = new MagnetizedNodeEntity(ModEntities.MAGNETIZED_NODE.get(), helper.getLevel());
            load(helper, node, transformed);
            helper.assertTrue(node.blockPos.equals(new BlockPos(17, 32, 41)) && node.getY() == 32.5,
                "磁化节点必须兼容新旧字段并贴合变换后支撑方块的碰撞面");
        }
        var outlet = new CompoundTag();
        outlet.putString("id", "anvilcraft:cauldron_outlet");
        outlet.store("CauldronPos", BlockPos.CODEC, new BlockPos(-50, 60, 70));
        outlet.store("TargetPos", BlockPos.CODEC, new BlockPos(-46, 60, 70));
        outlet.putInt("AttachedDirection", Direction.EAST.get3DDataValue());
        outlet.put("CauldronState", NbtUtils.writeBlockState(Blocks.CAULDRON.defaultBlockState()));
        var transformed = BuildingEntityTransform.transform(new StructureSnapshot.EntityEntry(new Vec3(1.5, 2.5, 3.5),
            new BlockPos(1, 2, 3), outlet), placement);
        var restored = new CauldronOutletEntity(ModEntities.CAULDRON_OUTLET.get(), helper.getLevel());
        load(helper, restored, transformed);
        helper.assertTrue(restored.getCauldronPos().equals(new BlockPos(17, 32, 41)) && restored.getAttachedDirection() == Direction.SOUTH
            && transformed.read("TargetPos", BlockPos.CODEC).orElseThrow().equals(new BlockPos(17, 32, 45)),
            "输出口支撑、方向及目标偏移必须同步变换");
        helper.succeed();
    }

    private static void dynamic(GameTestHelper helper) {
        var data = new CompoundTag();
        data.putString("id", "anvilcraft:sliding_block");
        data.store("Pos", Vec3.CODEC, new Vec3(101.5, 62, 203.5));
        data.store("RelativeStart", BlockPos.CODEC, new BlockPos(1, 0, 0));
        data.putString("MovingDirection", "east");
        final var section = new CompoundTag();
        final var parts = new ListTag();
        var part = new CompoundTag();
        part.store("offset", BlockPos.CODEC, new BlockPos(2, 0, 0));
        part.put("state", NbtUtils.writeBlockState(Blocks.CHEST.defaultBlockState()));
        var content = new CompoundTag();
        content.putString("id", "minecraft:chest");
        part.put("entityData", content);
        parts.add(part);
        section.put("blocks", parts);
        data.put("SlidingBlocks", section);
        var entry = new StructureSnapshot.EntityEntry(new Vec3(1.5, 2, 3.5), new BlockPos(1, 2, 3), data);
        var placement = new BlueprintPlacement(ANCHOR, Rotation.CLOCKWISE_90, Mirror.NONE);
        var result = BuildingEntityTransform.transform(entry, placement);
        var convertedPart = result.getCompoundOrEmpty("SlidingBlocks").getListOrEmpty("blocks").getCompoundOrEmpty(0);
        helper.assertTrue(result.getStringOr("MovingDirection", "").equals("south")
            && convertedPart.read("offset", BlockPos.CODEC).orElseThrow().equals(new BlockPos(0, 0, 2)), "滑动方向和部件偏移必须同步旋转");
        var entityData = convertedPart.getCompoundOrEmpty("entity_data");
        helper.assertTrue(entityData.getIntOr("x", 0) == 17 && entityData.getIntOr("y", 0) == 32
            && entityData.getIntOr("z", 0) == 43, "滑动部件必须采用 26.1 字段并重定位方块实体坐标");
        data.store("RelativeStart", BlockPos.CODEC, new BlockPos(Integer.MIN_VALUE, 0, 0));
        var invalid = BuildingEntityTransform.transform(entry, placement);
        helper.assertTrue(invalid.read("RelativeStart", BlockPos.CODEC).orElseThrow().equals(BlockPos.ZERO),
            "无效起点偏移不能绕过 64 格限制");
        helper.succeed();
    }

    private static void animated(GameTestHelper helper) {
        var data = new CompoundTag();
        data.putString("id", "anvilcraft:animate_ascending_block");
        data.store("RelativeStart", BlockPos.CODEC, new BlockPos(-2, 0, 1));
        data.store("RelativeEnd", BlockPos.CODEC, new BlockPos(0, 4, -1));
        data.put("BlockState", NbtUtils.writeBlockState(Blocks.OAK_STAIRS.defaultBlockState()
            .setValue(BlockStateProperties.HORIZONTAL_FACING, Direction.NORTH)));
        var entry = new StructureSnapshot.EntityEntry(new Vec3(1.5, 2, 3.5), new BlockPos(1, 2, 3), data);
        var placement = new BlueprintPlacement(ANCHOR, Rotation.CLOCKWISE_90, Mirror.FRONT_BACK);
        var transformed = BuildingEntityTransform.transform(entry, placement);
        helper.assertTrue(transformed.read("StartPos", BlockPos.CODEC).orElseThrow().equals(new BlockPos(16, 32, 41))
            && transformed.read("EndPos", BlockPos.CODEC).orElseThrow().equals(new BlockPos(18, 36, 39)),
            "浮升动画的相对起止点必须和实体一起镜像旋转");
        data.putString("id", "minecraft:falling_block");
        var falling = new FallingBlockEntity(EntityType.FALLING_BLOCK, helper.getLevel());
        load(helper, falling, BuildingEntityTransform.transform(entry, placement));
        helper.assertTrue(falling.position().distanceTo(new Vec3(17.5, 32, 39.5)) < 1.0E-8
            && falling.getBlockState().getValue(BlockStateProperties.HORIZONTAL_FACING) == Direction.EAST,
            "原生下落方块必须实际读入变换后的连续位置和方块状态");
        helper.succeed();
    }
}
