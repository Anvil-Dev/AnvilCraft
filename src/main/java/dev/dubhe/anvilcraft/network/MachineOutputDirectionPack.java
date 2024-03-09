package dev.dubhe.anvilcraft.network;

import dev.dubhe.anvilcraft.api.network.Packet;
import dev.dubhe.anvilcraft.client.gui.screen.inventory.BaseMachineScreen;
import dev.dubhe.anvilcraft.init.ModNetworks;
import dev.dubhe.anvilcraft.inventory.BaseMachineMenu;
import lombok.Getter;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.networking.v1.PacketSender;
import net.fabricmc.fabric.api.networking.v1.PacketType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.core.Direction;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import org.jetbrains.annotations.NotNull;

@Getter
public class MachineOutputDirectionPack implements Packet {
    private final Direction direction;

    public MachineOutputDirectionPack(Direction direction) {
        this.direction = direction;
    }

    public MachineOutputDirectionPack(@NotNull FriendlyByteBuf buf) {
        this(buf.readEnum(Direction.class));
    }

    @Override
    public void write(@NotNull FriendlyByteBuf buf) {
        buf.writeEnum(this.getDirection());
    }

    @Override
    public PacketType<?> getType() {
        return ModNetworks.DIRECTION_PACKET_TYPE;
    }

    @Override
    public void receive(@NotNull MinecraftServer server, ServerPlayer player, ServerGamePacketListenerImpl handler, PacketSender sender) {
        server.execute(() -> {
            if (!player.hasContainerOpen()) return;
            if (!(player.containerMenu instanceof BaseMachineMenu menu)) return;
            Direction direction = this.getDirection();
            menu.setDirection(direction);
            this.send(player);
        });
    }

    @Override
    @Environment(EnvType.CLIENT)
    public void receive(@NotNull Minecraft client, ClientPacketListener handler, PacketSender responseSender) {
        client.execute(() -> {
            if (!(client.screen instanceof BaseMachineScreen<?> screen)) return;
            if (screen.getDirectionButton() == null) return;
            screen.getDirectionButton().setDirection(this.direction);
        });
    }
}
