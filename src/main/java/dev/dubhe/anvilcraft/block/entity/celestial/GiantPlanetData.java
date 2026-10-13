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
    boolean brownDwarf,
    int energy
) implements CelestialBodyData {

    /** Backwards-compatible constructor for giant planets stored before energy was persisted. */
    public GiantPlanetData(
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
    ) {
        this(
            bodyClass,
            pressureType,
            windSpeed,
            ringType,
            size,
            paletteBaseRow,
            paletteOverlayRow,
            axialTilt,
            rotationSpeed,
            magneticFieldStrength,
            brownDwarf,
            0
        );
    }

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
        tag.putInt("energy", this.energy);
        return tag;
    }

    public static GiantPlanetData fromTag(CompoundTag tag) {
        CelestialBodyClass cls = CelestialBodyData.readClass(tag, CelestialBodyType.GIANT_PLANET);
        int mag = tag.contains("magneticFieldStrength") ? tag.getInt("magneticFieldStrength") : 0;
        boolean bd = tag.contains("brownDwarf") && tag.getBoolean("brownDwarf");
        int energy = tag.contains("energy") ? tag.getInt("energy") : 0;
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
            bd,
            energy
        );
    }
}
