package dev.dubhe.anvilcraft.network;

import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.block.entity.ShulkerContainerBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.network.handling.IPayloadHandler;

public record ShulkerContainerClosePacket(BlockPos pos) implements CustomPacketPayload {
    public static final Type<ShulkerContainerClosePacket> TYPE = new Type<>(AnvilCraft.of(
        "shulker_container_close"
    ));
    public static final StreamCodec<FriendlyByteBuf, ShulkerContainerClosePacket> STREAM_CODEC = StreamCodec.composite(
        BlockPos.STREAM_CODEC,
        ShulkerContainerClosePacket::pos,
        ShulkerContainerClosePacket::new
    );
    public static final IPayloadHandler<ShulkerContainerClosePacket> HANDLER =
        ShulkerContainerClosePacket::serverHandler;

    @Override
    public Type<ShulkerContainerClosePacket> type() {
        return TYPE;
    }

    private void serverHandler(IPayloadContext ctx) {
        BlockEntity entity = ctx.player().level().getBlockEntity(this.pos);
        if (!(entity instanceof ShulkerContainerBlockEntity scBE)) return;
        ctx.enqueueWork(scBE::someoneClosedMenu);
    }
}
