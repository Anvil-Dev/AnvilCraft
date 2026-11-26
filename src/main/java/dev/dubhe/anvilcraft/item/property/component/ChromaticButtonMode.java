package dev.dubhe.anvilcraft.item.property.component;

import com.mojang.serialization.Codec;
import dev.anvilcraft.lib.v2.codec.StreamCodecUtil;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.StringRepresentable;

import java.util.Locale;

public enum ChromaticButtonMode implements StringRepresentable {
    FLOWING,
    GRADIENT;

    public static final Codec<ChromaticButtonMode> CODEC = StringRepresentable.fromEnum(ChromaticButtonMode::values);
    public static final StreamCodec<ByteBuf, ChromaticButtonMode> STREAM_CODEC = StreamCodecUtil.enumStreamCodec(ChromaticButtonMode.class);

    @Override
    public String getSerializedName() {
        return this.name().toLowerCase(Locale.ROOT);
    }
}
