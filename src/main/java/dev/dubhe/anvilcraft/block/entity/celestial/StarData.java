package dev.dubhe.anvilcraft.block.entity.celestial;

import dev.dubhe.anvilcraft.util.MassRadiusDiagram;
import net.minecraft.nbt.CompoundTag;

public record StarData(
    int size,
    int colorR,
    int colorG,
    int colorB,
    float axialTilt,
    float rotationSpeed
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
        tag.putInt("size", size);
        tag.putInt("colorR", colorR);
        tag.putInt("colorG", colorG);
        tag.putInt("colorB", colorB);
        tag.putFloat("axialTilt", axialTilt);
        tag.putFloat("rotationSpeed", rotationSpeed);
        return tag;
    }

    public static StarData fromTag(CompoundTag tag) {
        int size = tag.getInt("size");
        int r;
        int g;
        int b;
        if (tag.contains("colorR")) {
            // New format: colors stored directly
            r = tag.getInt("colorR");
            g = tag.getInt("colorG");
            b = tag.getInt("colorB");
        } else {
            // Old format: compute color from legacy gradient using size
            float[] rgb = MassRadiusDiagram.starColorFallback(size);
            r = Math.clamp((int) (rgb[0] * 255), 0, 255);
            g = Math.clamp((int) (rgb[1] * 255), 0, 255);
            b = Math.clamp((int) (rgb[2] * 255), 0, 255);
        }
        return new StarData(size, r, g, b, tag.getFloat("axialTilt"), tag.getFloat("rotationSpeed"));
    }
}
