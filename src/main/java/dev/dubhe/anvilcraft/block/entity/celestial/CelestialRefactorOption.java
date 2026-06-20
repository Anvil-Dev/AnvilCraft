package dev.dubhe.anvilcraft.block.entity.celestial;

import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ItemLike;

/**
 * Represents a possible Celestial Restriction Ring refactoring option.
 * Each option turns a ring into a megastructure.
 *
 * @param ring          which ring (R1-R6) to refactor
 * @param megastructure the megastructure model name suffix (e.g. "eco_station", "dyson_sphere")
 * @param modelLocation the full {@link ModelResourceLocation} for the megastructure model
 * @param displayName   translation key for the megastructure display name
 * @param material      required building material as an ItemStack, or {@link ItemStack#EMPTY} if none
 * @param materialCount how many of the material are required
 */
public record CelestialRefactorOption(
    int ring,
    String megastructure,
    ModelResourceLocation modelLocation,
    String displayName,
    ItemStack material,
    int materialCount
) {
    /**
     * Create a refactor option that requires no building materials.
     */
    public static CelestialRefactorOption noMaterial(
        int ring, String megastructure, ModelResourceLocation modelLocation, String displayName
    ) {
        return new CelestialRefactorOption(
            ring, megastructure, modelLocation, displayName, ItemStack.EMPTY, 0
        );
    }

    /**
     * Create a refactor option that requires a building material.
     *
     * @param ring          ring index (1-6)
     * @param megastructure megastructure name suffix
     * @param modelLocation full model location
     * @param displayName   translation key
     * @param material      the required item
     * @param materialCount how many of the item are required
     */
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
