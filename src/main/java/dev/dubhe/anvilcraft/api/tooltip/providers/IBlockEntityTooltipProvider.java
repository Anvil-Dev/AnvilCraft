package dev.dubhe.anvilcraft.api.tooltip.providers;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;

import java.util.List;

/**
 * 头戴铁砧锤时显示的tooltip
 */
@Deprecated(since = "1.4.2", forRemoval = true)
public interface IBlockEntityTooltipProvider {
    boolean accepts(BlockEntity entity);

    List<Component> tooltip(BlockEntity e);

    ItemStack icon(BlockEntity entity);

    int priority();

    default ITooltipProvider.BlockEntityTooltipProvider toNewImplementation() {
        return new ITooltipProvider.BlockEntityTooltipProvider() {
            @Override
            public boolean accepts(BlockEntity value) {
                return IBlockEntityTooltipProvider.this.accepts(value);
            }

            @Override
            public List<Component> tooltip(BlockEntity value) {
                return IBlockEntityTooltipProvider.this.tooltip(value);
            }

            @Override
            public ItemStack icon(BlockEntity value) {
                return IBlockEntityTooltipProvider.this.icon(value);
            }

            @Override
            public int priority() {
                return IBlockEntityTooltipProvider.this.priority();
            }
        };
    }
}
