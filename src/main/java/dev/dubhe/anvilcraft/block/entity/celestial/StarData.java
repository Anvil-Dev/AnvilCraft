package dev.dubhe.anvilcraft.block.entity.celestial;

import net.minecraft.nbt.CompoundTag;

public record StarData(
    int size,
    int temperatureKelvin
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
        tag.putInt("temperatureKelvin", temperatureKelvin);
        return tag;
    }

    public static StarData fromTag(CompoundTag tag) {
        return new StarData(
            tag.getInt("size"),
            tag.getInt("temperatureKelvin")
        );
    }
}
