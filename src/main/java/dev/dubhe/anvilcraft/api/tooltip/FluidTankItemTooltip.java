package dev.dubhe.anvilcraft.api.tooltip;

import dev.dubhe.anvilcraft.util.UnitUtil;
import net.minecraft.ChatFormatting;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.neoforged.neoforge.fluids.FluidStack;

import java.util.ArrayList;
import java.util.List;

public final class FluidTankItemTooltip {
    private static final String TAG_TANK = "Tank";
    private static final String TAG_FLUID = "Fluid";
    private static final String TAG_FLUIDS = "Fluids";
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
        List<TooltipFluid> fluids = readSingleFluid(tankTag, context.registries(), capacity);
        append(tooltip, fluids, totalAmount(fluids), capacity, false);
    }

    public static void appendExpandableTank(
        ItemStack stack,
        Item.TooltipContext context,
        List<Component> tooltip,
        int baseCapacity,
        int enhancedCapacity
    ) {
        CompoundTag tankTag = getTankTag(stack);
        boolean enhanced = tankTag.getBoolean(TAG_ENHANCED);
        boolean infinite = enhanced && tankTag.getBoolean(TAG_INFINITE);
        int capacity = enhanced ? enhancedCapacity : baseCapacity;
        List<TooltipFluid> fluids = readSingleFluid(tankTag, context.registries(), capacity);
        if (infinite && !fluids.isEmpty()) {
            fluids.set(0, new TooltipFluid(fluids.get(0).fluid(), true));
        }
        append(tooltip, fluids, totalAmount(fluids), capacity, infinite);
    }

    public static void appendMultiTank(
        ItemStack stack,
        Item.TooltipContext context,
        List<Component> tooltip,
        int capacity
    ) {
        CompoundTag tankTag = getTankTag(stack);
        List<TooltipFluid> fluids = readMultipleFluids(tankTag, context.registries());
        append(tooltip, fluids, totalAmount(fluids), capacity, tankTag.getBoolean(TAG_ENHANCED));
    }

    private static CompoundTag getTankTag(ItemStack stack) {
        CustomData data = stack.get(DataComponents.BLOCK_ENTITY_DATA);
        if (data == null || data.isEmpty()) return new CompoundTag();
        return data.copyTag().getCompound(TAG_TANK);
    }

    private static List<TooltipFluid> readSingleFluid(
        CompoundTag tankTag,
        HolderLookup.Provider registries,
        int capacity
    ) {
        if (registries == null || !tankTag.contains(TAG_FLUID, Tag.TAG_COMPOUND)) return new ArrayList<>();
        FluidStack fluid = FluidStack.parseOptional(registries, tankTag.getCompound(TAG_FLUID));
        if (fluid.isEmpty()) return new ArrayList<>();
        fluid.setAmount(Math.min(fluid.getAmount(), capacity));
        return new ArrayList<>(List.of(new TooltipFluid(fluid, false)));
    }

    private static List<TooltipFluid> readMultipleFluids(
        CompoundTag tankTag,
        HolderLookup.Provider registries
    ) {
        List<TooltipFluid> fluids = new ArrayList<>();
        if (registries == null) return fluids;
        ListTag fluidsTag = tankTag.getList(TAG_FLUIDS, Tag.TAG_COMPOUND);
        for (int i = 0; i < fluidsTag.size(); i++) {
            CompoundTag storedFluidTag = fluidsTag.getCompound(i);
            FluidStack fluid = FluidStack.parseOptional(registries, storedFluidTag.getCompound(TAG_FLUID));
            if (!fluid.isEmpty()) {
                fluids.add(new TooltipFluid(fluid, storedFluidTag.getBoolean(TAG_INFINITE)));
            }
        }
        return fluids;
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
        int capacity,
        boolean infiniteCapacity
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
        Component capacityLine = infiniteCapacity
            ? Component.translatable(
                "tooltip.anvilcraft.fluid_tank.capacity.value.infinity",
                UnitUtil.fluidUnit(amount, false)
            )
            : Component.translatable(
                "tooltip.anvilcraft.fluid_tank.capacity.value",
                UnitUtil.fluidUnit(amount, false),
                UnitUtil.fluidUnit(capacity, false)
            );
        tooltip.add(capacityLine.copy().withStyle(ChatFormatting.GRAY));
    }

    private record TooltipFluid(FluidStack fluid, boolean infinite) {
    }
}
