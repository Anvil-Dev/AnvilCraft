package dev.dubhe.anvilcraft.network;

import dev.anvilcraft.lib.network.Packet;
import dev.dubhe.anvilcraft.client.gui.screen.inventory.IFilterScreen;
import dev.dubhe.anvilcraft.init.ModNetworks;
import dev.dubhe.anvilcraft.inventory.IFilterMenu;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;

public class SlotFilterChangePack implements Packet {
    private final int index;
    private final ItemStack filter;

    /**
     * 更改过滤
     *
     * @param index  槽位
     * @param filter 过滤
     */
    public SlotFilterChangePack(int index, @NotNull ItemStack filter) {
        this.index = index;
        this.filter = filter.copy();
        this.filter.setCount(1);
    }

    public SlotFilterChangePack(@NotNull FriendlyByteBuf buf) {
        this(buf.readInt(), buf.readItem());
    }

    @Override
    public void encode(@NotNull FriendlyByteBuf buf) {
        buf.writeInt(this.index);
        buf.writeItem(this.filter);
    }

    @Override
    public ResourceLocation getType() {
        return ModNetworks.SLOT_FILTER_CHANGE_PACKET;
    }

    @Override
    public void handler(@NotNull MinecraftServer server, ServerPlayer player) {
        server.execute(() -> {
            if (!player.hasContainerOpen()) return;
            if (!(player.containerMenu instanceof IFilterMenu menu)) return;
            menu.setFilter(this.index, this.filter);
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
            screen.setFilter(this.index, this.filter);
        });
    }
}
