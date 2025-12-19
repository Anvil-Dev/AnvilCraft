package dev.dubhe.anvilcraft.saved.datafixers;

import com.google.common.collect.Multimap;
import com.google.common.collect.MultimapBuilder;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;

import java.util.Comparator;

@NoArgsConstructor(access = AccessLevel.NONE)
public class DataFixers {
    private static final Multimap<ResourceLocation, DataFixer> FIXERS = MultimapBuilder
        .hashKeys()
        .treeSetValues(Comparator.comparing(DataFixer::version))
        .build();

    /**
     * 注册数据修复器
     *
     * @param id 一系列数据修复器的共同ID
     * @param fixers 一系列数据修复器。顺序不敏感
     */
    public static void registerFixer(ResourceLocation id, DataFixer... fixers) {
        for (DataFixer fixer : fixers) {
            DataFixers.FIXERS.put(id, fixer);
        }
    }

    /**
     * 根据提供的ID获取数据修复器，并使用它们修复数据
     *
     * @param id 需要使用的数据修复器们的共同ID
     * @param currentVer 目前版本。将会使用低于等于目前版本的所有数据修复器修复数据
     * @param nbt 需要修复的数据
     * @param registries 注册表提供器
     * @return 修复的数据
     */
    public static CompoundTag fixData(ResourceLocation id, double currentVer, CompoundTag nbt, HolderLookup.Provider registries) {
        for (DataFixer fixer : DataFixers.FIXERS.get(id)) {
            if (fixer.version() > currentVer) return nbt;
            nbt = fixer.fixData(nbt, registries);
        }
        return nbt;
    }
}
