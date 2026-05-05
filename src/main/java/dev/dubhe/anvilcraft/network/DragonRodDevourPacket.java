package dev.dubhe.anvilcraft.network;

import dev.anvilcraft.lib.v2.codec.StreamCodecUtil;
import dev.anvilcraft.lib.v2.network.packet.IPacket;
import dev.anvilcraft.lib.v2.network.packet.IServerboundPacket;
import dev.anvilcraft.lib.v2.util.Util;
import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.item.DragonRodItem;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;

public record DragonRodDevourPacket(InteractionHand hand, BlockPos pos, Direction blockFace) implements IServerboundPacket {
    public static final Type<DragonRodDevourPacket> TYPE = IPacket.type(AnvilCraft.of("dragon_rod_devour"));
    public static final StreamCodec<ByteBuf, DragonRodDevourPacket> STREAM_CODEC = StreamCodec.composite(
        StreamCodecUtil.enumStreamCodec(InteractionHand.class),
        DragonRodDevourPacket::hand,
        BlockPos.STREAM_CODEC,
        DragonRodDevourPacket::pos,
        Direction.STREAM_CODEC,
        DragonRodDevourPacket::blockFace,
        DragonRodDevourPacket::new
    );

    @Override
    public Type<DragonRodDevourPacket> type() {
        return TYPE;
    }

    @Override
    public void handleOnServer(Player player) {
        ServerPlayer serverside = Util.cast(player);
        ServerLevel level = serverside.serverLevel();
        DragonRodItem.devourBlock(
            level,
            player,
            this.hand,
            this.pos,
            level.getBlockState(this.pos),
            this.blockFace
        );
    }
}
