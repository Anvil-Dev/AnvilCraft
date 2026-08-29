package dev.dubhe.anvilcraft.network;

import dev.anvilcraft.lib.v2.network.packet.IClientboundPacket;
import dev.anvilcraft.lib.v2.network.packet.IPacket;
import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.api.power.PowerGrid;
import dev.dubhe.anvilcraft.client.support.PowerGridSupport;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.entity.player.Player;

public record PowerGridRemovePacket(int grid) implements IClientboundPacket {
    public static final Type<PowerGridRemovePacket> TYPE = IPacket.type(AnvilCraft.of("power_grid_remove"));
    public static final StreamCodec<ByteBuf, PowerGridRemovePacket> STREAM_CODEC = StreamCodec.composite(
        ByteBufCodecs.INT,
        PowerGridRemovePacket::grid,
        PowerGridRemovePacket::new
    );

    /**
     * 电网移除
     */
    public PowerGridRemovePacket(PowerGrid grid) {
        this(grid.hashCode());
    }

    @Override
    public Type<PowerGridRemovePacket> type() {
        return TYPE;
    }

    @Override
    public void handleOnClient(Player player) {
        PowerGridSupport.removeGrid(this.grid);
    }
}
