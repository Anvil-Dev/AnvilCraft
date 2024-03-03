package dev.dubhe.anvilcraft.utils;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import org.jetbrains.annotations.NotNull;

import static net.minecraft.world.item.ItemStack.TAG_ENCH;

@SuppressWarnings("all")
public interface ItemUtils {
    static @NotNull ItemStack ItemStackDataCopy(@NotNull ItemStack from, @NotNull ItemStack to) {
        to.setDamageValue(from.getDamageValue());
        if (from.hasTag()) to.setTag(from.getTag());
        if (from.hasCustomHoverName()) to.setHoverName(from.getHoverName());
        to.setEntityRepresentation(from.getEntityRepresentation());
        to.setRepairCost(from.getBaseRepairCost());
        to.setPopTime(from.getPopTime());
        to.setCount(from.getCount());
        return to;
    }

    static boolean removeEnchant(ItemStack stack, Enchantment enchantment) {
        CompoundTag tag = stack.getOrCreateTag();
        if (!tag.contains(TAG_ENCH, 9)) {
            tag.put(TAG_ENCH, new ListTag());
        }
        ListTag listTag = stack.getTag().getList(TAG_ENCH, 10);
        tag.remove(TAG_ENCH);
        boolean bl = false;
        for (Tag tag1 : listTag) {
            if (!(tag1 instanceof CompoundTag tag2)) continue;
            ResourceLocation id = EnchantmentHelper.getEnchantmentId(tag2);
            ResourceLocation id1 = EnchantmentHelper.getEnchantmentId(enchantment);
            if (!id.equals(id1)) continue;
            listTag.remove(tag1);
            bl = true;
            break;
        }
        tag.put(TAG_ENCH, listTag);
        stack.setTag(tag);
        return bl;
    }
}
