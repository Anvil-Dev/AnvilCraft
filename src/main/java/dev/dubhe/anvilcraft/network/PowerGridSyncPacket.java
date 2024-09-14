package dev.dubhe.anvilcraft.network;

import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.api.power.PowerGrid;
import dev.dubhe.anvilcraft.api.power.SimplePowerGrid;
import dev.dubhe.anvilcraft.client.renderer.PowerGridRenderer;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.network.handling.IPayloadHandler;

import lombok.Getter;
import org.jetbrains.annotations.NotNull;

@Getter
public class PowerGridSyncPacket implements CustomPacketPayload {
    public static final Type<PowerGridSyncPacket> TYPE = new Type<>(AnvilCraft.of("power_grid_sync"));
    public static final StreamCodec<RegistryFriendlyByteBuf, PowerGridSyncPacket> STREAM_CODEC =
            StreamCodec.ofMember(PowerGridSyncPacket::encode, PowerGridSyncPacket::new);
    public static final IPayloadHandler<PowerGridSyncPacket> HANDLER = PowerGridSyncPacket::clientHandler;

    private final SimplePowerGrid grid;

    /**
     * 电网同步
     */
    public PowerGridSyncPacket(PowerGrid grid) {
        this.grid = new SimplePowerGrid(grid);
    }

    /**
     * @param buf 缓冲区
     */
    public PowerGridSyncPacket(@NotNull FriendlyByteBuf buf) {
        CompoundTag tag = buf.readNbt();
        Tag data = tag.get("data");
        this.grid =
                SimplePowerGrid.CODEC.decode(NbtOps.INSTANCE, data).getOrThrow().getFirst();
    }

    public void encode(@NotNull FriendlyByteBuf buf) {
        this.grid.encode(buf);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void clientHandler(PowerGridSyncPacket data, IPayloadContext context) {
        context.enqueueWork(() -> PowerGridRenderer.getGridMap().put(data.grid.getHash(), data.grid));
    }
}
