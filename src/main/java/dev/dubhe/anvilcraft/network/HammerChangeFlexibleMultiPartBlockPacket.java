package dev.dubhe.anvilcraft.network;

import dev.anvilcraft.lib.util.CodecUtil;
import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.block.multipart.FlexibleMultiPartBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record HammerChangeFlexibleMultiPartBlockPacket(
    BlockPos pos,
    BlockState state,
    Direction direction
) implements CustomPacketPayload {
    public static final Type<HammerChangeFlexibleMultiPartBlockPacket> TYPE =
        new Type<>(AnvilCraft.of("hammer_change_flexible_multi_part_block"));

    public static final StreamCodec<RegistryFriendlyByteBuf, HammerChangeFlexibleMultiPartBlockPacket> STREAM_CODEC =
        StreamCodec.composite(
            BlockPos.STREAM_CODEC,
            HammerChangeFlexibleMultiPartBlockPacket::pos,
            CodecUtil.BLOCK_STATE_STREAM_CODEC,
            HammerChangeFlexibleMultiPartBlockPacket::state,
            Direction.STREAM_CODEC,
            HammerChangeFlexibleMultiPartBlockPacket::direction,
            HammerChangeFlexibleMultiPartBlockPacket::new
        );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public  void handle(IPayloadContext context) {
        context.enqueueWork(() -> {
            Level level = context.player().level();
            if (level.isLoaded(pos)) {
                if (state.getBlock() instanceof FlexibleMultiPartBlock<?, ?, ?> block) {
                    if (direction != null) {
                        block.change(
                            pos,
                            level,
                            blockState -> blockState.setValue(BlockStateProperties.FACING, direction)
                        );
                    }
                }
            }
        });
    }
}
