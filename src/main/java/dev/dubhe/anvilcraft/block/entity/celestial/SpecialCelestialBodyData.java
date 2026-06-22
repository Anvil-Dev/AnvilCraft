package dev.dubhe.anvilcraft.block.entity.celestial;

import dev.dubhe.anvilcraft.AnvilCraft;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.Identifier;
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
 *
 * <p>
 * Note: the {@code fromRecipe} factory method is added in a later phase
 * once {@code SpecialCelestialBodyRecipe} is ported.
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
     * Get the model resource identifier for this special body.
     */
    public Identifier getModelLocation() {
        return AnvilCraft.of("block/celestial_body/" + textureName);
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
        String recipeId = tag.getStringOr("recipeId", "");
        String name = tag.getStringOr("name", "");
        int size = tag.getIntOr("size", 0);
        float axialTilt = tag.getFloatOr("axialTilt", 0f);
        int rotationSpeed = tag.getIntOr("rotationSpeed", 0);
        int magneticFieldStrength = tag.getIntOr("magneticFieldStrength", 0);
        boolean hasAtmosphere = tag.getBooleanOr("hasAtmosphere", false);
        boolean isErrorPlanet = tag.getBooleanOr("isErrorPlanet", false);
        boolean needsCustomModel = tag.getBooleanOr("needsCustomModel", false);
        String textureName = tag.getStringOr("textureName", "");
        String tempStr = tag.getStringOr("temperature", "");
        Temperature temperature = !tempStr.isEmpty() ? Temperature.fromName(tempStr) : null;
        String lcStr = tag.getStringOr("liquidCoverage", "");
        LiquidCoverage liquidCoverage = !lcStr.isEmpty() ? LiquidCoverage.fromName(lcStr) : null;
        return new SpecialCelestialBodyData(
            recipeId, name, size, axialTilt, rotationSpeed, magneticFieldStrength,
            temperature, hasAtmosphere, liquidCoverage,
            isErrorPlanet, needsCustomModel, textureName
        );
    }
}
