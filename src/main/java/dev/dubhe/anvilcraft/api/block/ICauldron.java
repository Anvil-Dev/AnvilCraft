package dev.dubhe.anvilcraft.api.block;

/** 炼药锅实现共有的能力。 */
public interface ICauldron {
    /**
     * 检查此炼药锅是否可以处理包含多个流体输出的配方。
     *
     * @return 是否支持多个流体输出
     */
    default boolean supportsMultipleFluidOutputs() {
        return false;
    }
}
