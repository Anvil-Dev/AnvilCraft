package dev.dubhe.anvilcraft.item.tool;

import com.mojang.serialization.Codec;
import dev.anvilcraft.lib.v2.codec.StreamCodecUtil;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.StringRepresentable;

import java.util.Locale;

public enum ResonateMode implements StringRepresentable {
    AUTO,
    AXE,
    SHOVEL,
    HOE,
    PICKAXE,
    ;

    public static final Codec<ResonateMode> CODEC = StringRepresentable.fromEnum(ResonateMode::values);
    public static final StreamCodec<ByteBuf, ResonateMode> STREAM_CODEC = StreamCodecUtil.enumStreamCodec(ResonateMode.class);

    @Override
    public String getSerializedName() {
        return this.name().toLowerCase(Locale.ROOT);
    }
}
