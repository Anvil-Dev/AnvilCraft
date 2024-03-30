package dev.dubhe.anvilcraft.network;

import dev.dubhe.anvilcraft.api.network.Packet;
import dev.dubhe.anvilcraft.client.gui.screen.inventory.IFilterScreen;
import dev.dubhe.anvilcraft.init.ModNetworks;
import dev.dubhe.anvilcraft.inventory.IFilterMenu;
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
public class MachineRecordMaterialPack implements Packet {
    private final boolean recordMaterial;

    public MachineRecordMaterialPack(boolean recordMaterial) {
        this.recordMaterial = recordMaterial;
    }

    public MachineRecordMaterialPack(@NotNull FriendlyByteBuf buf) {
        this(buf.readBoolean());
    }

    @Override
    public void write(@NotNull FriendlyByteBuf buf) {
        buf.writeBoolean(this.isRecordMaterial());
    }

    @Override
    public PacketType<?> getType() {
        return ModNetworks.MATERIAL_PACKET_TYPE;
    }

    @Override
    public void receive(@NotNull MinecraftServer server, ServerPlayer player, ServerGamePacketListenerImpl handler, PacketSender sender) {
        server.execute(() -> {
            if (!player.hasContainerOpen()) return;
            if (!(player.containerMenu instanceof IFilterMenu menu)) return;
            menu.setRecord(this.isRecordMaterial());
            menu.update();
            if (!this.isRecordMaterial()) if (menu.getEntity() != null) {
                for (int i = 0; i < menu.getEntity().getFilter().size(); i++) {
                    new SlotFilterChangePack(i, menu.getEntity().getFilter().get(i)).send(player);
                }
            }
            this.send(player);
        });
    }

    @Override
    @Environment(EnvType.CLIENT)
    public void receive(@NotNull Minecraft client, ClientPacketListener handler, PacketSender responseSender) {
        client.execute(() -> {
            if (!(client.screen instanceof IFilterScreen screen)) return;
            if (screen.getRecordButton() == null) return;
            screen.getRecordButton().setRecord(this.isRecordMaterial());
        });
    }
}
