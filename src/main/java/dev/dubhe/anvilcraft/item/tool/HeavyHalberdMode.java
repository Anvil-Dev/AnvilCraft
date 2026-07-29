package dev.dubhe.anvilcraft.item.tool;

import com.mojang.serialization.Codec;
import dev.anvilcraft.lib.v2.codec.StreamCodecUtil;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.StringRepresentable;

import java.util.Locale;

public enum HeavyHalberdMode implements StringRepresentable {
    TRIDENT,
    SPEAR,
    SWORD,
    MACE,
    ;

    public static final Codec<HeavyHalberdMode> CODEC = StringRepresentable.fromEnum(HeavyHalberdMode::values);
    public static final StreamCodec<ByteBuf, HeavyHalberdMode> STREAM_CODEC = StreamCodecUtil.enumStreamCodec(HeavyHalberdMode.class);

    @Override
    public String getSerializedName() {
        return this.name().toLowerCase(Locale.ROOT);
    }
}
