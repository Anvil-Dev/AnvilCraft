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
public class SlotDisableChangePack implements CustomPacketPayload {
    public static final Type<SlotDisableChangePack> TYPE = new Type<>(AnvilCraft.of("slot_disable_change"));
    public static final StreamCodec<RegistryFriendlyByteBuf, SlotDisableChangePack> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.INT,
                    SlotDisableChangePack::getIndex,
                    ByteBufCodecs.BOOL,
                    SlotDisableChangePack::isState,
                    SlotDisableChangePack::new);
    public static final IPayloadHandler<SlotDisableChangePack> HANDLER =
            new DirectionalPayloadHandler<>(SlotDisableChangePack::clientHandler, SlotDisableChangePack::serverHandler);

    private final int index;
    private final boolean state;

    public SlotDisableChangePack(int index, boolean state) {
        this.index = index;
        this.state = state;
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void serverHandler(SlotDisableChangePack data, IPayloadContext context) {
        ServerPlayer player = (ServerPlayer) context.player();
        context.enqueueWork(() -> {
            if (!player.hasContainerOpen()) return;
            if (!(player.containerMenu instanceof IFilterMenu menu)) return;
            menu.setSlotDisabled(data.index, data.state);
            menu.flush();
            PacketDistributor.sendToPlayer(player, data);
        });
    }

    public static void clientHandler(SlotDisableChangePack data, IPayloadContext context) {
        Minecraft client = Minecraft.getInstance();
        context.enqueueWork(() -> {
            if (!(client.screen instanceof IFilterScreen<?> screen)) return;
            screen.setSlotDisabled(data.index, data.state);
        });
    }
}
