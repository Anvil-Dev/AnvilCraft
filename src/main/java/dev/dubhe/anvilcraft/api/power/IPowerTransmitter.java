package dev.dubhe.anvilcraft.api.power;

import dev.dubhe.anvilcraft.AnvilCraft;
import org.jetbrains.annotations.NotNull;

/**
 * 电力中继器
 */
public interface IPowerTransmitter extends IPowerComponent {
    @Override
    default int getRange() {
        return AnvilCraft.config.powerTransmitterRange;
    }

    @Override
    default @NotNull PowerComponentType getComponentType() {
        return PowerComponentType.TRANSMITTER;
    }
}
