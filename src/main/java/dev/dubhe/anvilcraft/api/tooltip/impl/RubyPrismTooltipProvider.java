package dev.dubhe.anvilcraft.api.tooltip.impl;

import dev.dubhe.anvilcraft.api.tooltip.providers.ITooltipProvider;
import dev.dubhe.anvilcraft.block.entity.RubyPrismBlockEntity;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.entity.BlockEntity;

import java.util.List;

public class RubyPrismTooltipProvider extends ITooltipProvider.BlockEntityTooltipProvider {
    public RubyPrismTooltipProvider() {
    }

    @Override
    public boolean accepts(BlockEntity entity) {
        return entity instanceof RubyPrismBlockEntity;
    }

    @Override
    public List<Component> tooltip(BlockEntity e) {
        if (e instanceof RubyPrismBlockEntity rubyPrismBlockEntity) {
            return List.of(Component.translatable("tooltip.anvilcraft.jade.ruby_prism.power", rubyPrismBlockEntity.getLaserLevel()));
        }
        return List.of();
    }

    @Override
    public int priority() {
        return 0;
    }
}
