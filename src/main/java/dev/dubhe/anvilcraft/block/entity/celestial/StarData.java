package dev.dubhe.anvilcraft.block.entity.celestial;

import net.minecraft.nbt.CompoundTag;

import java.util.UUID;
import javax.annotation.Nullable;

public record StarData(
    CelestialBodyClass bodyClass,
    int size,
    int colorR,
    int colorG,
    int colorB,
    float axialTilt,
    int rotationSpeed,
    int magneticFieldStrength,
    int energy,
    @Nullable UUID bodyUuid,
    boolean specialRedDwarf
) implements CelestialBodyData {

    /** Backwards-compatible constructor for ordinary stars. */
    public StarData(
        CelestialBodyClass bodyClass,
        int size,
        int colorR,
        int colorG,
        int colorB,
        float axialTilt,
        int rotationSpeed,
        int magneticFieldStrength,
        int energy,
        @Nullable UUID bodyUuid
    ) {
        this(
            bodyClass,
            size,
            colorR,
            colorG,
            colorB,
            axialTilt,
            rotationSpeed,
            magneticFieldStrength,
            energy,
            bodyUuid,
            false
        );
    }

    @Override
    public CelestialBodyType type() {
        return CelestialBodyType.STAR;
    }

    @Override
    public RingType ringType() {
        return RingType.NONE;
    }

    /// 创建一个带有新body UUID的副本，保留所有其他字段。
    public StarData withBodyUuid(UUID uuid) {
        return new StarData(
            this.bodyClass,
            this.size,
            this.colorR,
            this.colorG,
            this.colorB,
            this.axialTilt,
            this.rotationSpeed,
            this.magneticFieldStrength,
            this.energy,
            uuid,
            this.specialRedDwarf
        );
    }

    /// 从bodySeed派生一个可复现的UUID。相同的bodySeed总是产生相同的UUID，这使得奇点水晶副本能够共享原始发现的虫洞身份。
    public static UUID uuidFromBodySeed(long bodySeed) {
        return new UUID(bodySeed, bodySeed);
    }

    @Override
    public CompoundTag toTag() {
        CompoundTag tag = new CompoundTag();
        tag.putString("bodyType", this.type().getSerializedName());
        tag.putString("bodyClass", this.bodyClass.name());
        tag.putInt("size", this.size);
        tag.putInt("colorR", this.colorR);
        tag.putInt("colorG", this.colorG);
        tag.putInt("colorB", this.colorB);
        tag.putFloat("axialTilt", this.axialTilt);
        tag.putInt("rotationSpeed", this.rotationSpeed);
        tag.putInt("magneticFieldStrength", this.magneticFieldStrength);
        tag.putInt("energy", this.energy);
        tag.putBoolean("specialRedDwarf", this.specialRedDwarf);
        if (this.bodyUuid != null) {
            tag.putUUID("bodyUuid", this.bodyUuid);
        }
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
        int rotSpeed = tag.contains("rotationSpeed") ? tag.getInt("rotationSpeed") : 0;
        UUID uuid = tag.contains("bodyUuid") ? tag.getUUID("bodyUuid") : null;
        boolean specialRedDwarf = tag.contains("specialRedDwarf") && tag.getBoolean("specialRedDwarf");
        return new StarData(
            cls,
            size,
            r,
            g,
            b,
            tag.getFloat("axialTilt"),
            rotSpeed,
            mag,
            energy,
            uuid,
            specialRedDwarf
        );
    }
}
