package dev.dubhe.anvilcraft.items;

import dev.dubhe.anvilcraft.utils.ItemUtils;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.PickaxeItem;
import net.minecraft.world.item.Tiers;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;

public class ChangeablePickaxeItem extends PickaxeItem {
    public ChangeablePickaxeItem(@NotNull Properties properties) {
        super(Tiers.NETHERITE, 1, -2.8f, properties.fireResistant());
    }

    @Override
    public @NotNull ItemStack getDefaultInstance() {
        ItemStack stack = new ItemStack(this);
        Item item = this.asItem();
        Enchantment enchant = item.equals(ModItems.CHANGEABLE_PICKAXE_FORTUNE) ? Enchantments.BLOCK_FORTUNE : Enchantments.SILK_TOUCH;
        int level = item.equals(ModItems.CHANGEABLE_PICKAXE_FORTUNE) ? 3 : 1;
        stack.enchant(enchant, level);
        return stack;
    }

    @Override
    public @NotNull InteractionResultHolder<ItemStack> use(Level level, @NotNull Player player, InteractionHand interactionHand) {
        ItemStack fromStack = player.getItemInHand(interactionHand);
        if (!player.isShiftKeyDown()) return InteractionResultHolder.pass(fromStack);
        Item fromItem = fromStack.getItem();
        Item toItem = fromItem.equals(ModItems.CHANGEABLE_PICKAXE_FORTUNE) ? ModItems.CHANGEABLE_PICKAXE_SILK_TOUCH : ModItems.CHANGEABLE_PICKAXE_FORTUNE;
        Enchantment fromEnchant = fromItem.equals(ModItems.CHANGEABLE_PICKAXE_FORTUNE) ? Enchantments.BLOCK_FORTUNE : Enchantments.SILK_TOUCH;
        Enchantment toEnchant = toItem.equals(ModItems.CHANGEABLE_PICKAXE_FORTUNE) ? Enchantments.BLOCK_FORTUNE : Enchantments.SILK_TOUCH;
        int toLevel = toItem.equals(ModItems.CHANGEABLE_PICKAXE_FORTUNE) ? 3 : 1;
        ItemStack toStack = new ItemStack(toItem);
        ItemUtils.ItemStackDataCopy(fromStack, toStack);
        if (ItemUtils.removeEnchant(toStack, fromEnchant)) toStack.enchant(toEnchant, toLevel);
        return InteractionResultHolder.success(toStack);
    }
}
