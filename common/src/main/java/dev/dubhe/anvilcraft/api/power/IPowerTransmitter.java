package dev.dubhe.anvilcraft.api.power;

/**
 * 电力中继器
 */
public interface IPowerTransmitter extends IPowerComponent {
    @Override
    default PowerComponentType getType() {
        return PowerComponentType.TRANSMITTER;
    }
}
