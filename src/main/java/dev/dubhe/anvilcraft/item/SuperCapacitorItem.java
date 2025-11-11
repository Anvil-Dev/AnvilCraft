package dev.dubhe.anvilcraft.item;

import dev.dubhe.anvilcraft.api.item.IChargerDischargeable;
import dev.dubhe.anvilcraft.init.item.ModItems;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;

public class SuperCapacitorItem extends Item implements IChargerDischargeable {
    public SuperCapacitorItem(Properties properties) {
        super(properties);
    }

    @Override
    public ItemStack discharge(ItemStack input) {
        return ModItems.CAPACITOR_EMPTY.asStack(1);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltipComponents, TooltipFlag tooltipFlag) {
        tooltipComponents.add(Component.translatable("tooltip.anvilcraft.item.supercapacitor").withStyle(ChatFormatting.GRAY));
        super.appendHoverText(stack, context, tooltipComponents, tooltipFlag);
    }
}
