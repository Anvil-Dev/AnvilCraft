package dev.dubhe.anvilcraft.saved.datafixer;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;

/**
 * 数据格式修复器
 *
 * @apiNote 若类名内需要包含版本号，建议命名为其数据格式的版本。<br>
 *          如某个修复器可以修复从上个版本升级到v2.4版本的数据，则可命名为 {@code V2_4}
 */
public abstract class DataFixer {
    /**
     * 获取该修复器数据格式的版本
     *
     * @return 该修复器数据格式的版本
     * @apiNote 如某修复器负责修复v2->v2.1数据格式的数据，则返回 {@code 2.1}
     */
    public abstract double version();

    /**
     * 将数据修复成特定的数据格式。
     *
     * @param nbt 需要修复的数据
     * @param registries 注册表提供器
     * @return 修复后的单个数据
     * @implNote 建议单次修复整体数据结构。<br>
     *           为了代码复杂度和可维护性，也建议不修改数据范围。
     * @verNote 建议将格式版本范围限定到“进行了单次数据格式的修改”。<br>
     *          如v2版本后，v2.1和v2.2都修改了数据格式，那么应有两个修复器分别负责v2->v2.1和v2.1->v2.2
     */
    public abstract CompoundTag fixData(CompoundTag nbt, HolderLookup.Provider registries);
}
