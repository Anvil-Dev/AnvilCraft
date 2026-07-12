package dev.dubhe.anvilcraft.network;

import dev.anvilcraft.lib.v2.network.packet.IPacket;
import dev.anvilcraft.lib.v2.network.packet.IServerboundPacket;
import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.event.BlockEventListener;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.entity.player.Player;

public record CreativeCrateAttackPacket(BlockPos pos) implements IServerboundPacket {
    public static final Type<CreativeCrateAttackPacket> TYPE = IPacket.type(
        AnvilCraft.of("creative_crate_attack")
    );
    public static final StreamCodec<ByteBuf, CreativeCrateAttackPacket> STREAM_CODEC = StreamCodec.composite(
        BlockPos.STREAM_CODEC,
        CreativeCrateAttackPacket::pos,
        CreativeCrateAttackPacket::new
    );

    @Override
    public Type<CreativeCrateAttackPacket> type() {
        return TYPE;
    }

    @Override
    public void handleOnServer(Player player) {
        BlockEventListener.clearCreativeCrateAttack(player, this.pos);
    }
}
