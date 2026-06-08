package dev.dubhe.anvilcraft.block.entity.celestial;

import net.minecraft.nbt.CompoundTag;

public record GiantPlanetData(
    PressureType pressureType,
    WindSpeed windSpeed,
    RingType ringType,
    int size,
    int paletteBaseRow,
    int paletteOverlayRow,
    float axialTilt,
    float rotationSpeed
) implements CelestialBodyData {

    @Override
    public CelestialBodyType type() {
        return CelestialBodyType.GIANT_PLANET;
    }

    @Override
    public CompoundTag toTag() {
        CompoundTag tag = new CompoundTag();
        tag.putString("bodyType", type().getSerializedName());
        tag.putString("pressureType", pressureType.getSerializedName());
        tag.putString("windSpeed", windSpeed.getSerializedName());
        tag.putString("ringType", ringType.getSerializedName());
        tag.putInt("size", size);
        tag.putInt("paletteBaseRow", paletteBaseRow);
        tag.putInt("paletteOverlayRow", paletteOverlayRow);
        tag.putFloat("axialTilt", axialTilt);
        tag.putFloat("rotationSpeed", rotationSpeed);
        return tag;
    }

    public static GiantPlanetData fromTag(CompoundTag tag) {
        return new GiantPlanetData(
            PressureType.fromName(tag.getString("pressureType")),
            WindSpeed.fromName(tag.getString("windSpeed")),
            RingType.fromName(tag.getString("ringType")),
            tag.getInt("size"),
            tag.getInt("paletteBaseRow"),
            tag.getInt("paletteOverlayRow"),
            tag.getFloat("axialTilt"),
            tag.getFloat("rotationSpeed")
        );
    }
}
