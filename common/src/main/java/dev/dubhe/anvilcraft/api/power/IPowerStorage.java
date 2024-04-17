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

    @Override
    default @NotNull PowerComponentType getComponentType() {
        return PowerComponentType.STORAGE;
    }
}
