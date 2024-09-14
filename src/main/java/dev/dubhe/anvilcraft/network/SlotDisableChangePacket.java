package dev.dubhe.anvilcraft.network;

import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.client.gui.screen.inventory.IFilterScreen;
import dev.dubhe.anvilcraft.inventory.IFilterMenu;

import net.minecraft.client.Minecraft;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.handling.DirectionalPayloadHandler;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.network.handling.IPayloadHandler;

import lombok.Getter;

@Getter
public class SlotDisableChangePacket implements CustomPacketPayload {
    public static final Type<SlotDisableChangePacket> TYPE = new Type<>(AnvilCraft.of("slot_disable_change"));
    public static final StreamCodec<RegistryFriendlyByteBuf, SlotDisableChangePacket> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.INT,
                    SlotDisableChangePacket::getIndex,
                    ByteBufCodecs.BOOL,
                    SlotDisableChangePacket::isState,
                    SlotDisableChangePacket::new);
    public static final IPayloadHandler<SlotDisableChangePacket> HANDLER = new DirectionalPayloadHandler<>(
            SlotDisableChangePacket::clientHandler, SlotDisableChangePacket::serverHandler);

    private final int index;
    private final boolean state;

    public SlotDisableChangePacket(int index, boolean state) {
        this.index = index;
        this.state = state;
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void serverHandler(SlotDisableChangePacket data, IPayloadContext context) {
        ServerPlayer player = (ServerPlayer) context.player();
        context.enqueueWork(() -> {
            if (!player.hasContainerOpen()) return;
            if (!(player.containerMenu instanceof IFilterMenu menu)) return;
            menu.setSlotDisabled(data.index, data.state);
            menu.flush();
            PacketDistributor.sendToPlayer(player, data);
        });
    }

    public static void clientHandler(SlotDisableChangePacket data, IPayloadContext context) {
        Minecraft client = Minecraft.getInstance();
        context.enqueueWork(() -> {
            if (!(client.screen instanceof IFilterScreen<?> screen)) return;
            screen.setSlotDisabled(data.index, data.state);
        });
    }
}
