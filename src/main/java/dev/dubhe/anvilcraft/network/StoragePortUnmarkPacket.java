package dev.dubhe.anvilcraft.network;

import dev.anvilcraft.lib.v2.network.packet.IPacket;
import dev.anvilcraft.lib.v2.network.packet.IServerboundPacket;
import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.block.StoragePortBlock;
import dev.dubhe.anvilcraft.block.entity.StoragePortBlockEntity;
import dev.dubhe.anvilcraft.item.AnvilHammerItem;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/**
 * 客户端「铁砧锤长按右键并滑动」手势触发：请求移除仓储端口的标记。
 */
public record StoragePortUnmarkPacket(BlockPos pos) implements IServerboundPacket {
    public static final Type<StoragePortUnmarkPacket> TYPE = IPacket.type(AnvilCraft.of("storage_port_unmark"));
    public static final StreamCodec<ByteBuf, StoragePortUnmarkPacket> STREAM_CODEC = StreamCodec.composite(
        BlockPos.STREAM_CODEC,
        StoragePortUnmarkPacket::pos,
        StoragePortUnmarkPacket::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    @Override
    public void handleOnServer(Player player) {
        if (!(player.getMainHandItem().getItem() instanceof AnvilHammerItem)) {
            return;
        }
        if (player.distanceToSqr(this.pos.getCenter()) > 64.0) {
            return;
        }
        if (!(player.level().getBlockEntity(this.pos) instanceof StoragePortBlockEntity port)) {
            return;
        }
        if (!player.level().getBlockState(this.pos).getValue(StoragePortBlock.MARKED)) {
            return;
        }
        port.setMarkedItem(ItemStack.EMPTY);
    }
}