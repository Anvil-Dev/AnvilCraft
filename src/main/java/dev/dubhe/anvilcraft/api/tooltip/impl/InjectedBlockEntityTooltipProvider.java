package dev.dubhe.anvilcraft.api.tooltip.impl;

import dev.dubhe.anvilcraft.api.injection.tooltip.ITooltipProviderExtension;
import dev.dubhe.anvilcraft.api.tooltip.providers.ITooltipProvider;
import dev.dubhe.anvilcraft.client.AnvilCraftClient;
import dev.dubhe.anvilcraft.util.CompatUtil;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.entity.BlockEntity;

import java.util.List;
import java.util.Optional;

public class InjectedBlockEntityTooltipProvider extends ITooltipProvider.BlockEntityTooltipProvider {
    public InjectedBlockEntityTooltipProvider() {
    }

    protected Optional<ITooltipProviderExtension> cast(BlockEntity value) {
        try {
            if (value instanceof ITooltipProviderExtension producer) {
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
        if (CompatUtil.HAS_JADE.get() && AnvilCraftClient.CONFIG.doNotShowTooltipWhenJadePresent) return List.of();
        return cast(value).map(ITooltipProviderExtension::anvilcraft$getTooltip).orElse(List.of());
    }

    @Override
    public int priority() {
        return 1;
    }
}
