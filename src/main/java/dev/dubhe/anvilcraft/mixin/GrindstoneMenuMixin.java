package dev.dubhe.anvilcraft.mixin;

import dev.dubhe.anvilcraft.item.IInherentEnchantment;
import net.minecraft.core.component.DataComponents;
import net.minecraft.tags.EnchantmentTags;
import net.minecraft.world.inventory.AnvilMenu;
import net.minecraft.world.inventory.GrindstoneMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;

@Mixin(GrindstoneMenu.class)
abstract class GrindstoneMenuMixin {
    @Shadow
    protected abstract ItemStack removeNonCursesFrom(ItemStack item);

    @Shadow
    protected abstract void mergeEnchantsFrom(ItemStack inputItem, ItemStack additionalItem);

    /**
     * @author
     * @reason
     */
    @Overwrite
    private ItemStack mergeItems(ItemStack inputItem, ItemStack additionalItem) {
        if (!inputItem.is(additionalItem.getItem())) {
            return ItemStack.EMPTY;
        } else {
            int i = Math.max(inputItem.getMaxDamage(), additionalItem.getMaxDamage());
            int j = inputItem.getMaxDamage() - inputItem.getDamageValue();
            int k = additionalItem.getMaxDamage() - additionalItem.getDamageValue();
            int l = j + k + i * 5 / 100;
            int i1 = 1;
            if (!inputItem.isDamageableItem() || !inputItem.isRepairable()) {
                if (inputItem.getMaxStackSize() < 2 || !ItemStack.matches(inputItem, additionalItem)) {
                    return ItemStack.EMPTY;
                }

                i1 = 2;
            }

            ItemStack itemstack = inputItem.copyWithCount(i1);
            if (itemstack.isDamageableItem()) {
                itemstack.set(DataComponents.MAX_DAMAGE, i);
                itemstack.setDamageValue(Math.max(i - l, 0));
                if (!additionalItem.isRepairable()) {
                    itemstack.setDamageValue(inputItem.getDamageValue());
                }
            }

            this.mergeEnchantsFrom(itemstack, additionalItem);
            if (inputItem.getItem() instanceof IInherentEnchantment) {
                return this.anvilcraft$removeNonInherentFrom(itemstack);
            } else {
               return this.removeNonCursesFrom(itemstack);
            }
        }
    }

    /**
     * @author
     * @reason
     */
    @Overwrite
    private ItemStack computeResult(ItemStack inputItem, ItemStack additionalItem) {
        boolean flag = !inputItem.isEmpty() || !additionalItem.isEmpty();
        if (!flag) {
            return ItemStack.EMPTY;
        } else if (inputItem.getCount() <= 1 && additionalItem.getCount() <= 1) {
            boolean flag1 = !inputItem.isEmpty() && !additionalItem.isEmpty();
            if (!flag1) {
                ItemStack itemstack = !inputItem.isEmpty() ? inputItem : additionalItem;
                if (itemstack.getItem() instanceof IInherentEnchantment inherentEnchantment) {
                    return !EnchantmentHelper.hasAnyEnchantments(itemstack) ? ItemStack.EMPTY : this.anvilcraft$removeNonInherentFrom(itemstack.copy());
                }
                return !EnchantmentHelper.hasAnyEnchantments(itemstack) ? ItemStack.EMPTY : this.removeNonCursesFrom(itemstack.copy());
            } else {
                return this.mergeItems(inputItem, additionalItem);
            }
        } else {
            return ItemStack.EMPTY;
        }
    }

    @Unique
    private ItemStack anvilcraft$removeNonInherentFrom(ItemStack item) {
        ItemEnchantments itemenchantments = EnchantmentHelper.updateEnchantments(item, (p_330066_) -> {
            p_330066_.removeIf((p_344368_) -> !p_344368_.is(EnchantmentTags.CURSE) && !p_344368_.is(Enchantments.UNBREAKING));
        });
        if (item.is(Items.ENCHANTED_BOOK) && itemenchantments.isEmpty()) {
            item = item.transmuteCopy(Items.BOOK);
        }

        int i = 0;

        for(int j = 0; j < itemenchantments.size(); ++j) {
            i = AnvilMenu.calculateIncreasedRepairCost(i);
        }

        item.set(DataComponents.REPAIR_COST, i);
        return item;
    }
}
