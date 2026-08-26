package dev.dubhe.anvilcraft.api.tooltip;

import dev.dubhe.anvilcraft.inventory.tooltip.StoragePortTooltip;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.inventory.tooltip.TooltipComponent;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;

import java.util.Optional;

/**
 * 仓储端口物品 tooltip：携带方块实体的存档数据，
 * 客户端解析渲染为 图标 + 标记物品名称 + 缓存数量。
 */
public final class StoragePortItemTooltip {
    private StoragePortItemTooltip() {
    }

    public static Optional<TooltipComponent> storagePortTooltipImage(ItemStack stack) {
        CustomData data = stack.get(DataComponents.BLOCK_ENTITY_DATA);
        if (data == null || data.isEmpty()) {
            return Optional.empty();
        }
        CompoundTag tag = data.copyTag();
        if (!tag.contains("marked_item", Tag.TAG_COMPOUND)) {
            // 无标记：仅当缓存非空时才显示 tooltip
            ListTag items = tag.getCompound("buffer").getList("Items", Tag.TAG_COMPOUND);
            if (items.isEmpty()) {
                return Optional.empty();
            }
        }
        return Optional.of(new StoragePortTooltip(tag));
    }
}