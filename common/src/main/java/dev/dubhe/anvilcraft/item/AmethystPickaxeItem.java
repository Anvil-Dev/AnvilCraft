package dev.dubhe.anvilcraft.item;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.PickaxeItem;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class AmethystPickaxeItem extends PickaxeItem {
    public AmethystPickaxeItem(Properties properties) {
        super(ModTiers.AMETHYST, 1, -2.8f, properties);
    }

    @Override
    public void appendHoverText(
        @NotNull ItemStack stack,
        @Nullable Level level,
        @NotNull List<Component> tooltipComponents,
        @NotNull TooltipFlag isAdvanced
    ) {
        super.appendHoverText(stack, level, tooltipComponents, isAdvanced);
        tooltipComponents
            .add(Component.translatable("item.anvilcraft.amethyst_pickaxe.tooltip")
                .withStyle(ChatFormatting.GRAY));
    }

    @Override
    public @NotNull ItemStack getDefaultInstance() {
        ItemStack stack = super.getDefaultInstance();
        stack.enchant(Enchantments.BLOCK_FORTUNE, 3);
        return stack;
    }
}
