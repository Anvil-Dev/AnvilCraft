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

public class LargeCrateStorage extends BaseStorage {
    public static final MapCodec<LargeCrateStorage> CODEC = CodecUtil.mapCodec(
        UUIDUtil.CODEC
            .fieldOf("storage_id")
            .forGetter(LargeCrateStorage::getId),
        TypeLimitItemStacksResourceHandler.CODEC
            .forGetter(LargeCrateStorage::getItems),
        LargeCrateStorage::of
    );
    public static final StreamCodec<RegistryFriendlyByteBuf, LargeCrateStorage> STREAM_CODEC = StreamCodec.composite(
        UUIDUtil.STREAM_CODEC,
        LargeCrateStorage::getId,
        TypeLimitItemStacksResourceHandler.STREAM_CODEC,
        LargeCrateStorage::getItems,
        LargeCrateStorage::of
    );

    public LargeCrateStorage(UUID id) {
        super(id);
    }

    private static LargeCrateStorage of(UUID id, TypeLimitItemStacksResourceHandler items) {
        LargeCrateStorage storage = new LargeCrateStorage(id);
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
