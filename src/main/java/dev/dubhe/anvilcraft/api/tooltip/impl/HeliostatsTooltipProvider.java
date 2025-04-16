package dev.dubhe.anvilcraft.api.tooltip.impl;

import dev.dubhe.anvilcraft.api.tooltip.providers.ITooltipProvider;
import dev.dubhe.anvilcraft.block.entity.HeliostatsBlockEntity;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.entity.BlockEntity;

import java.util.ArrayList;
import java.util.List;

public class HeliostatsTooltipProvider extends ITooltipProvider.BlockEntityTooltipProvider {
    public HeliostatsTooltipProvider() {
    }

    @Override
    public boolean accepts(BlockEntity entity) {
        return entity instanceof HeliostatsBlockEntity heliostatsBlockEntity
            && !heliostatsBlockEntity.getWorkResult().isWorking();
    }

    @Override
    public List<Component> tooltip(BlockEntity e) {
        if (!(e instanceof HeliostatsBlockEntity heliostatsBlockEntity)) return null;
        final List<Component> lines = new ArrayList<>();
        lines.add(Component.translatable("tooltip.anvilcraft.heliostats.not_work"));
        lines.add(Component.translatable(heliostatsBlockEntity.getWorkResult().getTranslateKey()));
        return lines;
    }

    @Override
    public int priority() {
        return 0;
    }
}
