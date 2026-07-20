package dev.dubhe.anvilcraft.rpc;

import dev.anvilcraft.lib.v2.codec.StreamCodecUtil;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;
import org.jspecify.annotations.Nullable;

public enum StorageInput {
    PICKUP(new int[] {0, 1}),
    QUICK_MOVE_FROM_STORAGE,
    QUICK_MOVE_TO_STORAGE,
    CLONE,
    THROW(new int[] {0, 1, 2}),
    ;

    public static final StreamCodec<ByteBuf, StorageInput> STREAM_CODEC = StreamCodecUtil.enumStreamCodec(StorageInput.class);

    private final int @Nullable [] validButtons;

    StorageInput() {
        this.validButtons = null;
    }

    StorageInput(int @Nullable [] validButtons) {
        this.validButtons = validButtons;
    }

    public boolean isValid(int button) {
        if (this.validButtons == null) {
            return true;
        }

        for (int validButton : this.validButtons) {
            if (validButton == button) {
                return true;
            }
        }
        return false;
    }
}
