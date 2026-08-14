package dev.dubhe.anvilcraft.api.energy;

import net.minecraft.core.Direction;
import net.neoforged.neoforge.energy.IEnergyStorage;

import javax.annotation.Nullable;

/// 持有能量存储的
public interface IEnergyHandlerHolder {
    @Nullable IEnergyStorage getEnergyStorage(@Nullable Direction direction);
}
