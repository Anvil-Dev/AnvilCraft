package dev.dubhe.anvilcraft.building;

import com.mojang.authlib.GameProfile;
import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.entity.FluidTankMinecartEntity;
import dev.dubhe.anvilcraft.entity.SlidingBlockEntity;
import dev.dubhe.anvilcraft.init.block.ModBlocks;
import dev.dubhe.anvilcraft.init.entity.ModEntities;
import dev.dubhe.anvilcraft.init.item.ModComponents;
import dev.dubhe.anvilcraft.init.item.ModItems;
import dev.dubhe.anvilcraft.item.property.component.SavedEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.UUIDUtil;
import net.minecraft.core.Vec3i;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.FunctionGameTestInstance;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.TestData;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.DoubleTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.Container;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.Leashable;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.decoration.ItemFrame;
import net.minecraft.world.entity.decoration.LeashFenceKnotEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.SlabType;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;
import net.neoforged.neoforge.registries.RegisterEvent;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import net.neoforged.neoforge.transfer.transaction.Transaction;
import org.apache.commons.lang3.tuple.Triple;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Consumer;

@EventBusSubscriber(modid = AnvilCraft.MOD_ID)
public final class BlueprintEntityAdapterTests {
    private static final Map<String, Consumer<GameTestHelper>> TESTS = Map.of(
        "port_entity_vehicle_materials", BlueprintEntityAdapterTests::vehicles,
        "port_entity_equipment_contents", BlueprintEntityAdapterTests::equipment,
        "port_entity_motion_limits", BlueprintEntityAdapterTests::motion,
        "port_entity_leash_restore", BlueprintEntityAdapterTests::leash,
        "port_entity_identity_links", BlueprintEntityAdapterTests::links,
        "port_entity_actual_material", BlueprintEntityAdapterTests::material,
        "port_entity_dynamic_materials", BlueprintEntityAdapterTests::dynamic,
        "port_entity_fluid_cart", BlueprintEntityAdapterTests::fluidCart,
        "port_entity_memories", BlueprintEntityAdapterTests::memories
    );

    @SubscribeEvent
    public static void functions(RegisterEvent event) {
        event.register(Registries.TEST_FUNCTION, registry -> TESTS.forEach((name, test) -> registry.register(AnvilCraft.of(name), test)));
    }

    @SubscribeEvent
    public static void register(RegisterGameTestsEvent event) {
        var environment = event.registerEnvironment(AnvilCraft.of("port_entity_adapters"));
        TESTS.forEach((name, test) -> event.registerTest(AnvilCraft.of(name), new FunctionGameTestInstance(
            ResourceKey.create(Registries.TEST_FUNCTION, AnvilCraft.of(name)),
            new TestData<>(environment, AnvilCraft.of("port_logistics_empty"), 100, 0, true)
        )));
    }

    private static BlockPos base(GameTestHelper helper) {
        return helper.absolutePos(new BlockPos(2, 16, 4));
    }

    private static CompoundTag tag(String id, Vec3 pos) {
        CompoundTag tag = new CompoundTag();
        tag.putString("id", id);
        return BuildingEntityTransform.withWorldPos(tag, pos);
    }

    private static EntityBuildAdapter.Planned plan(GameTestHelper helper, CompoundTag tag) {
        Entity probe = EntityBuildAdapters.create(tag, helper.getLevel()).orElseThrow();
        var entry = new StructureSnapshot.EntityEntry(probe.position(), probe.blockPosition(), tag);
        return EntityBuildAdapters.find(probe, tag).orElseThrow().plan(helper.getLevel(), entry, tag);
    }

    private static Entity spawn(GameTestHelper helper, EntityBuildAdapter.Planned plan) {
        Entity entity = EntityBuildAdapters.spawn(helper.getLevel(), new BuildingEntityOp(plan.entityNbt(), plan.returned()));
        helper.assertTrue(entity != null, "适配器必须实际生成实体");
        EntityBuildAdapters.insertContents(entity, plan.contents(), helper.getLevel().registryAccess());
        return entity;
    }

    private static void vehicles(GameTestHelper helper) {
        var old = tag("minecraft:chest_boat", base(helper).getCenter());
        old.putString("Type", "bamboo");
        Entity vehicle = EntityBuildAdapters.create(old, helper.getLevel()).orElseThrow();
        helper.assertTrue(vehicle.getType() == EntityType.BAMBOO_CHEST_RAFT && vehicle instanceof Container,
            "旧版竹箱船类型必须映射到原生竹筏实体");
        var cargo = new ItemStack(Items.EMERALD, 3);
        cargo.set(DataComponents.CUSTOM_NAME, Component.literal("cargo"));
        ((Container) vehicle).setItem(6, cargo);
        var saved = BlueprintCapture.saveEntity(vehicle);
        saved.putString("id", "minecraft:chest_boat");
        saved.putString("Type", "bamboo");
        var before = saved.copy();
        var snapshot = new StructureSnapshot(new Vec3i(1, 1, 1), List.of(), List.of(),
            List.of(new StructureSnapshot.EntityEntry(Vec3.ZERO, BlockPos.ZERO, saved)));
        try {
            var parsed = StructureSnapshotCodec.parse(StructureSnapshotCodec.write(snapshot), helper.getLevel().registryAccess());
            helper.assertTrue(parsed.warnings().isEmpty()
                && parsed.snapshot().entities().getFirst().nbt().getStringOr("id", "").equals("minecraft:bamboo_chest_raft"),
                "真实蓝图解析入口必须接受旧船类型并转换为当前注册标识");
        } catch (ConstructionBlueprintException error) {
            throw new IllegalStateException(error);
        }
        var plan = plan(helper, saved);
        helper.assertTrue(plan.material().is(Items.BAMBOO_CHEST_RAFT) && plan.contents().size() == 1
            && plan.contents().getFirst().slot() == 6 && saved.equals(before), "箱船库存必须独立供料且不改变源数据");
        var restored = spawn(helper, plan);
        helper.assertTrue(ItemStack.matches(((Container) restored).getItem(6), cargo)
            && !restored.getUUID().equals(vehicle.getUUID()), "新箱船应恢复库存组件并取得新身份");
        helper.succeed();
    }

    private static void equipment(GameTestHelper helper) {
        final var level = helper.getLevel();
        var stand = EntityType.ARMOR_STAND.create(level, EntitySpawnReason.LOAD);
        stand.setPos(base(helper).getCenter());
        var helmet = new ItemStack(Items.DIAMOND_HELMET);
        helmet.set(DataComponents.CUSTOM_NAME, Component.literal("equipment"));
        stand.setItemSlot(EquipmentSlot.HEAD, helmet);
        var standPlan = plan(helper, BlueprintCapture.saveEntity(stand));
        helper.assertTrue(standPlan.material().is(Items.ARMOR_STAND) && standPlan.contents().size() == 1,
            "盔甲架装备必须与盔甲架材料分离");
        var restored = (ArmorStand) spawn(helper, standPlan);
        helper.assertTrue(ItemStack.matches(restored.getItemBySlot(EquipmentSlot.HEAD), helmet), "盔甲架装备组件必须保留");
        var frame = new ItemFrame(level, base(helper).east(2), Direction.SOUTH);
        frame.setItem(new ItemStack(Items.DIAMOND));
        frame.setRotation(3);
        var framePlan = plan(helper, BlueprintCapture.saveEntity(frame));
        var display = (ItemFrame) spawn(helper, framePlan);
        helper.assertTrue(framePlan.material().is(Items.ITEM_FRAME) && framePlan.contents().size() == 1
            && display.getItem().is(Items.DIAMOND) && display.getRotation() == 3, "展示框物品与旋转必须完整还原");
        helper.succeed();
    }

    private static void motion(GameTestHelper helper) {
        var tag = tag("minecraft:armor_stand", base(helper).getCenter());
        tag.put("Motion", vector(100, 0, 0));
        BlueprintEntities.limitMotion(tag);
        var entity = EntityBuildAdapters.create(tag, helper.getLevel()).orElseThrow();
        helper.assertTrue(Math.abs(entity.getDeltaMovement().x - 15.9) < 0.00001,
            "已限速的蓝图运动必须在原生加载清零之后恢复");
        tag.put("Motion", vector(Double.MAX_VALUE, Double.MAX_VALUE, 0));
        BlueprintEntities.limitMotion(tag);
        entity = EntityBuildAdapters.create(tag, helper.getLevel()).orElseThrow();
        helper.assertTrue(Math.abs(entity.getDeltaMovement().length() - 15.9) < 0.00001, "极大有限速度应稳定归一化而非溢出");
        tag.put("Motion", vector(Double.NaN, 0, 0));
        BlueprintEntities.limitMotion(tag);
        helper.assertTrue(EntityBuildAdapters.create(tag, helper.getLevel()).orElseThrow().getDeltaMovement().equals(Vec3.ZERO),
            "非有限速度应清零");
        helper.assertTrue(EntityBuildAdapters.isTransient(EntityType.MARKER) && EntityBuildAdapters.isTransient(EntityType.EXPERIENCE_ORB)
            && !EntityBuildAdapters.isTransient(EntityType.COW), "瞬态实体必须排除，普通生物不应被排除");
        helper.succeed();
    }

    private static ListTag vector(double x, double y, double z) {
        ListTag values = new ListTag();
        values.add(DoubleTag.valueOf(x));
        values.add(DoubleTag.valueOf(y));
        values.add(DoubleTag.valueOf(z));
        return values;
    }

    private static void leash(GameTestHelper helper) {
        final var tag = tag("minecraft:cow", base(helper).getCenter());
        var legacy = new CompoundTag();
        legacy.putInt("X", 101);
        legacy.putInt("Y", 20);
        legacy.putInt("Z", 100);
        tag.put("Leash", legacy);
        var placement = new BlueprintPlacement(base(helper), Rotation.CLOCKWISE_90, Mirror.NONE);
        BlueprintLeashes.transform(tag, new BlockPos(100, 20, 100), placement);
        var fence = placement.worldOf(new BlockPos(1, 0, 0));
        helper.getLevel().setBlock(fence, Blocks.OAK_FENCE.defaultBlockState(), Block.UPDATE_CLIENTS);
        var cow = EntityBuildAdapters.create(tag, helper.getLevel()).orElseThrow();
        helper.getLevel().addFreshEntity(cow);
        var added = BlueprintLeashes.link(List.of(cow));
        helper.assertTrue(added.size() == 1 && added.getFirst() instanceof LeashFenceKnotEntity
            && ((Leashable) cow).getLeashHolder() == added.getFirst(), "旧栓绳坐标应随蓝图变换并生成真实绳结");
        helper.assertTrue(BlueprintLeashes.link(List.of(cow)).isEmpty(), "已恢复的绳结不能重复生成");
        var saved = new CompoundTag();
        helper.assertTrue(BlueprintLeashes.save(added.getFirst(), saved)
            && fence.equals(BlueprintLeashes.attachment(saved).orElseThrow()), "不可常规保存的绳结也应保留支撑位置");
        helper.assertTrue(BlueprintLeashes.create(saved, helper.getLevel()) instanceof LeashFenceKnotEntity knot
            && knot.getPos().equals(fence), "绳结应从保存数据重建到同一围栏");
        helper.succeed();
    }

    private static void links(GameTestHelper helper) {
        final var level = helper.getLevel();
        var first = EntityType.COW.create(level, EntitySpawnReason.LOAD);
        var second = EntityType.COW.create(level, EntitySpawnReason.LOAD);
        var boat = EntityType.OAK_BOAT.create(level, EntitySpawnReason.LOAD);
        first.setPos(base(helper).getCenter());
        second.setPos(base(helper).east().getCenter());
        boat.setPos(base(helper).getCenter());
        ((Leashable) second).setLeashedTo(first, false);
        var a = BlueprintCapture.saveEntity(first);
        var b = BlueprintCapture.saveEntity(second);
        var c = BlueprintCapture.saveEntity(boat);
        a.store(BlueprintEntities.VEHICLE, UUIDUtil.CODEC, boat.getUUID());
        var entries = List.of(a, b, c).stream().map(data -> new StructureSnapshot.EntityEntry(Vec3.ZERO, BlockPos.ZERO, data)).toList();
        var snapshot = new StructureSnapshot(new Vec3i(1, 1, 1), List.of(), List.of(), entries);
        var one = linkedCopy(helper, snapshot, base(helper));
        var two = linkedCopy(helper, snapshot, base(helper).east(5));
        helper.assertTrue(one.get(0).getVehicle() == one.get(2) && two.get(0).getVehicle() == two.get(2)
            && ((Leashable) one.get(1)).getLeashHolder() == one.get(0)
            && ((Leashable) two.get(1)).getLeashHolder() == two.get(0), "复制体的骑乘和 UUID 栓绳关系不能串到原件或另一份蓝图");
        helper.assertTrue(!one.get(0).getUUID().equals(two.get(0).getUUID()), "两次复制必须有独立实体身份");
        helper.succeed();
    }

    private static List<Entity> linkedCopy(GameTestHelper helper, StructureSnapshot snapshot, BlockPos anchor) {
        var ids = BlueprintEntities.identities(snapshot, new BlueprintPlacement(anchor, Rotation.NONE, Mirror.NONE));
        List<Map.Entry<Entity, CompoundTag>> spawned = new ArrayList<>();
        for (var entry : snapshot.entities()) {
            var data = BuildingEntityTransform.withWorldPos(entry.nbt(), anchor.getCenter());
            BlueprintEntities.scope(data, entry.nbt(), ids);
            var entity = EntityBuildAdapters.spawn(helper.getLevel(), new BuildingEntityOp(data, ItemStack.EMPTY));
            spawned.add(Map.entry(entity, data));
        }
        BlueprintEntities.link(spawned);
        return spawned.stream().map(Map.Entry::getKey).toList();
    }

    private static void material(GameTestHelper helper) {
        final var level = helper.getLevel();
        var cow = EntityType.COW.create(level, EntitySpawnReason.LOAD);
        cow.setPos(base(helper).getCenter());
        cow.setHealth(1);
        cow.setCustomName(Component.literal("blueprint"));
        var plan = plan(helper, BlueprintCapture.saveEntity(cow));
        var player = FakePlayerFactory.get(level, new GameProfile(UUID.randomUUID(), "PortEntityMaterial"));
        var egg = new ItemStack(Items.COW_SPAWN_EGG);
        egg.set(DataComponents.CUSTOM_NAME, Component.literal("actual egg"));
        var fromEgg = BlueprintEntities.fromMaterial(player, egg, plan);
        var spawned = (Mob) EntityBuildAdapters.create(fromEgg.entityNbt(), level).orElseThrow();
        helper.assertTrue(Component.literal("actual egg").equals(spawned.getCustomName()) && spawned.getHealth() > 1,
            "刷怪蛋生成属性必须来自实际供料，不能照抄蓝图生物属性");
        var resin = ModBlocks.RESIN_BLOCK.asStack();
        cow.setHealth(3);
        resin.set(ModComponents.SAVED_ENTITY, SavedEntity.fromEntity(cow));
        resin.set(DataComponents.CUSTOM_NAME, Component.literal("actual resin"));
        var fromResin = BlueprintEntities.fromMaterial(player, resin, plan);
        var restored = (Mob) EntityBuildAdapters.create(fromResin.entityNbt(), level).orElseThrow();
        helper.assertTrue(Component.literal("actual resin").equals(restored.getCustomName()) && restored.getHealth() == 3,
            "树脂应保留实际保存生物及物品名称");
        helper.assertTrue(BlueprintEntities.fromMaterial(player, new ItemStack(Items.PIG_SPAWN_EGG), plan) == null,
            "不同生物类型不能充当所需实体材料");
        helper.succeed();
    }

    private static void dynamic(GameTestHelper helper) {
        final var level = helper.getLevel();
        var pos = base(helper);
        var falling = tag("minecraft:falling_block", pos.getCenter());
        falling.put("BlockState", NbtUtils.writeBlockState(Blocks.SAND.defaultBlockState()));
        falling.putInt("Time", 0);
        var fallingPlan = plan(helper, falling);
        helper.assertTrue(fallingPlan.material().is(Items.SAND) && fallingPlan.entityNbt().getIntOr("Time", 0) == 1,
            "恢复下落方块不能触发 time=0 的起点删除");
        var chest = new ChestBlockEntity(pos, Blocks.CHEST.defaultBlockState());
        var tool = new ItemStack(Items.DIAMOND_PICKAXE);
        var unbreaking = level.registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(Enchantments.UNBREAKING);
        tool.enchant(unbreaking, 3);
        chest.setItem(0, tool);
        var slab = Blocks.STONE_SLAB.defaultBlockState().setValue(BlockStateProperties.SLAB_TYPE, SlabType.DOUBLE);
        var sliding = new SlidingBlockEntity(level, pos, Direction.EAST, List.of(
            Triple.of(pos, chest.getBlockState(), Optional.of(chest)), Triple.of(pos.above(), slab, Optional.empty())));
        var slidingPlan = plan(helper, BlueprintCapture.saveEntity(sliding));
        helper.assertTrue(slidingPlan.contents().stream()
            .anyMatch(slot -> slot.stack().is(Items.STONE_SLAB) && slot.stack().getCount() == 2)
            && slidingPlan.contents().stream().anyMatch(slot -> slot.stack().is(Items.DIAMOND_PICKAXE)
                && slot.stack().get(DataComponents.ENCHANTMENTS).getLevel(unbreaking) == 3),
            "滑动部件材料数量及脱离世界的容器附魔必须完整保留");
        helper.succeed();
    }

    private static void fluidCart(GameTestHelper helper) {
        final var level = helper.getLevel();
        var cart = (FluidTankMinecartEntity) ModEntities.FLUID_TANK_MINECART.get().create(level, EntitySpawnReason.LOAD);
        cart.setPos(base(helper).getCenter());
        try (Transaction transaction = Transaction.openRoot()) {
            cart.getFluidHandler().insert(FluidResource.of(Fluids.WATER), 1234, transaction);
            transaction.commit();
        }
        var plan = plan(helper, BlueprintCapture.saveEntity(cart));
        helper.assertTrue(plan.material().is(ModItems.FLUID_TANK_MINECART.get()) && plan.fluids().size() == 1
            && plan.fluids().getFirst().fluid().getAmount() == 1234, "储罐矿车应单独计入精确流体消耗");
        var restored = (FluidTankMinecartEntity) spawn(helper, plan);
        helper.assertTrue(restored.getFluidHandler().getAmountAsLong(0) == 1234, "已计费流体通过实体数据恢复一次");
        helper.succeed();
    }

    private static void memories(GameTestHelper helper) {
        var data = tag("minecraft:villager", new Vec3(100.5, 20, 100.5));
        var memories = new CompoundTag();
        for (String key : List.of("home", "job_site")) {
            var value = new CompoundTag();
            value.store("pos", BlockPos.CODEC, key.equals("home") ? new BlockPos(101, 20, 100) : new BlockPos(120, 20, 100));
            value.putString("dimension", "minecraft:overworld");
            var memory = new CompoundTag();
            memory.put("value", value);
            memories.put("minecraft:" + key, memory);
        }
        var brain = new CompoundTag();
        brain.put("memories", memories);
        data.put("Brain", brain);
        var entry = new StructureSnapshot.EntityEntry(new Vec3(0.5, 0, 0.5), BlockPos.ZERO, data.copy());
        var snapshot = new StructureSnapshot(new Vec3i(2, 2, 2), List.of(), List.of(), List.of(entry));
        var placement = new BlueprintPlacement(base(helper), Rotation.CLOCKWISE_90, Mirror.NONE);
        BlueprintEntities.relocateMemories(data, entry, snapshot, placement, "minecraft:the_nether");
        var home = memories.getCompoundOrEmpty("minecraft:home").getCompoundOrEmpty("value");
        helper.assertTrue(home.read("pos", BlockPos.CODEC).orElseThrow().equals(placement.worldOf(new BlockPos(1, 0, 0)))
            && home.getStringOr("dimension", "").equals("minecraft:the_nether") && !memories.contains("minecraft:job_site"),
            "内部记忆坐标应旋转并切换维度，外部工作地点不能继续引用旧世界");
        helper.succeed();
    }
}
