package dev.dubhe.anvilcraft.item;

import net.minecraft.ChatFormatting;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.AxeItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.PickaxeItem;
import net.minecraft.world.item.Tiers;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.List;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class AmethystPickaxeItem extends PickaxeItem {
    public AmethystPickaxeItem(Properties properties) {
        super(ModTiers.AMETHYST, properties.attributes(AxeItem.createAttributes(
            ModTiers.AMETHYST, 1, -2.8f
        )));
    }

    @Override
    public void appendHoverText(
        ItemStack pStack,
        TooltipContext pContext,
        List<Component> pTooltipComponents,
        TooltipFlag pTooltipFlag
    ) {
        super.appendHoverText(pStack, pContext, pTooltipComponents, pTooltipFlag);
        pTooltipComponents
            .add(Component.translatable("item.anvilcraft.amethyst_pickaxe.tooltip")
                .withStyle(ChatFormatting.GRAY));
    }


    @Override
    public @NotNull ItemStack getDefaultInstance() {
        ItemStack stack = super.getDefaultInstance();
        stack.enchant(Enchantments.FORTUNE, 3);
        return stack;
    }
}
