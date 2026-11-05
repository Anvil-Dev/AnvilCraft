package dev.dubhe.anvilcraft.integration.jade.provider.client;

import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.block.RedstoneWireNetworkManager;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import snownee.jade.api.BlockAccessor;
import snownee.jade.api.IBlockComponentProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.config.IPluginConfig;
import snownee.jade.api.theme.IThemeHelper;

/**
 * 在 Jade 中显示红石导线当前的红石强度。
 *
 * <p>红石导线的强度由网络管理器同步到客户端缓存，不在方块状态里，
 * 因此无法复用原版红石粉那套读取 {@code BlockStateProperties#POWER} 的 Jade 插件，需要单独提供。</p>
 */
public enum RedstoneWirePowerClientProvider implements IBlockComponentProvider {
    INSTANCE;

    @Override
    public void appendTooltip(ITooltip tooltip, BlockAccessor accessor, IPluginConfig config) {
        int power = RedstoneWireNetworkManager.getPower(accessor.getLevel(), accessor.getPosition());
        tooltip.add(Component.translatable("tooltip.jade.power", IThemeHelper.get().info(power)));
    }

    @Override
    public Identifier getUid() {
        return AnvilCraft.of("redstone_wire_power");
    }
}
