package dev.dubhe.anvilcraft.api.heat;

import com.mojang.serialization.Codec;
import dev.dubhe.anvilcraft.util.CodecUtil;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.StreamCodec;

import java.util.Locale;

public enum HeatTier implements Comparable<HeatTier> {
    NORMAL, HEATED, REDHOT, GLOWING, INCANDESCENT, OVERHEATED;

    public static final Codec<HeatTier> INDEX_CODEC = CodecUtil.enumCodecInInt(HeatTier.class);
    public static final Codec<HeatTier> LOWER_NAME_CODEC = CodecUtil.enumCodecInLowerName(HeatTier.class);
    public static final StreamCodec<ByteBuf, HeatTier> STREAM_CODEC = CodecUtil.enumStreamCodec(HeatTier.class);

    public Component toComponent() {
        return Component.translatable("tooltip.anvilcraft.heat.tier." + this);
    }

    @Override
    public String toString() {
        return this.name().toLowerCase(Locale.ROOT);
    }
}
