package dev.dubhe.anvilcraft.api.power;

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
    default PowerComponentType getType() {
        return PowerComponentType.PRODUCER;
    }
}
