package dev.dubhe.anvilcraft.saved.storage;

import com.mojang.serialization.MapCodec;
import dev.anvilcraft.lib.v2.codec.CodecUtil;
import dev.dubhe.anvilcraft.api.itemhandler.TypeLimitItemStacksResourceHandler;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;

public class LargeCrateStorage extends BaseStorage {
    public static final MapCodec<LargeCrateStorage> CODEC = CodecUtil.mapCodec(
        TypeLimitItemStacksResourceHandler.CODEC
            .forGetter(LargeCrateStorage::getItems),
        LargeCrateStorage::of
    );
    public static final StreamCodec<RegistryFriendlyByteBuf, LargeCrateStorage> STREAM_CODEC = StreamCodec.composite(
        TypeLimitItemStacksResourceHandler.STREAM_CODEC,
        LargeCrateStorage::getItems,
        LargeCrateStorage::of
    );

    private static LargeCrateStorage of(TypeLimitItemStacksResourceHandler items) {
        return new LargeCrateStorage().sync(items);
    }

    @Override
    protected TypeLimitItemStacksResourceHandler constructItemHandler() {
        return new TypeLimitItemStacksResourceHandler(65536);
    }
}
