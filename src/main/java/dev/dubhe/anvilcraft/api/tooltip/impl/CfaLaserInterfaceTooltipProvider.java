package dev.dubhe.anvilcraft.api.tooltip.impl;

import dev.dubhe.anvilcraft.api.tooltip.providers.ITooltipProvider;
import dev.dubhe.anvilcraft.block.entity.CelestialForgingAnvilLaserInterfaceBlockEntity;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;

import java.util.ArrayList;
import java.util.List;

public class CfaLaserInterfaceTooltipProvider extends ITooltipProvider.BlockEntityTooltipProvider {
    @Override
    public boolean accepts(BlockEntity value) {
        return value instanceof CelestialForgingAnvilLaserInterfaceBlockEntity;
    }

    @Override
    public List<Component> tooltip(BlockEntity value) {
        if (!(value instanceof CelestialForgingAnvilLaserInterfaceBlockEntity laser)) return List.of();
        List<Component> lines = new ArrayList<>();

        int received = laser.getReceivedLaserLevel();
        int required = laser.getRequiredLaserLevel();
        boolean valid = laser.isLaserValid();

        if (received > 0) {
            lines.add(Component.translatable("screen.anvilcraft.cfa.laser_interface.received", received)
                .withStyle(ChatFormatting.AQUA));
        } else {
            lines.add(Component.translatable("screen.anvilcraft.cfa.laser_interface.no_laser")
                .withStyle(ChatFormatting.DARK_GRAY));
        }

        if (required > 0) {
            lines.add(Component.translatable("screen.anvilcraft.cfa.laser_interface.required", required)
                .withStyle(ChatFormatting.YELLOW));
            if (valid) {
                lines.add(Component.translatable("screen.anvilcraft.cfa.laser_interface.valid")
                    .withStyle(ChatFormatting.GREEN));
            } else {
                lines.add(Component.translatable("screen.anvilcraft.cfa.laser_interface.invalid")
                    .withStyle(ChatFormatting.RED));
            }
        }

        return lines;
    }

    @Override
    public ItemStack icon(BlockEntity value) {
        return ItemStack.EMPTY;
    }

    @Override
    public int priority() {
        return -1; // Higher priority than PowerComponentTooltipProvider
    }
}
