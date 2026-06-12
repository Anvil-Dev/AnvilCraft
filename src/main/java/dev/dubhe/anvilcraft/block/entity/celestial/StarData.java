package dev.dubhe.anvilcraft.block.entity.celestial;

import dev.dubhe.anvilcraft.util.MassRadiusDiagram;
import net.minecraft.nbt.CompoundTag;

public record StarData(
    CelestialBodyClass bodyClass,
    int size,
    int colorR,
    int colorG,
    int colorB,
    float axialTilt,
    float rotationSpeed,
    int magneticFieldStrength
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
        return tag;
    }

    public static StarData fromTag(CompoundTag tag) {
        CelestialBodyClass cls = CelestialBodyData.readClass(tag, CelestialBodyType.STAR);
        int size = tag.getInt("size");
        int r;
        int g;
        int b;
        if (tag.contains("colorR")) {
            r = tag.getInt("colorR");
            g = tag.getInt("colorG");
            b = tag.getInt("colorB");
        } else {
            float[] rgb = MassRadiusDiagram.starColorFallback(size);
            r = Math.clamp((int) (rgb[0] * 255), 0, 255);
            g = Math.clamp((int) (rgb[1] * 255), 0, 255);
            b = Math.clamp((int) (rgb[2] * 255), 0, 255);
        }
        int mag = tag.contains("magneticFieldStrength") ? tag.getInt("magneticFieldStrength") : 0;
        return new StarData(cls, size, r, g, b, tag.getFloat("axialTilt"), tag.getFloat("rotationSpeed"), mag);
    }
}
