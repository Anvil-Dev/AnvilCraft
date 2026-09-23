package dev.dubhe.anvilcraft.client.building;

import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.item.property.component.StructureDiskData;
import io.netty.buffer.Unpooled;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.FunctionGameTestInstance;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.TestData;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceKey;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;
import net.neoforged.neoforge.registries.RegisterEvent;

import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;

@EventBusSubscriber(modid = AnvilCraft.MOD_ID)
public final class BuildingClientStateTests {
    private static final Map<String, Consumer<GameTestHelper>> TESTS = Map.of(
        "port_building_motion", BuildingClientStateTests::motion,
        "port_building_projection_animation", BuildingClientStateTests::animation,
        "port_building_key_repeat", BuildingClientStateTests::repeat,
        "port_building_disk_auto_rotation", BuildingClientStateTests::disk
    );

    @SubscribeEvent
    public static void functions(RegisterEvent event) {
        event.register(Registries.TEST_FUNCTION, registry -> TESTS.forEach((name, test) -> registry.register(AnvilCraft.of(name), test)));
    }

    @SubscribeEvent
    public static void register(RegisterGameTestsEvent event) {
        var environment = event.registerEnvironment(AnvilCraft.of("port_building_client_state"));
        TESTS.forEach((name, test) -> event.registerTest(AnvilCraft.of(name), new FunctionGameTestInstance(
            ResourceKey.create(Registries.TEST_FUNCTION, AnvilCraft.of(name)),
            new TestData<>(environment, AnvilCraft.of("port_logistics_empty"), 100, 0, true)
        )));
    }

    private static float angle(int fps) {
        var motion = new BuildingRodMotion();
        motion.sample(0, true, false, false);
        BuildingRodMotion.Pose pose = null;
        for (int frame = 1; frame <= fps * 2; frame++) pose = motion.sample((double) frame / fps, true, false, false);
        return pose.angle();
    }

    private static void motion(GameTestHelper helper) {
        double expected = 180 - 18 * (1 - Math.exp(-10));
        helper.assertTrue(Math.abs(angle(20) - expected) < 0.0001 && Math.abs(angle(144) - expected) < 0.0001,
            "核心速度积分在不同帧率下保持相同两秒角度");
        var motion = new BuildingRodMotion();
        motion.placed(1);
        helper.assertTrue(Math.abs(motion.sample(1.056, true, true, false).kick() - 1) < 0.0001,
            "放置推送在 0.056 秒到达峰值");
        helper.assertTrue(motion.sample(1.28, true, true, false).kick() < 0.0001, "放置推送在 0.28 秒结束");
        motion.knock(2);
        helper.assertTrue(Math.abs(motion.sample(2.1, true, false, false).knock() + 0.22) < 0.0001, "敲击保留负向蓄势");
        helper.assertTrue(Math.abs(motion.sample(2.2, true, false, false).knock() - 1) < 0.0001, "敲击在 0.2 秒冲击");
        motion.stopKnock();
        helper.assertTrue(!motion.isKnocking(2.21) && motion.sample(2.21, false, false, false).knock() == 0,
            "取消敲击立即清除姿态");
        helper.succeed();
    }

    private static void animation(GameTestHelper helper) {
        var animation = new BuildingRodAnimation();
        animation.sample(0, 0, 0, 0, 1, 1, 0);
        animation.turn(3);
        var pose = animation.sample(10, 0, 0, 90, -1, 1, 50_000_000);
        helper.assertTrue(pose.yaw() < 0 && pose.x() > 0 && pose.x() < 10, "连续转三次必须保持负向旋转而非折返");
        var mirrored = new BuildingRodAnimation();
        mirrored.sample(0, 0, 0, 0, 1, 1, 0);
        pose = mirrored.sample(0, 0, 0, 0, -1, 1, (long) (Math.log(2) / 18 * 1_000_000_000));
        helper.assertTrue(Math.abs(pose.mirrorX()) >= 0.001f && Float.isFinite(1 / pose.mirrorX()), "镜像经过薄面时法线矩阵不退化");
        animation.clear();
        pose = animation.sample(20, 30, 40, 180, -1, -1, 60_000_000);
        helper.assertTrue(pose.x() == 20 && pose.y() == 30 && pose.yaw() == 180, "新会话不继承旧投影插值");
        helper.succeed();
    }

    private static void repeat(GameTestHelper helper) {
        var repeat = new BuildingRodKeyRepeat();
        helper.assertTrue(repeat.update(0, true, 0) == 1 && !repeat.press(0, 0), "首击立即执行且事件与轮询去重");
        helper.assertTrue(repeat.update(0, true, 179_000_000) == 0 && repeat.update(0, true, 180_000_000) == 1,
            "连移保留 180 毫秒初始延迟");
        helper.assertTrue(repeat.update(0, true, 10_000_000_000L) == 4 && repeat.update(0, true, 10_000_000_000L) == 0,
            "卡顿最多补四步并丢弃过期积压");
        helper.assertTrue(repeat.update(6, true, 0) == 1 && repeat.update(6, true, 10_000_000_000L) == 0, "旋转不跟随长按连发");
        repeat.release(0);
        helper.assertTrue(repeat.update(0, true, 10_000_000_001L) == 1, "松开后下一次按下重新立即执行");
        repeat.clear();
        helper.assertTrue(!repeat.isHeld(0) && !repeat.isHeld(6), "关闭会话清除所有按键状态");
        helper.succeed();
    }

    private static void disk(GameTestHelper helper) {
        var data = new StructureDiskData("test_" + UUID.randomUUID() + ".nbt", "Disk", UUID.randomUUID(),
            Direction.EAST, 3, 4, 5, true, false);
        var tag = (CompoundTag) StructureDiskData.CODEC.encodeStart(NbtOps.INSTANCE, data).getOrThrow();
        helper.assertTrue(StructureDiskData.CODEC.parse(NbtOps.INSTANCE, tag).getOrThrow().equals(data), "自动旋转关闭状态应存档往返");
        tag.remove("autoRotate");
        helper.assertTrue(StructureDiskData.CODEC.parse(NbtOps.INSTANCE, tag).getOrThrow().autoRotate(), "旧结构盘默认自动旋转");
        var buffer = new FriendlyByteBuf(Unpooled.buffer());
        try {
            StructureDiskData.STREAM_CODEC.encode(buffer, data);
            helper.assertTrue(StructureDiskData.STREAM_CODEC.decode(buffer).equals(data), "网络同步保留自动旋转关闭状态");
        } finally {
            buffer.release();
        }
        helper.succeed();
    }
}
