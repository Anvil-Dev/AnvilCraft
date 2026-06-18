package dev.dubhe.anvilcraft.network;

import dev.anvilcraft.lib.v2.network.packet.IClientboundPacket;
import dev.anvilcraft.lib.v2.network.packet.IPacket;
import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.block.entity.BaseLaserBlockEntity;
import dev.dubhe.anvilcraft.block.entity.CelestialForgingAnvilLaserInterfaceBlockEntity;
import io.netty.buffer.ByteBuf;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.entity.player.Player;

import java.util.Optional;
import javax.annotation.Nullable;

public record LaserEmitPacket(int level, BlockPos laserPos, @Nullable BlockPos irradiatePos, boolean gamma) implements IClientboundPacket {
    public static final Type<LaserEmitPacket> TYPE = IPacket.type(AnvilCraft.of("laser_emit"));
    public static final StreamCodec<ByteBuf, LaserEmitPacket> STREAM_CODEC = StreamCodec.composite(
        ByteBufCodecs.VAR_INT,
        LaserEmitPacket::level,
        BlockPos.STREAM_CODEC,
        LaserEmitPacket::laserPos,
        ByteBufCodecs.optional(BlockPos.STREAM_CODEC),
        LaserEmitPacket::irradiatePosOptional,
        ByteBufCodecs.BOOL,
        LaserEmitPacket::gamma,
        LaserEmitPacket::new
    );

    @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
    private LaserEmitPacket(int level, BlockPos laserPos, Optional<BlockPos> irradiatePos, boolean gamma) {
        this(level, laserPos, irradiatePos.orElse(null), gamma);
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
        if (laser instanceof CelestialForgingAnvilLaserInterfaceBlockEntity cfaLaser && this.gamma) {
            cfaLaser.clientUpdateGamma(this.irradiatePos, this.level);
        } else {
            laser.clientUpdate(this.irradiatePos, this.level); // 此处不可听信idea的谗言用if(this.irradiatePos != null)包围，不然激光会不消失
        }
        Minecraft.getInstance().levelRenderer.setBlockDirty(this.laserPos, false);
    }
}
