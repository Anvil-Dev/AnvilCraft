package dev.dubhe.anvilcraft.client.event;

import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.client.renderer.debug.CfaRenderBoundsDebugRenderer;
import net.minecraft.client.gui.components.debug.DebugEntryNoop;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterDebugEntriesEvent;
import net.neoforged.neoforge.client.event.RegisterDebugRenderersEvent;

/// 注册 AnvilCraft 的调试条目与调试渲染器，参考 anvillib 的 `ALRDebugEntries`。
///
/// 调试条目负责在 F3 调试选项中提供开关（默认关闭），调试渲染器负责实际绘制。
@EventBusSubscriber(modid = AnvilCraft.MOD_ID, value = Dist.CLIENT)
public class DebugRendererEventListener {
    @SubscribeEvent
    public static void on(RegisterDebugEntriesEvent event) {
        // 纯视觉的调试开关，沿用原版的无文本条目（类别为渲染器）。
        event.register(CfaRenderBoundsDebugRenderer.LOCATION, new DebugEntryNoop());
    }

    @SubscribeEvent
    public static void on(RegisterDebugRenderersEvent event) {
        event.register(new CfaRenderBoundsDebugRenderer());
    }
}
