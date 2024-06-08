package dev.dubhe.anvilcraft.api.tooltip;

import dev.dubhe.anvilcraft.block.entity.RubyPrismBlockEntity;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;

public class RubyPrismTooltipProvider implements TooltipProvider {
    RubyPrismTooltipProvider() {}

    @Override
    public boolean accepts(BlockEntity entity) {
        return entity instanceof RubyPrismBlockEntity;
    }

    @Override
    public List<Component> tooltip(BlockEntity e) {
        if (!(e instanceof RubyPrismBlockEntity rubyPrismBlockEntity)) return null;
        final List<Component> lines = new ArrayList<>();
        lines.add(Component.translatable("tooltip.anvilcraft.ruby_prism.power", rubyPrismBlockEntity.laserLevel));
        return lines;
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
