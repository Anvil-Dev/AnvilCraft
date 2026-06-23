package dev.dubhe.anvilcraft.block.entity.celestial;

import net.minecraft.nbt.CompoundTag;

public record RockyPlanetData(
    CelestialBodyClass bodyClass,
    boolean hasAtmosphere,
    LiquidCoverage liquidCoverage,
    Temperature temperature,
    RingType ringType,
    int size,
    int paletteBaseRow,
    int paletteOverlayRow,
    float axialTilt,
    int rotationSpeed,
    int magneticFieldStrength
) implements CelestialBodyData {

    @Override
    public CelestialBodyType type() {
        return CelestialBodyType.ROCKY_PLANET;
    }

    @Override
    public CompoundTag toTag() {
        CompoundTag tag = new CompoundTag();
        tag.putString("bodyType", this.type().getSerializedName());
        tag.putString("bodyClass", this.bodyClass.name());
        tag.putBoolean("hasAtmosphere", this.hasAtmosphere);
        tag.putString("liquidCoverage", this.liquidCoverage.getSerializedName());
        tag.putString("temperature", this.temperature.getSerializedName());
        tag.putString("ringType", this.ringType.getSerializedName());
        tag.putInt("size", this.size);
        tag.putInt("paletteBaseRow", this.paletteBaseRow);
        tag.putInt("paletteOverlayRow", this.paletteOverlayRow);
        tag.putFloat("axialTilt", this.axialTilt);
        tag.putInt("rotationSpeed", this.rotationSpeed);
        tag.putInt("magneticFieldStrength", this.magneticFieldStrength);
        return tag;
    }

    public static RockyPlanetData fromTag(CompoundTag tag) {
        CelestialBodyClass cls = CelestialBodyData.readClass(tag, CelestialBodyType.ROCKY_PLANET);
        int mag = tag.getIntOr("magneticFieldStrength", 0);
        return new RockyPlanetData(
            cls,
            tag.getBooleanOr("hasAtmosphere", false),
            LiquidCoverage.fromName(tag.getStringOr("liquidCoverage", "none")),
            Temperature.fromName(tag.getStringOr("temperature", "mild")),
            RingType.fromName(tag.getStringOr("ringType", "none")),
            tag.getIntOr("size", 0),
            tag.getIntOr("paletteBaseRow", 0),
            tag.getIntOr("paletteOverlayRow", 0),
            tag.getFloatOr("axialTilt", 0f),
            tag.getIntOr("rotationSpeed", 0),
            mag
        );
    }
}
