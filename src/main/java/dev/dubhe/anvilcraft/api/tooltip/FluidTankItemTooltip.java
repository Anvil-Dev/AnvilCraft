package dev.dubhe.anvilcraft.api.tooltip;

import dev.dubhe.anvilcraft.inventory.tooltip.FluidTankTooltip;
import dev.dubhe.anvilcraft.util.UnitUtil;
import net.minecraft.ChatFormatting;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.inventory.tooltip.TooltipComponent;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.neoforged.neoforge.fluids.FluidStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public final class FluidTankItemTooltip {
    private static final String TAG_TANK = "Tank";
    private static final String TAG_FLUID = "Fluid";
    private static final String TAG_ENHANCED = "Enhanced";
    private static final String TAG_INFINITE = "Infinite";

    private FluidTankItemTooltip() {
    }

    public static void appendFixedTank(
        ItemStack stack,
        Item.TooltipContext context,
        List<Component> tooltip,
        int capacity
    ) {
        CompoundTag tankTag = getTankTag(stack);
        List<TooltipFluid> fluids = readSingleFluid(tankTag, Objects.requireNonNull(context.registries()), capacity);
        append(tooltip, fluids, totalAmount(fluids), capacity);
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

    private static List<TooltipFluid> readSingleFluid(
        CompoundTag tankTag,
        HolderLookup.Provider registries,
        int maxAmount
    ) {
        if (!tankTag.contains(TAG_FLUID, Tag.TAG_COMPOUND)) return new ArrayList<>();
        FluidStack fluid = FluidStack.parseOptional(registries, tankTag.getCompound(TAG_FLUID));
        if (fluid.isEmpty()) return new ArrayList<>();
        fluid.setAmount(Math.min(fluid.getAmount(), maxAmount));
        return new ArrayList<>(List.of(new TooltipFluid(fluid, false)));
    }

    private static long totalAmount(List<TooltipFluid> fluids) {
        long amount = 0;
        for (TooltipFluid fluid : fluids) {
            amount += fluid.fluid().getAmount();
        }
        return amount;
    }

    private static void append(
        List<Component> tooltip,
        List<TooltipFluid> fluids,
        long amount,
        int capacity
    ) {
        if (!fluids.isEmpty()) {
            tooltip.add(Component.translatable("tooltip.anvilcraft.fluid_tank.fluid").withStyle(ChatFormatting.BLUE));
            for (TooltipFluid stored : fluids) {
                String fluidAmount = stored.infinite()
                    ? UnitUtil.INFINITE_POWER
                    : UnitUtil.fluidUnit(stored.fluid().getAmount(), false);
                tooltip.add(Component.literal("  ")
                    .append(stored.fluid().getHoverName())
                    .append(Component.literal(" " + fluidAmount))
                    .withStyle(ChatFormatting.GRAY));
            }
        }

        tooltip.add(Component.translatable("tooltip.anvilcraft.fluid_tank.capacity").withStyle(ChatFormatting.BLUE));
        Component capacityLine = Component.translatable(
            "tooltip.anvilcraft.fluid_tank.capacity.value",
            UnitUtil.fluidUnit(amount, false),
            UnitUtil.fluidUnit(capacity, false)
        );
        tooltip.add(capacityLine.copy().withStyle(ChatFormatting.GRAY));
    }

    private record TooltipFluid(FluidStack fluid, boolean infinite) {
    }
}
