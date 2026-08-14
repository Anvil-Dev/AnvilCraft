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
        Fluid fluid = stack.getFluid();
        if (fluid.isSame(Fluids.EMPTY)) {
            return tooltip;
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

        MutableComponent amountString = Component.translatable(
            "tooltip.anvilcraft.fluid.amount",
            UnitUtil.fluidUnit(stack.getAmount(), flag.hasShiftDown()),
            UnitUtil.fluidUnit(capacity, flag.hasShiftDown())
        );
        tooltip.add(amountString.withStyle(ChatFormatting.GRAY));

        MutableComponent modName = stack.getFluidHolder().unwrapKey().flatMap(
            key -> ModList.get().getModContainerById(key.location().getNamespace())
        ).map(
            container -> Component.literal(container.getModInfo().getDisplayName())
        ).orElse(Component.literal("<unknown>")).withStyle(ChatFormatting.ITALIC, ChatFormatting.BLUE);
        tooltip.add(modName);

        return tooltip;
    }
}
