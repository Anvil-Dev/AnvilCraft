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
        boolean requiredGamma = laser.isRequiredGamma();
        boolean valid = laser.isLaserValid();
        boolean emittingGamma = laser.isEmittingGamma();
        int gammaLevel = laser.getGammaLevel();
        int laserLevel = laser.getLaserLevel();

        if (received > 0) {
            if (laser.isReceivedGamma()) {
                // Receiving gamma laser — purple text
                lines.add(Component.translatable("screen.anvilcraft.cfa.laser_interface.received_gamma", received)
                    .withStyle(ChatFormatting.DARK_PURPLE));
            } else {
                // Receiving red laser — red text
                lines.add(Component.translatable("screen.anvilcraft.cfa.laser_interface.received", received)
                    .withStyle(ChatFormatting.RED));
            }
        } else if (emittingGamma && gammaLevel > 0) {
            // Emitting gamma laser — purple text
            lines.add(Component.translatable("screen.anvilcraft.cfa.laser_interface.emitting_gamma", gammaLevel)
                .withStyle(ChatFormatting.DARK_PURPLE));
        } else if (laserLevel > 0) {
            // Emitting normal (red) laser — red text
            lines.add(Component.translatable("screen.anvilcraft.cfa.laser_interface.emitting", laserLevel)
                .withStyle(ChatFormatting.RED));
        } else {
            // No laser activity
            lines.add(Component.translatable("screen.anvilcraft.cfa.laser_interface.no_laser")
                .withStyle(ChatFormatting.DARK_GRAY));
        }

        if (required > 0) {
            if (requiredGamma) {
                lines.add(Component.translatable("screen.anvilcraft.cfa.laser_interface.required_gamma", required)
                    .withStyle(ChatFormatting.YELLOW));
            } else {
                lines.add(Component.translatable("screen.anvilcraft.cfa.laser_interface.required", required)
                    .withStyle(ChatFormatting.YELLOW));
            }
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
