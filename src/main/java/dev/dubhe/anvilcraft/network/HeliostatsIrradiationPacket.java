package dev.dubhe.anvilcraft.network;

import dev.anvilcraft.lib.v2.network.packet.IPacket;
import dev.anvilcraft.lib.v2.network.packet.ISensitiveBiPacket;
import dev.anvilcraft.lib.v2.util.Util;
import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.block.entity.HeliostatsBlockEntity;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.Optional;
import javax.annotation.Nullable;

public record HeliostatsIrradiationPacket(BlockPos pos, @Nullable BlockPos irritatePos) implements ISensitiveBiPacket {
    public static final Type<HeliostatsIrradiationPacket> TYPE = IPacket.type(AnvilCraft.of("heliostats_irradiation_pack"));
    public static final StreamCodec<ByteBuf, HeliostatsIrradiationPacket> STREAM_CODEC = StreamCodec.composite(
        BlockPos.STREAM_CODEC,
        HeliostatsIrradiationPacket::pos,
        ByteBufCodecs.optional(BlockPos.STREAM_CODEC),
        HeliostatsIrradiationPacket::irritatePosOptional,
        HeliostatsIrradiationPacket::new
    );

    @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
    private HeliostatsIrradiationPacket(BlockPos pos, Optional<BlockPos> irritatePos) {
        this(pos, irritatePos.orElse(null));
    }

    private Optional<BlockPos> irritatePosOptional() {
        return Optional.ofNullable(this.irritatePos);
    }

    @Override
    public Type<HeliostatsIrradiationPacket> type() {
        return TYPE;
    }

    @Override
    public void handleOnClient(Player player) {
        if (!(player.level().getBlockEntity(this.pos) instanceof HeliostatsBlockEntity heliostats)) return;
        heliostats.setIrritatePos(this.irritatePos);
    }

    @Override
    public void handleOnServer(Player player) {
        if (!player.level().isLoaded(this.pos)) {
            return;
        }
        if (!(player.level().getBlockEntity(this.pos) instanceof HeliostatsBlockEntity heliostats)) return;
        PacketDistributor.sendToPlayer(
            Util.cast(player),
            new HeliostatsIrradiationPacket(this.pos, heliostats.getIrritatePos())
        );
    }
}
