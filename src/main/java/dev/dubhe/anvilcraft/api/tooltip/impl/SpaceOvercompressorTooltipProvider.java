package dev.dubhe.anvilcraft.api.tooltip.impl;

import dev.dubhe.anvilcraft.api.tooltip.providers.ITooltipProvider;
import dev.dubhe.anvilcraft.block.entity.SpaceOvercompressorBlockEntity;
import dev.dubhe.anvilcraft.client.AnvilCraftClient;
import dev.dubhe.anvilcraft.util.Util;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.entity.BlockEntity;

import java.util.List;

public class SpaceOvercompressorTooltipProvider extends ITooltipProvider.BlockEntityTooltipProvider {
    public SpaceOvercompressorTooltipProvider() {
    }

    @Override
    public boolean accepts(BlockEntity entity) {
        return entity instanceof SpaceOvercompressorBlockEntity;
    }

    @Override
    public List<Component> tooltip(BlockEntity e) {
        if (Util.jadePresent.get() && AnvilCraftClient.CONFIG.doNotShowTooltipWhenJadePresent) return List.of();
        if (!(e instanceof SpaceOvercompressorBlockEntity compressor)) return List.of();
        return List.of(Component.translatable("tooltip.anvilcraft.space_overcompressor.stored_mass", compressor.displayStoredMass()));
    }

    @Override
    public int priority() {
        return 0;
    }
}
