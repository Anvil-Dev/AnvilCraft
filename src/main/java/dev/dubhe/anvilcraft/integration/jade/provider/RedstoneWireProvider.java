package dev.dubhe.anvilcraft.integration.jade.provider;

import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.block.RedstoneWireBlock;
import dev.dubhe.anvilcraft.block.RedstoneWireNetworkManager;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import snownee.jade.api.BlockAccessor;
import snownee.jade.api.IBlockComponentProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.config.IPluginConfig;
import snownee.jade.api.theme.IThemeHelper;

public enum RedstoneWireProvider implements IBlockComponentProvider {
    INSTANCE;

    @Override
    public void appendTooltip(ITooltip tooltip, BlockAccessor accessor, IPluginConfig config) {
        if (!(accessor.getBlockState().getBlock() instanceof RedstoneWireBlock)) return;
        int power = RedstoneWireNetworkManager.getPower(accessor.getLevel(), accessor.getPosition());
        tooltip.add(Component.translatable("tooltip.jade.power", IThemeHelper.get().info(power)));
    }

    @Override
    public ResourceLocation getUid() {
        return AnvilCraft.of("redstone_wire");
    }
}
