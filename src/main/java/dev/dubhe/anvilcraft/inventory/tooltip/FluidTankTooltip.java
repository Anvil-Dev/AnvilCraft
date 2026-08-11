package dev.dubhe.anvilcraft.inventory.tooltip;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.inventory.tooltip.TooltipComponent;

/**
 * 流体储罐物品 tooltip 的数据载体（仅携带储罐 NBT）。
 * 流体解析、图标与文字渲染在 {@code ClientFluidTankTooltip} 中进行。
 *
 * @param tankTag         储罐的流体容器 NBT（单流体为含 {@code Fluid} 的复合标签，
 *                        多流体为含 {@code Fluids} 列表的复合标签）
 * @param multi           {@code true} = 多流体储罐（读 Fluids 列表），{@code false} = 单流体储罐（读 Fluid）
 * @param capacity        有效容量（含增强/无限标记后的显示容量）
 * @param infiniteCapacity 是否无限容量（容量行显示 ∞）
 */
public record FluidTankTooltip(
    CompoundTag tankTag, boolean multi, int capacity, boolean infiniteCapacity, boolean showCapacity
) implements TooltipComponent {
    public FluidTankTooltip(CompoundTag tankTag, boolean multi, int capacity, boolean infiniteCapacity) {
        this(tankTag, multi, capacity, infiniteCapacity, true);
    }
}
