package dev.dubhe.anvilcraft.api.fluid;

import net.minecraft.client.Minecraft;
import net.minecraft.core.HolderLookup;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.loading.FMLLoader;
import net.neoforged.neoforge.server.ServerLifecycleHooks;

/// 物品能力回调里拿不到注册表，只能按当前运行环境现取。
///
/// <p>服务端优先，取不到时回落到客户端的当前世界；两边都没有（例如数据生成阶段）返回 {@code null}，
/// 调用方据此退化为「读不到内容」而不是抛异常。</p>
public final class FluidItemRegistryAccess {
    private FluidItemRegistryAccess() {
    }

    /// 当前可用的注册表访问器；不可用时返回 {@code null}。
    public static HolderLookup.Provider get() {
        if (ServerLifecycleHooks.getCurrentServer() != null) {
            return ServerLifecycleHooks.getCurrentServer().registryAccess();
        }
        return FMLLoader.getCurrent().getDist() == Dist.CLIENT ? ClientHolder.get() : null;
    }

    /// 客户端分支单独成内部类，专用服务端不会加载 {@link Minecraft}。
    private static final class ClientHolder {
        private static HolderLookup.Provider get() {
            var level = Minecraft.getInstance().level;
            return level == null ? null : level.registryAccess();
        }
    }
}