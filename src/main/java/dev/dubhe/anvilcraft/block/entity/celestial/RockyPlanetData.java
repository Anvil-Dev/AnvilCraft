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
        tag.putString("bodyType", type().getSerializedName());
        tag.putString("bodyClass", bodyClass.name());
        tag.putBoolean("hasAtmosphere", hasAtmosphere);
        tag.putString("liquidCoverage", liquidCoverage.getSerializedName());
        tag.putString("temperature", temperature.getSerializedName());
        tag.putString("ringType", ringType.getSerializedName());
        tag.putInt("size", size);
        tag.putInt("paletteBaseRow", paletteBaseRow);
        tag.putInt("paletteOverlayRow", paletteOverlayRow);
        tag.putFloat("axialTilt", axialTilt);
        tag.putInt("rotationSpeed", rotationSpeed);
        tag.putInt("magneticFieldStrength", magneticFieldStrength);
        return tag;
    }

    public static RockyPlanetData fromTag(CompoundTag tag) {
        CelestialBodyClass cls = CelestialBodyData.readClass(tag, CelestialBodyType.ROCKY_PLANET);
        int mag = tag.contains("magneticFieldStrength") ? tag.getInt("magneticFieldStrength") : 0;
        return new RockyPlanetData(
            cls,
            tag.getBoolean("hasAtmosphere"),
            LiquidCoverage.fromName(tag.getString("liquidCoverage")),
            Temperature.fromName(tag.getString("temperature")),
            RingType.fromName(tag.getString("ringType")),
            tag.getInt("size"),
            tag.getInt("paletteBaseRow"),
            tag.getInt("paletteOverlayRow"),
            tag.getFloat("axialTilt"),
            tag.getInt("rotationSpeed"),
            mag
        );
    }
}
