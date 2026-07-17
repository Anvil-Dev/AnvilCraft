package dev.dubhe.anvilcraft.api.tooltip.impl;

import dev.dubhe.anvilcraft.api.fluid.IFluidHandlerHolder;
import dev.dubhe.anvilcraft.api.tooltip.providers.ITooltipProvider;
import dev.dubhe.anvilcraft.block.entity.FishTankBlockEntity;
import dev.dubhe.anvilcraft.block.entity.FluidTankBlockEntity;
import dev.dubhe.anvilcraft.block.entity.LargeFluidTankBlockEntity;
import dev.dubhe.anvilcraft.client.AnvilCraftClient;
import dev.dubhe.anvilcraft.util.CompatUtil;
import dev.dubhe.anvilcraft.util.UnitUtil;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;

import java.util.ArrayList;
import java.util.List;

public class FluidTankTooltipProvider extends ITooltipProvider.BlockEntityTooltipProvider {
    @Override
    public boolean accepts(BlockEntity value) {
        return value instanceof FluidTankBlockEntity || value instanceof LargeFluidTankBlockEntity
            || value instanceof FishTankBlockEntity;
    }

    @Override
    public List<Component> tooltip(BlockEntity value) {
        if (CompatUtil.HAS_JADE.get() && AnvilCraftClient.CONFIG.doNotShowTooltipWhenJadePresent) return List.of();

        final List<Component> lines = new ArrayList<>();
        if (value instanceof IFluidHandlerHolder tank) {
            boolean original = false;
            LocalPlayer player = Minecraft.getInstance().player;
            if (player != null && player.isShiftKeyDown()) {
                original = true;
            }
            IFluidHandler handler = tank.getFluidHandler();
            long amount = 0;
            List<FluidStack> fluids = new ArrayList<>();
            for (int i = 0; i < handler.getTanks(); i++) {
                FluidStack fluid = handler.getFluidInTank(i);
                if (fluid.isEmpty()) continue;
                amount += fluid.getAmount();
                fluids.add(fluid);
            }
            if (amount > 0) {
                lines.add(Component.translatable("tooltip.anvilcraft.fluid_tank.fluid")
                    .setStyle(Style.EMPTY.applyFormat(ChatFormatting.BLUE)));
                for (FluidStack fluid : fluids) {
                    Component fluidAmount = value instanceof LargeFluidTankBlockEntity largeTank
                        && largeTank.isInfinite(fluid)
                        ? Component.literal(" ∞")
                        : Component.literal(" " + UnitUtil.fluidUnit(fluid.getAmount(), original));
                    lines.add(Component.literal("  ").append(fluid.getHoverName()).append(fluidAmount)
                        .setStyle(Style.EMPTY.applyFormat(ChatFormatting.GRAY)));
                }
            }
            lines.add(Component.translatable("tooltip.anvilcraft.fluid_tank.capacity")
                    .setStyle(Style.EMPTY.applyFormat(ChatFormatting.BLUE)));
            boolean infinity = value instanceof LargeFluidTankBlockEntity largeTank && largeTank.isEnhanced()
                || value instanceof FluidTankBlockEntity smallTank && smallTank.isInfinite();
            if (infinity) {
                lines.add(Component.translatable(
                        "tooltip.anvilcraft.fluid_tank.capacity.value.infinity",
                        UnitUtil.fluidUnit(amount, original)
                    )
                    .setStyle(Style.EMPTY.applyFormat(ChatFormatting.GRAY)));
            } else {
                int capacity = value instanceof LargeFluidTankBlockEntity
                    ? LargeFluidTankBlockEntity.BASE_CAPACITY
                    : handler.getTankCapacity(0);
                lines.add(Component.translatable(
                        "tooltip.anvilcraft.fluid_tank.capacity.value",
                        UnitUtil.fluidUnit(amount, original),
                        UnitUtil.fluidUnit(capacity, original)
                    )
                    .setStyle(Style.EMPTY.applyFormat(ChatFormatting.GRAY)));
            }
        }
        return lines;
    }

    @Override
    public int priority() {
        return 0;
    }
}
