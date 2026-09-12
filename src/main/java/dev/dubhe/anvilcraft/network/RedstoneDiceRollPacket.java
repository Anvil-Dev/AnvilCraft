package dev.dubhe.anvilcraft.network;

import dev.anvilcraft.lib.v2.codec.StreamCodecUtil;
import dev.anvilcraft.lib.v2.network.packet.IPacket;
import dev.anvilcraft.lib.v2.network.packet.IServerboundPacket;
import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.block.entity.RedstoneDiceBlockEntity;
import dev.dubhe.anvilcraft.item.AnvilHammerItem;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;

public record RedstoneDiceRollPacket(BlockPos pos, InteractionHand hand) implements IServerboundPacket {
    public static final Type<RedstoneDiceRollPacket> TYPE = IPacket.type(AnvilCraft.of("redstone_dice_roll"));
    public static final StreamCodec<FriendlyByteBuf, RedstoneDiceRollPacket> STREAM_CODEC = StreamCodec.composite(
        BlockPos.STREAM_CODEC, RedstoneDiceRollPacket::pos,
        StreamCodecUtil.enumStreamCodec(InteractionHand.class), RedstoneDiceRollPacket::hand,
        RedstoneDiceRollPacket::new
    );

    @Override
    public Type<RedstoneDiceRollPacket> type() {
        return TYPE;
    }

    @Override
    public void handleOnServer(Player player) {
        Level level = player.level();
        if (!level.hasChunkAt(this.pos) || !player.isAlive() || player.isSpectator()
            || !player.getAbilities().mayBuild || !player.canInteractWithBlock(this.pos, 0)
            || !level.mayInteract(player, this.pos)) return;
        if (!(player.getItemInHand(this.hand).getItem() instanceof AnvilHammerItem)) return;
        if (!(level.getBlockEntity(this.pos) instanceof RedstoneDiceBlockEntity dice)) return;
        BlockHitResult hit = level.clip(new ClipContext(
            player.getEyePosition(), this.pos.getCenter(), ClipContext.Block.OUTLINE, ClipContext.Fluid.NONE, player
        ));
        if (hit.getType() != HitResult.Type.BLOCK || !hit.getBlockPos().equals(this.pos)) return;
        dice.roll();
    }
}
