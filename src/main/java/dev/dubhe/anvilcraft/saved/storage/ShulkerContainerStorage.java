package dev.dubhe.anvilcraft.saved.storage;

import com.mojang.serialization.MapCodec;
import dev.anvilcraft.lib.v2.codec.CodecUtil;
import dev.dubhe.anvilcraft.api.itemhandler.TypeLimitItemStacksResourceHandler;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;

public class ShulkerContainerStorage extends BaseStorage {
    public static final MapCodec<ShulkerContainerStorage> CODEC = CodecUtil.mapCodec(
        TypeLimitItemStacksResourceHandler.CODEC
            .forGetter(ShulkerContainerStorage::getItems),
        ShulkerContainerStorage::of
    );
    public static final StreamCodec<RegistryFriendlyByteBuf, ShulkerContainerStorage> STREAM_CODEC = StreamCodec.composite(
        TypeLimitItemStacksResourceHandler.STREAM_CODEC,
        ShulkerContainerStorage::getItems,
        ShulkerContainerStorage::of
    );

    private static ShulkerContainerStorage of(TypeLimitItemStacksResourceHandler items) {
        return new ShulkerContainerStorage().sync(items);
    }

    @Override
    protected TypeLimitItemStacksResourceHandler constructItemHandler() {
        return new TypeLimitItemStacksResourceHandler(65536, 65536);
    }
}
