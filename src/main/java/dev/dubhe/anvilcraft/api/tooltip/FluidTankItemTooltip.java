package dev.dubhe.anvilcraft.api.tooltip;

import dev.dubhe.anvilcraft.inventory.tooltip.FluidTankTooltip;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.inventory.tooltip.TooltipComponent;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;

import java.util.Optional;

public final class FluidTankItemTooltip {
    private static final String TAG_TANK = "Tank";
    private static final String TAG_FLUID = "Fluid";
    private static final String TAG_ENHANCED = "Enhanced";
    private static final String TAG_INFINITE = "Infinite";

    private FluidTankItemTooltip() {
    }

    /** 单流体储罐的 tooltip 数据（携带 Tank NBT，客户端解析渲染为 图标+文字）。 */
    public static Optional<TooltipComponent> singleFluidTooltipImage(
        ItemStack stack, int baseCapacity, int enhancedCapacity
    ) {
        CompoundTag tankTag = getTankTag(stack);
        boolean enhanced = tankTag.getBoolean(TAG_ENHANCED);
        int capacity = enhanced ? enhancedCapacity : baseCapacity;
        boolean infinite = enhanced && tankTag.getBoolean(TAG_INFINITE);
        return Optional.of(new FluidTankTooltip(tankTag, false, capacity, infinite));
    }

    /** 多流体储罐的 tooltip 数据（携带 Tank NBT，客户端解析渲染为 图标+文字）。 */
    public static Optional<TooltipComponent> multiFluidTooltipImage(ItemStack stack, int baseCapacity) {
        CompoundTag tankTag = getTankTag(stack);
        boolean infinite = tankTag.getBoolean(TAG_ENHANCED);
        return Optional.of(new FluidTankTooltip(tankTag, true, baseCapacity, infinite));
    }

    /** 创造流体储罐的 tooltip 数据（流体恒为无限）。 */
    public static Optional<TooltipComponent> creativeTankTooltipImage(ItemStack stack) {
        CompoundTag infinityFluid = getBlockEntityData(stack).getCompound("infinityFluid");
        if (!infinityFluid.contains(TAG_FLUID, Tag.TAG_COMPOUND)) {
            return Optional.empty();
        }
        return Optional.of(new FluidTankTooltip(infinityFluid, false, Integer.MAX_VALUE, true, false));
    }

    private static CompoundTag getTankTag(ItemStack stack) {
        return getBlockEntityData(stack).getCompound(TAG_TANK);
    }

    private static CompoundTag getBlockEntityData(ItemStack stack) {
        CustomData data = stack.get(DataComponents.BLOCK_ENTITY_DATA);
        if (data == null || data.isEmpty()) return new CompoundTag();
        return data.copyTag();
    }

}
