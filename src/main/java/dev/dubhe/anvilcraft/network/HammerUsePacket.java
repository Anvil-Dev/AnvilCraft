package dev.dubhe.anvilcraft.network;

import dev.anvilcraft.lib.v2.codec.StreamCodecUtil;
import dev.anvilcraft.lib.v2.network.packet.IPacket;
import dev.anvilcraft.lib.v2.network.packet.IServerboundPacket;
import dev.anvilcraft.lib.v2.util.Util;
import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.item.AnvilHammerItem;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.BlockHitResult;

public record HammerUsePacket(BlockPos pos, InteractionHand hand, BlockHitResult result) implements IServerboundPacket {
    public static final Type<HammerUsePacket> TYPE = IPacket.type(AnvilCraft.of("hammer_use"));
    public static final StreamCodec<RegistryFriendlyByteBuf, HammerUsePacket> STREAM_CODEC = StreamCodec.composite(
        BlockPos.STREAM_CODEC,
        HammerUsePacket::pos,
        StreamCodecUtil.enumStreamCodec(InteractionHand.class),
        HammerUsePacket::hand,
        StreamCodec.of(FriendlyByteBuf::writeBlockHitResult, FriendlyByteBuf::readBlockHitResult),
        HammerUsePacket::result,
        HammerUsePacket::new
    );

    @Override
    public Type<HammerUsePacket> type() {
        return TYPE;
    }

    @Override
    public void handleOnServer(Player player) {
        ServerPlayer serverside = Util.cast(player);
        ItemStack itemInHand = serverside.getItemInHand(this.hand);
        AnvilHammerItem.useBlock(serverside, this.pos, serverside.serverLevel(), itemInHand, this.hand, this.result);
    }
}
