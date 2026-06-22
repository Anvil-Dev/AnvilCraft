package dev.dubhe.anvilcraft.saved;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import dev.dubhe.anvilcraft.AnvilCraft;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import net.minecraft.core.RegistryAccess;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.saveddata.SavedDataType;

/**
 * Global saved data that stores the canonical state of wormhole interfaces
 * (logistics, fluid, laser) across the entire wormhole network group.
 *
 * <p>
 * Phase 7 stub — full implementation deferred to Phase 7+ wormhole content sync.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class WormholeInterfaceStates extends BetterSavedData {

    static final WormholeInterfaceStates CLIENT_COPY = new WormholeInterfaceStates();

    public static final Codec<WormholeInterfaceStates> CODEC = CompoundTag.CODEC.comapFlatMap(
        tag -> DataResult.success(new WormholeInterfaceStates()),
        net -> {
            CompoundTag tag = new CompoundTag();
            return tag;
        }
    );

    public static final SavedDataType<WormholeInterfaceStates> TYPE = new SavedDataType<>(
        AnvilCraft.of("wormhole_interface_states"),
        WormholeInterfaceStates::new,
        WormholeInterfaceStates.CODEC,
        null
    );

    public static WormholeInterfaceStates get() {
        return BetterSavedData.get(TYPE, CLIENT_COPY);
    }

    @Override
    protected void registerDataFixers() {
    }

    @Override
    protected Packet<? extends net.minecraft.network.protocol.common.custom.CustomPacketPayload> createPacket(
        RegistryAccess registryAccess
    ) {
        // TODO Phase 7+: implement network sync for WormholeInterfaceStates
        return null;
    }
}
