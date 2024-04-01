package dev.dubhe.anvilcraft.network;

import dev.dubhe.anvilcraft.api.network.Packet;
import dev.dubhe.anvilcraft.client.gui.screen.inventory.IFilterScreen;
import dev.dubhe.anvilcraft.init.ModNetworks;
import dev.dubhe.anvilcraft.inventory.IFilterMenu;
import lombok.Getter;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
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
    public ResourceLocation getType() {
        return ModNetworks.MATERIAL_PACKET;
    }

    @Override
    public void encode(@NotNull FriendlyByteBuf buf) {
        buf.writeBoolean(this.isRecordMaterial());
    }

    @Override
    public void handler(@NotNull MinecraftServer server, ServerPlayer player) {
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
    public void handler() {
        Minecraft client = Minecraft.getInstance();
        client.execute(() -> {
            if (!(client.screen instanceof IFilterScreen screen)) return;
            if (screen.getRecordButton() == null) return;
            screen.getRecordButton().setRecord(this.isRecordMaterial());
        });
    }
}
