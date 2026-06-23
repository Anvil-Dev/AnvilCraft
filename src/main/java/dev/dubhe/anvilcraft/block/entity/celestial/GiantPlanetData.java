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
        tag.putString("bodyType", this.type().getSerializedName());
        tag.putString("bodyClass", this.bodyClass.name());
        tag.putString("pressureType", this.pressureType.getSerializedName());
        tag.putString("windSpeed", this.windSpeed.getSerializedName());
        tag.putString("ringType", this.ringType.getSerializedName());
        tag.putInt("size", this.size);
        tag.putInt("paletteBaseRow", this.paletteBaseRow);
        tag.putInt("paletteOverlayRow", this.paletteOverlayRow);
        tag.putFloat("axialTilt", this.axialTilt);
        tag.putInt("rotationSpeed", this.rotationSpeed);
        tag.putInt("magneticFieldStrength", this.magneticFieldStrength);
        tag.putBoolean("brownDwarf", this.brownDwarf);
        return tag;
    }

    public static GiantPlanetData fromTag(CompoundTag tag) {
        CelestialBodyClass cls = CelestialBodyData.readClass(tag, CelestialBodyType.GIANT_PLANET);
        int mag = tag.getIntOr("magneticFieldStrength", 0);
        boolean bd = tag.getBooleanOr("brownDwarf", false);
        return new GiantPlanetData(
            cls,
            PressureType.fromName(tag.getStringOr("pressureType", "gas")),
            WindSpeed.fromName(tag.getStringOr("windSpeed", "high")),
            RingType.fromName(tag.getStringOr("ringType", "none")),
            tag.getIntOr("size", 0),
            tag.getIntOr("paletteBaseRow", 0),
            tag.getIntOr("paletteOverlayRow", 0),
            tag.getFloatOr("axialTilt", 0f),
            tag.getIntOr("rotationSpeed", 0),
            mag,
            bd
        );
    }
}
