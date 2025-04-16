package dev.dubhe.anvilcraft.api.tooltip.impl;

import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.api.injection.tooltip.IInjectedTooltipProducer;
import dev.dubhe.anvilcraft.api.tooltip.providers.ITooltipProvider;
import dev.dubhe.anvilcraft.util.Util;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.entity.BlockEntity;

import java.util.List;
import java.util.Optional;

public class InjectedBlockEntityTooltipProvider extends ITooltipProvider.BlockEntityTooltipProvider {
    public InjectedBlockEntityTooltipProvider() {
    }

    protected Optional<IInjectedTooltipProducer> cast(BlockEntity value) {
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
    public boolean accepts(BlockEntity value) {
        return cast(value).isPresent();
    }

    @Override
    public List<Component> tooltip(BlockEntity value) {
        if (Util.jadePresent.get() && AnvilCraft.config.doNotShowTooltipWhenJadePresent) return null;
        return cast(value).map(IInjectedTooltipProducer::anvilcraft$getTooltip).orElse(null);
    }

    @Override
    public int priority() {
        return 1;
    }
}
