package dev.dubhe.anvilcraft.api.tooltip.impl;

import dev.dubhe.anvilcraft.api.tooltip.providers.ITooltipProvider;
import dev.dubhe.anvilcraft.block.BurningHeaterBlock;
import dev.dubhe.anvilcraft.block.entity.BurningHeaterBlockEntity;
import dev.dubhe.anvilcraft.client.AnvilCraftClient;
import dev.dubhe.anvilcraft.util.CompatUtil;
import dev.dubhe.anvilcraft.util.FormattingUtil;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.List;

public class BurningHeaterTooltipProvider extends ITooltipProvider.BlockEntityTooltipProvider {
    @Override
    public boolean accepts(BlockEntity value) {
        return value instanceof BurningHeaterBlockEntity;
    }

    @Override
    public List<Component> tooltip(BlockEntity value) {
        if (CompatUtil.HAS_JADE.get() && AnvilCraftClient.CONFIG.doNotShowTooltipWhenJadePresent) return List.of();
        if (!(value instanceof BurningHeaterBlockEntity be)) return List.of();

        List<Component> lines = new ArrayList<>();

        BlockState state = be.getBlockState();
        int level;
        if (state.getBlock() instanceof BurningHeaterBlock) {
            level = state.getValue(BurningHeaterBlock.LEVEL);
        } else {
            // fallback: infer from burnTime
            int burnTime = be.getBurnTime();
            if (burnTime >= BurningHeaterBlockEntity.LIT_THRESHOLD) {
                level = 2;
            } else if (burnTime > 0) {
                level = 1;
            } else {
                level = 0;
            }
        }

        // State header (BLUE)
        lines.add(Component.translatable("tooltip.anvilcraft.burning_heater.state_name")
            .withStyle(ChatFormatting.BLUE));

        // State value (GRAY)
        String stateKey = switch (level) {
            case 1 -> "tooltip.anvilcraft.burning_heater.state_name.smoldering";
            case 2 -> "tooltip.anvilcraft.burning_heater.state_name.lit";
            default -> "tooltip.anvilcraft.burning_heater.state_name.off";
        };
        lines.add(Component.translatable(stateKey).withStyle(ChatFormatting.GRAY));

        // Burn time header (BLUE) + value (GRAY)
        int burnTime = be.getDisplayBurnTime();
        if (burnTime > 0) {
            lines.add(Component.translatable("tooltip.anvilcraft.burning_heater.burn_time_label")
                .withStyle(ChatFormatting.BLUE));
            lines.add(Component.literal(
                "  " + FormattingUtil.toFormattedTime(burnTime, 1)
            ).withStyle(ChatFormatting.GRAY));
        }

        // Can smelt indicator (BLUE + GRAY)
        lines.add(Component.translatable("tooltip.anvilcraft.burning_heater.can_smelt")
            .withStyle(ChatFormatting.BLUE));
        if (level == 2) {
            lines.add(Component.translatable("tooltip.anvilcraft.burning_heater.can_smelt.yes")
                .withStyle(ChatFormatting.GRAY));
        } else {
            lines.add(Component.translatable("tooltip.anvilcraft.burning_heater.can_smelt.no")
                .withStyle(ChatFormatting.DARK_GRAY));
        }

        return lines;
    }

    @Override
    public int priority() {
        return 0;
    }
}

