package dev.dubhe.anvilcraft.network;

import dev.anvilcraft.lib.v2.network.packet.IPacket;
import dev.anvilcraft.lib.v2.network.packet.IServerboundPacket;
import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.block.entity.StoragePortBlockEntity;
import dev.dubhe.anvilcraft.item.AnvilHammerItem;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.entity.player.Player;

/**
 * 客户端左键仓储端口时发送：请求服务端取出 1 个（shift 时取出一组）。
 *
 * <p>取出完全在服务端执行（缓存与玩家背包均为权威数据），客户端不直接改动任何物品，
 * 避免幻影物品与快速点击刷物品。</p>
 */
public record StoragePortTakeOutPacket(BlockPos pos, boolean fullStack) implements IServerboundPacket {
    public static final Type<StoragePortTakeOutPacket> TYPE = IPacket.type(
        AnvilCraft.of("storage_port_take_out")
    );
    public static final StreamCodec<ByteBuf, StoragePortTakeOutPacket> STREAM_CODEC = StreamCodec.composite(
        BlockPos.STREAM_CODEC,
        StoragePortTakeOutPacket::pos,
        ByteBufCodecs.BOOL,
        StoragePortTakeOutPacket::fullStack,
        StoragePortTakeOutPacket::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    @Override
    public void handleOnServer(Player player) {
        if (player.getMainHandItem().getItem() instanceof AnvilHammerItem) {
            return;
        }
        if (player.distanceToSqr(this.pos.getCenter()) > 64.0) {
            return;
        }
        if (!(player.level().getBlockEntity(this.pos) instanceof StoragePortBlockEntity port)) {
            return;
        }
        port.giveToPlayer(player, this.fullStack);
    }
}
