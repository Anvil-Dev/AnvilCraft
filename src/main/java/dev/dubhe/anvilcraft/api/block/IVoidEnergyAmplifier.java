package dev.dubhe.anvilcraft.api.block;

/**
 * 方块实现此接口后，可修改虚空能发电机的虚空能增幅量。
 */
public interface IVoidEnergyAmplifier {
    /**
     * @return 计入虚空能发电机方块数量时的增幅量，可为负数
     */
    int getVoidEnergyAmplification();
}
