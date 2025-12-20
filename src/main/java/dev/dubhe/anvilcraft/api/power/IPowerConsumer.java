package dev.dubhe.anvilcraft.api.power;

/**
 * 用电
 */
public interface IPowerConsumer extends IPowerComponent {
    default int getInputPower() {
        return 0;
    }

    @Override
    default PowerComponentType getComponentType() {
        return PowerComponentType.CONSUMER;
    }
}
