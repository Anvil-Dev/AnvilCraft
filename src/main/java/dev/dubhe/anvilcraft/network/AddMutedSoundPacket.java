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

public class AddMutedSoundPacket implements CustomPacketPayload {
    public static final Type<AddMutedSoundPacket> TYPE = new Type<>(AnvilCraft.of("muted_sound_add"));
    public static final StreamCodec<RegistryFriendlyByteBuf, AddMutedSoundPacket> STREAM_CODEC =
            StreamCodec.ofMember(AddMutedSoundPacket::encode, AddMutedSoundPacket::new);
    public static final IPayloadHandler<AddMutedSoundPacket> HANDLER = AddMutedSoundPacket::serverHandler;

    private final ResourceLocation soundId;

    public AddMutedSoundPacket(ResourceLocation soundId) {
        this.soundId = soundId;
    }

    public AddMutedSoundPacket(RegistryFriendlyByteBuf buf) {
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
    public static void serverHandler(AddMutedSoundPacket data, IPayloadContext context) {
        ServerPlayer player = (ServerPlayer) context.player();
        context.enqueueWork(() -> {
            if (player.containerMenu instanceof ActiveSilencerMenu menu) {
                menu.addSound(data.soundId);
            }
        });
    }
}
