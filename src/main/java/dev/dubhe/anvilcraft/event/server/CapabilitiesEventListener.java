package dev.dubhe.anvilcraft.event.server;

import dev.dubhe.anvilcraft.AnvilCraft;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;

/**
 * 方块实体能力注册事件入口。基础物品和流体能力统一在 {@code ModCapabilities} 中注册。
 */
@EventBusSubscriber(modid = AnvilCraft.MOD_ID)
public class CapabilitiesEventListener {
    @SubscribeEvent
    public static void registerCapabilities(final RegisterCapabilitiesEvent event) {
        // 锻星砧接口能力与其他方块实体能力均在 ModCapabilities 中注册。
        // 后续自定义能力可在此事件入口补充。
    }
}
