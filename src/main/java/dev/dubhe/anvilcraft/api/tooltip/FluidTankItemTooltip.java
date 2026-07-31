package dev.dubhe.anvilcraft.api.tooltip;

import dev.dubhe.anvilcraft.util.UnitUtil;
import net.minecraft.ChatFormatting;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.TypedEntityData;
import net.neoforged.neoforge.fluids.FluidStack;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/// 为储罐类物品渲染「已存流体 / 容量」提示行
public final class FluidTankItemTooltip {
    private static final String TAG_TANK = "Tank";
    private static final String TAG_FLUID = "Fluid";
    private static final String TAG_FLUIDS = "Fluids";
    private static final String TAG_ENHANCED = "Enhanced";
    private static final String TAG_INFINITE = "Infinite";

    private FluidTankItemTooltip() {
    }

    /// 容量固定的储罐（如储罐矿车）
    public static void appendFixedTank(
        ItemStack stack,
        Item.TooltipContext context,
        Consumer<Component> tooltip,
        int capacity
    ) {
        CompoundTag tankTag = FluidTankItemTooltip.getTankTag(stack);
        List<TooltipFluid> fluids = FluidTankItemTooltip.readSingleFluid(tankTag, context.registries(), capacity);
        FluidTankItemTooltip.append(tooltip, fluids, FluidTankItemTooltip.totalAmount(fluids), capacity, false);
    }

    /// 可被门格海绵扩容的单流体储罐
    public static void appendExpandableTank(
        ItemStack stack,
        Item.TooltipContext context,
        Consumer<Component> tooltip,
        int baseCapacity,
        int enhancedCapacity
    ) {
        CompoundTag tankTag = FluidTankItemTooltip.getTankTag(stack);
        boolean enhanced = tankTag.getBooleanOr(FluidTankItemTooltip.TAG_ENHANCED, false);
        boolean infinite = enhanced && tankTag.getBooleanOr(FluidTankItemTooltip.TAG_INFINITE, false);
        int capacity = enhanced ? enhancedCapacity : baseCapacity;
        List<TooltipFluid> fluids = FluidTankItemTooltip.readSingleFluid(tankTag, context.registries(), Integer.MAX_VALUE);
        if (infinite && !fluids.isEmpty()) {
            fluids.set(0, new TooltipFluid(fluids.getFirst().fluid(), true));
        }
        FluidTankItemTooltip.append(tooltip, fluids, FluidTankItemTooltip.totalAmount(fluids), capacity, infinite);
    }

    /// 可同时存放多种流体的大型储罐
    public static void appendMultiTank(
        ItemStack stack,
        Item.TooltipContext context,
        Consumer<Component> tooltip,
        int capacity
    ) {
        CompoundTag tankTag = FluidTankItemTooltip.getTankTag(stack);
        List<TooltipFluid> fluids = FluidTankItemTooltip.readMultipleFluids(tankTag, context.registries());
        FluidTankItemTooltip.append(
            tooltip,
            fluids,
            FluidTankItemTooltip.totalAmount(fluids),
            capacity,
            tankTag.getBooleanOr(FluidTankItemTooltip.TAG_ENHANCED, false)
        );
    }

    /// 读出单罐物品中的流体与是否已扩容，供物品渲染复用
    public static @Nullable SingleTankData readSingleTank(ItemStack stack) {
        CompoundTag tankTag = FluidTankItemTooltip.getTankTag(stack);
        FluidStack fluid = FluidTankItemTooltip.readFluid(tankTag);
        if (fluid.isEmpty()) return null;
        return new SingleTankData(fluid, tankTag.getBooleanOr(FluidTankItemTooltip.TAG_ENHANCED, false));
    }

    /// 读出大型储罐物品中的所有流体，供物品渲染复用
    public static List<FluidStack> readMultiTankFluids(ItemStack stack) {
        List<FluidStack> fluids = new ArrayList<>();
        for (TooltipFluid stored : FluidTankItemTooltip.readMultipleFluids(FluidTankItemTooltip.getTankTag(stack), null)) {
            fluids.add(stored.fluid());
        }
        return fluids;
    }

    /// 大型储罐物品是否处于扩容状态
    public static boolean isMultiTankEnhanced(ItemStack stack) {
        return FluidTankItemTooltip.getTankTag(stack).getBooleanOr(FluidTankItemTooltip.TAG_ENHANCED, false);
    }

    private static CompoundTag getTankTag(ItemStack stack) {
        TypedEntityData<?> data = stack.get(DataComponents.BLOCK_ENTITY_DATA);
        if (data == null) return new CompoundTag();
        return data.copyTagWithoutId().getCompoundOrEmpty(FluidTankItemTooltip.TAG_TANK);
    }

    private static FluidStack readFluid(CompoundTag tag) {
        return tag.read(FluidTankItemTooltip.TAG_FLUID, FluidStack.OPTIONAL_CODEC).orElse(FluidStack.EMPTY);
    }

    private static List<TooltipFluid> readSingleFluid(
        CompoundTag tankTag,
        HolderLookup.@Nullable Provider registries,
        int capacity
    ) {
        FluidStack fluid = FluidTankItemTooltip.readFluid(tankTag);
        if (fluid.isEmpty()) return new ArrayList<>();
        int amount = Math.min(fluid.getAmount(), capacity);
        return new ArrayList<>(List.of(new TooltipFluid(fluid.copyWithAmount(amount), false)));
    }

    private static List<TooltipFluid> readMultipleFluids(
        CompoundTag tankTag,
        HolderLookup.@Nullable Provider registries
    ) {
        List<TooltipFluid> fluids = new ArrayList<>();
        boolean enhanced = tankTag.getBooleanOr(FluidTankItemTooltip.TAG_ENHANCED, false);
        ListTag fluidsTag = tankTag.getListOrEmpty(FluidTankItemTooltip.TAG_FLUIDS);
        for (int i = 0; i < fluidsTag.size(); i++) {
            CompoundTag storedFluidTag = fluidsTag.getCompound(i).orElse(null);
            if (storedFluidTag == null) continue;
            FluidStack fluid = FluidTankItemTooltip.readFluid(storedFluidTag);
            if (!fluid.isEmpty()) {
                fluids.add(new TooltipFluid(
                    fluid,
                    enhanced && storedFluidTag.getBooleanOr(FluidTankItemTooltip.TAG_INFINITE, false)
                ));
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
        Consumer<Component> tooltip,
        List<TooltipFluid> fluids,
        long amount,
        int capacity,
        boolean infiniteCapacity
    ) {
        if (!fluids.isEmpty()) {
            tooltip.accept(Component.translatable("tooltip.anvilcraft.fluid_tank.fluid").withStyle(ChatFormatting.BLUE));
            for (TooltipFluid stored : fluids) {
                String fluidAmount = stored.infinite()
                    ? UnitUtil.INFINITE_POWER
                    : UnitUtil.fluidUnit(stored.fluid().getAmount(), false);
                tooltip.accept(Component.literal("  ")
                    .append(stored.fluid().getHoverName())
                    .append(Component.literal(" " + fluidAmount))
                    .withStyle(ChatFormatting.GRAY));
            }
        }

        tooltip.accept(Component.translatable("tooltip.anvilcraft.fluid_tank.capacity").withStyle(ChatFormatting.BLUE));
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
        tooltip.accept(capacityLine.copy().withStyle(ChatFormatting.GRAY));
    }

    /// 单罐物品里的流体内容
    public record SingleTankData(FluidStack fluid, boolean enhanced) {
    }

    private record TooltipFluid(FluidStack fluid, boolean infinite) {
    }
}
