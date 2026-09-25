package dev.dubhe.anvilcraft.porting;

import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.api.item.IFullCapacitor;
import dev.dubhe.anvilcraft.init.item.ModComponents;
import dev.dubhe.anvilcraft.init.item.ModItems;
import dev.dubhe.anvilcraft.item.property.component.StoredEnergy;
import dev.dubhe.anvilcraft.item.weapon.EnergyWeaponItem;
import dev.dubhe.anvilcraft.item.weapon.EnergyWeaponReload;
import dev.dubhe.anvilcraft.network.EnergyWeaponReloadPacket;
import io.netty.buffer.Unpooled;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.FunctionGameTestInstance;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.TestData;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.inventory.ClickAction;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomModelData;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.registries.RegisterEvent;

import java.lang.reflect.Field;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;

@EventBusSubscriber(modid = AnvilCraft.MOD_ID)
public final class EnergyReloadTests {
    private static final Map<String, Consumer<GameTestHelper>> TESTS = Map.of(
        "port_reload_timing", EnergyReloadTests::timing,
        "port_reload_cancel", EnergyReloadTests::cancel,
        "port_reload_force_slot", EnergyReloadTests::forceSlot,
        "port_reload_guards", EnergyReloadTests::guards,
        "port_reload_recovery", EnergyReloadTests::recovery,
        "port_reload_packet", EnergyReloadTests::packet,
        "port_reload_source_selection", EnergyReloadTests::automatic,
        "port_reload_energy_models", EnergyReloadTests::models
    );

    @SubscribeEvent
    public static void functions(RegisterEvent event) {
        event.register(Registries.TEST_FUNCTION, registry -> TESTS.forEach((name, test) -> registry.register(AnvilCraft.of(name), test)));
    }

    @SubscribeEvent
    public static void register(RegisterGameTestsEvent event) {
        var environment = event.registerEnvironment(AnvilCraft.of("port_energy_reload"));
        TESTS.forEach((name, test) -> event.registerTest(AnvilCraft.of(name), new FunctionGameTestInstance(
            ResourceKey.create(Registries.TEST_FUNCTION, AnvilCraft.of(name)),
            new TestData<>(environment, AnvilCraft.of("port_logistics_empty"), 100, 0, true)
        )));
    }

    private static ItemStack equip(ServerPlayer player, InteractionHand hand) {
        player.getInventory().setSelectedSlot(0);
        ItemStack weapon = ModItems.LASER_GUN.asStack();
        weapon.set(ModComponents.STORED_ENERGY, StoredEnergy.EMPTY);
        player.setItemInHand(hand, weapon);
        return weapon;
    }

    private static boolean start(ServerPlayer player, ItemStack weapon, ItemStack capacitor) {
        return EnergyWeaponReload.start(player, weapon, (IFullCapacitor) capacitor.getItem(), capacitor, false, null);
    }

    private static void tick(ServerPlayer player, int count) {
        for (int i = 0; i < count; i++) EnergyWeaponReload.tick(new PlayerTickEvent.Post(player));
    }

    private static int energy(ItemStack stack) {
        return stack.getOrDefault(ModComponents.STORED_ENERGY, StoredEnergy.EMPTY).value();
    }

    private static void finish(ServerPlayer player) {
        EnergyWeaponReload.logout(new PlayerEvent.PlayerLoggedOutEvent(player));
    }

    private static void timing(GameTestHelper helper) {
        for (InteractionHand hand : InteractionHand.values()) {
            try (var fixture = new StorageFluidRpcTests.Fixture(helper, true)) {
                var player = fixture.player();
                var weapon = equip(player, hand);
                var capacitor = ModItems.CAPACITOR.asStack(2);
                helper.assertTrue(start(player, weapon, capacitor) && capacitor.getCount() == 1, "开始应暂存一枚电容");
                tick(player, 23);
                helper.assertTrue(energy(weapon) == 0 && player.getInventory().countItem(ModItems.CAPACITOR_EMPTY.get()) == 0,
                    "第 24 tick 前不能充能或提前返还空电容");
                tick(player, 1);
                helper.assertTrue(energy(weapon) == 8000000 && EnergyWeaponReload.isReloading(player, weapon), "第 24 tick 应充能并继续动画");
                tick(player, 16);
                helper.assertTrue(!EnergyWeaponReload.isReloading(player, weapon)
                    && player.getInventory().countItem(ModItems.CAPACITOR_EMPTY.get()) == 1, "第 40 tick 应结束并且只返还一个空电容");
                finish(player);
            }
        }
        helper.succeed();
    }

    private static void cancel(GameTestHelper helper) {
        for (int ticks : new int[]{0, 23, 24, 39}) {
            try (var fixture = new StorageFluidRpcTests.Fixture(helper, true)) {
                var player = fixture.player();
                var weapon = equip(player, InteractionHand.MAIN_HAND);
                var capacitor = ModItems.CAPACITOR.asStack();
                capacitor.set(DataComponents.CUSTOM_NAME, Component.literal("escrow-name"));
                helper.assertTrue(start(player, weapon, capacitor), "应开始换电");
                tick(player, ticks);
                player.getInventory().setSelectedSlot(1);
                EnergyWeaponReload.checkHolding(new PlayerTickEvent.Pre(player));
                var returned = ticks < 24 ? ModItems.CAPACITOR.get() : ModItems.CAPACITOR_EMPTY.get();
                helper.assertTrue(player.getInventory().countItem(returned) == 1 && energy(weapon) == (ticks < 24 ? 0 : 8000000),
                    "切换槽位取消应根据充能前后返还正确物品");
                helper.assertTrue(player.getInventory().getNonEquipmentItems().stream().anyMatch(stack -> stack.is(returned)
                    && Component.literal("escrow-name").equals(stack.get(DataComponents.CUSTOM_NAME))), "返还必须保留电容组件");
                finish(player);
                helper.assertTrue(player.getInventory().countItem(returned) == 1, "重复结束不能再次返还");
            }
        }
        helper.succeed();
    }

    private static void forceSlot(GameTestHelper helper) {
        try (var fixture = new StorageFluidRpcTests.Fixture(helper, true)) {
            var player = fixture.player();
            var weapon = equip(player, InteractionHand.MAIN_HAND);
            weapon.set(ModComponents.STORED_ENERGY, new StoredEnergy(EnergyWeaponItem.MAX_ENERGY - 100));
            var capacitor = ModItems.CAPACITOR.asStack();
            helper.assertTrue(IFullCapacitor.tryForceChargeTarget((IFullCapacitor) capacitor.getItem(), capacitor,
                player.inventoryMenu.getSlot(36), ClickAction.SECONDARY, player), "真实快捷栏槽应允许强制换电");
            tick(player, 24);
            helper.assertTrue(energy(weapon) == EnergyWeaponItem.MAX_ENERGY, "强制换电允许截断到容量上限");
            player.inventoryMenu.getSlot(36).set(weapon.copy());
            EnergyWeaponReload.checkHolding(new PlayerTickEvent.Pre(player));
            helper.assertTrue(player.getInventory().countItem(ModItems.CAPACITOR_EMPTY.get()) == 1, "目标栈身份改变后必须终止并返还空电容");
            finish(player);
        }
        helper.succeed();
    }

    private static void guards(GameTestHelper helper) {
        try (var fixture = new StorageFluidRpcTests.Fixture(helper, true)) {
            var player = fixture.player();
            var weapon = equip(player, InteractionHand.MAIN_HAND);
            var capacitor = ModItems.CAPACITOR.asStack(3);
            helper.assertTrue(!start(player, weapon.copy(), capacitor), "不在手中的相同武器不能开始换电");
            helper.assertTrue(start(player, weapon, capacitor) && !start(player, weapon, capacitor) && capacitor.getCount() == 2,
                "同一玩家的重复请求不能多扣电容");
            weapon.set(ModComponents.STORED_ENERGY, new StoredEnergy(EnergyWeaponItem.MAX_ENERGY));
            tick(player, 24);
            helper.assertTrue(player.getInventory().countItem(ModItems.CAPACITOR.get()) == 1
                && !EnergyWeaponReload.isReloading(player, weapon), "提交前被其它来源充满时应返还满电容");
            finish(player);
        }
        helper.succeed();
    }

    private static void recovery(GameTestHelper helper) {
        for (int ticks : new int[]{0, 24}) {
            try (var fixture = new StorageFluidRpcTests.Fixture(helper, true)) {
                var player = fixture.player();
                final var weapon = equip(player, InteractionHand.OFF_HAND);
                helper.assertTrue(start(player, weapon, ModItems.SUPER_CAPACITOR.asStack()), "应开始超级电容换电");
                tick(player, ticks);
                try {
                    Field field = EnergyWeaponReload.class.getDeclaredField("SESSIONS");
                    field.setAccessible(true);
                    ((Map<UUID, ?>) field.get(null)).remove(player.getUUID());
                } catch (ReflectiveOperationException error) {
                    throw new IllegalStateException(error);
                }
                EnergyWeaponReload.login(new PlayerEvent.PlayerLoggedInEvent(player));
                EnergyWeaponReload.login(new PlayerEvent.PlayerLoggedInEvent(player));
                var returned = ticks == 0 ? ModItems.SUPER_CAPACITOR.get() : ModItems.SUPER_CAPACITOR_EMPTY.get();
                helper.assertTrue(player.getInventory().countItem(returned) == 1, "会话丢失后的持久化恢复必须恰好返还一次");
            }
        }
        try (var fixture = new StorageFluidRpcTests.Fixture(helper, true)) {
            var player = fixture.player();
            var weapon = equip(player, InteractionHand.MAIN_HAND);
            start(player, weapon, ModItems.CAPACITOR.asStack());
            EnergyWeaponReload.death(new LivingDeathEvent(player, player.damageSources().generic()));
            helper.assertTrue(player.getInventory().countItem(ModItems.CAPACITOR.get()) == 1, "死亡路径必须释放暂存电容");
        }
        helper.succeed();
    }

    private static void packet(GameTestHelper helper) {
        var buffer = new RegistryFriendlyByteBuf(Unpooled.buffer(), helper.getLevel().registryAccess());
        try {
            var packet = new EnergyWeaponReloadPacket(ModItems.TESLA_GUN.asStack(), ModItems.SUPER_CAPACITOR_EMPTY.asStack(), 40, 24);
            EnergyWeaponReloadPacket.STREAM_CODEC.encode(buffer, packet);
            var restored = EnergyWeaponReloadPacket.STREAM_CODEC.decode(buffer);
            helper.assertTrue(restored.slot() == 40 && restored.ticks() == 24
                && restored.weapon().is(ModItems.TESLA_GUN) && restored.capacitor().is(ModItems.SUPER_CAPACITOR_EMPTY),
                "换电阶段、槽位和物品应完整同步");
        } finally {
            buffer.release();
        }
        helper.succeed();
    }

    private static void automatic(GameTestHelper helper) {
        try (var fixture = new StorageFluidRpcTests.Fixture(helper, true)) {
            var player = fixture.player();
            final var weapon = equip(player, InteractionHand.OFF_HAND);
            player.getInventory().setItem(0, ItemStack.EMPTY);
            player.setItemSlot(net.minecraft.world.entity.EquipmentSlot.LEGS, ModItems.POCKETS_LEGGINGS.asStack());
            var source = ModItems.CAPACITOR.asStack(2);
            player.inventoryMenu.getSlot(46).set(source);
            ((IFullCapacitor) source.getItem()).inventoryTick(source, player);
            helper.assertTrue(source.getCount() == 1 && EnergyWeaponReload.isReloading(player, weapon), "口袋电容也应为副手武器启动自动换电");
            finish(player);
        }
        helper.succeed();
    }

    private static void models(GameTestHelper helper) {
        try (var fixture = new StorageFluidRpcTests.Fixture(helper, true)) {
            var player = fixture.player();
            var weapon = equip(player, InteractionHand.MAIN_HAND);
            ModItems.LASER_GUN.get().use(helper.getLevel(), player, InteractionHand.MAIN_HAND);
            var data = weapon.getOrDefault(DataComponents.CUSTOM_MODEL_DATA, CustomModelData.EMPTY);
            helper.assertTrue(!data.floats().isEmpty() && data.floats().getFirst() == 1, "电量不足时应选择耗尽模型");
            start(player, weapon, ModItems.CAPACITOR.asStack());
            tick(player, 24);
            helper.assertTrue(weapon.get(DataComponents.CUSTOM_MODEL_DATA).equals(CustomModelData.EMPTY), "成功换电应恢复可用模型");
            helper.assertTrue(ModItems.LASER_GUN.get().getBarWidth(weapon) >= 1, "非零能量必须显示至少一像素能量条");
            finish(player);
        }
        helper.succeed();
    }
}
