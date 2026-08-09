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

/**
 * 客户端请求某根自定义红石导线当前非红石粉输入强度的数据包。
 *
 * @param pos 要查询的导线位置
 */
public record RedstoneWirePowerRequestPacket(BlockPos pos) implements IServerboundPacket {
    /** 每名玩家上次成功处理请求的游戏时间，用于限制同 tick 重复请求。 */
    // 使用弱键让离线玩家可被回收，避免为这一轻量限频状态额外接入玩家退出事件。
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
        return RedstoneWirePowerRequestPacket.TYPE;
    }

    @Override
    public void handleOnServer(Player player) {
        if (!(player instanceof ServerPlayer serverPlayer)
            || !serverPlayer.level().isLoaded(this.pos)
            || serverPlayer.blockPosition().distSqr(this.pos) > 4096
            || !(serverPlayer.level().getBlockState(this.pos).getBlock() instanceof RedstoneWireBlock)) {
            // 客户端输入不可信；拒绝未加载、超出合理观察距离或已经不再是导线的位置，避免强制加载和任意查询。
            return;
        }
        long gameTime = serverPlayer.level().getGameTime();
        Long lastRequest = RedstoneWirePowerRequestPacket.LAST_REQUEST.put(serverPlayer, gameTime);
        if (lastRequest != null && lastRequest == gameTime) {
            // 客户端本地按位置限频，服务端再按玩家限频，防止修改客户端在同一 tick 批量探测网络。
            return;
        }
        PacketDistributor.sendToPlayer(
            serverPlayer,
            new RedstoneWirePowerResponsePacket(
                this.pos,
                RedstoneWireNetworkManager.getPower(serverPlayer.level(), this.pos),
                RedstoneWireNetworkManager.getNonDustPower(serverPlayer.level(), this.pos)
            )
        );
    }
}
