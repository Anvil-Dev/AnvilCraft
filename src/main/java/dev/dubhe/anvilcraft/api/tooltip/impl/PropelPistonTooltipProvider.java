package dev.dubhe.anvilcraft.api.tooltip.impl;

import dev.dubhe.anvilcraft.api.tooltip.providers.ITooltipProvider;
import dev.dubhe.anvilcraft.block.PropelPiston;
import dev.dubhe.anvilcraft.block.entity.PropelPistonBlockEntity;
import dev.dubhe.anvilcraft.util.UnitUtil;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.List;

public class PropelPistonTooltipProvider extends ITooltipProvider.BlockEntityTooltipProvider {
    @Override
    public boolean accepts(BlockEntity blockEntity) {
        return blockEntity instanceof PropelPistonBlockEntity;
    }

    @Override
    public List<Component> tooltip(BlockEntity blockEntity) {
        Level level = blockEntity.getLevel();
        BlockPos pos = blockEntity.getBlockPos();
        if (level != null) {
            BlockState state = level.getBlockState(pos);
            if (state.getValue(PropelPiston.MOVING)) {
                return List.of();
            }
        }
        boolean original = false;
        LocalPlayer player = Minecraft.getInstance().player;
        if (player != null && player.isShiftKeyDown()) {
            original = true;
        }
        List<Component> tooltips = new ArrayList<>();
        if (blockEntity instanceof PropelPistonBlockEntity propelPistonBlockEntity) {
            int storedEnergy = propelPistonBlockEntity.getStoredEnergy();
            String count = String.format("%.0f", Math.ceil(storedEnergy / 5f));
            tooltips.add(Component.translatable("tooltip.anvilcraft.propel_piston.state").withStyle(ChatFormatting.BLUE));
            tooltips.add(Component.translatable("tooltip.anvilcraft.propel_piston.remaining_energy",
                    UnitUtil.energyUnit(storedEnergy, original))
                .withStyle(ChatFormatting.GRAY));
            tooltips.add(Component.translatable("tooltip.anvilcraft.propel_piston.remaining_push", count)
                .withStyle(ChatFormatting.GRAY));
        }
        return tooltips;
    }

    @Override
    public int priority() {
        return 0;
    }
}
