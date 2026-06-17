package dev.dubhe.anvilcraft.block.entity.celestial;

import net.minecraft.nbt.CompoundTag;

public record GiantPlanetData(
    CelestialBodyClass bodyClass,
    PressureType pressureType,
    WindSpeed windSpeed,
    RingType ringType,
    int size,
    int paletteBaseRow,
    int paletteOverlayRow,
    float axialTilt,
    int rotationSpeed,
    int magneticFieldStrength,
    boolean brownDwarf
) implements CelestialBodyData {

    @Override
    public CelestialBodyType type() {
        return CelestialBodyType.GIANT_PLANET;
    }

    @Override
    public CompoundTag toTag() {
        CompoundTag tag = new CompoundTag();
        tag.putString("bodyType", type().getSerializedName());
        tag.putString("bodyClass", bodyClass.name());
        tag.putString("pressureType", pressureType.getSerializedName());
        tag.putString("windSpeed", windSpeed.getSerializedName());
        tag.putString("ringType", ringType.getSerializedName());
        tag.putInt("size", size);
        tag.putInt("paletteBaseRow", paletteBaseRow);
        tag.putInt("paletteOverlayRow", paletteOverlayRow);
        tag.putFloat("axialTilt", axialTilt);
        tag.putInt("rotationSpeed", rotationSpeed);
        tag.putInt("magneticFieldStrength", magneticFieldStrength);
        tag.putBoolean("brownDwarf", brownDwarf);
        return tag;
    }

    public static GiantPlanetData fromTag(CompoundTag tag) {
        CelestialBodyClass cls = CelestialBodyData.readClass(tag, CelestialBodyType.GIANT_PLANET);
        int mag = tag.contains("magneticFieldStrength") ? tag.getInt("magneticFieldStrength") : 0;
        boolean bd = tag.contains("brownDwarf") && tag.getBoolean("brownDwarf");
        return new GiantPlanetData(
            cls,
            PressureType.fromName(tag.getString("pressureType")),
            WindSpeed.fromName(tag.getString("windSpeed")),
            RingType.fromName(tag.getString("ringType")),
            tag.getInt("size"),
            tag.getInt("paletteBaseRow"),
            tag.getInt("paletteOverlayRow"),
            tag.getFloat("axialTilt"),
            tag.getInt("rotationSpeed"),
            mag,
            bd
        );
    }
}
