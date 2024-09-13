package dev.dubhe.anvilcraft.network;

import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.api.IHasDisplayItem;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.network.handling.IPayloadHandler;

import org.jetbrains.annotations.NotNull;

public class UpdateDisplayItemPacket implements CustomPacketPayload {
    public static final Type<UpdateDisplayItemPacket> TYPE = new Type<>(AnvilCraft.of("client_update_display_item"));
    public static final StreamCodec<RegistryFriendlyByteBuf, UpdateDisplayItemPacket> STREAM_CODEC =
            StreamCodec.ofMember(UpdateDisplayItemPacket::encode, UpdateDisplayItemPacket::new);
    public static final IPayloadHandler<UpdateDisplayItemPacket> HANDLER = UpdateDisplayItemPacket::clientHandler;

    private final ItemStack displayItem;
    private final BlockPos pos;

    public UpdateDisplayItemPacket(ItemStack displayItem, BlockPos pos) {
        this.displayItem = displayItem;
        this.pos = pos;
    }

    public UpdateDisplayItemPacket(@NotNull RegistryFriendlyByteBuf buf) {
        this.displayItem = ItemStack.OPTIONAL_STREAM_CODEC.decode(buf);
        this.pos = buf.readBlockPos();
    }

    public void encode(@NotNull RegistryFriendlyByteBuf buf) {
        ItemStack.OPTIONAL_STREAM_CODEC.encode(buf, displayItem);
        buf.writeBlockPos(pos);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void clientHandler(UpdateDisplayItemPacket data, IPayloadContext context) {
        Minecraft mc = Minecraft.getInstance();
        context.enqueueWork(() -> {
            if (mc.level == null) return;
            BlockState state = mc.level.getBlockState(data.pos);
            if (state.isAir()
                    || !state.hasBlockEntity()
                    || mc.level.getBlockEntity(data.pos) instanceof IHasDisplayItem) {
                IHasDisplayItem be = (IHasDisplayItem) mc.level.getBlockEntity(data.pos);
                if (be == null) return; // make idea happy
                be.updateDisplayItem(data.displayItem);
            }
        });
    }
}
