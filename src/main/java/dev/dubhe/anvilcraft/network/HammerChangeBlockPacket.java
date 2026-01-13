package dev.dubhe.anvilcraft.network;

import dev.anvilcraft.lib.recipe.util.CodecUtil;
import dev.dubhe.anvilcraft.AnvilCraft;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record HammerChangeBlockPacket(
    BlockPos pos,
    BlockState state
) implements CustomPacketPayload {
    public static final Type<HammerChangeBlockPacket> TYPE = new Type<>(AnvilCraft.of("hammer_change_block"));

    public static final StreamCodec<RegistryFriendlyByteBuf, HammerChangeBlockPacket> STREAM_CODEC =
        StreamCodec.composite(
            BlockPos.STREAM_CODEC,
            HammerChangeBlockPacket::pos,
            CodecUtil.BLOCK_STATE_STREAM_CODEC,
            HammerChangeBlockPacket::state,
            HammerChangeBlockPacket::new
        );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public  void handle(IPayloadContext context) {
        context.enqueueWork(() -> {
            Level level = context.player().level();
            if (level.isLoaded(pos)) {
                level.setBlock(pos, state, Block.UPDATE_ALL_IMMEDIATE);
            }
        });
    }
}
