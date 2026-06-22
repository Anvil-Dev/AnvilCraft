package dev.dubhe.anvilcraft.block.entity.celestial;

import dev.dubhe.anvilcraft.AnvilCraft;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.nbt.CompoundTag;
import org.jetbrains.annotations.Nullable;

/**
 * Celestial body data for hidden (special) bodies discovered via seed items.
 * These bodies bypass the normal three-step diagram matching and texture baking
 * pipeline — they use fixed model textures directly.
 *
 * <p>
 * All properties are cached at creation time from a
 * {@link SpecialCelestialBodyRecipe}, so no recipe-manager lookup is needed
 * during rendering or NBT deserialization.
 * </p>
 */
@SuppressWarnings("checkstyle:MissingJavadocMethod")
public record SpecialCelestialBodyData(
    String recipeId,
    String name,
    int size,
    float axialTilt,
    int rotationSpeed,
    int magneticFieldStrength,
    @Nullable Temperature temperature,
    boolean hasAtmosphere,
    @Nullable LiquidCoverage liquidCoverage,
    boolean isErrorPlanet,
    boolean needsCustomModel,
    String textureName
) implements CelestialBodyData {

    /**
     * Create from a recipe and its resource location ID.
     */
    public static SpecialCelestialBodyData fromRecipe(SpecialCelestialBodyRecipe recipe, String recipeId) {
        return new SpecialCelestialBodyData(
            recipeId,
            recipe.name(),
            recipe.space(),
            recipe.axialTilt(),
            recipe.rotationSpeed(),
            recipe.magneticFieldStrength(),
            recipe.temperature(),
            recipe.hasAtmosphere(),
            recipe.getLiquidCoverage(),
            recipe.isErrorPlanet(),
            recipe.needsCustomModel(),
            recipe.textureName()
        );
    }

    @Override
    public CelestialBodyType type() {
        return CelestialBodyType.SPECIAL;
    }

    @Override
    public CelestialBodyClass bodyClass() {
        return CelestialBodyClass.LARGE_MOON;
    }

    @Override
    public RingType ringType() {
        return RingType.NONE;
    }

    /**
     * Get the standalone model resource location for this special body.
     */
    public ModelResourceLocation getModelLocation() {
        return ModelResourceLocation.standalone(AnvilCraft.of("block/celestial_body/" + textureName));
    }

    @Override
    public CompoundTag toTag() {
        CompoundTag tag = new CompoundTag();
        tag.putString("bodyType", CelestialBodyType.SPECIAL.getSerializedName());
        tag.putString("recipeId", recipeId);
        tag.putString("name", name);
        tag.putInt("size", size);
        tag.putFloat("axialTilt", axialTilt);
        tag.putInt("rotationSpeed", rotationSpeed);
        tag.putInt("magneticFieldStrength", magneticFieldStrength);
        tag.putBoolean("hasAtmosphere", hasAtmosphere);
        tag.putBoolean("isErrorPlanet", isErrorPlanet);
        tag.putBoolean("needsCustomModel", needsCustomModel);
        tag.putString("textureName", textureName);
        if (temperature != null) {
            tag.putString("temperature", temperature.getSerializedName());
        }
        if (liquidCoverage != null) {
            tag.putString("liquidCoverage", liquidCoverage.getSerializedName());
        }
        return tag;
    }

    /**
     * Deserialize a SpecialCelestialBodyData from NBT.
     */
    public static SpecialCelestialBodyData fromTag(CompoundTag tag) {
        String recipeId = tag.getString("recipeId");
        String name = tag.getString("name");
        int size = tag.getInt("size");
        float axialTilt = tag.getFloat("axialTilt");
        int rotationSpeed = tag.getInt("rotationSpeed");
        int magneticFieldStrength = tag.getInt("magneticFieldStrength");
        boolean hasAtmosphere = tag.getBoolean("hasAtmosphere");
        boolean isErrorPlanet = tag.getBoolean("isErrorPlanet");
        boolean needsCustomModel = tag.getBoolean("needsCustomModel");
        String textureName = tag.getString("textureName");
        Temperature temperature = tag.contains("temperature")
            ? Temperature.fromName(tag.getString("temperature")) : null;
        LiquidCoverage liquidCoverage = tag.contains("liquidCoverage")
            ? LiquidCoverage.fromName(tag.getString("liquidCoverage")) : null;
        return new SpecialCelestialBodyData(
            recipeId, name, size, axialTilt, rotationSpeed, magneticFieldStrength,
            temperature, hasAtmosphere, liquidCoverage,
            isErrorPlanet, needsCustomModel, textureName
        );
    }
}
