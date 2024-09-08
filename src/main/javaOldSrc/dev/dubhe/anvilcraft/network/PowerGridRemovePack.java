package dev.dubhe.anvilcraft.network;

import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.api.power.PowerGrid;
import dev.dubhe.anvilcraft.client.renderer.PowerGridRenderer;
import lombok.Getter;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.network.handling.IPayloadHandler;
import org.jetbrains.annotations.NotNull;

@Getter
public class PowerGridRemovePack implements CustomPacketPayload {
    public static final Type<PowerGridRemovePack> TYPE = new Type<>(AnvilCraft.of("power_grid_remove"));
    public static final StreamCodec<RegistryFriendlyByteBuf, PowerGridRemovePack> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.INT, PowerGridRemovePack::getGrid, PowerGridRemovePack::new
    );
    public static final IPayloadHandler<PowerGridRemovePack> HANDLER = PowerGridRemovePack::clientHandler;

    private final int grid;

    /**
     * 电网移除
     */
    public PowerGridRemovePack(@NotNull PowerGrid grid) {
        this(grid.hashCode());
    }

    public PowerGridRemovePack(int grid) {
        this.grid = grid;
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }


    public static void clientHandler(PowerGridRemovePack data, IPayloadContext context) {
        context.enqueueWork(() -> PowerGridRenderer.getGridMap().remove(data.grid));
    }
}
