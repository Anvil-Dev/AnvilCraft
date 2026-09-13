package dev.dubhe.anvilcraft.inventory.tooltip;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.inventory.tooltip.TooltipComponent;

public record FluidTankTooltip(
    CompoundTag tankTag, boolean multi, int capacity, boolean infiniteCapacity, boolean showCapacity
) implements TooltipComponent {
    public FluidTankTooltip(CompoundTag tankTag, boolean multi, int capacity, boolean infiniteCapacity) {
        this(tankTag, multi, capacity, infiniteCapacity, true);
    }
}
