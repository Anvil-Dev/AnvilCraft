package dev.dubhe.anvilcraft.api.power;

/**
 * 用电
 */
public interface IPowerConsumer extends IPowerComponent {
    /**
     * @return 输入功率
     */
    default int getInputPower() {
        return 0;
    }

    @Override
    default PowerComponentType getType() {
        return PowerComponentType.CONSUMER;
    }
}
