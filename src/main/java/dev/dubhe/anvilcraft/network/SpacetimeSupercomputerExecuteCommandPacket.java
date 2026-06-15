package dev.dubhe.anvilcraft.network;

import dev.anvilcraft.lib.v2.network.packet.IPacket;
import dev.anvilcraft.lib.v2.network.packet.IServerboundPacket;
import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.block.entity.SpacetimeSupercomputerBlockEntity;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

public record SpacetimeSupercomputerExecuteCommandPacket(BlockPos pos, String command, boolean run) implements IServerboundPacket {
    private static final Type<SpacetimeSupercomputerExecuteCommandPacket> TYPE =
        IPacket.type(AnvilCraft.of("save_spacetime_supercomputer_command"));
    public static final StreamCodec<ByteBuf, SpacetimeSupercomputerExecuteCommandPacket> STREAM_CODEC = StreamCodec.composite(
        BlockPos.STREAM_CODEC,
        SpacetimeSupercomputerExecuteCommandPacket::pos,
        ByteBufCodecs.STRING_UTF8,
        SpacetimeSupercomputerExecuteCommandPacket::command,
        ByteBufCodecs.BOOL,
        SpacetimeSupercomputerExecuteCommandPacket::run,
        SpacetimeSupercomputerExecuteCommandPacket::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    @Override
    public void handleOnServer(Player player) {
        Level level = player.level();
        BlockEntity blockEntity = level.getBlockEntity(this.pos);
        if (blockEntity instanceof SpacetimeSupercomputerBlockEntity spacetimeSupercomputerBlockEntity) {
            spacetimeSupercomputerBlockEntity.setCommand(this.command);
            if (this.run) {
                spacetimeSupercomputerBlockEntity.runCommand(player);
            }
            spacetimeSupercomputerBlockEntity.onChange();
        }
    }
}
