package dev.dubhe.anvilcraft.api.tooltip;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;

import java.util.List;

/**
 * 头戴铁砧锤时显示的tooltip
 */
public interface TooltipProvider {
    boolean accepts(BlockEntity entity);

    List<Component> tooltip(BlockEntity e);

    ItemStack icon(BlockEntity entity);

    int priority();
}
