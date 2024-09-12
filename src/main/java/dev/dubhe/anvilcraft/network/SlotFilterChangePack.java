package dev.dubhe.anvilcraft.network;

import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.client.gui.screen.inventory.IFilterScreen;
import dev.dubhe.anvilcraft.inventory.IFilterMenu;

import net.minecraft.client.Minecraft;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.handling.DirectionalPayloadHandler;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.network.handling.IPayloadHandler;

import org.jetbrains.annotations.NotNull;

public class SlotFilterChangePack implements CustomPacketPayload {
    public static final Type<SlotFilterChangePack> TYPE =
            new Type<>(AnvilCraft.of("slot_filter_change"));
    public static final StreamCodec<RegistryFriendlyByteBuf, SlotFilterChangePack> STREAM_CODEC =
            StreamCodec.ofMember(SlotFilterChangePack::encode, SlotFilterChangePack::new);
    public static final IPayloadHandler<SlotFilterChangePack> HANDLER =
            new DirectionalPayloadHandler<>(
                    SlotFilterChangePack::clientHandler, SlotFilterChangePack::serverHandler);

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

    public SlotFilterChangePack(@NotNull RegistryFriendlyByteBuf buf) {
        this(buf.readInt(), ItemStack.OPTIONAL_STREAM_CODEC.decode(buf));
    }

    public void encode(@NotNull RegistryFriendlyByteBuf buf) {
        buf.writeInt(this.index);
        ItemStack.OPTIONAL_STREAM_CODEC.encode(buf, this.filter);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void serverHandler(SlotFilterChangePack data, IPayloadContext context) {
        ServerPlayer player = (ServerPlayer) context.player();
        context.enqueueWork(() -> {
            if (!player.hasContainerOpen()) return;
            if (!(player.containerMenu instanceof IFilterMenu menu)) return;
            menu.setFilter(data.index, data.filter);
            menu.flush();
            PacketDistributor.sendToPlayer(player, data);
        });
    }

    public static void clientHandler(SlotFilterChangePack data, IPayloadContext context) {
        Minecraft client = Minecraft.getInstance();
        context.enqueueWork(() -> {
            if (!(client.screen instanceof IFilterScreen<?> screen)) return;
            screen.setFilter(data.index, data.filter);
        });
    }
}
