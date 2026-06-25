package dev.dubhe.anvilcraft.network;

import dev.anvilcraft.lib.v2.network.packet.IClientboundPacket;
import dev.anvilcraft.lib.v2.network.packet.IPacket;
import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.block.entity.SmartBlockPlacerBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.entity.player.Player;

/**
 * 智能方块放置器结构/预览数据同步包（S2C）
 * 同步结构数据、层数据、模式设置，用于客户端UI预览。
 */
public record SmartBlockPlacerDataSyncPacket(BlockPos pos, CompoundTag data) implements IClientboundPacket {
    public static final Type<SmartBlockPlacerDataSyncPacket> TYPE = IPacket.type(
        AnvilCraft.of("smart_block_placer_data_sync")
    );

    public static final StreamCodec<RegistryFriendlyByteBuf, SmartBlockPlacerDataSyncPacket> STREAM_CODEC = StreamCodec.composite(
        BlockPos.STREAM_CODEC,
        SmartBlockPlacerDataSyncPacket::pos,
        ByteBufCodecs.COMPOUND_TAG,
        SmartBlockPlacerDataSyncPacket::data,
        SmartBlockPlacerDataSyncPacket::new
    );

    @Override
    public Type<SmartBlockPlacerDataSyncPacket> type() {
        return TYPE;
    }

    @Override
    public void handleOnClient(Player player) {
        if (player.level().getBlockEntity(this.pos) instanceof SmartBlockPlacerBlockEntity be) {
            be.applyDataSyncFromPacket(this.data);
        }
    }
}
