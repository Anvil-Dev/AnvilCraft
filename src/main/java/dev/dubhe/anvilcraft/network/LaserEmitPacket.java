package dev.dubhe.anvilcraft.network;

import dev.anvilcraft.lib.v2.network.packet.IClientboundPacket;
import dev.anvilcraft.lib.v2.network.packet.IPacket;
import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.block.entity.BaseLaserBlockEntity;
import io.netty.buffer.ByteBuf;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.entity.player.Player;
import org.jspecify.annotations.Nullable;

import java.util.Optional;

public record LaserEmitPacket(int level, BlockPos laserPos, @Nullable BlockPos irradiatePos) implements IClientboundPacket {
    public static final Type<LaserEmitPacket> TYPE = IPacket.type(AnvilCraft.of("laser_emit"));
    public static final StreamCodec<ByteBuf, LaserEmitPacket> STREAM_CODEC = StreamCodec.composite(
        ByteBufCodecs.VAR_INT,
        LaserEmitPacket::level,
        BlockPos.STREAM_CODEC,
        LaserEmitPacket::laserPos,
        ByteBufCodecs.optional(BlockPos.STREAM_CODEC),
        LaserEmitPacket::irradiatePosOptional,
        LaserEmitPacket::new
    );

    @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
    private LaserEmitPacket(int level, BlockPos laserPos, Optional<BlockPos> irradiatePos) {
        this(level, laserPos, irradiatePos.orElse(null));
    }

    private Optional<BlockPos> irradiatePosOptional() {
        return Optional.ofNullable(this.irradiatePos);
    }

    @Override
    public Type<LaserEmitPacket> type() {
        return TYPE;
    }

    @Override
    public void handleOnClient(Player player) {
        if (!(player.level().getBlockEntity(this.laserPos) instanceof BaseLaserBlockEntity laser)) return;
        laser.clientUpdate(this.irradiatePos, this.level);
        Minecraft.getInstance().levelRenderer.setBlockDirty(this.laserPos, false);
    }
}
