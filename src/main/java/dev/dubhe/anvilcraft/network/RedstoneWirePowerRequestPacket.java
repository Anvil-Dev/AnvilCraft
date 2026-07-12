package dev.dubhe.anvilcraft.network;

import dev.anvilcraft.lib.v2.network.packet.IPacket;
import dev.anvilcraft.lib.v2.network.packet.IServerboundPacket;
import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.block.RedstoneWireBlock;
import dev.dubhe.anvilcraft.block.RedstoneWireNetworkManager;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.Map;
import java.util.WeakHashMap;

public record RedstoneWirePowerRequestPacket(BlockPos pos) implements IServerboundPacket {
    private static final Map<ServerPlayer, Long> LAST_REQUEST = new WeakHashMap<>();
    public static final Type<RedstoneWirePowerRequestPacket> TYPE = IPacket.type(
        AnvilCraft.of("redstone_wire_power_request")
    );
    public static final StreamCodec<ByteBuf, RedstoneWirePowerRequestPacket> STREAM_CODEC = StreamCodec.composite(
        BlockPos.STREAM_CODEC,
        RedstoneWirePowerRequestPacket::pos,
        RedstoneWirePowerRequestPacket::new
    );

    @Override
    public Type<RedstoneWirePowerRequestPacket> type() {
        return TYPE;
    }

    @Override
    public void handleOnServer(Player player) {
        if (!(player instanceof ServerPlayer serverPlayer)
            || !serverPlayer.level().isLoaded(this.pos)
            || serverPlayer.blockPosition().distSqr(this.pos) > 4096
            || !(serverPlayer.level().getBlockState(this.pos).getBlock() instanceof RedstoneWireBlock)) {
            return;
        }
        long gameTime = serverPlayer.level().getGameTime();
        Long lastRequest = LAST_REQUEST.put(serverPlayer, gameTime);
        if (lastRequest != null && lastRequest == gameTime) {
            return;
        }
        PacketDistributor.sendToPlayer(
            serverPlayer,
            new RedstoneWirePowerResponsePacket(
                this.pos, RedstoneWireNetworkManager.getNonDustPower(serverPlayer.serverLevel(), this.pos)
            )
        );
    }
}
