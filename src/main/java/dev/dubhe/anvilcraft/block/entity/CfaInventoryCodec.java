package dev.dubhe.anvilcraft.block.entity;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.item.ItemStack;

import java.util.Objects;

/**
 * 锻星砧控制器内部小型物品栏的 NBT 编解码工具。
 */
final class CfaInventoryCodec {
    private CfaInventoryCodec() {
    }

    static CompoundTag save(SimpleContainer container) {
        CompoundTag tag = new CompoundTag();
        for (int slot = 0; slot < container.getContainerSize(); slot++) {
            ItemStack stack = container.getItem(slot);
            if (!stack.isEmpty()) {
                tag.put(CfaInventoryCodec.key(slot), ItemStack.CODEC.encodeStart(NbtOps.INSTANCE, stack).getOrThrow());
            }
        }
        return tag;
    }

    static void load(CompoundTag tag, SimpleContainer container) {
        for (int slot = 0; slot < container.getContainerSize(); slot++) {
            ItemStack stack = tag.contains(CfaInventoryCodec.key(slot))
                ? ItemStack.CODEC.parse(NbtOps.INSTANCE, Objects.requireNonNull(tag.get(
                CfaInventoryCodec.key(slot)))).result().orElse(ItemStack.EMPTY)
                : ItemStack.EMPTY;
            container.setItem(slot, stack);
        }
    }

    private static String key(int slot) {
        return "s" + slot;
    }
}
