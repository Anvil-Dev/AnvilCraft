package dev.dubhe.anvilcraft.api.tooltip;

import dev.dubhe.anvilcraft.inventory.tooltip.CreativeCrateTooltip;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.inventory.tooltip.TooltipComponent;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;

import java.util.Optional;

public final class CreativeCrateItemTooltip {
    private static final String TAG_ITEM = "item";
    private static final String TAG_ITEMS = "Items";

    private CreativeCrateItemTooltip() {
    }

    /** 创造板条箱的 tooltip 数据（携带存储物品 NBT，客户端解析渲染为 图标+文字）。 */
    public static Optional<TooltipComponent> creativeCrateTooltipImage(ItemStack stack) {
        CompoundTag itemTag = getItemTag(stack);
        if (itemTag.getList(TAG_ITEMS, Tag.TAG_COMPOUND).isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(new CreativeCrateTooltip(itemTag));
    }

    private static CompoundTag getItemTag(ItemStack stack) {
        CustomData data = stack.get(DataComponents.BLOCK_ENTITY_DATA);
        if (data == null || data.isEmpty()) return new CompoundTag();
        return data.copyTag().getCompound(TAG_ITEM);
    }
}
