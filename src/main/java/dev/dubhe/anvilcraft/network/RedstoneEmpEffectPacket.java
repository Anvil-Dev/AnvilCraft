package dev.dubhe.anvilcraft.network;

import dev.anvilcraft.lib.v2.network.packet.IClientboundPacket;
import dev.anvilcraft.lib.v2.network.packet.IPacket;
import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.client.support.AnvilParticleManager;
import io.netty.buffer.ByteBuf;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.entity.player.Player;

import java.util.List;

public record RedstoneEmpEffectPacket(BlockPos centerPos, int radius, List<BlockPos> affectedTorches) implements IClientboundPacket {
    public static final Type<RedstoneEmpEffectPacket> TYPE = IPacket.type(AnvilCraft.of("redstone_emp_effect"));
    public static final StreamCodec<ByteBuf, RedstoneEmpEffectPacket> STREAM_CODEC = StreamCodec.composite(
        BlockPos.STREAM_CODEC,
        RedstoneEmpEffectPacket::centerPos,
        ByteBufCodecs.VAR_INT,
        RedstoneEmpEffectPacket::radius,
        BlockPos.STREAM_CODEC.apply(ByteBufCodecs.list()),
        RedstoneEmpEffectPacket::affectedTorches,
        RedstoneEmpEffectPacket::new
    );

    @Override
    public Type<RedstoneEmpEffectPacket> type() {
        return RedstoneEmpEffectPacket.TYPE;
    }

    @Override
    public void handleOnClient(Player player) {
        if (!AnvilCraft.CLIENT_CONFIG.effects.displayRedstoneEmpParticles) return;
        AnvilParticleManager.redstoneEmp((ClientLevel) player.level(), this.centerPos, this.radius, this.affectedTorches);
    }
}
