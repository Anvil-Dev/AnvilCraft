package dev.dubhe.anvilcraft.api.power;

import net.minecraft.util.StringRepresentable;

import com.mojang.serialization.Codec;
import org.jetbrains.annotations.NotNull;

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
    @NotNull
    public String getSerializedName() {
        return name();
    }
}
