package dev.dubhe.anvilcraft.saved.storage.category.store;

import com.mojang.serialization.Codec;
import dev.anvilcraft.lib.v2.codec.StreamCodecUtil;
import io.netty.buffer.ByteBuf;
import lombok.Getter;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.StringRepresentable;

import java.util.Locale;

@Getter
public enum CategoryMode implements StringRepresentable {
    UNLIMITED(0),
    ALLOWLIST(40),
    BLOCKLIST(60),
    ;

    public static final Codec<CategoryMode> CODEC = StringRepresentable.fromEnum(CategoryMode::values);
    public static final StreamCodec<ByteBuf, CategoryMode> STREAM_CODEC = StreamCodecUtil.enumStreamCodec(CategoryMode.class);
    private final int texYDiff;

    CategoryMode(int texYDiff) {
        this.texYDiff = texYDiff;
    }

    @Override
    public String getSerializedName() {
        return this.name().toLowerCase(Locale.ROOT);
    }

    public Component getModeName() {
        return Component.translatable("screen.anvilcraft.storage.category.mode." + this.getSerializedName());
    }

    public CategoryMode next(boolean inversed) {
        return switch (this) {
            case UNLIMITED -> inversed ? CategoryMode.BLOCKLIST : CategoryMode.ALLOWLIST;
            case ALLOWLIST -> inversed ? CategoryMode.UNLIMITED : CategoryMode.BLOCKLIST;
            case BLOCKLIST -> inversed ? CategoryMode.ALLOWLIST : CategoryMode.UNLIMITED;
        };
    }
}
