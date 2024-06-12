package dev.dubhe.anvilcraft.api.energy.forge;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.energy.IEnergyStorage;

import org.jetbrains.annotations.NotNull;

public class EnergyHelperImpl {
    /**
     * 插入能量
     */
    public static void insertEnergy(
            @NotNull Level level, BlockPos pos, Direction direction, int amount) {
        BlockEntity blockEntity = level.getBlockEntity(pos);
        if (blockEntity != null) {
            LazyOptional<IEnergyStorage> capability =
                    blockEntity.getCapability(ForgeCapabilities.ENERGY, direction);
            capability.ifPresent(energyStorage -> energyStorage.receiveEnergy(amount, false));
        }
    }
}
