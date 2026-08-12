package dev.dubhe.anvilcraft.saved.setting.mode;

import com.mojang.serialization.Codec;
import dev.anvilcraft.lib.v2.codec.StreamCodecUtil;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.StringRepresentable;

import java.util.Locale;

public enum OrderMode implements StringRepresentable {
    SEQUENTIAL,
    REVERSE,
    ;

    public static final Codec<OrderMode> CODEC = StringRepresentable.fromEnum(OrderMode::values);
    public static final StreamCodec<ByteBuf, OrderMode> STREAM_CODEC = StreamCodecUtil.enumStreamCodec(OrderMode.class);

    @Override
    public String getSerializedName() {
        return this.name().toLowerCase(Locale.ROOT);
    }

    public Component getModeName() {
        return Component.translatable("screen.anvilcraft.storage.order." + this.getSerializedName());
    }
}
