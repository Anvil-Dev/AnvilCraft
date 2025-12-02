package dev.dubhe.anvilcraft.network;

import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.inventory.ItemDetectorMenu;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.network.handling.IPayloadHandler;
import org.jetbrains.annotations.NotNull;

public record ItemDetectorChangeRangePacket(int range) implements CustomPacketPayload {

    public static final Type<ItemDetectorChangeRangePacket> TYPE = new Type<>(AnvilCraft.of("item_detector_change_range"));
    public static final StreamCodec<RegistryFriendlyByteBuf, ItemDetectorChangeRangePacket> STREAM_CODEC =
        StreamCodec.composite(
            ByteBufCodecs.INT,
            ItemDetectorChangeRangePacket::range,
            ItemDetectorChangeRangePacket::new);
    public static final IPayloadHandler<ItemDetectorChangeRangePacket> HANDLER = ItemDetectorChangeRangePacket::serverHandler;

    @Override
    @NotNull
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void serverHandler(ItemDetectorChangeRangePacket data, IPayloadContext context) {
        context.enqueueWork(() -> {
            ServerPlayer player = (ServerPlayer) context.player();
            if (player.containerMenu instanceof ItemDetectorMenu menu) {
                menu.setRange(data.range);
            }
        });
    }

    // public static void clientHandler(ItemDetectorChangeRangePacket data, IPayloadContext context) {
    //     Minecraft client = Minecraft.getInstance();
    //     context.enqueueWork(() -> {
    //         if (client.screen instanceof ItemDetectorScreen screen) {
    //             screen.getMenu().setRange(data.range);
    //         }
    //     });
    // }
}
