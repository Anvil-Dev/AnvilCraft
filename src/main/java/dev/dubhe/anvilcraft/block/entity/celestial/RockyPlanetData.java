package dev.dubhe.anvilcraft.block.entity.celestial;

import net.minecraft.nbt.CompoundTag;

public record RockyPlanetData(
    boolean hasAtmosphere,
    LiquidCoverage liquidCoverage,
    Temperature temperature,
    RingType ringType,
    int size,
    int paletteBaseRow,
    int paletteOverlayRow,
    float axialTilt,
    float rotationSpeed
) implements CelestialBodyData {

    @Override
    public CelestialBodyType type() {
        return CelestialBodyType.ROCKY_PLANET;
    }

    @Override
    public CompoundTag toTag() {
        CompoundTag tag = new CompoundTag();
        tag.putString("bodyType", type().getSerializedName());
        tag.putBoolean("hasAtmosphere", hasAtmosphere);
        tag.putString("liquidCoverage", liquidCoverage.getSerializedName());
        tag.putString("temperature", temperature.getSerializedName());
        tag.putString("ringType", ringType.getSerializedName());
        tag.putInt("size", size);
        tag.putInt("paletteBaseRow", paletteBaseRow);
        tag.putInt("paletteOverlayRow", paletteOverlayRow);
        tag.putFloat("axialTilt", axialTilt);
        tag.putFloat("rotationSpeed", rotationSpeed);
        return tag;
    }

    public static RockyPlanetData fromTag(CompoundTag tag) {
        return new RockyPlanetData(
            tag.getBoolean("hasAtmosphere"),
            LiquidCoverage.fromName(tag.getString("liquidCoverage")),
            Temperature.fromName(tag.getString("temperature")),
            RingType.fromName(tag.getString("ringType")),
            tag.getInt("size"),
            tag.getInt("paletteBaseRow"),
            tag.getInt("paletteOverlayRow"),
            tag.getFloat("axialTilt"),
            tag.getFloat("rotationSpeed")
        );
    }
}
