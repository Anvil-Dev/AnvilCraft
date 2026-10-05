package dev.dubhe.anvilcraft.api.block;

/**
 * 方块实现此接口后，可修改虚空能发电机的虚空能增幅量。
 */
public interface IVoidEnergyAmplifier {
    /**
     * 返回该方块提供的虚空能增幅量。
     *
     * @return 计入虚空能发电机方块数量时的增幅量，负数为增益
     */
    int getVoidEnergyAmplification();
}
