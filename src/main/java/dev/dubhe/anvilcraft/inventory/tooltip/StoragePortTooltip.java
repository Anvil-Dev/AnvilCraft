package dev.dubhe.anvilcraft.inventory.tooltip;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.inventory.tooltip.TooltipComponent;

/**
 * 仓储端口物品 tooltip 的数据载体（携带方块实体的存档数据 NBT）。
 * 物品解析、图标与文字渲染在 {@code ClientStoragePortTooltip} 中进行。
 *
 * @param blockEntityTag 方块实体存档数据（含 {@code marked_item} 与 {@code buffer}）
 */
public record StoragePortTooltip(CompoundTag blockEntityTag) implements TooltipComponent {
}