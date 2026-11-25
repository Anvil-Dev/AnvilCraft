package dev.dubhe.anvilcraft.api.fluid;

import dev.dubhe.anvilcraft.client.ClientFluidItemRegistryAccess;
import net.minecraft.core.HolderLookup;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.loading.FMLEnvironment;
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
        return FMLEnvironment.dist == Dist.CLIENT ? ClientFluidItemRegistryAccess.get() : null;
    }
}
