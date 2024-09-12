package dev.dubhe.anvilcraft.item;

import net.minecraft.ChatFormatting;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.AxeItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.PickaxeItem;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class AmethystPickaxeItem extends PickaxeItem {
    public AmethystPickaxeItem(Properties properties) {
        /**
         *
         */
        super(
                ModTiers.AMETHYST,
                properties.attributes(AxeItem.createAttributes(ModTiers.AMETHYST, 1, -2.8f)));
    }

    @Override
    public void appendHoverText(
            ItemStack pStack,
            TooltipContext pContext,
            List<Component> pTooltipComponents,
            TooltipFlag pTooltipFlag) {
        super.appendHoverText(pStack, pContext, pTooltipComponents, pTooltipFlag);
        pTooltipComponents.add(Component.translatable("item.anvilcraft.amethyst_pickaxe.tooltip")
                .withStyle(ChatFormatting.GRAY));
    }
}
