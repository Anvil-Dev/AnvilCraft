package dev.dubhe.anvilcraft.porting;

import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.init.item.ModComponents;
import dev.dubhe.anvilcraft.init.item.ModItems;
import dev.dubhe.anvilcraft.item.EquipmentAbilities;
import dev.dubhe.anvilcraft.util.AtmosphereManager;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.FunctionGameTestInstance;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.TestData;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.animal.pig.Pig;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.registries.RegisterEvent;

import java.util.Map;
import java.util.function.Consumer;

@EventBusSubscriber(modid = AnvilCraft.MOD_ID)
public final class BufferBootsTests {
    private static final Map<String, Consumer<GameTestHelper>> TESTS = Map.of(
        "port_boots_charge_release", BufferBootsTests::charge,
        "port_boots_native_jump", BufferBootsTests::jump,
        "port_boots_support", BufferBootsTests::support,
        "port_boots_fluid_surface", BufferBootsTests::fluid,
        "port_boots_bubble_columns", BufferBootsTests::bubbles,
        "port_boots_fall_damage", BufferBootsTests::damage
    );

    @SubscribeEvent
    public static void functions(RegisterEvent event) {
        event.register(Registries.TEST_FUNCTION, registry -> TESTS.forEach((name, test) -> registry.register(AnvilCraft.of(name), test)));
    }

    @SubscribeEvent
    public static void register(RegisterGameTestsEvent event) {
        var environment = event.registerEnvironment(AnvilCraft.of("port_boots"));
        TESTS.forEach((name, test) -> event.registerTest(AnvilCraft.of(name), new FunctionGameTestInstance(
            ResourceKey.create(Registries.TEST_FUNCTION, AnvilCraft.of(name)),
            new TestData<>(environment, AnvilCraft.of("port_logistics_empty"), 100, 0, true)
        )));
    }

    private static void charge(GameTestHelper helper) {
        try (var fixture = new StorageFluidRpcTests.Fixture(helper, true)) {
            var player = fixture.player();
            equip(player, false);
            player.setOnGround(true);
            player.setShiftKeyDown(true);
            ticks(player, 20);
            helper.assertTrue(EquipmentAbilities.chargeTicks(player) == 20, "潜行 20 tick 必须蓄满");
            player.setShiftKeyDown(false);
            ticks(player, 20);
            helper.assertTrue(EquipmentAbilities.chargeTicks(player) == 20 && EquipmentAbilities.isChargeHeld(player),
                "松开潜行应完整保留 20 tick 起跳窗口");
            ticks(player, 5);
            helper.assertTrue(EquipmentAbilities.chargeTicks(player) == 10 && !EquipmentAbilities.isChargeHeld(player),
                "衰减中点必须线性减半");
            player.setShiftKeyDown(true);
            ticks(player, 1);
            helper.assertTrue(EquipmentAbilities.chargeTicks(player) == 11, "重新蓄力必须从衰减后的进度继续");
            player.setShiftKeyDown(false);
            ticks(player, 30);
            helper.assertTrue(EquipmentAbilities.chargeTicks(player) == 0, "保持与衰减结束必须清空");
            player.setShiftKeyDown(true);
            ticks(player, 10);
            player.getItemBySlot(EquipmentSlot.FEET).set(ModComponents.CHARGED_JUMP_ENABLED, false);
            ticks(player, 1);
            player.getItemBySlot(EquipmentSlot.FEET).set(ModComponents.CHARGED_JUMP_ENABLED, true);
            helper.assertTrue(EquipmentAbilities.chargeTicks(player) == 0, "关闭后重新开启不能恢复旧蓄力");
            ticks(player, 10);
            player.setItemSlot(EquipmentSlot.FEET, ItemStack.EMPTY);
            ticks(player, 1);
            equip(player, false);
            helper.assertTrue(EquipmentAbilities.chargeTicks(player) == 0, "脱下装备必须清理蓄力");
        }
        helper.succeed();
    }

    private static void jump(GameTestHelper helper) {
        try (var fixture = new StorageFluidRpcTests.Fixture(helper, true)) {
            var player = fixture.player();
            equip(player, false);
            player.setOnGround(true);
            player.setShiftKeyDown(false);
            player.jumpFromGround();
            double normal = player.getDeltaMovement().y;
            for (double resistance : new double[]{0, 1, 2}) {
                AtmosphereManager.registerDimensionAirResistance(helper.getLevel().dimension(), resistance);
                try {
                    player.setOnGround(true);
                    player.setDeltaMovement(Vec3.ZERO);
                    player.setShiftKeyDown(true);
                    ticks(player, 20);
                    player.setShiftKeyDown(false);
                    ticks(player, 1);
                    player.jumpFromGround();
                    double velocity = player.getDeltaMovement().y;
                    double drag = Math.pow(0.9800000190734863, resistance);
                    double ratio = height(velocity, drag) / height(normal, drag);
                    helper.assertTrue(Math.abs(ratio - 3.5) < 0.00001 && EquipmentAbilities.chargeTicks(player) == 0,
                        "真实起跳必须一次性消耗蓄力，并在不同阻力下达到 3.5 倍高度：" + ratio);
                    player.setDeltaMovement(Vec3.ZERO);
                    player.jumpFromGround();
                    helper.assertTrue(Math.abs(player.getDeltaMovement().y - normal) < 0.000001, "第二次起跳不能重复使用蓄力");
                } finally {
                    AtmosphereManager.clearDimensionAirResistance(helper.getLevel().dimension());
                }
            }
        }
        helper.succeed();
    }

    private static double height(double velocity, double drag) {
        double result = 0;
        while (velocity > 0) {
            result += velocity;
            velocity = (velocity - 0.08) * drag;
        }
        return result;
    }

    private static void support(GameTestHelper helper) {
        try (var fixture = new StorageFluidRpcTests.Fixture(helper, true)) {
            var player = fixture.player();
            equip(player, false);
            helper.setBlock(new BlockPos(2, 1, 2), Blocks.STONE_SLAB);
            BlockPos pos = helper.absolutePos(new BlockPos(2, 1, 2));
            player.setPos(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5);
            player.setOnGround(false);
            player.setDeltaMovement(Vec3.ZERO);
            player.setShiftKeyDown(true);
            ticks(player, 1);
            helper.assertTrue(EquipmentAbilities.chargeTicks(player) == 1, "贴在半砖上即使 onGround 未置位也必须识别支撑");
            player.setPos(player.getX(), player.getY() + 2, player.getZ());
            ticks(player, 1);
            helper.assertTrue(EquipmentAbilities.chargeTicks(player) == 1, "悬空不能继续蓄力");
            player.setOnGround(true);
            player.getAbilities().flying = true;
            ticks(player, 1);
            helper.assertTrue(EquipmentAbilities.chargeTicks(player) == 1, "飞行时不能蓄力");
        }
        helper.succeed();
    }

    private static void fluid(GameTestHelper helper) {
        try (var fixture = new StorageFluidRpcTests.Fixture(helper, true)) {
            var player = fixture.player();
            equip(player, true);
            BlockPos relative = new BlockPos(2, 2, 2);
            BlockPos pos = helper.absolutePos(relative);
            helper.setBlock(relative, Blocks.WATER);
            final var fluid = helper.getLevel().getFluidState(pos);
            player.setPos(pos.getX() + 0.5, pos.getY() + 1.2, pos.getZ() + 0.5);
            player.setShiftKeyDown(false);
            player.move(MoverType.SELF, new Vec3(0, -0.5, 0));
            helper.assertTrue(Math.abs(player.getY() - pos.getY() - fluid.getHeight(helper.getLevel(), pos)) < 0.000001,
                "实际碰撞移动必须停在源流体表面");
            helper.assertTrue(!EquipmentAbilities.canStandOnFluid(player, Fluids.FLOWING_WATER.defaultFluidState()), "流动流体不能承重");
            player.setShiftKeyDown(true);
            ticks(player, 1);
            player.move(MoverType.SELF, new Vec3(0, -0.5, 0));
            player.baseTick();
            helper.assertTrue(player.getY() < pos.getY() + 0.5, "潜行必须能穿过液面");
            player.setDeltaMovement(Vec3.ZERO);
            ticks(player, 1);
            helper.assertTrue(player.getDeltaMovement().y <= -0.08, "水中持续潜行应加速下沉");
            player.setShiftKeyDown(false);
            ticks(player, 1);
            helper.assertTrue(!EquipmentAbilities.canStandOnFluid(player, fluid), "水中松开潜行不能重新生成托举碰撞面");
            player.setPos(pos.getX() + 3.5, pos.getY() + 3, pos.getZ() + 0.5);
            player.baseTick();
            ticks(player, 1);
            helper.assertTrue(EquipmentAbilities.canStandOnFluid(player, fluid), "离开流体后必须恢复液面行走");
            helper.setBlock(relative.above(), Blocks.WATER);
            player.setPos(pos.getX() + 0.5, pos.getY() + 1.2, pos.getZ() + 0.5);
            helper.assertTrue(helper.getLevel().getBlockState(pos).getCollisionShape(helper.getLevel(), pos,
                CollisionContext.of(player)).isEmpty(), "流体内部不能生成碰撞面");
            equip(player, false);
            helper.assertTrue(!EquipmentAbilities.canStandOnFluid(player, fluid), "普通缓冲靴不提供液面行走");
        }
        helper.succeed();
    }

    private static void bubbles(GameTestHelper helper) {
        try (var fixture = new StorageFluidRpcTests.Fixture(helper, true)) {
            var player = fixture.player();
            equip(player, true);
            player.setShiftKeyDown(true);
            player.setDeltaMovement(Vec3.ZERO);
            player.onInsideBubbleColumn(false);
            helper.assertTrue(player.getDeltaMovement().y < 0, "液体内部上升气泡柱必须支持潜行下沉");
            player.setDeltaMovement(Vec3.ZERO);
            player.onAboveBubbleColumn(false, player.blockPosition());
            helper.assertTrue(player.getDeltaMovement().y < 0, "液面上方气泡柱必须支持潜行下沉");
            player.setShiftKeyDown(false);
            player.setDeltaMovement(Vec3.ZERO);
            player.onInsideBubbleColumn(false);
            helper.assertTrue(player.getDeltaMovement().y > 0, "不潜行时应保留气泡柱上升");
        }
        helper.succeed();
    }

    private static void damage(GameTestHelper helper) {
        for (var boots : new ItemStack[]{ModItems.BUFFER_BOOTS.asStack(), ModItems.WEATHERPROOF_SPACESUIT_BOOTS.asStack()}) {
            var pig = new Pig(EntityType.PIG, helper.getLevel());
            pig.setItemSlot(EquipmentSlot.FEET, boots);
            float health = pig.getHealth();
            pig.hurtServer(helper.getLevel(), pig.damageSources().fall(), 4);
            helper.assertTrue(pig.getHealth() == health, "两种靴子都必须取消实际摔落伤害");
            pig.setItemSlot(EquipmentSlot.FEET, new ItemStack(Items.IRON_BOOTS));
            pig.hurtServer(helper.getLevel(), pig.damageSources().fall(), 4);
            helper.assertTrue(pig.getHealth() < health, "普通靴子不能获得摔落免疫");
        }
        helper.succeed();
    }

    private static void equip(Player player, boolean weatherproof) {
        if (player instanceof net.minecraft.server.level.ServerPlayer serverPlayer) serverPlayer.setGameMode(GameType.SURVIVAL);
        player.getAbilities().flying = false;
        player.setItemSlot(EquipmentSlot.FEET,
            weatherproof ? ModItems.WEATHERPROOF_SPACESUIT_BOOTS.asStack() : ModItems.BUFFER_BOOTS.asStack());
    }

    private static void ticks(Player player, int count) {
        for (int i = 0; i < count; i++) EquipmentAbilities.beforeTick(new PlayerTickEvent.Pre(player));
    }
}
