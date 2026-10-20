package dev.dubhe.anvilcraft.network;

import dev.anvilcraft.lib.v2.codec.StreamCodecUtil;
import dev.anvilcraft.lib.v2.network.packet.IPacket;
import dev.anvilcraft.lib.v2.network.packet.IServerboundPacket;
import dev.anvilcraft.lib.v2.util.Util;
import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.init.block.ModBlockTags;
import dev.dubhe.anvilcraft.init.item.ModItemTags;
import dev.dubhe.anvilcraft.item.AnvilHammerItem;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

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
        if (!serverside.isAlive() || serverside.isSpectator() || !serverside.serverLevel().isLoaded(this.pos)
            || !serverside.canInteractWithBlock(this.pos, 1.0) || !this.result.getBlockPos().equals(this.pos)) {
            return;
        }
        ItemStack itemInHand = serverside.getItemInHand(this.hand);
        if (!itemInHand.is(ModItemTags.ANVIL_HAMMER)) return;
        Vec3 distance = this.result.getLocation().subtract(Vec3.atCenterOf(this.pos));
        if (!(Math.abs(distance.x()) < 1.0000001 && Math.abs(distance.y()) < 1.0000001 && Math.abs(distance.z()) < 1.0000001)) {
            return;
        }
        BlockState state = serverside.serverLevel().getBlockState(this.pos);
        if (!AnvilHammerItem.canRocketJump(serverside)
            && !AnvilHammerItem.ableToUseAnvilHammer(serverside.serverLevel(), this.pos, serverside)
            && (state.is(BlockTags.CAULDRONS) || state.is(ModBlockTags.ANVIL_HAMMER_DENYLIST))) return;
        AnvilHammerItem.useBlock(serverside, this.pos, serverside.serverLevel(), itemInHand, this.hand, this.result);
    }
}
