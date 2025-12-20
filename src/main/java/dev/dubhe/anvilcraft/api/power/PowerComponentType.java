package dev.dubhe.anvilcraft.api.power;

import com.mojang.serialization.Codec;
import net.minecraft.util.StringRepresentable;

/**
 * 电力元件类型
 */
public enum PowerComponentType implements StringRepresentable {
    INVALID,
    PRODUCER,
    CONSUMER,
    STORAGE,
    TRANSMITTER;
    public static final Codec<PowerComponentType> CODEC = StringRepresentable.fromEnum(PowerComponentType::values);

    @Override
    public String getSerializedName() {
        return name();
    }
}
