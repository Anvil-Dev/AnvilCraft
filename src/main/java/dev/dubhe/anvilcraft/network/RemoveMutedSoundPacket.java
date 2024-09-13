package dev.dubhe.anvilcraft.network;

import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.inventory.ActiveSilencerMenu;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.network.handling.IPayloadHandler;

import org.jetbrains.annotations.NotNull;

public class RemoveMutedSoundPacket implements CustomPacketPayload {
    public static final Type<RemoveMutedSoundPacket> TYPE = new Type<>(AnvilCraft.of("muted_sound_remove"));
    public static final StreamCodec<RegistryFriendlyByteBuf, RemoveMutedSoundPacket> STREAM_CODEC =
            StreamCodec.ofMember(RemoveMutedSoundPacket::encode, RemoveMutedSoundPacket::new);
    public static final IPayloadHandler<RemoveMutedSoundPacket> HANDLER = RemoveMutedSoundPacket::serverHandler;

    private final ResourceLocation soundId;

    public RemoveMutedSoundPacket(ResourceLocation soundId) {
        this.soundId = soundId;
    }

    public RemoveMutedSoundPacket(RegistryFriendlyByteBuf buf) {
        this.soundId = buf.readResourceLocation();
    }

    public void encode(@NotNull RegistryFriendlyByteBuf buf) {
        buf.writeResourceLocation(soundId);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    /**
     *
     */
    public static void serverHandler(RemoveMutedSoundPacket data, IPayloadContext context) {
        ServerPlayer player = (ServerPlayer) context.player();
        context.enqueueWork(() -> {
            if (player.containerMenu instanceof ActiveSilencerMenu menu) {
                menu.removeSound(data.soundId);
            }
        });
    }
}
