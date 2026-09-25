package dev.dubhe.anvilcraft.porting;

import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.client.gui.component.StructureScannerButtonState;
import dev.dubhe.anvilcraft.network.StructureScannerFilePacket;
import dev.dubhe.anvilcraft.network.StructureScannerFileResultPacket;
import dev.dubhe.anvilcraft.network.StructureScannerSavePacket;
import dev.dubhe.anvilcraft.util.StructureFileTransfer;
import io.netty.buffer.Unpooled;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.FunctionGameTestInstance;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.TestData;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;
import net.neoforged.neoforge.registries.RegisterEvent;

import java.util.Arrays;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;

@EventBusSubscriber(modid = AnvilCraft.MOD_ID)
public final class ScannerUiStateTests {
    private static final Map<String, Consumer<GameTestHelper>> TESTS = Map.of(
        "port_scanner_release_buttons", ScannerUiStateTests::buttons,
        "port_scanner_file_codecs", ScannerUiStateTests::codecs
    );

    @SubscribeEvent
    public static void functions(RegisterEvent event) {
        event.register(Registries.TEST_FUNCTION, registry -> TESTS.forEach((name, test) -> registry.register(AnvilCraft.of(name), test)));
    }

    @SubscribeEvent
    public static void register(RegisterGameTestsEvent event) {
        var environment = event.registerEnvironment(AnvilCraft.of("port_scanner_ui_state"));
        TESTS.forEach((name, test) -> event.registerTest(AnvilCraft.of(name), new FunctionGameTestInstance(
            ResourceKey.create(Registries.TEST_FUNCTION, AnvilCraft.of(name)),
            new TestData<>(environment, AnvilCraft.of("port_logistics_empty"), 100, 0, true)
        )));
    }

    private static void buttons(GameTestHelper helper) {
        for (int frames : new int[]{3, 5, 6}) {
            var state = new StructureScannerButtonState(frames);
            helper.assertTrue(!state.press(0, false), "禁用按钮不记录按下");
            helper.assertTrue(state.press(0, true) && !state.press(1, true), "鼠标和键盘按压互斥");
            helper.assertTrue(!state.release(0, true, false) && !state.pressedBy(0), "移出按钮松开取消");
            state.press(0, true);
            helper.assertTrue(state.release(0, true, true) && !state.release(0, true, true), "内部松开恰好提交一次");
            state.press(257, true);
            helper.assertTrue(state.keyboardPressed(), "键盘按压不依赖鼠标悬停");
            state.frame(false, true, false);
            helper.assertTrue(!state.keyboardPressed() && !state.release(257, true, true), "禁用会取消键盘按压");
        }
        var toggle = new StructureScannerButtonState(5);
        helper.assertTrue(toggle.frame(true, false, true) == 3 && toggle.frame(true, true, true) == 4, "选中及悬停帧");
        var folder = new StructureScannerButtonState(6);
        helper.assertTrue(folder.frame(true, false, true) == 3 && folder.frame(true, true, true) == 4, "文件按钮目录态帧");
        folder.press(0, true);
        helper.assertTrue(folder.frame(true, true, true) == 5, "目录态按压帧");
        helper.succeed();
    }

    private static void codecs(GameTestHelper helper) {
        var id = UUID.randomUUID();
        var request = new StructureScannerFilePacket(37, id, StructureScannerFilePacket.Action.IMPORT, "蓝图.litematic");
        var buffer = new FriendlyByteBuf(Unpooled.buffer());
        try {
            StructureScannerFilePacket.STREAM_CODEC.encode(buffer, request);
            helper.assertTrue(StructureScannerFilePacket.STREAM_CODEC.decode(buffer).equals(request), "请求保留菜单、身份、动作和名称");
            buffer.clear();
            byte[] data = new byte[StructureFileTransfer.CHUNK_BYTES];
            Arrays.fill(data, (byte) 39);
            var result = new StructureScannerFileResultPacket(id, "", data.length * 2, data.length, data);
            StructureScannerFileResultPacket.STREAM_CODEC.encode(buffer, result);
            var restored = StructureScannerFileResultPacket.STREAM_CODEC.decode(buffer);
            helper.assertTrue(restored.id().equals(id) && restored.offset() == data.length && Arrays.equals(restored.bytes(), data),
                "响应保持分块偏移与内容");
        } finally {
            buffer.release();
        }
        var marker = new ItemStack(Items.DIAMOND, 7);
        marker.set(DataComponents.CUSTOM_NAME, Component.literal("Marker"));
        var save = new StructureScannerSavePacket(37, "Blueprint", false, marker);
        var registry = new RegistryFriendlyByteBuf(Unpooled.buffer(), helper.getLevel().registryAccess());
        try {
            StructureScannerSavePacket.STREAM_CODEC.encode(registry, save);
            var restored = StructureScannerSavePacket.STREAM_CODEC.decode(registry);
            helper.assertTrue(restored.containerId() == 37 && !restored.autoRotate()
                && ItemStack.matches(restored.marker(), marker), "保存设置和图标组件网络往返");
        } finally {
            registry.release();
        }
        helper.succeed();
    }
}
