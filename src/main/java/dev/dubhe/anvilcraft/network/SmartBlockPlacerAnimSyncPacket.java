package dev.dubhe.anvilcraft.network;

import dev.anvilcraft.lib.v2.network.packet.IClientboundPacket;
import dev.anvilcraft.lib.v2.network.packet.IPacket;
import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.block.entity.SmartBlockPlacerBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/**
 * 智能方块放置器动画状态同步包（S2C）
 * 同步 placeCooldown、currentHeldBlock、currentPlacementIndex、电源状态，
 * 用于客户端渲染机械臂动画。
 */
public record SmartBlockPlacerAnimSyncPacket(
    BlockPos pos,
    int placeCooldown,
    ItemStack currentHeldBlock,
    int currentPlacementIndex,
    boolean isPowered,
    boolean hasRedstoneSignal
) implements IClientboundPacket {
    public static final Type<SmartBlockPlacerAnimSyncPacket> TYPE = IPacket.type(
        AnvilCraft.of("smart_block_placer_anim_sync")
    );

    public static final StreamCodec<RegistryFriendlyByteBuf, SmartBlockPlacerAnimSyncPacket> STREAM_CODEC = StreamCodec.composite(
        BlockPos.STREAM_CODEC,
        SmartBlockPlacerAnimSyncPacket::pos,
        ByteBufCodecs.VAR_INT,
        SmartBlockPlacerAnimSyncPacket::placeCooldown,
        ItemStack.OPTIONAL_STREAM_CODEC,
        SmartBlockPlacerAnimSyncPacket::currentHeldBlock,
        ByteBufCodecs.VAR_INT,
        SmartBlockPlacerAnimSyncPacket::currentPlacementIndex,
        ByteBufCodecs.BOOL,
        SmartBlockPlacerAnimSyncPacket::isPowered,
        ByteBufCodecs.BOOL,
        SmartBlockPlacerAnimSyncPacket::hasRedstoneSignal,
        SmartBlockPlacerAnimSyncPacket::new
    );

    @Override
    public Type<SmartBlockPlacerAnimSyncPacket> type() {
        return TYPE;
    }

    @Override
    public void handleOnClient(Player player) {
        if (player.level().getBlockEntity(this.pos) instanceof SmartBlockPlacerBlockEntity be) {
            be.applyAnimSyncData(this.placeCooldown, this.currentHeldBlock,
                this.currentPlacementIndex, this.isPowered, this.hasRedstoneSignal);
        }
    }
}
