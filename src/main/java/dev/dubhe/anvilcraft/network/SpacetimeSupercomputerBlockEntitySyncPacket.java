package dev.dubhe.anvilcraft.network;

import dev.anvilcraft.lib.v2.network.packet.IClientboundPacket;
import dev.anvilcraft.lib.v2.network.packet.IPacket;
import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.block.entity.SpacetimeSupercomputerBlockEntity;
import dev.dubhe.anvilcraft.client.gui.screen.SpacetimeSupercomputerScreen;
import io.netty.buffer.ByteBuf;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

public record SpacetimeSupercomputerBlockEntitySyncPacket(BlockPos pos) implements IClientboundPacket {
    private static final Type<SpacetimeSupercomputerBlockEntitySyncPacket> TYPE =
        IPacket.type(AnvilCraft.of("spacetime_supercomputer_block_entity"));
    public static final StreamCodec<ByteBuf, SpacetimeSupercomputerBlockEntitySyncPacket> STREAM_CODEC = StreamCodec.composite(
        BlockPos.STREAM_CODEC,
        SpacetimeSupercomputerBlockEntitySyncPacket::pos,
        SpacetimeSupercomputerBlockEntitySyncPacket::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return SpacetimeSupercomputerBlockEntitySyncPacket.TYPE;
    }

    @Override
    public void handleOnClient(Player player) {
        Level level = player.level();
        BlockEntity blockEntity = level.getBlockEntity(this.pos);
        if (blockEntity instanceof SpacetimeSupercomputerBlockEntity) {
            this.updateScreenIfOpen();
        }
    }

    private void updateScreenIfOpen() {
        var mc = Minecraft.getInstance();
        if (mc.screen instanceof SpacetimeSupercomputerScreen screen) {
            screen.updateGui();
        }
    }
}
