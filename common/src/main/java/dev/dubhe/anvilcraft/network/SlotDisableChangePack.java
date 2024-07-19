package dev.dubhe.anvilcraft.network;

import dev.anvilcraft.lib.network.Packet;
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
public class SlotDisableChangePack implements Packet {
    private final int index;
    private final boolean state;

    public SlotDisableChangePack(int index, boolean state) {
        this.index = index;
        this.state = state;
    }

    public SlotDisableChangePack(@NotNull FriendlyByteBuf buf) {
        this(buf.readInt(), buf.readBoolean());
    }

    @Override
    public void encode(@NotNull FriendlyByteBuf buf) {
        buf.writeInt(index);
        buf.writeBoolean(this.isState());
    }

    @Override
    public ResourceLocation getType() {
        return ModNetworks.SLOT_DISABLE_CHANGE_PACKET;
    }

    @Override
    public void handler(@NotNull MinecraftServer server, ServerPlayer player) {
        server.execute(() -> {
            if (!player.hasContainerOpen()) return;
            if (!(player.containerMenu instanceof IFilterMenu menu)) return;
            menu.setSlotDisabled(this.index, this.state);
            menu.flush();
            this.send(player);
        });
    }

    @Override
    @Environment(EnvType.CLIENT)
    public void handler() {
        Minecraft client = Minecraft.getInstance();
        client.execute(() -> {
            if (!(client.screen instanceof IFilterScreen<?> screen)) return;
            screen.setSlotDisabled(this.index, this.state);
        });
    }
}
