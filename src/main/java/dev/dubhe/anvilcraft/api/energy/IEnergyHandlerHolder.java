package dev.dubhe.anvilcraft.api.energy;

import net.minecraft.core.Direction;
import net.neoforged.neoforge.transfer.energy.EnergyHandler;
import org.jspecify.annotations.Nullable;

/// 持有 EnergyHandler 的
public interface IEnergyHandlerHolder {
    @Nullable EnergyHandler getEnergyHandler(@Nullable Direction direction);
}
