package dev.dubhe.anvilcraft.api.tooltip.impl;

import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.api.injection.tooltip.IInjectedTooltipProvider;
import dev.dubhe.anvilcraft.api.tooltip.providers.ITooltipProvider;
import dev.dubhe.anvilcraft.util.Util;
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
        return Util.instanceOfAny(value.getBlock(), IInjectedTooltipProvider.class);
    }

    @Override
    public List<Component> tooltip(Level level, BlockPos pos, BlockState state) {
        if (Util.jadePresent.get() && AnvilCraft.config.doNotShowTooltipWhenJadePresent) return null;
        return Util.castSafely(state.getBlock(), IInjectedTooltipProvider.class)
            .map(provider -> provider.anvilcraft$getTooltip(state)).orElse(null);
    }

    @Override
    public int priority() {
        return 1;
    }
}
