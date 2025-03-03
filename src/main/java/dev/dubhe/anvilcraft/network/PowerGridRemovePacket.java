package dev.dubhe.anvilcraft.network;

import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.api.power.PowerGrid;
import dev.dubhe.anvilcraft.api.power.SimplePowerGrid;
import dev.dubhe.anvilcraft.client.PowerGridClient;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.network.handling.IPayloadHandler;

import lombok.Getter;
import org.jetbrains.annotations.NotNull;

@Getter
public class PowerGridRemovePacket implements CustomPacketPayload {
    public static final Type<PowerGridRemovePacket> TYPE = new Type<>(AnvilCraft.of("power_grid_remove"));
    public static final StreamCodec<RegistryFriendlyByteBuf, PowerGridRemovePacket> STREAM_CODEC =
        StreamCodec.composite(ByteBufCodecs.INT, PowerGridRemovePacket::getGrid, PowerGridRemovePacket::new);
    public static final IPayloadHandler<PowerGridRemovePacket> HANDLER = PowerGridRemovePacket::clientHandler;

    private final int grid;

    /**
     * 电网移除
     */
    public PowerGridRemovePacket(@NotNull PowerGrid grid) {
        this(grid.hashCode());
    }

    public PowerGridRemovePacket(int grid) {
        this.grid = grid;
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public void clientHandler(IPayloadContext context) {
        context.enqueueWork(() -> {
            SimplePowerGrid powerGrid = PowerGridClient.getGridMap().get(this.grid);
            if (powerGrid != null) {
                powerGrid.destroy();
            }
            PowerGridClient.getGridMap().remove(this.grid);
        });
    }
}
