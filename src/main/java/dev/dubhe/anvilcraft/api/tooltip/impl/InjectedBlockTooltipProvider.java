package dev.dubhe.anvilcraft.api.tooltip.impl;

import dev.anvilcraft.lib.v2.util.Util;
import dev.dubhe.anvilcraft.api.injection.tooltip.ITooltipProviderExtension;
import dev.dubhe.anvilcraft.api.tooltip.providers.ITooltipProvider;
import dev.dubhe.anvilcraft.client.AnvilCraftClient;
import dev.dubhe.anvilcraft.util.CompatUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

import java.util.List;

public class InjectedBlockTooltipProvider extends ITooltipProvider.BlockTooltipProvider {
    public InjectedBlockTooltipProvider() {
    }

    @Override
    public boolean accepts(Level level, BlockPos pos, BlockState value) {
        return Util.instanceOfAny(value.getBlock(), ITooltipProviderExtension.class);
    }

    @Override
    public List<Component> tooltip(Level level, BlockPos pos, BlockState state) {
        if (CompatUtil.HAS_JADE.get() && AnvilCraftClient.CONFIG.doNotShowTooltipWhenJadePresent) return List.of();
        return Util.castSafely(state.getBlock(), ITooltipProviderExtension.class)
            .map(provider -> provider.anvilcraft$getTooltip(state))
            .orElse(List.of());
    }

    @Override
    public int priority() {
        return 1;
    }
}
