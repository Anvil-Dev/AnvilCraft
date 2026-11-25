package dev.dubhe.anvilcraft.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.HolderLookup;

/// 客户端侧取当前世界的注册表；与 {@link dev.dubhe.anvilcraft.api.fluid.FluidItemRegistryAccess} 配套。
///
/// <p>单独成类是为了让公共代码只在客户端分支加载它，专用服务端不会碰到 {@link Minecraft}。</p>
public final class ClientFluidItemRegistryAccess {
    private ClientFluidItemRegistryAccess() {
    }

    /// 当前客户端世界的注册表访问器；未进入世界时返回 {@code null}。
    public static HolderLookup.Provider get() {
        ClientLevel level = Minecraft.getInstance().level;
        return level == null ? null : level.registryAccess();
    }
}
