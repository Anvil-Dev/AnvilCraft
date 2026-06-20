package dev.dubhe.anvilcraft.block.entity.celestial;

import net.minecraft.nbt.CompoundTag;
import org.jetbrains.annotations.Nullable;

/**
 * Celestial body data for hidden (special) bodies discovered via seed items.
 * These bodies bypass the normal three-step diagram matching and texture baking
 * pipeline — they use fixed model textures directly.
 */
@SuppressWarnings("checkstyle:MissingJavadocMethod")
public record SpecialCelestialBodyData(SpecialCelestialBodyType specialType) implements CelestialBodyData {

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

    @Override
    public int size() {
        return specialType.getSpace();
    }

    @Override
    public float axialTilt() {
        return specialType.getAxialTilt();
    }

    @Override
    public float rotationSpeed() {
        return specialType.getRotationSpeed();
    }

    @Override
    public int magneticFieldStrength() {
        return specialType.getMagneticFieldStrength();
    }

    /**
     * The planet surface temperature.
     * Returns {@code null} for Error Planet (displayed as "???").
     */
    @Nullable
    public Temperature temperature() {
        return specialType.getTemperature();
    }

    /**
     * Whether the planet has an atmosphere.
     */
    public boolean hasAtmosphere() {
        return specialType.hasAtmosphere();
    }

    /**
     * The planet's liquid coverage.
     * Returns {@code null} for Error Planet (displayed as "???").
     */
    @Nullable
    public LiquidCoverage liquidCoverage() {
        return specialType.getLiquidCoverage();
    }

    @Override
    public CompoundTag toTag() {
        CompoundTag tag = new CompoundTag();
        tag.putString("bodyType", CelestialBodyType.SPECIAL.getSerializedName());
        tag.putString("specialType", specialType.name());
        return tag;
    }

    /**
     * Deserialize a SpecialCelestialBodyData from NBT.
     */
    public static SpecialCelestialBodyData fromTag(CompoundTag tag) {
        SpecialCelestialBodyType type = SpecialCelestialBodyType.valueOf(tag.getString("specialType"));
        return new SpecialCelestialBodyData(type);
    }
}
