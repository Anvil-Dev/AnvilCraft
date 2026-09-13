package dev.dubhe.anvilcraft.inventory.tooltip;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.inventory.tooltip.TooltipComponent;

public record StoragePortTooltip(CompoundTag blockEntityTag) implements TooltipComponent {
}
