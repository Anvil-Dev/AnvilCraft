package dev.dubhe.anvilcraft.porting;

import com.mojang.serialization.JsonOps;
import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.rpc.SettingServerStub;
import dev.dubhe.anvilcraft.saved.setting.PlayerSettings;
import dev.dubhe.anvilcraft.saved.setting.StorageSetting;
import io.netty.buffer.Unpooled;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.FunctionGameTestInstance;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.TestData;
import net.minecraft.resources.ResourceKey;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.registries.RegisterEvent;

import java.lang.reflect.Proxy;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;

@EventBusSubscriber(modid = AnvilCraft.MOD_ID)
public final class StorageFlipTests {
    private static final Map<String, Consumer<GameTestHelper>> TESTS = Map.of(
        "port_storage_flip_codec", StorageFlipTests::codec,
        "port_storage_flip_rpc", StorageFlipTests::rpc
    );

    @SubscribeEvent
    public static void functions(RegisterEvent event) {
        event.register(Registries.TEST_FUNCTION, registry -> TESTS.forEach((name, test) -> registry.register(AnvilCraft.of(name), test)));
    }

    @SubscribeEvent
    public static void register(RegisterGameTestsEvent event) {
        var environment = event.registerEnvironment(AnvilCraft.of("port_storage_flip"));
        TESTS.forEach((name, test) -> event.registerTest(AnvilCraft.of(name), new FunctionGameTestInstance(
            ResourceKey.create(Registries.TEST_FUNCTION, AnvilCraft.of(name)),
            new TestData<>(environment, AnvilCraft.of("port_logistics_empty"), 100, 0, true)
        )));
    }

    private static void codec(GameTestHelper helper) {
        var setting = new StorageSetting();
        setting.setFlipped(true);
        setting.setSearchContent("saved search");
        var json = StorageSetting.CODEC.codec().encodeStart(JsonOps.INSTANCE, setting).getOrThrow();
        helper.assertTrue(StorageSetting.CODEC.codec().parse(JsonOps.INSTANCE, json).getOrThrow().equals(setting), "翻转设置必须完整保存");
        json.getAsJsonObject().remove("flipped");
        helper.assertTrue(!StorageSetting.CODEC.codec().parse(JsonOps.INSTANCE, json).getOrThrow().isFlipped(), "旧设置默认不翻转");
        var buffer = Unpooled.buffer();
        try {
            StorageSetting.STREAM_CODEC.encode(buffer, setting);
            helper.assertTrue(StorageSetting.STREAM_CODEC.decode(buffer).equals(setting), "网络同步不能丢失翻转及其他设置");
        } finally {
            buffer.release();
        }
        helper.succeed();
    }

    private static void rpc(GameTestHelper helper) {
        try (var fixture = new StorageFluidRpcTests.Fixture(helper)) {
            IPayloadContext context = (IPayloadContext) Proxy.newProxyInstance(IPayloadContext.class.getClassLoader(),
                new Class<?>[]{IPayloadContext.class}, (proxy, method, args) -> {
                    if (method.getName().equals("player")) return fixture.player();
                    throw new UnsupportedOperationException(method.getName());
                });
            var method = SettingServerStub.class.getMethod("updateFlipped", UUID.class, boolean.class);
            var validator = new SettingServerStub.OwnSettingValidator();
            helper.assertTrue(!validator.validate(context, method, new Object[]{UUID.randomUUID(), true}), "不得改写其他玩家的布局");
            helper.assertTrue(validator.validate(context, method, new Object[]{fixture.playerId(), true}), "本人布局更新必须通过验证");
            SettingServerStub.updateFlipped(fixture.playerId(), true);
            helper.assertTrue(PlayerSettings.getSetting(helper.getLevel().registryAccess(), fixture.playerId()).storage().isFlipped(),
                "翻转必须更新服务端持久设置");
        } catch (ReflectiveOperationException error) {
            throw new IllegalStateException(error);
        }
        helper.succeed();
    }
}
