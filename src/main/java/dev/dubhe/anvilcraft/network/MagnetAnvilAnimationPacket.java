package dev.dubhe.anvilcraft.network;

import dev.anvilcraft.lib.v2.network.packet.IClientboundPacket;
import dev.anvilcraft.lib.v2.network.packet.IPacket;
import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.client.support.MagnetAnvilAnimation;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

public record MagnetAnvilAnimationPacket(Vec3 start, BlockPos end, BlockState state, boolean requiresMagnet)
    implements IClientboundPacket {
    public static final Type<MagnetAnvilAnimationPacket> TYPE = IPacket.type(AnvilCraft.of("magnet_anvil_animation"));
    public static final StreamCodec<ByteBuf, MagnetAnvilAnimationPacket> STREAM_CODEC = StreamCodec.composite(
        ByteBufCodecs.DOUBLE,
        packet -> packet.start().x(),
        ByteBufCodecs.DOUBLE,
        packet -> packet.start().y(),
        ByteBufCodecs.DOUBLE,
        packet -> packet.start().z(),
        BlockPos.STREAM_CODEC,
        MagnetAnvilAnimationPacket::end,
        ByteBufCodecs.idMapper(Block.BLOCK_STATE_REGISTRY),
        MagnetAnvilAnimationPacket::state,
        ByteBufCodecs.BOOL,
        MagnetAnvilAnimationPacket::requiresMagnet,
        (x, y, z, end, state, requiresMagnet) -> new MagnetAnvilAnimationPacket(new Vec3(x, y, z), end, state, requiresMagnet)
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return MagnetAnvilAnimationPacket.TYPE;
    }

    @Override
    public void handleOnClient(Player player) {
        MagnetAnvilAnimation.start(this.start, this.end, this.state, this.requiresMagnet);
    }
}
