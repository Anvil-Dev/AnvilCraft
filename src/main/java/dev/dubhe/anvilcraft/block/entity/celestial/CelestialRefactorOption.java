package dev.dubhe.anvilcraft.block.entity.celestial;

import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ItemLike;

/// 表示天体约束环的可能重构选项。
/// 每个选项将一个环转化为巨构建筑。
///
/// ring - 要重构的环编号（R1-R6）
/// megastructure - 巨构模型名称后缀（例如"eco_station"、"dyson_sphere"）
/// modelLocation - 巨构模型的完整 {@link ModelResourceLocation}
/// displayName - 巨构显示名称的翻译键
/// material - 所需建筑材料的物品栈，若无则为 {@link ItemStack#EMPTY}
/// materialCount - 所需材料的数量
public record CelestialRefactorOption(
    int ring,
    String megastructure,
    ModelResourceLocation modelLocation,
    String displayName,
    ItemStack material,
    int materialCount
) {

    /// 创建一个不需要建筑材料的重构选项。
    public static CelestialRefactorOption noMaterial(
        int ring, String megastructure, ModelResourceLocation modelLocation, String displayName
    ) {
        return new CelestialRefactorOption(
            ring, megastructure, modelLocation, displayName, ItemStack.EMPTY, 0
        );
    }

    /// 创建一个需要建筑材料的重构选项。
    ///
    /// ring - 环编号（1-6）
    /// megastructure - 巨构名称后缀
    /// modelLocation - 完整模型定位符
    /// displayName - 翻译键
    /// material - 所需物品
    /// materialCount - 所需物品数量
    public static CelestialRefactorOption withMaterial(
        int ring, String megastructure, ModelResourceLocation modelLocation, String displayName,
        ItemLike material, int materialCount
    ) {
        return new CelestialRefactorOption(
            ring, megastructure, modelLocation, displayName,
            new ItemStack(material), materialCount
        );
    }

    public boolean needsMaterial() {
        return materialCount > 0 && !material.isEmpty();
    }
}
