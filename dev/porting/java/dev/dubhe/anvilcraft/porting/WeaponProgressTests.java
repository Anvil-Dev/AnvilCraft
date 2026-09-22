package dev.dubhe.anvilcraft.porting;

import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.init.item.ModComponents;
import dev.dubhe.anvilcraft.init.item.ModItems;
import dev.dubhe.anvilcraft.item.tool.ResonateMode;
import dev.dubhe.anvilcraft.item.weapon.AnvilRailgunItem;
import dev.dubhe.anvilcraft.item.weapon.LaserGunItem;
import dev.dubhe.anvilcraft.network.WeaponChargeProgressPacket;
import io.netty.buffer.Unpooled;
import io.netty.channel.embedded.EmbeddedChannel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.FunctionGameTestInstance;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.TestData;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.network.protocol.common.ClientboundCustomPayloadPacket;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.monster.zombie.Zombie;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.component.ChargedProjectiles;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.BlockHitResult;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;
import net.neoforged.neoforge.registries.RegisterEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

@EventBusSubscriber(modid = AnvilCraft.MOD_ID)
public final class WeaponProgressTests {
    private static final Map<String, Consumer<GameTestHelper>> TESTS = Map.of(
        "port_weapon_progress_prediction", WeaponProgressTests::prediction,
        "port_weapon_progress_codec", WeaponProgressTests::codec,
        "port_weapon_progress_throttle", WeaponProgressTests::throttle,
        "port_weapon_progress_railgun", WeaponProgressTests::railgun,
        "port_weapon_progress_laser", WeaponProgressTests::laser,
        "port_weapon_progress_resonator", WeaponProgressTests::resonator
    );

    @SubscribeEvent
    public static void functions(RegisterEvent event) {
        event.register(Registries.TEST_FUNCTION, registry -> TESTS.forEach((name, test) -> registry.register(AnvilCraft.of(name), test)));
    }

    @SubscribeEvent
    public static void register(RegisterGameTestsEvent event) {
        var environment = event.registerEnvironment(AnvilCraft.of("port_weapon_progress"));
        TESTS.forEach((name, test) -> event.registerTest(AnvilCraft.of(name), new FunctionGameTestInstance(
            ResourceKey.create(Registries.TEST_FUNCTION, AnvilCraft.of(name)),
            new TestData<>(environment, AnvilCraft.of("port_logistics_empty"), 100, 0, true)
        )));
    }

    private static void prediction(GameTestHelper helper) {
        var normal = new WeaponChargeProgressPacket(1, 0, 50, 100, false, 20);
        helper.assertTrue(Math.abs(normal.progressAfter(50) - 0.51F) < 0.00001
            && Math.abs(normal.progressAfter(10000) - 0.52F) < 0.00001, "正常速率下预测最多领先两 tick");
        helper.assertTrue(normal.progressAfter(-100) == 0.5F, "时钟倒退不能减少已收到进度");
        var slow = new WeaponChargeProgressPacket(1, 0, 50, 100, false, 4);
        var fast = new WeaponChargeProgressPacket(1, 0, 50, 100, false, 200);
        helper.assertTrue(Math.abs(slow.progressAfter(10000) - 0.52F) < 0.00001 && fast.progressAfter(10000) == 0.7F,
            "低速按两 tick、高速按 100ms 限制预测");
        helper.assertTrue(new WeaponChargeProgressPacket(1, 0, 99, 100, true, 20).progressAfter(100) == 0.01F
            && new WeaponChargeProgressPacket(1, 0, 99, 100, false, 20).progressAfter(100) == 1,
            "循环进度应越界回绕，单次进度应保持满格");
        helper.assertTrue(new WeaponChargeProgressPacket(1, 0, 10, 0, false, 20).progressAfter(100) == -1,
            "装填或无目标样本必须隐藏进度");
        helper.succeed();
    }

    private static void codec(GameTestHelper helper) {
        var buffer = Unpooled.buffer();
        try {
            var sample = new WeaponChargeProgressPacket(123, 1, 99, 100, true, 7.5F);
            WeaponChargeProgressPacket.STREAM_CODEC.encode(buffer, sample);
            helper.assertTrue(sample.equals(WeaponChargeProgressPacket.STREAM_CODEC.decode(buffer)), "完整保留手、阶段和实际 tick 速率");
        } finally {
            buffer.release();
        }
        helper.succeed();
    }

    private static void throttle(GameTestHelper helper) {
        float previousRate = helper.getLevel().tickRateManager().tickrate();
        try (var capture = new Capture(helper)) {
            var player = capture.fixture.player();
            final var stack = capture.hold(ModItems.LASER_GUN.asStack());
            helper.getLevel().tickRateManager().setTickRate(100);
            player.tickCount = 1;
            WeaponChargeProgressPacket.sync(player, stack, 2, 100, true);
            helper.assertTrue(capture.samples.isEmpty(), "高 tick 速率下应节流普通样本");
            player.tickCount = 5;
            WeaponChargeProgressPacket.sync(player, stack, 2, 100, true);
            player.tickCount = 6;
            WeaponChargeProgressPacket.sync(player, stack, 1, 100, true);
            WeaponChargeProgressPacket.sync(player, stack, 100, 100, true);
            WeaponChargeProgressPacket.sync(player, stack.copy(), 1, 100, true);
            helper.assertTrue(capture.samples.size() == 3 && capture.last().tickRate() == 100,
                "常规节流、起始和周期边界均应发送，非当前使用栈不能冒充");
        } finally {
            helper.getLevel().tickRateManager().setTickRate(previousRate);
        }
        helper.succeed();
    }

    private static void railgun(GameTestHelper helper) {
        try (var capture = new Capture(helper)) {
            var player = capture.fixture.player();
            final var stack = capture.hold(ModItems.ANVIL_RAILGUN.asStack());
            stack.set(ModComponents.RAILGUN_AMMO, ChargedProjectiles.of(new ItemStackTemplate(Blocks.ANVIL.asItem())));
            ModItems.ANVIL_RAILGUN.get().onUseTick(helper.getLevel(), player, stack, stack.getUseDuration(player) - 5);
            helper.assertTrue(capture.last().elapsed() == 5
                && capture.last().duration() == AnvilRailgunItem.fullChargeTicks(helper.getLevel(), stack)
                && capture.last().repeating(), "磁轨炮真实使用入口必须同步服务器蓄力");
            stack.set(ModComponents.RAILGUN_AMMO, ChargedProjectiles.EMPTY);
            player.setItemInHand(InteractionHand.OFF_HAND, new ItemStack(Blocks.ANVIL));
            ModItems.ANVIL_RAILGUN.get().onUseTick(helper.getLevel(), player, stack, stack.getUseDuration(player) - 6);
            helper.assertTrue(capture.last().duration() == 0, "磁轨炮转入装填时应清除旧蓄力显示");
        }
        helper.succeed();
    }

    private static void laser(GameTestHelper helper) {
        try (var capture = new Capture(helper)) {
            var player = capture.fixture.player();
            final var stack = capture.hold(ModItems.LASER_GUN.asStack());
            BlockPos air = helper.absolutePos(new BlockPos(1, 15, 1));
            player.setPos(air.getX() + 0.5, air.getY(), air.getZ() + 0.5);
            player.setYRot(0);
            player.setXRot(0);
            player.setOldPosAndRot();
            var target = new Zombie(EntityType.ZOMBIE, helper.getLevel());
            target.setPos(player.getX(), player.getY(), player.getZ() + 4);
            target.setNoAi(true);
            helper.getLevel().addFreshEntity(target);
            for (int elapsed = 1; elapsed <= 5; elapsed++) {
                ModItems.LASER_GUN.get().onUseTick(helper.getLevel(), player, stack, stack.getUseDuration(player) - elapsed);
            }
            helper.assertTrue(capture.last().elapsed() == 5 && capture.last().duration() == 100 && capture.last().repeating(),
                "激光实体目标阶段必须来自服务器累计时间：" + capture.last());
            target.discard();
            ModItems.LASER_GUN.get().onUseTick(helper.getLevel(), player, stack, stack.getUseDuration(player) - 6);
            helper.assertTrue(capture.last().duration() == 0, "目标消失且无矿脉时必须隐藏进度");
            BlockPos ore = BlockPos.containing(player.getEyePosition().add(0, 0, 4));
            helper.getLevel().setBlockAndUpdate(ore, Blocks.DIAMOND_ORE.defaultBlockState());
            ModItems.LASER_GUN.get().onUseTick(helper.getLevel(), player, stack, stack.getUseDuration(player) - 7);
            helper.assertTrue(capture.last().duration() == LaserGunItem.miningPeriod(helper.getLevel(), stack)
                && capture.last().elapsed() == 1, "采矿必须切换到服务器矿脉周期");
            LaserGunItem.clearState(player.getUUID());
        }
        helper.succeed();
    }

    private static void resonator(GameTestHelper helper) {
        try (var capture = new Capture(helper)) {
            var player = capture.fixture.player();
            final var stack = capture.hold(ModItems.TRANSCENDENCE_RESONATOR.asStack());
            stack.set(ModComponents.RESONATE_MODE, ResonateMode.AUTO);
            BlockPos air = helper.absolutePos(new BlockPos(1, 15, 1));
            player.setPos(air.getX() + 0.5, air.getY(), air.getZ() + 0.5);
            player.setYRot(0);
            player.setXRot(0);
            player.setOldPosAndRot();
            BlockPos pos = BlockPos.containing(player.getEyePosition().add(0, 0, 2));
            helper.getLevel().setBlockAndUpdate(pos, Blocks.STONE.defaultBlockState());
            var hit = new BlockHitResult(pos.getCenter(), Direction.NORTH, pos, false);
            ModItems.TRANSCENDENCE_RESONATOR.get().useOn(new UseOnContext(player, InteractionHand.MAIN_HAND, hit));
            ModItems.TRANSCENDENCE_RESONATOR.get().onUseTick(helper.getLevel(), player, stack, stack.getUseDuration(player) - 1);
            helper.assertTrue(capture.last().elapsed() == 1 && capture.last().duration() > 0 && !capture.last().repeating(),
                "现有共振采掘应同步单次服务器进度");
        }
        helper.succeed();
    }

    private static final class Capture implements AutoCloseable {
        private final StorageFluidRpcTests.Fixture fixture;
        private final ServerGamePacketListenerImpl previous;
        private final EmbeddedChannel channel;
        private final List<WeaponChargeProgressPacket> samples = new ArrayList<>();

        private Capture(GameTestHelper helper) {
            this.fixture = new StorageFluidRpcTests.Fixture(helper, true);
            var player = this.fixture.player();
            this.previous = player.connection;
            var connection = new Connection(PacketFlow.SERVERBOUND);
            this.channel = new EmbeddedChannel(connection);
            player.connection = new ServerGamePacketListenerImpl(helper.getLevel().getServer(), connection, player,
                CommonListenerCookie.createInitial(player.getGameProfile(), false)) {
                @Override
                public void send(Packet<?> packet) {
                    if (packet instanceof ClientboundCustomPayloadPacket custom
                        && custom.payload() instanceof WeaponChargeProgressPacket sample) {
                        Capture.this.samples.add(sample);
                    }
                }
            };
        }

        private ItemStack hold(ItemStack stack) {
            var player = this.fixture.player();
            player.getInventory().setSelectedSlot(0);
            player.setItemInHand(InteractionHand.MAIN_HAND, stack);
            player.startUsingItem(InteractionHand.MAIN_HAND);
            return stack;
        }

        private WeaponChargeProgressPacket last() {
            return this.samples.getLast();
        }

        @Override
        public void close() {
            this.fixture.player().stopUsingItem();
            this.fixture.player().connection = this.previous;
            this.channel.finishAndReleaseAll();
            this.fixture.close();
        }
    }
}
