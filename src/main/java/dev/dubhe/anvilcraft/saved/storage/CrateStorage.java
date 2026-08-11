package dev.dubhe.anvilcraft.saved.storage;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.anvilcraft.lib.v2.util.stack.UnlimitedItemStack;
import dev.dubhe.anvilcraft.api.itemhandler.unlimited.SpaceSizeItemStacksResourceHandler;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;

import java.util.UUID;
import java.util.function.BiConsumer;

public class CrateStorage extends BaseStorage<SpaceSizeItemStacksResourceHandler> {
    public static final MapCodec<CrateStorage> CODEC = RecordCodecBuilder.mapCodec(ins -> ins.group(
        UUIDUtil.CODEC
            .fieldOf("storage_id")
            .forGetter(CrateStorage::getId),
        SpaceSizeItemStacksResourceHandler.CODEC
            .forGetter(CrateStorage::getItems)
    ).apply(ins, CrateStorage::of));
    public static final StreamCodec<RegistryFriendlyByteBuf, CrateStorage> STREAM_CODEC = StreamCodec.composite(
        UUIDUtil.STREAM_CODEC,
        CrateStorage::getId,
        SpaceSizeItemStacksResourceHandler.STREAM_CODEC,
        CrateStorage::getItems,
        CrateStorage::of
    );

    public CrateStorage(UUID id) {
        super(id);
    }

    private static CrateStorage of(UUID id, SpaceSizeItemStacksResourceHandler items) {
        CrateStorage storage = new CrateStorage(id);
        storage.getItems().sync(items);
        return storage;
    }

    @Override
    protected SpaceSizeItemStacksResourceHandler constructItemHandler(BiConsumer<Integer, UnlimitedItemStack> onContentsChanged) {
        return new SpaceSizeItemStacksResourceHandler(2048) {
            @Override
            protected void onContentsChanged(int index, UnlimitedItemStack original) {
                onContentsChanged.accept(index, original);
            }
        };
    }
}