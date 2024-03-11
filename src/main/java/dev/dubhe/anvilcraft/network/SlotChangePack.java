package dev.dubhe.anvilcraft.network;

import dev.dubhe.anvilcraft.api.network.Packet;
import dev.dubhe.anvilcraft.client.gui.screen.inventory.AutoCrafterScreen;
import dev.dubhe.anvilcraft.init.ModNetworks;
import dev.dubhe.anvilcraft.inventory.AutoCrafterMenu;
import lombok.Getter;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.networking.v1.PacketSender;
import net.fabricmc.fabric.api.networking.v1.PacketType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import org.jetbrains.annotations.NotNull;

@Getter
public class SlotChangePack implements Packet {
    private final int index;
    private final boolean state;

    public SlotChangePack(int index, boolean state) {
        this.index = index;
        this.state = state;
    }

    public SlotChangePack(@NotNull FriendlyByteBuf buf) {
        this(buf.readInt(), buf.readBoolean());
    }

    @Override
    public void write(@NotNull FriendlyByteBuf buf) {
        buf.writeInt(index);
        buf.writeBoolean(this.isState());
    }

    @Override
    public PacketType<?> getType() {
        return ModNetworks.SLOT_CHANGE_TYPE;
    }

    @Override
    public void receive(@NotNull MinecraftServer server, ServerPlayer player, ServerGamePacketListenerImpl handler, PacketSender sender) {
        server.execute(() -> {
            if (!player.hasContainerOpen()) return;
            if (!(player.containerMenu instanceof AutoCrafterMenu menu)) return;
            menu.setSlotDisabled(this.index, this.state);
            this.send(player);
        });
    }

    @Override
    @Environment(EnvType.CLIENT)
    public void receive(@NotNull Minecraft client, ClientPacketListener handler, PacketSender responseSender) {
        client.execute(() -> {
            if (!(client.screen instanceof AutoCrafterScreen screen)) return;
            screen.changeSlot(this.index, this.state);
        });
    }
}
