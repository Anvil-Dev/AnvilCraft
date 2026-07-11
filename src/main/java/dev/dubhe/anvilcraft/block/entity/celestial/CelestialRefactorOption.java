package dev.dubhe.anvilcraft.block.entity.celestial;

import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ItemLike;

/**
 * 束星环的巨构重构选项，每个选项把指定环改造为一种巨构。
 *
 * @param ring          要重构的环编号（R1-R6）
 * @param megastructure 巨构模型名称后缀
 * @param modelLocation 巨构模型资源标识
 * @param displayName   巨构显示名称翻译键
 * @param material      所需建材；不需要时为 {@link ItemStack#EMPTY}
 * @param materialCount 所需建材数量
 */
public record CelestialRefactorOption(
    int ring,
    String megastructure,
    Identifier modelLocation,
    String displayName,
    ItemStack material,
    int materialCount
) {
    /** 创建不需要建材的重构选项。 */
    public static CelestialRefactorOption noMaterial(
        int ring, String megastructure, Identifier modelLocation, String displayName
    ) {
        return new CelestialRefactorOption(
            ring, megastructure, modelLocation, displayName, ItemStack.EMPTY, 0
        );
    }

    /**
     * 创建需要指定建材的重构选项。
     *
     * @param ring          环编号（1-6）
     * @param megastructure 巨构名称后缀
     * @param modelLocation 模型资源标识
     * @param displayName   显示名称翻译键
     * @param material      所需物品
     * @param materialCount 所需数量
     */
    public static CelestialRefactorOption withMaterial(
        int ring, String megastructure, Identifier modelLocation, String displayName,
        ItemLike material, int materialCount
    ) {
        return new CelestialRefactorOption(
            ring, megastructure, modelLocation, displayName,
            new ItemStack(material), materialCount
        );
    }

    public boolean needsMaterial() {
        return this.materialCount > 0 && !this.material.isEmpty();
    }
}
