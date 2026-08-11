package dev.dubhe.anvilcraft.inventory.tooltip;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.inventory.tooltip.TooltipComponent;

/**
 * 创造板条箱物品 tooltip 的数据载体（仅携带存储物品的 NBT）。
 * 物品解析、图标与文字渲染在 {@code ClientCreativeCrateTooltip} 中进行。
 *
 * @param itemTag 存储物品的 NBT（ItemStackHandler 序列化结果，含 {@code Items} 列表）
 */
public record CreativeCrateTooltip(CompoundTag itemTag) implements TooltipComponent {
}
