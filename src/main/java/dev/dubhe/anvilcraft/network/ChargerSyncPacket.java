package dev.dubhe.anvilcraft.network;

import dev.anvilcraft.lib.v2.network.packet.IClientboundPacket;
import dev.anvilcraft.lib.v2.network.packet.IPacket;
import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.block.entity.ChargerBlockEntity;
import dev.dubhe.anvilcraft.block.entity.DischargerBlockEntity;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.entity.player.Player;

public record ChargerSyncPacket(BlockPos pos, int timeLeft, int timeTotal, boolean feCharging) implements IClientboundPacket {
    public static final Type<ChargerSyncPacket> TYPE = IPacket.type(AnvilCraft.of("charger_sync"));
    public static final StreamCodec<ByteBuf, ChargerSyncPacket> STREAM_CODEC = StreamCodec.composite(
        BlockPos.STREAM_CODEC,
        ChargerSyncPacket::pos,
        ByteBufCodecs.VAR_INT,
        ChargerSyncPacket::timeLeft,
        ByteBufCodecs.VAR_INT,
        ChargerSyncPacket::timeTotal,
        ByteBufCodecs.BOOL,
        ChargerSyncPacket::feCharging,
        ChargerSyncPacket::new
    );

    @Override
    public Type<ChargerSyncPacket> type() {
        return TYPE;
    }

    @Override
    public void handleOnClient(Player player) {
        if (player.level().getBlockEntity(this.pos) instanceof ChargerBlockEntity charger) {
            charger.setTimeLeft(this.timeLeft);
            charger.setTimeTotalCache(this.timeTotal);
            charger.setFeCharging(this.feCharging);
        } else if (player.level().getBlockEntity(this.pos) instanceof DischargerBlockEntity discharger) {
            discharger.setTimeLeft(this.timeLeft);
            discharger.setTimeTotalCache(this.timeTotal);
            discharger.setFeDischarging(this.feCharging);
        }
    }
}
