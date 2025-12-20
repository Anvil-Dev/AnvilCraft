package dev.dubhe.anvilcraft.network;

import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.block.entity.IFilterBlockEntity;
import dev.dubhe.anvilcraft.client.gui.screen.IFilterScreen;
import dev.dubhe.anvilcraft.inventory.IFilterMenu;
import net.minecraft.client.Minecraft;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.handling.DirectionalPayloadHandler;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.network.handling.IPayloadHandler;

public class SlotFilterMaxStackSizeChangePacket implements CustomPacketPayload {
    public static final Type<SlotFilterMaxStackSizeChangePacket> TYPE = new Type<>(AnvilCraft.of("slot_filter_max_stack_size_change"));
    public static final StreamCodec<RegistryFriendlyByteBuf, SlotFilterMaxStackSizeChangePacket> STREAM_CODEC =
        StreamCodec.ofMember(SlotFilterMaxStackSizeChangePacket::encode, SlotFilterMaxStackSizeChangePacket::decode);
    public static final IPayloadHandler<SlotFilterMaxStackSizeChangePacket> HANDLER = new DirectionalPayloadHandler<>(
        SlotFilterMaxStackSizeChangePacket::clientHandler, SlotFilterMaxStackSizeChangePacket::serverHandler);

    private final int index;
    private final int maxStackSize;

    /**
     * 更改过滤槽位最大堆叠数
     *
     * @param index        槽位
     * @param maxStackSize 最大堆叠数
     */
    public SlotFilterMaxStackSizeChangePacket(int index, int maxStackSize) {
        this.index = index;
        this.maxStackSize = maxStackSize;
    }

    public static SlotFilterMaxStackSizeChangePacket decode(RegistryFriendlyByteBuf buf) {
        int index = buf.readInt();
        int maxStackSize = buf.readInt();
        return new SlotFilterMaxStackSizeChangePacket(index, maxStackSize);
    }

    public void encode(RegistryFriendlyByteBuf buf) {
        buf.writeInt(this.index);
        buf.writeInt(this.maxStackSize);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void serverHandler(SlotFilterMaxStackSizeChangePacket data, IPayloadContext context) {
        ServerPlayer player = (ServerPlayer) context.player();
        context.enqueueWork(() -> {
            if (!player.hasContainerOpen()) return;
            if (!(player.containerMenu instanceof IFilterMenu menu)) return;
            if (!(menu.getFilterBlockEntity() instanceof IFilterBlockEntity blockEntity)) return;
            blockEntity.setSlotLimit(data.index, data.maxStackSize);
            if (blockEntity instanceof BlockEntity be) {
                be.setChanged();
                be.getLevel().sendBlockUpdated(be.getBlockPos(), be.getBlockState(), be.getBlockState(), 3);
            }
            menu.flush();
            PacketDistributor.sendToPlayer(player, data);
        });
    }

    public static void clientHandler(SlotFilterMaxStackSizeChangePacket data, IPayloadContext context) {
        Minecraft client = Minecraft.getInstance();
        context.enqueueWork(() -> {
            if (!(client.screen instanceof IFilterScreen<?> screen)) return;
            if (!(screen.getFilterMenu().getFilterBlockEntity() instanceof IFilterBlockEntity blockEntity)) return;
            blockEntity.setSlotLimit(data.index, data.maxStackSize);
        });
    }
}