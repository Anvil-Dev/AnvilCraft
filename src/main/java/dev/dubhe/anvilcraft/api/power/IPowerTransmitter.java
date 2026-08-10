package dev.dubhe.anvilcraft.api.power;

import dev.dubhe.anvilcraft.AnvilCraft;

/// 电力中继器
public interface IPowerTransmitter extends IPowerComponent {
    @Override
    default int getRange() {
        return AnvilCraft.CONFIG.powerTransmitterRange;
    }

    @Override
    default PowerComponentType getComponentType() {
        return PowerComponentType.TRANSMITTER;
    }

    @Override
    default PowerComponentInfo toPowerComponentInfo() {
        return new PowerComponentInfo(
            this.getPos(),
            0,
            0,
            0,
            0,
            this.getRange(),
            this.getShape(),
            PowerComponentType.TRANSMITTER
        );
    }
}
