package dev.dubhe.anvilcraft.api.power;

import org.jetbrains.annotations.NotNull;

/**
 * 电力中继器
 */
public interface IPowerTransmitter extends IPowerComponent {
    @Override
    default @NotNull PowerComponentType getComponentType() {
        return PowerComponentType.TRANSMITTER;
    }
}
