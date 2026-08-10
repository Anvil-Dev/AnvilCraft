package dev.dubhe.anvilcraft.network;

import dev.anvilcraft.lib.v2.network.packet.IPacket;
import dev.anvilcraft.lib.v2.network.packet.IServerboundPacket;
import dev.anvilcraft.lib.v2.util.Util;
import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.util.InfiniteFluidTankBreakProtection;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

public record InfiniteFluidTankBreakModifierPacket(
    BlockPos pos,
    boolean modifiersHeld
) implements IServerboundPacket {
    public static final Type<InfiniteFluidTankBreakModifierPacket> TYPE = IPacket.type(
        AnvilCraft.of("infinite_fluid_tank_break_modifier")
    );
    public static final StreamCodec<ByteBuf, InfiniteFluidTankBreakModifierPacket> STREAM_CODEC = StreamCodec.composite(
        BlockPos.STREAM_CODEC,
        InfiniteFluidTankBreakModifierPacket::pos,
        ByteBufCodecs.BOOL,
        InfiniteFluidTankBreakModifierPacket::modifiersHeld,
        InfiniteFluidTankBreakModifierPacket::new
    );

    @Override
    public Type<InfiniteFluidTankBreakModifierPacket> type() {
        return InfiniteFluidTankBreakModifierPacket.TYPE;
    }

    @Override
    public void handleOnServer(Player player) {
        ServerPlayer serverPlayer = Util.cast(player);
        InfiniteFluidTankBreakProtection.updateModifierAuthorization(
            serverPlayer,
            this.pos,
            this.modifiersHeld
        );
    }
}
