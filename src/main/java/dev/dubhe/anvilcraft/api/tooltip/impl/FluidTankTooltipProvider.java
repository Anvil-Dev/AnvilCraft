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
            int amount = tank.getFluidHandler().getFluidInTank(0).getAmount();
            if (amount > 0) {
                lines.add(Component.translatable("tooltip.anvilcraft.fluid_tank.fluid")
                    .setStyle(Style.EMPTY.applyFormat(ChatFormatting.BLUE)));
                lines.add(Component.literal("  ").append(tank.getFluidHandler().getFluidInTank(0).getHoverName())
                    .setStyle(Style.EMPTY.applyFormat(ChatFormatting.GRAY)));
            }
            lines.add(Component.translatable("tooltip.anvilcraft.fluid_tank.capacity")
                    .setStyle(Style.EMPTY.applyFormat(ChatFormatting.BLUE)));
            if (value instanceof LargeFluidTankBlockEntity be && be.isInfinity()) {
                lines.add(Component.translatable(
                        "tooltip.anvilcraft.fluid_tank.capacity.value.infinity",
                        UnitUtil.fluidUnit(amount, original)
                    )
                    .setStyle(Style.EMPTY.applyFormat(ChatFormatting.GRAY)));
            } else {
                lines.add(Component.translatable(
                        "tooltip.anvilcraft.fluid_tank.capacity.value",
                        UnitUtil.fluidUnit(amount, original),
                        UnitUtil.fluidUnit(tank.getFluidHandler().getTankCapacity(0), original)
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
