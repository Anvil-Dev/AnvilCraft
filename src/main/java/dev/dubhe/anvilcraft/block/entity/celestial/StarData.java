package dev.dubhe.anvilcraft.block.entity.celestial;

import net.minecraft.nbt.CompoundTag;

public record StarData(
    CelestialBodyClass bodyClass,
    int size,
    int colorR,
    int colorG,
    int colorB,
    float axialTilt,
    float rotationSpeed,
    int magneticFieldStrength,
    int energy
) implements CelestialBodyData {

    @Override
    public CelestialBodyType type() {
        return CelestialBodyType.STAR;
    }

    @Override
    public RingType ringType() {
        return RingType.NONE;
    }

    @Override
    public CompoundTag toTag() {
        CompoundTag tag = new CompoundTag();
        tag.putString("bodyType", type().getSerializedName());
        tag.putString("bodyClass", bodyClass.name());
        tag.putInt("size", size);
        tag.putInt("colorR", colorR);
        tag.putInt("colorG", colorG);
        tag.putInt("colorB", colorB);
        tag.putFloat("axialTilt", axialTilt);
        tag.putFloat("rotationSpeed", rotationSpeed);
        tag.putInt("magneticFieldStrength", magneticFieldStrength);
        tag.putInt("energy", energy);
        return tag;
    }

    public static StarData fromTag(CompoundTag tag) {
        CelestialBodyClass cls = CelestialBodyData.readClass(tag, CelestialBodyType.STAR);
        int size = tag.getInt("size");
        int r = 0;
        int g = 0;
        int b = 0;
        if (tag.contains("colorR")) {
            r = tag.getInt("colorR");
            g = tag.getInt("colorG");
            b = tag.getInt("colorB");
        }
        int mag = tag.contains("magneticFieldStrength") ? tag.getInt("magneticFieldStrength") : 0;
        int energy = tag.contains("energy") ? tag.getInt("energy") : 0;
        return new StarData(cls, size, r, g, b, tag.getFloat("axialTilt"), tag.getFloat("rotationSpeed"), mag, energy);
    }
}
