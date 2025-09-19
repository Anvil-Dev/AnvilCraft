package dev.dubhe.anvilcraft.api.crate.level;

import com.mojang.serialization.Codec;
import dev.anvilcraft.lib.util.CodecUtil;
import io.netty.buffer.ByteBuf;
import lombok.Getter;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.StringRepresentable;

import java.util.Locale;

@Getter
public enum CrateLevel implements StringRepresentable {
    MIN(54, 4),
    ONE(108, 16),
    TWO(216, 64),
    THREE(864, 512),
    MAX(Integer.MAX_VALUE, Integer.MAX_VALUE),
    ;

    public static final Codec<CrateLevel> CODEC = StringRepresentable.fromEnum(CrateLevel::values);
    public static final StreamCodec<ByteBuf, CrateLevel> STREAM_CODEC = CodecUtil.enumStreamCodec(CrateLevel.class);
    private final int entryLimit;
    private final int stackPower;

    CrateLevel(int entryLimit, int stackPower) {
        this.entryLimit = entryLimit;
        this.stackPower = stackPower;
    }

    @Override
    public String getSerializedName() {
        return this.name().toLowerCase(Locale.ROOT);
    }
}
