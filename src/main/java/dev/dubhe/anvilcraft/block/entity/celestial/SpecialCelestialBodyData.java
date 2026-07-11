package dev.dubhe.anvilcraft.block.entity.celestial;

import dev.dubhe.anvilcraft.AnvilCraft;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.Identifier;
import org.jspecify.annotations.Nullable;

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
    String textureName,
    @Nullable CompoundTag playerHeadProfile
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
            recipe.textureName(),
            null
        );
    }

    /**
     * Create a dynamic player-head body from a player profile NBT (no resource, rendered via the skull model).
     * The size is determined by the number of space anvils.
     */
    public static SpecialCelestialBodyData fromPlayerHead(CompoundTag profileNbt, int space) {
        return new SpecialCelestialBodyData(
            "player_head",
            "player_head",
            space,
            0f,
            2,
            0,
            Temperature.MILD,
            false,
            LiquidCoverage.NONE,
            false,
            true,
            "player_head",
            profileNbt
        );
    }

    /**
     * Whether this body is a dynamic player-head body.
     */
    public boolean isPlayerHead() {
        return this.playerHeadProfile != null;
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
     * Get the model resource identifier for this special body.
     */
    public Identifier getModelLocation() {
        return AnvilCraft.of("block/celestial_body/" + this.textureName);
    }

    @Override
    public CompoundTag toTag() {
        CompoundTag tag = new CompoundTag();
        tag.putString("bodyType", CelestialBodyType.SPECIAL.getSerializedName());
        tag.putString("recipeId", this.recipeId);
        tag.putString("name", this.name);
        tag.putInt("size", this.size);
        tag.putFloat("axialTilt", this.axialTilt);
        tag.putInt("rotationSpeed", this.rotationSpeed);
        tag.putInt("magneticFieldStrength", this.magneticFieldStrength);
        tag.putBoolean("hasAtmosphere", this.hasAtmosphere);
        tag.putBoolean("isErrorPlanet", this.isErrorPlanet);
        tag.putBoolean("needsCustomModel", this.needsCustomModel);
        tag.putString("textureName", this.textureName);
        if (this.temperature != null) {
            tag.putString("temperature", this.temperature.getSerializedName());
        }
        if (this.liquidCoverage != null) {
            tag.putString("liquidCoverage", this.liquidCoverage.getSerializedName());
        }
        if (this.playerHeadProfile != null) {
            tag.put("playerHeadProfile", this.playerHeadProfile);
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
        CompoundTag playerHeadProfile = tag.contains("playerHeadProfile")
            ? tag.getCompoundOrEmpty("playerHeadProfile") : null;
        return new SpecialCelestialBodyData(
            recipeId, name, size, axialTilt, rotationSpeed, magneticFieldStrength,
            temperature, hasAtmosphere, liquidCoverage,
            isErrorPlanet, needsCustomModel, textureName, playerHeadProfile
        );
    }
}
