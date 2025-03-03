package dev.dubhe.anvilcraft.api.tooltip.impl;

import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.api.tooltip.providers.IBlockEntityTooltipProvider;
import dev.dubhe.anvilcraft.block.entity.RubyPrismBlockEntity;
import dev.dubhe.anvilcraft.util.Util;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;

import java.util.List;

public class RubyPrismTooltipProvider implements IBlockEntityTooltipProvider {
    public RubyPrismTooltipProvider() {
    }

    @Override
    public boolean accepts(BlockEntity entity) {
        return entity instanceof RubyPrismBlockEntity;
    }

    @Override
    public List<Component> tooltip(BlockEntity e) {
        if (Util.jadePresent.get() && AnvilCraft.config.doNotShowTooltipWhenJadePresent) {
            return null;
        }
        if (e instanceof RubyPrismBlockEntity rubyPrismBlockEntity) {
            return List.of(Component.translatable(
                "tooltip.anvilcraft.jade.ruby_prism.power", rubyPrismBlockEntity.getLaserLevel()));
        }
        return null;
    }

    @Override
    public ItemStack icon(BlockEntity entity) {
        return entity.getBlockState().getBlock().asItem().getDefaultInstance();
    }

    @Override
    public int priority() {
        return 0;
    }
}
