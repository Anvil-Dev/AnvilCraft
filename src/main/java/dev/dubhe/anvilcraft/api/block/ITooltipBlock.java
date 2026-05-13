package dev.dubhe.anvilcraft.api.block;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.function.Consumer;

public interface ITooltipBlock {

    void appendHoverText(
        ItemStack stack,
        Item.TooltipContext context,
        Consumer<Component> builder,
        TooltipFlag flag
    );
}
