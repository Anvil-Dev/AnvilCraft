package dev.dubhe.anvilcraft.network;

import dev.anvilcraft.lib.network.Packet;
import dev.dubhe.anvilcraft.client.gui.screen.inventory.ActiveSilencerScreen;
import dev.dubhe.anvilcraft.init.ModNetworks;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;

public class ClientboundMutedSoundSyncPacket implements Packet {
    private final List<ResourceLocation> sounds;

    public ClientboundMutedSoundSyncPacket(FriendlyByteBuf buf) {
        sounds = buf.readList(FriendlyByteBuf::readResourceLocation);
    }

    public ClientboundMutedSoundSyncPacket(List<ResourceLocation> sounds) {
        this.sounds = sounds;
    }

    @Override
    public ResourceLocation getType() {
        return ModNetworks.MUTED_SOUND_SYNC;
    }

    @Override
    public void encode(@NotNull FriendlyByteBuf buf) {
        buf.writeCollection(sounds, FriendlyByteBuf::writeResourceLocation);
    }

    @Override
    public void handler() {
        Minecraft.getInstance().execute(() -> {
            if (Minecraft.getInstance().screen instanceof ActiveSilencerScreen screen) {
                screen.handleSync(sounds);
            }
        });
    }
}
