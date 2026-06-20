package dev.dubhe.anvilcraft.saved.storage;

import com.mojang.serialization.MapCodec;
import dev.anvilcraft.lib.v2.codec.CodecUtil;
import dev.anvilcraft.lib.v2.util1.stack.UnlimitedItemStack;
import dev.dubhe.anvilcraft.api.itemhandler.TypeLimitItemStacksResourceHandler;
import it.unimi.dsi.fastutil.ints.IntObjectBiConsumer;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;

import java.util.UUID;

public class CrateStorage extends BaseStorage {
    public static final MapCodec<CrateStorage> CODEC = CodecUtil.mapCodec(
        UUIDUtil.CODEC
            .fieldOf("storage_id")
            .forGetter(CrateStorage::getId),
        TypeLimitItemStacksResourceHandler.CODEC
            .forGetter(CrateStorage::getItems),
        CrateStorage::of
    );
    public static final StreamCodec<RegistryFriendlyByteBuf, CrateStorage> STREAM_CODEC = StreamCodec.composite(
        UUIDUtil.STREAM_CODEC,
        CrateStorage::getId,
        TypeLimitItemStacksResourceHandler.STREAM_CODEC,
        CrateStorage::getItems,
        CrateStorage::of
    );

    public CrateStorage(UUID id) {
        super(id);
    }

    private static CrateStorage of(UUID id, TypeLimitItemStacksResourceHandler items) {
        CrateStorage storage = new CrateStorage(id);
        storage.getItems().sync(items);
        return storage;
    }

    @Override
    protected TypeLimitItemStacksResourceHandler constructItemHandler(IntObjectBiConsumer<UnlimitedItemStack> onContentsChanged) {
        return new TypeLimitItemStacksResourceHandler(2048) {
            @Override
            protected void onContentsChanged(int index, UnlimitedItemStack original) {
                onContentsChanged.accept(index, original);
            }
        };
    }
}
