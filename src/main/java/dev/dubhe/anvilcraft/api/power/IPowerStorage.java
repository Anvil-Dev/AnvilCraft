package dev.dubhe.anvilcraft.api.power;

import org.jetbrains.annotations.NotNull;

/**
 * 储电
 */
public interface IPowerStorage extends IPowerProducer, IPowerConsumer {
    /**
     * 输入电量
     *
     * @param power 输入值
     * @return 无法输入的值
     */
    int insert(int power);

    /**
     * 获取电量
     *
     * @param power 想要获取的值
     * @return 实际获取的值
     */
    int extract(int power);


    /**
     * 获取已存储的电量
     *
     * @return 电量值
     */
    int getPowerAmount();

    /**
     * 获取储电容量
     *
     * @return 储电容量
     */
    int getCapacity();

    @Override
    default @NotNull PowerComponentType getComponentType() {
        return PowerComponentType.STORAGE;
    }
}
