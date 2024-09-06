package dev.dubhe.anvilcraft.network;

import dev.anvilcraft.lib.network.Packet;
import dev.dubhe.anvilcraft.init.ModNetworks;
import dev.dubhe.anvilcraft.inventory.SliderMenu;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import org.jetbrains.annotations.NotNull;

public class SliderUpdatePack implements Packet {
    private final int value;

    public SliderUpdatePack(int value) {
        this.value = value;
    }

    public SliderUpdatePack(@NotNull FriendlyByteBuf buf) {
        this.value = buf.readInt();
    }

    @Override
    public ResourceLocation getType() {
        return ModNetworks.SLIDER_UPDATE;
    }

    @Override
    public void encode(@NotNull FriendlyByteBuf buf) {
        buf.writeInt(this.value);
    }

    @Override
    public void handler(@NotNull MinecraftServer server, ServerPlayer player) {
        server.execute(() -> {
            if (!player.hasContainerOpen()) return;
            if (!(player.containerMenu instanceof SliderMenu menu)) return;
            SliderMenu.Update update = menu.getUpdate();
            if (update != null) update.update(value);
        });
    }
}
