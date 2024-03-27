package dev.dubhe.anvilcraft.network;

import dev.dubhe.anvilcraft.api.network.Packet;
import dev.dubhe.anvilcraft.client.gui.screen.inventory.AutoCrafterScreen;
import dev.dubhe.anvilcraft.init.ModNetworks;
import dev.dubhe.anvilcraft.inventory.AutoCrafterMenu;
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
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;

public class SlotFilterChangePack implements Packet {
    private final int index;
    private final ItemStack filter;

    public SlotFilterChangePack(int index, @NotNull ItemStack filter) {
        this.index = index;
        this.filter = filter.copy();
        this.filter.setCount(1);
    }

    public SlotFilterChangePack(@NotNull FriendlyByteBuf buf) {
        this(buf.readInt(), buf.readItem());
    }

    @Override
    public void write(@NotNull FriendlyByteBuf buf) {
        buf.writeInt(this.index);
        buf.writeItem(this.filter);
    }

    @Override
    public PacketType<?> getType() {
        return ModNetworks.SLOT_FILTER_CHANGE_TYPE;
    }

    @Override
    public void receive(@NotNull MinecraftServer server, ServerPlayer player, ServerGamePacketListenerImpl handler, PacketSender responseSender) {
        server.execute(() -> {
            if (!player.hasContainerOpen()) return;
            if (!(player.containerMenu instanceof AutoCrafterMenu menu)) return;
            menu.setFilter(this.index, this.filter);
            this.send(player);
        });
    }

    @Override
    @Environment(EnvType.CLIENT)
    public void receive(@NotNull Minecraft client, ClientPacketListener handler, PacketSender responseSender) {
        client.execute(() -> {
            if (!(client.screen instanceof AutoCrafterScreen screen)) return;
            screen.changeSlotFilter(this.index, this.filter);
        });
    }
}
