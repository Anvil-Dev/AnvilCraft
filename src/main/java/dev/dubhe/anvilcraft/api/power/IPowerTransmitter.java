package dev.dubhe.anvilcraft.api.power;

import dev.dubhe.anvilcraft.AnvilCraft;

/**
 * 电力中继器
 */
public interface IPowerTransmitter extends IPowerComponent {
    @Override
    default int getRange() {
        return AnvilCraft.CONFIG.machines.powerTransmitterRange;
    }

    @Override
    default PowerComponentType getComponentType() {
        return PowerComponentType.TRANSMITTER;
    }
}
