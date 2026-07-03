package dev.dubhe.anvilcraft.api.tooltip.impl;

import dev.dubhe.anvilcraft.api.tooltip.providers.ITooltipProvider;
import dev.dubhe.anvilcraft.block.CrabTrapBlock;
import dev.dubhe.anvilcraft.client.AnvilCraftClient;
import dev.dubhe.anvilcraft.init.block.ModBlocks;
import dev.dubhe.anvilcraft.util.CompatUtil;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

import java.util.List;

public class CrabTrapTooltipProvider extends ITooltipProvider.BlockTooltipProvider {
    @Override
    public boolean accepts(Level level, BlockPos pos, BlockState state) {
        return state.is(ModBlocks.CRAB_TRAP);
    }

    @Override
    public List<Component> tooltip(Level level, BlockPos pos, BlockState state) {
        if (CompatUtil.HAS_JADE.get() && AnvilCraftClient.CONFIG.doNotShowTooltipWhenJadePresent) {
            return List.of();
        }
        List<Component> lines = new ObjectArrayList<>();
        int fishing = state.getValue(CrabTrapBlock.FISHING);
        lines.add(Component.translatable("tooltip.anvilcraft.crab_trap.state_name").withStyle(ChatFormatting.BLUE));
        lines.add(Component.literal("  ").append(Component.translatable("tooltip.anvilcraft.crab_trap.jade.fishing", fishing)));
        return lines;
    }

    @Override
    public int priority() {
        return 0;
    }
}
