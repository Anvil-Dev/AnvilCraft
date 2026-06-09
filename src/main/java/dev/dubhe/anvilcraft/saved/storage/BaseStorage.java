package dev.dubhe.anvilcraft.saved.storage;

import dev.dubhe.anvilcraft.api.itemhandler.TypeLimitItemStacksResourceHandler;
import dev.dubhe.anvilcraft.saved.BetterSavedData;
import dev.dubhe.anvilcraft.saved.storage.category.Categories;
import lombok.Getter;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

@Getter
public abstract class BaseStorage extends BetterSavedData {
    private final TypeLimitItemStacksResourceHandler items = this.constructItemHandler();
    private final Categories categories = new Categories();

    protected abstract TypeLimitItemStacksResourceHandler constructItemHandler();

    @Override
    protected void registerDataFixers() {
    }

    @Override
    protected Packet<? extends CustomPacketPayload> createPacket(RegistryAccess registryAccess) {
        return null;
    }
}
