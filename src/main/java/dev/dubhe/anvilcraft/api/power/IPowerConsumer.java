package dev.dubhe.anvilcraft.api.power;

import org.jetbrains.annotations.NotNull;

/**
 * 用电
 */
public interface IPowerConsumer extends IPowerComponent {
    default int getInputPower() {
        return 0;
    }

    @Override
    default @NotNull PowerComponentType getComponentType() {
        return PowerComponentType.CONSUMER;
    }
}
