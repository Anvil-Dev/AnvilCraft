package dev.dubhe.anvilcraft.saved.storage;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.anvilcraft.lib.v2.util.stack.UnlimitedItemStack;
import dev.dubhe.anvilcraft.api.itemhandler.unlimited.TypeLimitItemStacksResourceHandler;
import it.unimi.dsi.fastutil.ints.IntObjectBiConsumer;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;

import java.util.UUID;

public class ShulkerContainerStorage extends BaseStorage<TypeLimitItemStacksResourceHandler> {
    public static final MapCodec<ShulkerContainerStorage> CODEC = RecordCodecBuilder.mapCodec(ins -> ins.group(
        UUIDUtil.CODEC
            .fieldOf("storage_id")
            .forGetter(ShulkerContainerStorage::getId),
        TypeLimitItemStacksResourceHandler.CODEC
            .forGetter(ShulkerContainerStorage::getItems)
    ).apply(ins, ShulkerContainerStorage::of));
    public static final StreamCodec<RegistryFriendlyByteBuf, ShulkerContainerStorage> STREAM_CODEC = StreamCodec.composite(
        UUIDUtil.STREAM_CODEC,
        ShulkerContainerStorage::getId,
        TypeLimitItemStacksResourceHandler.STREAM_CODEC,
        ShulkerContainerStorage::getItems,
        ShulkerContainerStorage::of
    );

    public ShulkerContainerStorage(UUID id) {
        super(id);
    }

    private static ShulkerContainerStorage of(UUID id, TypeLimitItemStacksResourceHandler items) {
        ShulkerContainerStorage storage = new ShulkerContainerStorage(id);
        storage.getItems().sync(items);
        return storage;
    }

    @Override
    protected TypeLimitItemStacksResourceHandler constructItemHandler(IntObjectBiConsumer<UnlimitedItemStack> onContentsChanged) {
        return new TypeLimitItemStacksResourceHandler(65536, 65536) {
            @Override
            protected void onContentsChanged(int index, UnlimitedItemStack original) {
                onContentsChanged.accept(index, original);
            }
        };
    }
}