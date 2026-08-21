package dev.dubhe.anvilcraft.block.entity;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.item.ItemStack;

/** Shared NBT codec for the five internal CFA anvil slots. */
final class CfaInventoryCodec {
    private CfaInventoryCodec() {
    }

    static CompoundTag save(SimpleContainer inventory, HolderLookup.Provider registries) {
        CompoundTag tag = new CompoundTag();
        for (int slot = 0; slot < inventory.getContainerSize(); slot++) {
            ItemStack stack = inventory.getItem(slot);
            if (!stack.isEmpty()) {
                tag.put("s" + slot, stack.save(registries));
            }
        }
        return tag;
    }

    static void load(CompoundTag tag, SimpleContainer inventory, HolderLookup.Provider registries) {
        for (int slot = 0; slot < inventory.getContainerSize(); slot++) {
            String key = "s" + slot;
            ItemStack stack = ItemStack.EMPTY;
            if (tag.get(key) instanceof CompoundTag stackTag) {
                stack = ItemStack.parse(registries, stackTag).orElse(ItemStack.EMPTY);
            }
            inventory.setItem(slot, stack);
        }
    }
}
