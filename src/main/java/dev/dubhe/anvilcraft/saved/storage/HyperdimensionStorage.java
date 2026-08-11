package dev.dubhe.anvilcraft.saved.storage;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.anvilcraft.lib.v2.util.stack.UnlimitedItemStack;
import dev.dubhe.anvilcraft.api.itemhandler.unlimited.UnlimitedItemStacksResourceHandler;
import it.unimi.dsi.fastutil.ints.IntObjectBiConsumer;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;

import java.util.UUID;

public class HyperdimensionStorage extends BaseStorage<UnlimitedItemStacksResourceHandler> {
    public static final MapCodec<HyperdimensionStorage> CODEC = RecordCodecBuilder.mapCodec(ins -> ins.group(
        UUIDUtil.CODEC
            .fieldOf("storage_id")
            .forGetter(HyperdimensionStorage::getId),
        UnlimitedItemStacksResourceHandler.CODEC
            .forGetter(HyperdimensionStorage::getItems)
    ).apply(ins, HyperdimensionStorage::of));
    public static final StreamCodec<RegistryFriendlyByteBuf, HyperdimensionStorage> STREAM_CODEC = StreamCodec.composite(
        UUIDUtil.STREAM_CODEC,
        HyperdimensionStorage::getId,
        UnlimitedItemStacksResourceHandler.STREAM_CODEC,
        HyperdimensionStorage::getItems,
        HyperdimensionStorage::of
    );

    public HyperdimensionStorage(UUID id) {
        super(id);
    }

    private static HyperdimensionStorage of(UUID id, UnlimitedItemStacksResourceHandler items) {
        HyperdimensionStorage storage = new HyperdimensionStorage(id);
        storage.getItems().sync(items);
        return storage;
    }

    @Override
    protected UnlimitedItemStacksResourceHandler constructItemHandler(IntObjectBiConsumer<UnlimitedItemStack> onContentsChanged) {
        return new UnlimitedItemStacksResourceHandler(65536) {
            @Override
            protected void onContentsChanged(int index, UnlimitedItemStack original) {
                onContentsChanged.accept(index, original);
            }
        };
    }
}