package dev.dubhe.anvilcraft.network;

import dev.anvilcraft.lib.v2.network.packet.IClientboundPacket;
import dev.anvilcraft.lib.v2.network.packet.IPacket;
import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.client.support.MagnetAnvilAnimation;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

public record MagnetAnvilAnimationPacket(Vec3 start, BlockPos end, BlockState state) implements IClientboundPacket {
    public static final Type<MagnetAnvilAnimationPacket> TYPE = IPacket.type(AnvilCraft.of("magnet_anvil_animation"));
    public static final StreamCodec<FriendlyByteBuf, MagnetAnvilAnimationPacket> STREAM_CODEC = StreamCodec.composite(
        Vec3.STREAM_CODEC, MagnetAnvilAnimationPacket::start,
        BlockPos.STREAM_CODEC, MagnetAnvilAnimationPacket::end,
        ByteBufCodecs.idMapper(Block.BLOCK_STATE_REGISTRY), MagnetAnvilAnimationPacket::state,
        MagnetAnvilAnimationPacket::new
    );

    @Override
    public Type<MagnetAnvilAnimationPacket> type() {
        return TYPE;
    }

    @Override
    public void handleOnClient(Player player) {
        MagnetAnvilAnimation.start(this.start, this.end, this.state);
    }
}
