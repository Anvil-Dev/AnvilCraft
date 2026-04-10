package dev.dubhe.anvilcraft.network;

import dev.anvilcraft.lib.v2.network.packet.IClientboundPacket;
import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.api.power.PowerGrid;
import dev.dubhe.anvilcraft.api.power.SimplePowerGrid;
import dev.dubhe.anvilcraft.client.support.PowerGridSupport;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.entity.player.Player;

public record PowerGridSyncPacket(SimplePowerGrid grid) implements IClientboundPacket {
    public static final Type<PowerGridSyncPacket> TYPE = new Type<>(AnvilCraft.of("power_grid_sync"));
    public static final StreamCodec<FriendlyByteBuf, PowerGridSyncPacket> STREAM_CODEC = StreamCodec.composite(
        SimplePowerGrid.STREAM_CODEC,
        PowerGridSyncPacket::grid,
        PowerGridSyncPacket::new
    );

    /**
     * 电网同步
     */
    public PowerGridSyncPacket(PowerGrid grid) {
        this(new SimplePowerGrid(grid));
    }

    @Override
    public Type<PowerGridSyncPacket> type() {
        return TYPE;
    }

    @Override
    public void handleOnClient(Player player) {
        PowerGridSupport.getGridMap().compute(
            this.grid.getId(),
            (id, grid) -> {
                if (grid != null) grid.destroy();
                return this.grid;
            }
        );
    }
}
