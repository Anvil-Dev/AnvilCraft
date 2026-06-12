package dev.dubhe.anvilcraft.saved.storage;

import com.mojang.serialization.MapCodec;
import dev.anvilcraft.lib.v2.codec.CodecUtil;
import dev.dubhe.anvilcraft.api.itemhandler.TypeLimitItemStacksResourceHandler;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;

public class HyperdimensionStorage extends BaseStorage {
    public static final MapCodec<HyperdimensionStorage> CODEC = CodecUtil.mapCodec(
        TypeLimitItemStacksResourceHandler.CODEC
            .forGetter(HyperdimensionStorage::getItems),
        HyperdimensionStorage::of
    );
    public static final StreamCodec<RegistryFriendlyByteBuf, HyperdimensionStorage> STREAM_CODEC = StreamCodec.composite(
        TypeLimitItemStacksResourceHandler.STREAM_CODEC,
        HyperdimensionStorage::getItems,
        HyperdimensionStorage::of
    );

    private static HyperdimensionStorage of(TypeLimitItemStacksResourceHandler items) {
        return new HyperdimensionStorage().sync(items);
    }

    @Override
    protected TypeLimitItemStacksResourceHandler constructItemHandler() {
        return new TypeLimitItemStacksResourceHandler(65536, 65536);
    }
}
