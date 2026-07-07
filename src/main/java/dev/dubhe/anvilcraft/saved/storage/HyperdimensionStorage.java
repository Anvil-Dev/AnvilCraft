package dev.dubhe.anvilcraft.saved.storage;

import com.mojang.serialization.MapCodec;
import dev.anvilcraft.lib.v2.codec.CodecUtil;
import dev.anvilcraft.lib.v2.util.UnlimitedItemStack;
import dev.dubhe.anvilcraft.api.itemhandler.TypeLimitItemStacksResourceHandler;
import it.unimi.dsi.fastutil.ints.IntObjectBiConsumer;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;

import java.util.UUID;

public class HyperdimensionStorage extends BaseStorage {
    public static final MapCodec<HyperdimensionStorage> CODEC = CodecUtil.mapCodec(
        UUIDUtil.CODEC
            .fieldOf("storage_id")
            .forGetter(HyperdimensionStorage::getId),
        TypeLimitItemStacksResourceHandler.CODEC
            .forGetter(HyperdimensionStorage::getItems),
        HyperdimensionStorage::of
    );
    public static final StreamCodec<RegistryFriendlyByteBuf, HyperdimensionStorage> STREAM_CODEC = StreamCodec.composite(
        UUIDUtil.STREAM_CODEC,
        HyperdimensionStorage::getId,
        TypeLimitItemStacksResourceHandler.STREAM_CODEC,
        HyperdimensionStorage::getItems,
        HyperdimensionStorage::of
    );

    public HyperdimensionStorage(UUID id) {
        super(id);
    }

    private static HyperdimensionStorage of(UUID id, TypeLimitItemStacksResourceHandler items) {
        HyperdimensionStorage storage = new HyperdimensionStorage(id);
        storage.getItems().sync(items);
        return storage;
    }

    @Override
    protected TypeLimitItemStacksResourceHandler constructItemHandler(IntObjectBiConsumer<UnlimitedItemStack> onContentsChanged) {
        return new TypeLimitItemStacksResourceHandler(65536) {
            @Override
            protected void onContentsChanged(int index, UnlimitedItemStack original) {
                onContentsChanged.accept(index, original);
            }
        };
    }
}
