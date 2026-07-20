package dev.dubhe.anvilcraft.saved.storage;

import com.mojang.serialization.MapCodec;
import dev.anvilcraft.lib.v2.util.UnlimitedItemStack;
import dev.anvilcraft.lib.v2.util.Util;
import dev.dubhe.anvilcraft.api.itemhandler.TypeLimitItemStacksResourceHandler;
import dev.dubhe.anvilcraft.rpc.StorageServerStub;
import dev.dubhe.anvilcraft.saved.BetterSavedData;
import it.unimi.dsi.fastutil.ints.IntObjectBiConsumer;
import lombok.Getter;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

import java.util.UUID;

@Getter
public abstract class BaseStorage extends BetterSavedData {
    public static final MapCodec<BaseStorage> CODEC = StorageType.CODEC
        .dispatchMap(StorageType::find, StorageType::codec);
    public static final StreamCodec<RegistryFriendlyByteBuf, BaseStorage> STREAM_CODEC = StorageType.STREAM_CODEC
        .<RegistryFriendlyByteBuf>cast()
        .dispatch(StorageType::find, StorageType::streamCodec);
    private final UUID id;
    private final TypeLimitItemStacksResourceHandler items = this.constructItemHandler(this::onContentsChanged);

    protected BaseStorage(UUID id) {
        this.id = id;
    }

    protected abstract TypeLimitItemStacksResourceHandler constructItemHandler(
        IntObjectBiConsumer<UnlimitedItemStack> onContentsChanged
    );

    protected void onContentsChanged(int index, UnlimitedItemStack original) {
        StorageServerStub.onContentsChanged(this.id);
        Storages.get().setDirty();
    }

    protected <T extends BaseStorage> T sync(TypeLimitItemStacksResourceHandler items) {
        this.items.sync(items);
        return Util.cast(this);
    }

    @Override
    protected void registerDataFixers() {
    }

    @Override
    protected Packet<? extends CustomPacketPayload> createPacket(RegistryAccess registryAccess) {
        return null;
    }
}
