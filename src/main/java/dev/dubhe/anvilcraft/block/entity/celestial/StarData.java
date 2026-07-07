package dev.dubhe.anvilcraft.block.entity.celestial;

import net.minecraft.core.UUIDUtil;
import net.minecraft.nbt.CompoundTag;
import org.jspecify.annotations.Nullable;

import java.util.UUID;

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
    @Nullable UUID bodyUuid
) implements CelestialBodyData {

    @Override
    public CelestialBodyType type() {
        return CelestialBodyType.STAR;
    }

    @Override
    public RingType ringType() {
        return RingType.NONE;
    }

    /**
     * Create a copy with a new body UUID, preserving all other fields.
     */
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
            uuid
        );
    }

    /**
     * Derive a reproducible UUID from the bodySeed. The same bodySeed always
     * produces the same UUID, which enables singularity crystal copies to share
     * the wormhole identity of the original discovery.
     */
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
        if (this.bodyUuid != null) {
            tag.store("bodyUuid", UUIDUtil.CODEC, this.bodyUuid);
        }
        return tag;
    }

    public static StarData fromTag(CompoundTag tag) {
        CelestialBodyClass cls = CelestialBodyData.readClass(tag, CelestialBodyType.STAR);
        int size = tag.getIntOr("size", 0);
        int r = tag.getIntOr("colorR", 0);
        int g = tag.getIntOr("colorG", 0);
        int b = tag.getIntOr("colorB", 0);
        int mag = tag.getIntOr("magneticFieldStrength", 0);
        int energy = tag.getIntOr("energy", 0);
        int rotSpeed = tag.getIntOr("rotationSpeed", 0);
        UUID uuid = tag.read("bodyUuid", UUIDUtil.CODEC).orElse(null);
        return new StarData(cls, size, r, g, b, tag.getFloatOr("axialTilt", 0f), rotSpeed, mag, energy, uuid);
    }
}
