package dev.dubhe.anvilcraft.mixin;

import dev.dubhe.anvilcraft.util.IItemStackInjector;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import static net.minecraft.world.item.ItemStack.TAG_ENCH;

@Mixin(ItemStack.class)
@SuppressWarnings("AddedMixinMembersNamePattern")
public abstract class ItemStackInjector implements IItemStackInjector {
    @Shadow
    public abstract int getDamageValue();

    @Shadow
    public abstract boolean hasTag();

    @Shadow
    public abstract @Nullable CompoundTag getTag();

    @Shadow
    public abstract boolean hasCustomHoverName();

    @Shadow
    public abstract Component getHoverName();

    @Shadow
    @Nullable
    public abstract Entity getEntityRepresentation();

    @Shadow
    public abstract int getBaseRepairCost();

    @Shadow
    public abstract int getPopTime();

    @Shadow
    public abstract int getCount();

    @Shadow
    public abstract void setTag(@Nullable CompoundTag compoundTag);

    @Shadow
    public abstract CompoundTag getOrCreateTag();

    @Override
    public ItemStack dataCopy(@NotNull ItemStack stack) {
        stack.setDamageValue(this.getDamageValue());
        if (this.hasTag()) stack.setTag(this.getTag());
        if (this.hasCustomHoverName()) stack.setHoverName(this.getHoverName());
        stack.setEntityRepresentation(this.getEntityRepresentation());
        stack.setRepairCost(this.getBaseRepairCost());
        stack.setPopTime(this.getPopTime());
        stack.setCount(this.getCount());
        return stack;
    }

    @Override
    public boolean removeEnchant(Enchantment enchantment) {
        CompoundTag tag = this.getOrCreateTag();
        if (!tag.contains(TAG_ENCH, 9)) {
            tag.put(TAG_ENCH, new ListTag());
        }
        if (!tag.contains(TAG_ENCH)) return false;
        ListTag listTag = tag.getList(TAG_ENCH, 10);
        tag.remove(TAG_ENCH);
        boolean bl = false;
        for (Tag tag1 : listTag) {
            if (!(tag1 instanceof CompoundTag tag2)) continue;
            ResourceLocation id = EnchantmentHelper.getEnchantmentId(tag2);
            ResourceLocation id1 = EnchantmentHelper.getEnchantmentId(enchantment);
            if (null == id|| null == id1) return false;
            if (!id.equals(id1)) continue;
            listTag.remove(tag1);
            bl = true;
            break;
        }
        tag.put(TAG_ENCH, listTag);
        this.setTag(tag);
        return bl;
    }
}
