package dev.dubhe.anvilcraft.saved.setting.mode;

import com.mojang.serialization.Codec;
import dev.anvilcraft.lib.v2.codec.StreamCodecUtil;
import io.netty.buffer.ByteBuf;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.StringRepresentable;

import java.util.Locale;
import java.util.function.Consumer;

public enum SearchMode implements StringRepresentable {
    CLEAR(text -> text.withStyle(ChatFormatting.RED)),
    RETENTION(text -> text.withStyle(ChatFormatting.GREEN)),
    ;

    public static final Codec<SearchMode> CODEC = StringRepresentable.fromEnum(SearchMode::values);
    public static final StreamCodec<ByteBuf, SearchMode> STREAM_CODEC = StreamCodecUtil.enumStreamCodec(SearchMode.class);
    private final Consumer<MutableComponent> styler;

    SearchMode(Consumer<MutableComponent> styler) {
        this.styler = styler;
    }

    @Override
    public String getSerializedName() {
        return this.name().toLowerCase(Locale.ROOT);
    }

    public Component getModeName() {
        var tooltip = Component.translatable("screen.anvilcraft.storage.search." + this.getSerializedName());
        this.styler.accept(tooltip);
        return tooltip;
    }
}
