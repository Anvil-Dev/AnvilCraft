package dev.dubhe.anvilcraft.saved.setting.mode;

import com.mojang.serialization.Codec;
import dev.anvilcraft.lib.v2.codec.StreamCodecUtil;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.StringRepresentable;

import java.util.Locale;

public enum SortMode implements StringRepresentable {
    COUNT,
    MOD,
    NAME,
    ;

    public static final Codec<SortMode> CODEC = StringRepresentable.fromEnum(SortMode::values);
    public static final StreamCodec<ByteBuf, SortMode> STREAM_CODEC = StreamCodecUtil.enumStreamCodec(SortMode.class);

    @Override
    public String getSerializedName() {
        return this.name().toLowerCase(Locale.ROOT);
    }

    public Component getModeName() {
        return Component.translatable("screen.anvilcraft.storage.sort." + this.getSerializedName());
    }
}
