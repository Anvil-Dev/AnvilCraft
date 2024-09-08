package dev.dubhe.anvilcraft.api.power;

import org.jetbrains.annotations.NotNull;

/**
 * 发电
 */
public interface IPowerProducer extends IPowerComponent {
    /**
     * @return 输出功率
     */
    default int getOutputPower() {
        return 0;
    }

    @Override
    default @NotNull PowerComponentType getComponentType() {
        return PowerComponentType.PRODUCER;
    }
}
