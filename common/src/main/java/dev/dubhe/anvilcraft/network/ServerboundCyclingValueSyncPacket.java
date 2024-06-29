package dev.dubhe.anvilcraft.network;

import dev.anvilcraft.lib.network.Packet;
import dev.dubhe.anvilcraft.init.ModNetworks;
import dev.dubhe.anvilcraft.inventory.ItemCollectorMenu;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import org.jetbrains.annotations.NotNull;

public class ServerboundCyclingValueSyncPacket implements Packet {
    private final int index;
    private final String name;

    public ServerboundCyclingValueSyncPacket(int index, String name) {
        this.index = index;
        this.name = name;
    }

    public ServerboundCyclingValueSyncPacket(@NotNull FriendlyByteBuf buf) {
        this.index = buf.readInt();
        this.name = buf.readUtf();
    }

    @Override
    public ResourceLocation getType() {
        return ModNetworks.CYCLING_VALUE;
    }

    @Override
    public void encode(@NotNull FriendlyByteBuf buf) {
        buf.writeInt(index);
        buf.writeUtf(name);
    }

    @Override
    public void handler(@NotNull MinecraftServer server, ServerPlayer player) {
        if (player.containerMenu instanceof ItemCollectorMenu menu) {
            menu.notify(index, name);
        }
    }
}
