package dev.dubhe.anvilcraft.network;

import dev.anvilcraft.lib.v2.network.packet.IPacket;
import dev.anvilcraft.lib.v2.network.packet.IServerboundPacket;
import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.block.BigRedButtonBlock;
import dev.dubhe.anvilcraft.block.entity.BigRedButtonBlockEntity;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

public record BigRedButtonHoldPacket(BlockPos pos, boolean held) implements IServerboundPacket {
    public static final Type<BigRedButtonHoldPacket> TYPE = IPacket.type(AnvilCraft.of("big_red_button_hold"));
    public static final StreamCodec<ByteBuf, BigRedButtonHoldPacket> STREAM_CODEC = StreamCodec.composite(
        BlockPos.STREAM_CODEC, BigRedButtonHoldPacket::pos,
        ByteBufCodecs.BOOL, BigRedButtonHoldPacket::held,
        BigRedButtonHoldPacket::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    @Override
    public void handleOnServer(Player player) {
        if (!player.level().hasChunkAt(this.pos)) return;
        if (!(player.level().getBlockEntity(this.pos) instanceof BigRedButtonBlockEntity button)) return;
        if (!this.held) {
            button.release(player);
            return;
        }
        if (!player.isAlive() || player.isSpectator() || !player.canInteractWithBlock(this.pos, 1.0)) return;
        Vec3 target = this.pos.getCenter().relative(button.getBlockState().getValue(BigRedButtonBlock.FACING), -0.375);
        HitResult hit = player.level().clip(new ClipContext(
            player.getEyePosition(), target, ClipContext.Block.OUTLINE, ClipContext.Fluid.NONE, player
        ));
        if (!(hit instanceof BlockHitResult blockHit) || hit.getType() != HitResult.Type.BLOCK
            || !blockHit.getBlockPos().equals(this.pos)) return;
        button.press(player);
    }
}
