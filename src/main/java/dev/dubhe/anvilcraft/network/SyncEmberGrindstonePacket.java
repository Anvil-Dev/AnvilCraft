package dev.dubhe.anvilcraft.network;

import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.inventory.EmberGrindstoneMenu;
import io.netty.buffer.ByteBuf;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.network.handling.IPayloadHandler;

import javax.annotation.ParametersAreNonnullByDefault;

@MethodsReturnNonnullByDefault
@ParametersAreNonnullByDefault
public record SyncEmberGrindstonePacket(int selectedIndex) implements CustomPacketPayload {
    public static final Type<SyncEmberGrindstonePacket> TYPE = new Type<>(AnvilCraft.of("sync_ember_grindstone"));
    public static final StreamCodec<ByteBuf, SyncEmberGrindstonePacket> STREAM_CODEC = ByteBufCodecs.VAR_INT.map(
        SyncEmberGrindstonePacket::new, SyncEmberGrindstonePacket::selectedIndex
    );
    public static final IPayloadHandler<SyncEmberGrindstonePacket> HANDLER = SyncEmberGrindstonePacket::serverHandler;

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void serverHandler(SyncEmberGrindstonePacket data, IPayloadContext context) {
        ServerPlayer player = (ServerPlayer) context.player();
        context.enqueueWork(() -> {
            if (!player.hasContainerOpen()) return;
            if (!(player.containerMenu instanceof EmberGrindstoneMenu menu)) return;
            menu.setSelectedEnchantment(data.selectedIndex);
        });
    }
}
