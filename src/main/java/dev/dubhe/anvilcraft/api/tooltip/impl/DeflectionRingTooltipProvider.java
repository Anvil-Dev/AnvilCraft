package dev.dubhe.anvilcraft.api.tooltip.impl;

import dev.dubhe.anvilcraft.block.DeflectionRingBlock;
import dev.dubhe.anvilcraft.block.entity.DeflectionRingBlockEntity;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

import java.util.ArrayList;
import java.util.List;

public class DeflectionRingTooltipProvider extends PowerComponentTooltipProvider {

    @Override
    public boolean accepts(BlockEntity entity) {
        return entity instanceof DeflectionRingBlockEntity;
    }

    @Override
    public int priority() {
        return -1;
    }

    @Override
    public List<Component> tooltip(BlockEntity blockEntity) {
        final List<Component> lines = new ArrayList<>();
        if (!(blockEntity.getBlockState().getBlock() instanceof DeflectionRingBlock deflectionRingBlock)) return lines;
        BlockPos center = deflectionRingBlock.getMainPartPos(blockEntity.getBlockPos(), blockEntity.getBlockState());
        Level level = blockEntity.getLevel();
        if (level == null) return lines;
        if (!(level.getBlockEntity(center) instanceof DeflectionRingBlockEntity deflectionRingBlockEntity)) return lines;
        lines.add(
                Component.translatable("screen.anvilcraft.deflection_ring.state")
                        .setStyle(Style.EMPTY.applyFormat(ChatFormatting.BLUE))
        );
        lines.add(
                Component.translatable(
                        "screen.anvilcraft.deflection_ring.speed",
                        ((int) (deflectionRingBlockEntity.getLastEntitySpeed() * 100) / 100.0)
                ).setStyle(Style.EMPTY.applyFormat(ChatFormatting.GRAY))
        );
        return lines;
    }
}
