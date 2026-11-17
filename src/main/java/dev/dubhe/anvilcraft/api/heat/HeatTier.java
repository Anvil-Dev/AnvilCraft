package dev.dubhe.anvilcraft.api.heat;

import com.mojang.serialization.Codec;
import dev.anvilcraft.lib.v2.codec.CodecUtil;
import dev.anvilcraft.lib.v2.codec.StreamCodecUtil;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.StreamCodec;

import java.util.Locale;
import java.util.Optional;

public enum HeatTier implements Comparable<HeatTier> {
    NORMAL, HEATED, REDHOT, GLOWING, INCANDESCENT, OVERHEATED;

    public static final Codec<HeatTier> INDEX_CODEC = CodecUtil.enumCodecInInt(HeatTier.class);
    public static final Codec<HeatTier> LOWER_NAME_CODEC = CodecUtil.enumCodecInLowerName(HeatTier.class);
    public static final StreamCodec<ByteBuf, HeatTier> STREAM_CODEC = StreamCodecUtil.enumStreamCodec(HeatTier.class);

    private static final HeatTier[] VALUES = values();

    public Optional<HeatTier> lower(int steps) {
        int index = this.ordinal() - steps;
        if (index < 0) return Optional.empty();
        return Optional.of(VALUES[index]);
    }

    public Component getDisplayName() {
        return Component.translatable("tooltip.anvilcraft.heat.tier." + this);
    }

    @Override
    public String toString() {
        return this.name().toLowerCase(Locale.ROOT);
    }
}
