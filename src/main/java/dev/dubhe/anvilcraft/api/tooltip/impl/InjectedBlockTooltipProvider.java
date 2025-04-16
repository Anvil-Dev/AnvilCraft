package dev.dubhe.anvilcraft.api.tooltip.impl;

import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.api.injection.tooltip.IInjectedTooltipProducer;
import dev.dubhe.anvilcraft.api.tooltip.providers.ITooltipProvider;
import dev.dubhe.anvilcraft.util.Util;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

import java.util.List;
import java.util.Optional;

public class InjectedBlockTooltipProvider extends ITooltipProvider.BlockTooltipProvider {
    public InjectedBlockTooltipProvider() {
    }

    protected Optional<IInjectedTooltipProducer> cast(Block value) {
        try {
            if (value instanceof IInjectedTooltipProducer producer) {
                return Optional.of(producer);
            } else {
                return Optional.empty();
            }
        } catch (ClassCastException ignored) {
            return Optional.empty();
        }
    }

    @Override
    public boolean accepts(Block value) {
        return cast(value).isPresent();
    }

    @Override
    public List<Component> tooltip(BlockState state) {
        if (Util.jadePresent.get() && AnvilCraft.config.doNotShowTooltipWhenJadePresent) return null;
        return cast(state.getBlock()).map(producer -> producer.anvilcraft$getTooltip(state)).orElse(null);
    }

    @Override
    public int priority() {
        return 1;
    }
}
