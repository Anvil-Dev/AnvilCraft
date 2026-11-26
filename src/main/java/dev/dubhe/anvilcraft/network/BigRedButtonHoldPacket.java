package dev.dubhe.anvilcraft.network;

import dev.anvilcraft.lib.v2.codec.StreamCodecUtil;
import dev.anvilcraft.lib.v2.network.packet.IPacket;
import dev.anvilcraft.lib.v2.network.packet.ISensitiveBiPacket;
import dev.anvilcraft.lib.v2.util.Util;
import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.block.entity.BigRedButtonBlockEntity;
import dev.dubhe.anvilcraft.block.utility.redstone.BigRedButtonBlock;
import dev.dubhe.anvilcraft.client.event.BigRedButtonInputListener;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jspecify.annotations.Nullable;

public record BigRedButtonHoldPacket(BlockPos pos, Vec3 hitLocation, boolean held, int holdId) implements ISensitiveBiPacket {
    public static final Type<BigRedButtonHoldPacket> TYPE = IPacket.type(AnvilCraft.of("big_red_button_hold"));
    public static final StreamCodec<FriendlyByteBuf, BigRedButtonHoldPacket> STREAM_CODEC = StreamCodec.composite(
        BlockPos.STREAM_CODEC, BigRedButtonHoldPacket::pos,
        StreamCodecUtil.VEC3, BigRedButtonHoldPacket::hitLocation,
        ByteBufCodecs.BOOL, BigRedButtonHoldPacket::held,
        ByteBufCodecs.VAR_INT, BigRedButtonHoldPacket::holdId,
        BigRedButtonHoldPacket::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    @Override
    public void handleOnClient(Player player) {
        BigRedButtonInputListener.handleHoldResult(this.pos, this.holdId, this.held);
    }

    @Override
    public void handleOnServer(Player player) {
        boolean accepted = this.updateButton(player);
        PacketDistributor.sendToPlayer(
            Util.cast(player), new BigRedButtonHoldPacket(this.pos, this.hitLocation, accepted, this.holdId)
        );
    }

    private boolean updateButton(Player player) {
        if (!player.level().hasChunkAt(this.pos)) return false;
        if (!(player.level().getBlockEntity(this.pos) instanceof BigRedButtonBlockEntity button)) return false;
        if (!this.held || !this.canPress(player, button)) {
            button.release(player);
            return false;
        }
        return button.press(player);
    }

    private boolean canPress(Player player, BigRedButtonBlockEntity button) {
        if (!player.isAlive() || player.isSpectator() || button.isBareBase()) {
            return false;
        }
        BlockHitResult hit = visibleHit(player, button, this.hitLocation, button.isCapHeldBy(player));
        var mounted = button.getMountedBlock();
        return hit != null && (mounted == null || !mounted.canAdjustWithHammer(player)
            || !BigRedButtonBlock.isBaseHit(button.getBlockState(), player.level(), hit));
    }

    static @Nullable BlockHitResult visibleHit(Player player, BigRedButtonBlockEntity button, Vec3 location, boolean followCap) {
        Vec3 eye = player.getEyePosition();
        double reach = player.blockInteractionRange() + 1;
        double distance = eye.distanceTo(location);
        if (!Double.isFinite(distance) || distance < 1E-6 || distance > reach) {
            return null;
        }
        Vec3 direction = location.subtract(eye).scale(1 / distance);
        // 按下后帽面会移动，续按沿上次的命中方向重新寻找当前帽面。
        Vec3 end = eye.add(direction.scale(followCap ? reach : distance + 1E-4));
        var level = player.level();
        var pos = button.getBlockPos();
        var shape = button.getBlockState().getShape(level, pos, CollisionContext.of(player));
        BlockHitResult hit = shape.clip(eye, end, pos);
        if (hit == null) {
            return null;
        }
        var obstruction = level.clip(new ClipContext(eye, hit.getLocation(), ClipContext.Block.OUTLINE, ClipContext.Fluid.NONE, player));
        if (obstruction.getType() == HitResult.Type.BLOCK && !obstruction.getBlockPos().equals(pos)
            && eye.distanceToSqr(obstruction.getLocation()) < eye.distanceToSqr(hit.getLocation()) - 1E-6) {
            return null;
        }
        return hit;
    }
}
