package dev.dubhe.anvilcraft.network;

import dev.anvilcraft.lib.network.Packet;
import dev.dubhe.anvilcraft.init.ModNetworks;
import dev.dubhe.anvilcraft.inventory.ActiveSilencerMenu;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import org.jetbrains.annotations.NotNull;

public class ServerboundAddMutedSoundPacket implements Packet {
    private final ResourceLocation soundId;

    public ServerboundAddMutedSoundPacket(ResourceLocation soundId) {
        this.soundId = soundId;
    }

    public ServerboundAddMutedSoundPacket(FriendlyByteBuf buf) {
        this.soundId = buf.readResourceLocation();
    }

    @Override
    public ResourceLocation getType() {
        return ModNetworks.MUTED_SOUND_ADD;
    }

    @Override
    public void encode(@NotNull FriendlyByteBuf buf) {
        buf.writeResourceLocation(soundId);
    }

    @Override
    public void handler(@NotNull MinecraftServer server, ServerPlayer player) {
        server.execute(() -> {
            if (player.containerMenu instanceof ActiveSilencerMenu menu) {
                menu.addSound(soundId);
            }
        });
    }
}
