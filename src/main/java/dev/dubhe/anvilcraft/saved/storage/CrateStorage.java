package dev.dubhe.anvilcraft.saved.storage;

import com.mojang.serialization.MapCodec;
import dev.anvilcraft.lib.v2.codec.CodecUtil;
import dev.dubhe.anvilcraft.api.itemhandler.TypeLimitItemStacksResourceHandler;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;

public class CrateStorage extends BaseStorage {
    public static final MapCodec<CrateStorage> CODEC = CodecUtil.mapCodec(
        TypeLimitItemStacksResourceHandler.CODEC
            .forGetter(CrateStorage::getItems),
        CrateStorage::of
    );
    public static final StreamCodec<RegistryFriendlyByteBuf, CrateStorage> STREAM_CODEC = StreamCodec.composite(
        TypeLimitItemStacksResourceHandler.STREAM_CODEC,
        CrateStorage::getItems,
        CrateStorage::of
    );

    private static CrateStorage of(TypeLimitItemStacksResourceHandler items) {
        return new CrateStorage().sync(items);
    }

    @Override
    protected TypeLimitItemStacksResourceHandler constructItemHandler() {
        return new TypeLimitItemStacksResourceHandler(2048);
    }
}
