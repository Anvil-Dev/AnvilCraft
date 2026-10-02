package dev.dubhe.anvilcraft.network;

import dev.anvilcraft.lib.v2.codec.StreamCodecUtil;
import dev.anvilcraft.lib.v2.network.packet.IPacket;
import dev.anvilcraft.lib.v2.network.packet.ISensitiveBiPacket;
import dev.anvilcraft.lib.v2.util.Util;
import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.block.entity.BigRedButtonBlockEntity;
import dev.dubhe.anvilcraft.client.event.BigRedButtonInputListener;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.PacketDistributor;

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
        if (!this.held || !this.canPress(player)) {
            button.release(player);
            return false;
        }
        return button.press(player);
    }

    private boolean canPress(Player player) {
        if (!player.isAlive() || player.isSpectator() || !player.canInteractWithBlock(this.pos, 1.0)) return false;
        if (!Double.isFinite(this.hitLocation.x) || !Double.isFinite(this.hitLocation.y) || !Double.isFinite(this.hitLocation.z)
            || !new AABB(this.pos).inflate(1.0E-5).contains(this.hitLocation)) return false;
        Vec3 eye = player.getEyePosition();
        Vec3 target = this.hitLocation.add(this.hitLocation.subtract(eye).normalize().scale(1.0E-4));
        BlockHitResult hit = player.level().clip(new ClipContext(
            eye, target, ClipContext.Block.OUTLINE, ClipContext.Fluid.NONE, player
        ));
        return hit.getType() == HitResult.Type.BLOCK && hit.getBlockPos().equals(this.pos);
    }
}
