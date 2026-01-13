package dev.dubhe.anvilcraft.api.sc.category;

import com.mojang.serialization.Codec;
import dev.anvilcraft.lib.recipe.util.CodecUtil;
import io.netty.buffer.ByteBuf;
import lombok.Getter;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.StreamCodec;

import java.util.Locale;

@Getter
public enum CategoryMode {
    UNLIMITED(0),
    WHITELIST(40),
    BLACKLIST(60),
    ;

    public static final Codec<CategoryMode> CODEC = CodecUtil.enumCodecInLowerName(CategoryMode.class);
    public static final StreamCodec<ByteBuf, CategoryMode> STREAM_CODEC = CodecUtil.enumStreamCodec(CategoryMode.class);
    private final int texYDiff;

    CategoryMode(int texYDiff) {
        this.texYDiff = texYDiff;
    }

    public Component getDisplayName() {
        return Component.translatable("screen.anvilcraft.shulker_container.category." + this.name().toLowerCase(Locale.ROOT));
    }

    public CategoryMode next() {
        return switch (this) {
            case UNLIMITED -> CategoryMode.WHITELIST;
            case WHITELIST -> CategoryMode.BLACKLIST;
            case BLACKLIST -> CategoryMode.UNLIMITED;
        };
    }
}
