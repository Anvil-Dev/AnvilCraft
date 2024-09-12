package dev.dubhe.anvilcraft.network;

import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.inventory.SliderMenu;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.network.handling.IPayloadHandler;

import lombok.Getter;

@Getter
public class SliderUpdatePack implements CustomPacketPayload {
    public static final Type<SliderUpdatePack> TYPE = new Type<>(AnvilCraft.of("slider_update"));
    public static final StreamCodec<RegistryFriendlyByteBuf, SliderUpdatePack> STREAM_CODEC =
            StreamCodec.composite(ByteBufCodecs.INT, SliderUpdatePack::getValue, SliderUpdatePack::new);
    public static final IPayloadHandler<SliderUpdatePack> HANDLER = SliderUpdatePack::serverHandler;

    private final int value;

    public SliderUpdatePack(int value) {
        this.value = value;
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void serverHandler(SliderUpdatePack data, IPayloadContext context) {
        ServerPlayer player = (ServerPlayer) context.player();
        context.enqueueWork(() -> {
            if (!player.hasContainerOpen()) return;
            if (!(player.containerMenu instanceof SliderMenu menu)) return;
            SliderMenu.Update update = menu.getUpdate();
            if (update != null) update.update(data.value);
        });
    }
}
