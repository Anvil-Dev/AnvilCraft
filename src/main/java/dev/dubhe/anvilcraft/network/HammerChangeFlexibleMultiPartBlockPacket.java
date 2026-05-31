package dev.dubhe.anvilcraft.network;

import dev.anvilcraft.lib.v2.codec.StreamCodecUtil;
import dev.anvilcraft.lib.v2.network.packet.IPacket;
import dev.anvilcraft.lib.v2.network.packet.IServerboundPacket;
import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.block.multipart.FlexibleMultiPartBlock;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;

public record HammerChangeFlexibleMultiPartBlockPacket(BlockPos pos, BlockState state, Direction direction) implements IServerboundPacket {
    public static final Type<HammerChangeFlexibleMultiPartBlockPacket> TYPE = IPacket.type(AnvilCraft.of(
        "hammer_change_flexible_multi_part_block"
    ));
    public static final StreamCodec<ByteBuf, HammerChangeFlexibleMultiPartBlockPacket> STREAM_CODEC = StreamCodec.composite(
        BlockPos.STREAM_CODEC,
        HammerChangeFlexibleMultiPartBlockPacket::pos,
        StreamCodecUtil.BLOCK_STATE,
        HammerChangeFlexibleMultiPartBlockPacket::state,
        Direction.STREAM_CODEC,
        HammerChangeFlexibleMultiPartBlockPacket::direction,
        HammerChangeFlexibleMultiPartBlockPacket::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    @Override
    public void handleOnServer(Player player) {
        Level level = player.level();
        if (!level.isLoaded(this.pos)) {
            return;
        }
        if (!(this.state.getBlock() instanceof FlexibleMultiPartBlock<?, ?, ?> block)) return;
        if (this.direction == null) return;
        block.change(
            this.pos,
            level,
            state -> state.setValue(BlockStateProperties.FACING, this.direction)
        );
    }
}
