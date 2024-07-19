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
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;

@Getter
public class MachineEnableFilterPack implements Packet {
    private final boolean filterEnabled;

    public MachineEnableFilterPack(boolean filterEnabled) {
        this.filterEnabled = filterEnabled;
    }

    public MachineEnableFilterPack(@NotNull FriendlyByteBuf buf) {
        this(buf.readBoolean());
    }

    @Override
    public ResourceLocation getType() {
        return ModNetworks.MATERIAL_PACKET;
    }

    @Override
    public void encode(@NotNull FriendlyByteBuf buf) {
        buf.writeBoolean(this.isFilterEnabled());
    }

    @Override
    public void handler(@NotNull MinecraftServer server, ServerPlayer player) {
        server.execute(() -> {
            if (!player.hasContainerOpen()) return;
            if (!(player.containerMenu instanceof IFilterMenu menu)) return;
            menu.setFilterEnabled(this.isFilterEnabled());
            menu.flush();
            if (!this.isFilterEnabled() && menu.getFilterBlockEntity() != null) {
                for (int i = 0; i < menu.getFilteredItems().size(); i++) {
                    ItemStack stack = menu.getFilteredItems().get(i);
                    if (stack.isEmpty()) continue;
                    new SlotFilterChangePack(i, stack).send(player);
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
            if (client.screen instanceof IFilterScreen<?> screen) {
                screen.setFilterEnabled(this.isFilterEnabled());
                screen.flush();
            }
        });
    }
}
