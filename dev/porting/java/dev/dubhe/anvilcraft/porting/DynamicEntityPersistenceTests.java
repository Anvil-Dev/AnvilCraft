package dev.dubhe.anvilcraft.porting;

import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.api.sliding.SlidingBlockInfo;
import dev.dubhe.anvilcraft.block.entity.SpaceOvercompressorBlockEntity;
import dev.dubhe.anvilcraft.building.BlueprintPlacement;
import dev.dubhe.anvilcraft.building.BuildingEntityTransform;
import dev.dubhe.anvilcraft.building.StructureSnapshot;
import dev.dubhe.anvilcraft.entity.AnimateAscendingBlockEntity;
import dev.dubhe.anvilcraft.entity.CauldronOutletEntity;
import dev.dubhe.anvilcraft.entity.SlidingBlockEntity;
import dev.dubhe.anvilcraft.init.block.ModBlocks;
import dev.dubhe.anvilcraft.init.entity.ModEntities;
import io.netty.buffer.Unpooled;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.FunctionGameTestInstance;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.TestData;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.level.storage.TagValueOutput;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;
import net.neoforged.neoforge.registries.RegisterEvent;
import org.apache.commons.lang3.tuple.Triple;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;

@EventBusSubscriber(modid = AnvilCraft.MOD_ID)
public final class DynamicEntityPersistenceTests {
    private static final Map<String, Consumer<GameTestHelper>> TESTS = Map.of(
        "port_ascending_resume", DynamicEntityPersistenceTests::ascending,
        "port_ascending_transform_restore", DynamicEntityPersistenceTests::transformed,
        "port_sliding_resume_age", DynamicEntityPersistenceTests::sliding,
        "port_sliding_contents_restore", DynamicEntityPersistenceTests::contents,
        "port_outlet_target_restore", DynamicEntityPersistenceTests::outlet,
        "port_outlet_motion_restore", DynamicEntityPersistenceTests::outletMotion
    );

    @SubscribeEvent
    public static void functions(RegisterEvent event) {
        event.register(Registries.TEST_FUNCTION, registry -> TESTS.forEach((name, test) -> registry.register(AnvilCraft.of(name), test)));
    }

    @SubscribeEvent
    public static void register(RegisterGameTestsEvent event) {
        var environment = event.registerEnvironment(AnvilCraft.of("port_dynamic_entity_persistence"));
        TESTS.forEach((name, test) -> event.registerTest(AnvilCraft.of(name), new FunctionGameTestInstance(
            ResourceKey.create(Registries.TEST_FUNCTION, AnvilCraft.of(name)),
            new TestData<>(environment, AnvilCraft.of("port_logistics_empty"), 100, 0, true)
        )));
    }

    private static CompoundTag save(GameTestHelper helper, Entity entity) {
        var output = TagValueOutput.createWithContext(ProblemReporter.DISCARDING, helper.getLevel().registryAccess());
        entity.saveAsPassenger(output);
        return output.buildResult();
    }

    private static void load(GameTestHelper helper, Entity entity, CompoundTag tag) {
        entity.load(TagValueInput.create(ProblemReporter.DISCARDING, helper.getLevel().registryAccess(), tag));
    }

    private static AnimateAscendingBlockEntity ascending(GameTestHelper helper, BlockPos start) {
        var tag = new CompoundTag();
        tag.store("Pos", Vec3.CODEC, Vec3.atBottomCenterOf(start));
        tag.store("Motion", Vec3.CODEC, new Vec3(0, 0.15, 0));
        tag.put("BlockState", NbtUtils.writeBlockState(Blocks.BLUE_CONCRETE.defaultBlockState()));
        tag.store("StartPos", BlockPos.CODEC, start);
        tag.store("EndPos", BlockPos.CODEC, start.above(20));
        var entity = new AnimateAscendingBlockEntity(ModEntities.ASCENDING_BLOCK_ENTITY.get(), helper.getLevel());
        load(helper, entity, tag);
        return entity;
    }

    private static void ascending(GameTestHelper helper) {
        var original = ascending(helper, helper.absolutePos(new BlockPos(2, 16, 4)));
        original.tick();
        original.tick();
        var saved = save(helper, original);
        var restored = new AnimateAscendingBlockEntity(ModEntities.ASCENDING_BLOCK_ENTITY.get(), helper.getLevel());
        load(helper, restored, saved);
        helper.assertTrue(restored.getBlockState().is(Blocks.BLUE_CONCRETE)
            && restored.getStartPos().equals(original.getStartPos()) && restored.getEndPos().equals(original.getEndPos()),
            "中途存档必须保留实际方块与起止点");
        for (int i = 0; i < 20 && !original.isRemoved(); i++) {
            original.tick();
            restored.tick();
            helper.assertTrue(original.position().distanceTo(restored.position()) < 1.0E-8
                && original.getDeltaMovement().distanceTo(restored.getDeltaMovement()) < 1.0E-8
                && original.isRemoved() == restored.isRemoved(), "重载后的浮升必须沿相同轨迹并同时结束");
        }
        helper.assertTrue(original.isRemoved() && restored.isRemoved(), "动画必须按保存的终点结束");
        helper.succeed();
    }

    private static void transformed(GameTestHelper helper) {
        var original = ascending(helper, helper.absolutePos(new BlockPos(2, 16, 4)));
        original.setPos(original.position().add(0, 2, 0));
        var saved = save(helper, original);
        var anchor = helper.absolutePos(new BlockPos(7, 17, 7));
        var placement = new BlueprintPlacement(anchor, Rotation.CLOCKWISE_90, Mirror.FRONT_BACK);
        var entry = new StructureSnapshot.EntityEntry(new Vec3(0.5, 2, 0.5), new BlockPos(0, 2, 0), saved);
        var restored = new AnimateAscendingBlockEntity(ModEntities.ASCENDING_BLOCK_ENTITY.get(), helper.getLevel());
        load(helper, restored, BuildingEntityTransform.transform(entry, placement));
        helper.assertTrue(restored.getStartPos().equals(anchor) && restored.getEndPos().equals(anchor.above(20))
            && restored.getBlockState().is(Blocks.BLUE_CONCRETE), "实际保存的相对端点必须经过蓝图变换进入实体运行状态");
        var old = new CompoundTag();
        old.store("Pos", Vec3.CODEC, Vec3.atBottomCenterOf(anchor));
        old.put("BlockState", NbtUtils.writeBlockState(Blocks.STONE.defaultBlockState()));
        load(helper, restored, old);
        helper.assertTrue(restored.getStartPos().equals(anchor) && restored.getEndPos().equals(anchor), "缺少端点的旧数据应回退到当前位置");
        helper.succeed();
    }

    private static void sliding(GameTestHelper helper) {
        var level = helper.getLevel();
        var pos = helper.absolutePos(new BlockPos(1, 16, 2));
        var entity = new SlidingBlockEntity(level, pos, Direction.EAST,
            List.of(Triple.of(pos, Blocks.STONE.defaultBlockState(), Optional.empty())));
        entity.setPos(Vec3.atBottomCenterOf(pos.east(2)).add(0.25, 0, 0));
        entity.setDeltaMovement(new Vec3(0.35, 0, 0));
        var saved = save(helper, entity);
        saved.putInt("Time", 7);
        var restored = new SlidingBlockEntity(ModEntities.SLIDING_BLOCK.get(), level);
        load(helper, restored, saved);
        var roundTrip = save(helper, restored);
        helper.assertTrue(restored.getStartPos().equals(pos) && restored.getMoveDirection() == Direction.EAST
            && roundTrip.getIntOr("Time", 0) == 7
            && roundTrip.read("RelativeStart", BlockPos.CODEC).orElseThrow().equals(new BlockPos(-2, 0, 0)),
            "滑动起点、年龄、方向和相对起点必须完整保存");
        level.setBlock(restored.blockPosition().below(), Blocks.CAMPFIRE.defaultBlockState(), Block.UPDATE_CLIENTS);
        var landing = restored.blockPosition();
        helper.assertTrue(level.getBlockState(landing.east()).isAir(), "停止测试前方必须没有障碍物");
        restored.tick();
        helper.assertTrue(restored.isRemoved() && level.getBlockState(landing).is(Blocks.STONE),
            "已运行超过两 tick 的实体重载后遇停止表面应立即落地，不能重置预热期");
        helper.succeed();
    }

    private static void contents(GameTestHelper helper) {
        var level = helper.getLevel();
        var pos = helper.absolutePos(new BlockPos(2, 15, 4));
        var state = Blocks.CHEST.defaultBlockState();
        var chest = new ChestBlockEntity(pos, state);
        chest.setItem(0, new ItemStack(Items.DIAMOND, 3));
        var tool = new ItemStack(Items.DIAMOND_PICKAXE);
        var unbreaking = level.registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(Enchantments.UNBREAKING);
        tool.enchant(unbreaking, 3);
        chest.setItem(1, tool);
        var original = new SlidingBlockEntity(level, pos, Direction.EAST, List.of(Triple.of(pos, state, Optional.of(chest))));
        var restored = new SlidingBlockEntity(ModEntities.SLIDING_BLOCK.get(), level);
        load(helper, restored, save(helper, original));
        var second = new SlidingBlockEntity(ModEntities.SLIDING_BLOCK.get(), level);
        load(helper, second, save(helper, restored));
        var info = second.getSection().blocks().getFirst();
        var restoredChest = (ChestBlockEntity) info.blockEntity();
        helper.assertTrue(restoredChest != null && restoredChest.getItem(0).getCount() == 3
            && restoredChest.getItem(1).get(DataComponents.ENCHANTMENTS).getLevel(unbreaking) == 3,
            "重复存档必须在没有 Level 的方块实体中保留容器物品和动态附魔注册项");
        var legacyPart = save(helper, second).getCompoundOrEmpty("SlidingBlocks").getListOrEmpty("blocks")
            .getCompoundOrEmpty(0).copy();
        legacyPart.put("entityData", legacyPart.getCompoundOrEmpty("entity_data"));
        legacyPart.remove("entity_data");
        var legacy = (ChestBlockEntity) SlidingBlockInfo.CODEC.codec()
            .parse(level.registryAccess().createSerializationContext(NbtOps.INSTANCE), legacyPart).getOrThrow().blockEntity();
        helper.assertTrue(legacy != null && legacy.getItem(0).getCount() == 3, "源版 entityData 字段必须兼容");
        legacyPart.remove("entityData");
        helper.assertTrue(SlidingBlockInfo.CODEC.codec().parse(NbtOps.INSTANCE, legacyPart).getOrThrow().blockEntity() == null,
            "没有方块实体数据的旧部件应正常解码，不丢弃整段滑动实体");
        var buffer = new RegistryFriendlyByteBuf(Unpooled.buffer(), level.registryAccess());
        try {
            SlidingBlockInfo.STREAM_CODEC.encode(buffer, info);
            var synced = (ChestBlockEntity) SlidingBlockInfo.STREAM_CODEC.decode(buffer).blockEntity();
            helper.assertTrue(synced != null && synced.getItem(0).getCount() == 3
                && synced.getItem(1).get(DataComponents.ENCHANTMENTS).getLevel(unbreaking) == 3,
                "同步编解码也必须真正加载方块实体内容");
        } finally {
            buffer.release();
        }
        var machineState = ModBlocks.SPACE_OVERCOMPRESSOR.getDefaultState();
        var machine = new SpaceOvercompressorBlockEntity(pos, machineState);
        var mass = new CompoundTag();
        mass.putLong("storedMass", 1729);
        machine.loadCustomOnly(TagValueInput.create(ProblemReporter.DISCARDING, level.registryAccess(), mass));
        var moving = new SlidingBlockEntity(level, pos, Direction.EAST,
            List.of(Triple.of(pos, machineState, Optional.of(machine))));
        var loaded = new SlidingBlockEntity(ModEntities.SLIDING_BLOCK.get(), level);
        load(helper, loaded, save(helper, moving));
        loaded.stop();
        var placed = (SpaceOvercompressorBlockEntity) level.getBlockEntity(pos);
        helper.assertTrue(placed != null && placed.getStoredMass() == 1729 && placed.getBlockPos().equals(pos),
            "支持移动的机器必须在真实落地后保留运行数据和正确坐标");
        helper.succeed();
    }

    private static CompoundTag outletData(BlockPos pos, boolean legacy) {
        var tag = new CompoundTag();
        tag.store("Pos", Vec3.CODEC, Vec3.atCenterOf(pos));
        tag.store(legacy ? "CauldronPos" : "cauldron_pos", BlockPos.CODEC, pos);
        tag.put(legacy ? "CauldronState" : "cauldron_state", NbtUtils.writeBlockState(Blocks.CAULDRON.defaultBlockState()));
        tag.putInt(legacy ? "AttachedDirection" : "attached_direction", Direction.EAST.get3DDataValue());
        tag.putBoolean("WasMoving", true);
        return tag;
    }

    private static void outlet(GameTestHelper helper) {
        var level = helper.getLevel();
        var pos = helper.absolutePos(new BlockPos(2, 15, 4));
        var target = pos.east();
        for (boolean legacy : new boolean[]{false, true}) {
            level.setBlock(pos, Blocks.AIR.defaultBlockState(), Block.UPDATE_CLIENTS);
            level.setBlock(target, Blocks.MOVING_PISTON.defaultBlockState(), Block.UPDATE_CLIENTS);
            var tag = outletData(pos, legacy);
            tag.store("TargetPos", BlockPos.CODEC, target);
            var entity = new CauldronOutletEntity(ModEntities.CAULDRON_OUTLET.get(), level);
            load(helper, entity, tag);
            var restored = new CauldronOutletEntity(ModEntities.CAULDRON_OUTLET.get(), level);
            load(helper, restored, save(helper, entity));
            restored.tick();
            helper.assertTrue(!restored.isRemoved() && restored.getCauldronPos().equals(pos), "目标仍在移动时应保留原锚点并等待");
            level.setBlock(target, Blocks.CAULDRON.defaultBlockState(), Block.UPDATE_ALL);
            restored.tick();
            var finalTag = save(helper, restored);
            helper.assertTrue(restored.getCauldronPos().equals(target) && restored.getAttachedDirection() == Direction.EAST
                && !finalTag.contains("TargetPos") && !finalTag.getBooleanOr("WasMoving", true),
                "目标恢复为炼药锅后应落到保存的目标并结束移动状态");
        }
        helper.succeed();
    }

    private static void outletMotion(GameTestHelper helper) {
        var pos = helper.absolutePos(new BlockPos(2, 15, 4));
        var moving = new CauldronOutletEntity(ModEntities.CAULDRON_OUTLET.get(), helper.getLevel());
        load(helper, moving, outletData(pos, false));
        var restored = new CauldronOutletEntity(ModEntities.CAULDRON_OUTLET.get(), helper.getLevel());
        load(helper, restored, save(helper, moving));
        restored.tick();
        helper.assertTrue(!restored.isRemoved(), "临时没有支撑且仍在移动的输出口不能在重载后被误删除");
        var idleTag = outletData(pos, false);
        idleTag.remove("WasMoving");
        var idle = new CauldronOutletEntity(ModEntities.CAULDRON_OUTLET.get(), helper.getLevel());
        load(helper, idle, idleTag);
        idle.tick();
        helper.assertTrue(idle.isRemoved(), "没有移动标志的失去支撑输出口仍应按原规则清理");
        helper.succeed();
    }
}
