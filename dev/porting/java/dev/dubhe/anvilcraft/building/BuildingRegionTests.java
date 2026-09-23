package dev.dubhe.anvilcraft.building;

import com.mojang.authlib.GameProfile;
import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.block.RedstoneWireBlock;
import dev.dubhe.anvilcraft.block.RedstoneWireNetworkManager;
import dev.dubhe.anvilcraft.block.entity.PulseGeneratorBlockEntity;
import dev.dubhe.anvilcraft.init.block.ModBlocks;
import dev.dubhe.anvilcraft.mixin.accessor.BlueprintBlockEventsAccessor;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.FunctionGameTestInstance;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.TestData;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ObserverBlock;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.ticks.TickPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;
import net.neoforged.neoforge.registries.RegisterEvent;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;

@EventBusSubscriber(modid = AnvilCraft.MOD_ID)
public final class BuildingRegionTests {
    private static final Map<String, Consumer<GameTestHelper>> TESTS = Map.of(
        "port_region_quiet_scope", BuildingRegionTests::quiet,
        "port_region_inventory_restore", BuildingRegionTests::inventory,
        "port_region_ticks_events", BuildingRegionTests::ticks,
        "port_region_observer_activation", BuildingRegionTests::observer,
        "port_region_native_bookkeeping", BuildingRegionTests::bookkeeping,
        "port_region_entity_restore", BuildingRegionTests::entities,
        "port_region_wire_restore", BuildingRegionTests::wires
    );

    private record Cell(BlockPos pos, BlockState state) implements BuildingCommit.PlacedCell {
    }

    @SubscribeEvent
    public static void functions(RegisterEvent event) {
        event.register(Registries.TEST_FUNCTION, registry -> TESTS.forEach((name, test) -> registry.register(AnvilCraft.of(name), test)));
    }

    @SubscribeEvent
    public static void register(RegisterGameTestsEvent event) {
        var environment = event.registerEnvironment(AnvilCraft.of("port_building_region"));
        TESTS.forEach((name, test) -> event.registerTest(AnvilCraft.of(name), new FunctionGameTestInstance(
            ResourceKey.create(Registries.TEST_FUNCTION, AnvilCraft.of(name)),
            new TestData<>(environment, AnvilCraft.of("port_logistics_empty"), 100, 0, true)
        )));
    }

    private static BlockPos pos(GameTestHelper helper) {
        return helper.absolutePos(new BlockPos(2, 16, 4));
    }

    private static ServerPlayer player(GameTestHelper helper) {
        var player = FakePlayerFactory.get(helper.getLevel(), new GameProfile(UUID.randomUUID(), "PortUndoRegion"));
        player.setGameMode(GameType.CREATIVE);
        return player;
    }

    private static void quiet(GameTestHelper helper) {
        var level = helper.getLevel();
        var pos = pos(helper);
        try {
            BuildingCommit.quietly(level, () -> {
                level.setBlock(pos, Blocks.SAND.defaultBlockState(), Block.UPDATE_ALL);
                level.scheduleTick(pos, Blocks.SAND, 5);
                level.blockEvent(pos, Blocks.NOTE_BLOCK, 1, 2);
                helper.assertTrue(BuildingCommit.isQuiet(level), "提交内部应处于静默范围");
                try {
                    BuildingCommit.quietly(level.getServer().getLevel(Level.NETHER), () -> {
                        throw new IllegalStateException("nested");
                    });
                } catch (IllegalStateException expected) {
                    helper.assertTrue(BuildingCommit.isQuiet(level), "嵌套异常后应恢复外层世界范围");
                }
                throw new IllegalStateException("outer");
            });
        } catch (IllegalStateException expected) {
            helper.assertTrue(!BuildingCommit.isQuiet(level) && BuildingCommit.quietLevel() == null, "异常退出必须清除静默状态");
        }
        helper.assertTrue(level.getBlockState(pos).is(Blocks.SAND) && !level.getBlockTicks().hasScheduledTick(pos, Blocks.SAND)
            && ((BlueprintBlockEventsAccessor) level).anvilcraft$getBlockEvents().stream().noneMatch(event -> event.pos().equals(pos)),
            "静默写入不得提前执行放置逻辑、安排 tick 或积累方块事件");
        level.scheduleTick(pos, Blocks.SAND, 20);
        helper.assertTrue(level.getBlockTicks().hasScheduledTick(pos, Blocks.SAND), "离开范围后正常计划 tick 必须恢复");
        helper.succeed();
    }

    private static void inventory(GameTestHelper helper) {
        var level = helper.getLevel();
        var pos = pos(helper);
        final var player = player(helper);
        level.setBlock(pos, Blocks.CHEST.defaultBlockState(), Block.UPDATE_CLIENTS);
        var chest = (ChestBlockEntity) level.getBlockEntity(pos);
        var diamond = new ItemStack(Items.DIAMOND, 5);
        diamond.set(DataComponents.CUSTOM_NAME, Component.literal("saved"));
        chest.setItem(0, diamond);
        var region = new BuildingRegionSnapshot(player, new BoundingBox(pos));
        chest.setItem(0, diamond.copyWithCount(3));
        var restored = region.restoredMaterials();
        helper.assertTrue(restored.size() == 1 && restored.getFirst().getCount() == 2 && chest.getItem(0).getCount() == 3,
            "计算恢复增量不得清空或修改当前容器");
        chest.setItem(0, new ItemStack(Items.DIAMOND, 3));
        helper.assertTrue(region.restoredMaterials().getFirst().getCount() == 5 && chest.getItem(0).getCount() == 3,
            "不同组件的物品不能抵扣待恢复库存");
        BuildingCommit.quietly(level, () -> BuildingCommit.set(level, pos, Blocks.STONE.defaultBlockState()));
        helper.assertTrue(level.getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class, new AABB(pos).inflate(1)).isEmpty(),
            "静默替换容器不得触发移除掉落");
        helper.assertTrue(region.canRestore(player), "可修改区域应允许恢复");
        region.restore();
        helper.assertTrue(level.getBlockEntity(pos) instanceof ChestBlockEntity saved && ItemStack.matches(saved.getItem(0), diamond),
            "区域恢复必须重建原方块实体和物品组件");
        player.setGameMode(GameType.ADVENTURE);
        helper.assertTrue(!region.canRestore(player), "失去建造权限后不得恢复区域");
        helper.succeed();
    }

    private static void ticks(GameTestHelper helper) {
        final var level = helper.getLevel();
        final var pos = pos(helper);
        final var note = pos.east();
        final var pulsePos = pos.east(2);
        final var bounds = BoundingBox.fromCorners(pos, pulsePos);
        level.setBlock(pos, Blocks.STONE.defaultBlockState(), Block.UPDATE_CLIENTS);
        level.setBlock(note, Blocks.NOTE_BLOCK.defaultBlockState(), Block.UPDATE_CLIENTS);
        level.setBlock(pulsePos, ModBlocks.PULSE_GENERATOR.getDefaultState(), Block.UPDATE_CLIENTS);
        var pulse = (PulseGeneratorBlockEntity) level.getBlockEntity(pulsePos);
        pulse.setState(PulseGeneratorBlockEntity.State.WAITING);
        pulse.setStartMode(PulseGeneratorBlockEntity.Mode.LOOP.index());
        pulse.setWaitingTime(30);
        pulse.setSignalDuration(5);
        pulse.setPhaseStartGameTime(level.getGameTime());
        pulse.setPhaseDuration(30);
        level.scheduleTick(pos, Blocks.STONE, 40, TickPriority.HIGH);
        level.scheduleTick(pulsePos, ModBlocks.PULSE_GENERATOR.get(), 30);
        level.blockEvent(note, Blocks.NOTE_BLOCK, 1, 2);
        final var region = new BuildingRegionSnapshot(player(helper), bounds);
        helper.runAfterDelay(4, () -> {
            level.scheduleTick(pos, Blocks.DIRT, 100);
            region.restore();
            var restored = BlueprintTicks.capture(level, bounds);
            helper.assertTrue(restored.stream().anyMatch(tick -> tick.pos().equals(pos) && tick.delay() == 40
                && tick.priority() == TickPriority.HIGH.getValue()), "原有 tick 的相对延迟与优先级必须恢复");
            helper.assertTrue(restored.stream().noneMatch(tick -> tick.type().toString().equals("minecraft:dirt")),
                "撤销后不得保留操作期间新增的计划 tick");
            var loaded = (PulseGeneratorBlockEntity) level.getBlockEntity(pulsePos);
            helper.assertTrue(loaded.isProcessing() && loaded.getPhaseRemainingTicks() == 30, "运行时钟应重基到撤销时刻");
            helper.assertTrue(((BlueprintBlockEventsAccessor) level).anvilcraft$getBlockEvents().stream()
                .anyMatch(event -> event.pos().equals(note) && event.paramA() == 1 && event.paramB() == 2),
                "原有方块事件应在状态恢复后重新排入队列");
            helper.succeed();
        });
    }

    private static void observer(GameTestHelper helper) {
        var level = helper.getLevel();
        var pos = pos(helper);
        var state = Blocks.OBSERVER.defaultBlockState().setValue(ObserverBlock.FACING, Direction.EAST);
        BuildingCommit.quietly(level, () -> {
            BuildingCommit.set(level, pos, state);
            BuildingCommit.set(level, pos.east(), Blocks.STONE.defaultBlockState());
        });
        BuildingCommit.activate(level, List.of(new Cell(pos, state), new Cell(pos.east(), Blocks.STONE.defaultBlockState())), List.of());
        helper.assertTrue(!level.getBlockTicks().hasScheduledTick(pos, Blocks.OBSERVER), "恢复内部邻居形状不能制造伪侦测器脉冲");
        level.setBlock(pos.east(), Blocks.GOLD_BLOCK.defaultBlockState(), Block.UPDATE_ALL);
        helper.assertTrue(level.getBlockTicks().hasScheduledTick(pos, Blocks.OBSERVER), "激活结束后真实邻居变化应正常触发侦测器");
        helper.succeed();
    }

    private static void bookkeeping(GameTestHelper helper) {
        final var level = helper.getLevel();
        final var pos = helper.absolutePos(new BlockPos(2, 60, 4));
        BuildingCommit.quietly(level, () -> BuildingCommit.set(level, pos, Blocks.COMPOSTER.defaultBlockState()));
        helper.assertTrue(level.getHeight(Heightmap.Types.WORLD_SURFACE, pos.getX(), pos.getZ()) == pos.getY() + 1,
            "静默写入必须同步原生高度图");
        helper.runAfterDelay(2, () -> {
            helper.assertTrue(level.getPoiManager().getType(pos).isPresent(), "静默写入必须同步兴趣点");
            BuildingCommit.quietly(level, () -> BuildingCommit.set(level, pos, Blocks.AIR.defaultBlockState()));
            helper.runAfterDelay(2, () -> {
                helper.assertTrue(level.getPoiManager().getType(pos).isEmpty()
                    && level.getHeight(Heightmap.Types.WORLD_SURFACE, pos.getX(), pos.getZ()) <= pos.getY(),
                    "移除静默写入的方块也必须正确更新高度图和兴趣点");
                helper.succeed();
            });
        });
    }

    private static void entities(GameTestHelper helper) {
        final var level = helper.getLevel();
        final var pos = pos(helper);
        final var player = player(helper);
        var cow = EntityType.COW.create(level, EntitySpawnReason.LOAD);
        cow.setPos(pos.getCenter());
        cow.setNoAi(true);
        cow.setNoGravity(true);
        cow.setCustomName(Component.literal("original"));
        level.addFreshEntity(cow);
        final UUID id = cow.getUUID();
        final var region = new BuildingRegionSnapshot(player, BoundingBox.fromCorners(pos, pos.above(2)));
        cow.setPos(pos.east(8).getCenter());
        cow.setCustomName(Component.literal("changed"));
        region.restore();
        helper.assertTrue(cow.position().equals(pos.getCenter()) && Component.literal("original").equals(cow.getCustomName()),
            "离开区域但仍存在的原实体应按 UUID 回到原位置和状态");
        cow.discard();
        helper.runAfterDelay(1, () -> {
            helper.assertTrue(region.canRestore(player), "已销毁原实体应允许重建");
            region.restore();
            Entity restored = level.getEntity(id);
            helper.assertTrue(restored != null && restored != cow && Component.literal("original").equals(restored.getCustomName()),
                "重建必须保留原实体 UUID 和数据");
            var another = new BuildingRegionSnapshot(player, BoundingBox.fromCorners(pos, pos.above(2)));
            restored.setRemoved(Entity.RemovalReason.UNLOADED_TO_CHUNK);
            helper.assertTrue(!another.canRestore(player), "仅卸载的实体不能被当作销毁而重复生成");
            helper.succeed();
        });
    }

    private static void wires(GameTestHelper helper) {
        var level = helper.getLevel();
        var pos = pos(helper);
        var state = ModBlocks.REDSTONE_WIRE.getDefaultState().setValue(RedstoneWireBlock.ATTACHMENT, Direction.DOWN)
            .setValue(RedstoneWireBlock.NORTH, RedstoneWireBlock.ConnectionType.NONE)
            .setValue(RedstoneWireBlock.SOUTH, RedstoneWireBlock.ConnectionType.NONE)
            .setValue(RedstoneWireBlock.EAST, RedstoneWireBlock.ConnectionType.SIDE)
            .setValue(RedstoneWireBlock.WEST, RedstoneWireBlock.ConnectionType.SIDE);
        var observer = pos.north();
        BuildingCommit.quietly(level, () -> {
            BuildingCommit.set(level, pos.below(), Blocks.STONE.defaultBlockState());
            BuildingCommit.set(level, pos.east(), Blocks.REDSTONE_BLOCK.defaultBlockState());
            BuildingCommit.set(level, observer, Blocks.OBSERVER.defaultBlockState().setValue(ObserverBlock.FACING, Direction.SOUTH));
            BuildingCommit.set(level, pos, state);
        });
        RedstoneWireNetworkManager.restoreBlueprint(level, Map.of(pos, state));
        helper.assertTrue(level.getBlockState(pos) == state && RedstoneWireNetworkManager.getPower(level, pos) == 15,
            "导线恢复应保留指定外观并建立正确功率缓存");
        helper.assertTrue(!level.getBlockTicks().hasScheduledTick(observer, Blocks.OBSERVER), "导线初始化不能通知侦测器产生伪脉冲");
        BuildingCommit.activate(level, List.of(new Cell(pos, state), new Cell(observer, level.getBlockState(observer)),
            new Cell(pos.east(), Blocks.REDSTONE_BLOCK.defaultBlockState())), List.of());
        helper.assertTrue(!level.getBlockTicks().hasScheduledTick(observer, Blocks.OBSERVER)
            && RedstoneWireNetworkManager.getPower(level, pos) == 15, "缓存恢复后的统一激活也不能重置功率并触发伪脉冲");
        helper.succeed();
    }
}
