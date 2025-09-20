package dev.dubhe.anvilcraft.api.container.level;

import com.mojang.serialization.Codec;
import dev.anvilcraft.lib.util.CodecUtil;
import io.netty.buffer.ByteBuf;
import lombok.Getter;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.StringRepresentable;

import java.util.Locale;

@Getter
public enum ContainerLevel implements StringRepresentable {
    MIN(54, 4),
    ONE(108, 16),
    TWO(216, 64),
    THREE(864, 512),
    MAX(Integer.MAX_VALUE, Integer.MAX_VALUE),
    ;

    public static final Codec<ContainerLevel> CODEC = StringRepresentable.fromEnum(ContainerLevel::values);
    public static final StreamCodec<ByteBuf, ContainerLevel> STREAM_CODEC = CodecUtil.enumStreamCodec(ContainerLevel.class);
    private final int entryLimit;
    private final int stackPower;

    ContainerLevel(int entryLimit, int stackPower) {
        this.entryLimit = entryLimit;
        this.stackPower = stackPower;
    }

    @Override
    public String getSerializedName() {
        return this.name().toLowerCase(Locale.ROOT);
    }
}
