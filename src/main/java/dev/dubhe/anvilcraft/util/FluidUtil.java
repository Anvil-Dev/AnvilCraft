package dev.dubhe.anvilcraft.util;

import net.minecraft.ChatFormatting;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.fluids.FluidStack;

import java.util.ArrayList;
import java.util.List;

public class FluidUtil {
    public static List<Component> getTooltip(FluidStack stack, int capacity, TooltipFlag flag) {
        List<Component> tooltip = new ArrayList<>();
        if (!appendName(tooltip, stack, flag)) {
            return tooltip;
        }

        MutableComponent amountString = Component.translatable(
            "tooltip.anvilcraft.fluid.amount",
            UnitUtil.fluidUnit(stack.getAmount(), flag.hasShiftDown()),
            UnitUtil.fluidUnit(capacity, flag.hasShiftDown())
        );
        tooltip.add(amountString.withStyle(ChatFormatting.GRAY));
        appendModName(tooltip, stack);

        return tooltip;
    }
    public static List<Component> getFilterTooltip(FluidStack stack, TooltipFlag flag) {
        List<Component> tooltip = new ArrayList<>();
        if (!appendName(tooltip, stack, flag)) {
            return tooltip;
        }
        appendModName(tooltip, stack);

        return tooltip;
    }

    private static boolean appendName(List<Component> tooltip, FluidStack stack, TooltipFlag flag) {
        Fluid fluid = stack.getFluid();
        if (fluid.isSame(Fluids.EMPTY)) {
            return false;
        }

        Component displayName = stack.getHoverName();
        if (!fluid.isSource(fluid.defaultFluidState())) {
            displayName = Component.translatable("tooltip.anvilcraft.fluid.flowing", displayName);
        }
        tooltip.add(displayName);

        if (flag.isAdvanced()) {
            ResourceLocation id = BuiltInRegistries.FLUID.getKey(fluid);
            if (id != BuiltInRegistries.FLUID.getDefaultKey()) {
                MutableComponent advancedId = Component.literal(id.toString()).withStyle(ChatFormatting.DARK_GRAY);
                tooltip.add(advancedId);
            }
        }
        return true;
    }

    private static void appendModName(List<Component> tooltip, FluidStack stack) {
        MutableComponent modName = stack.getFluidHolder().unwrapKey().flatMap(
            key -> ModList.get().getModContainerById(key.location().getNamespace())
        ).map(
            container -> Component.literal(container.getModInfo().getDisplayName())
        ).orElse(Component.literal("<unknown>")).withStyle(ChatFormatting.ITALIC, ChatFormatting.BLUE);
        tooltip.add(modName);
    }
}
