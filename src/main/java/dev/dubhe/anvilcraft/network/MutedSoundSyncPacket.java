package dev.dubhe.anvilcraft.network;


import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.client.gui.screen.inventory.ActiveSilencerScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.network.handling.IPayloadHandler;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class MutedSoundSyncPacket implements CustomPacketPayload {
    public static final Type<MutedSoundSyncPacket> TYPE = new Type<>(AnvilCraft.of("muted_sound_sync"));
    public static final StreamCodec<RegistryFriendlyByteBuf, MutedSoundSyncPacket> STREAM_CODEC = StreamCodec.ofMember(
            MutedSoundSyncPacket::encode,
            MutedSoundSyncPacket::new
    );
    public static final IPayloadHandler<MutedSoundSyncPacket> HANDLER = MutedSoundSyncPacket::clientHandler;

    private final List<ResourceLocation> sounds;

    public MutedSoundSyncPacket(List<ResourceLocation> sounds) {
        this.sounds = sounds;
    }

    public MutedSoundSyncPacket(RegistryFriendlyByteBuf buf) {
        sounds = buf.readList(FriendlyByteBuf::readResourceLocation);
    }

    public void encode(@NotNull FriendlyByteBuf buf) {
        buf.writeCollection(sounds, FriendlyByteBuf::writeResourceLocation);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void clientHandler(MutedSoundSyncPacket data, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (Minecraft.getInstance().screen instanceof ActiveSilencerScreen screen) {
                screen.handleSync(data.sounds);
            }
        });
    }
}
