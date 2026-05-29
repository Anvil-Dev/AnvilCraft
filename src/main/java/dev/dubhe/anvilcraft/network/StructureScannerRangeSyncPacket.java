package dev.dubhe.anvilcraft.network;

import dev.anvilcraft.lib.v2.network.packet.IClientboundPacket;
import dev.anvilcraft.lib.v2.network.packet.IPacket;
import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.block.entity.StructureScannerBlockEntity;
import dev.dubhe.anvilcraft.inventory.StructureScannerMenu;
import io.netty.buffer.ByteBuf;
import net.minecraft.client.Minecraft;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.entity.player.Player;

public record StructureScannerRangeSyncPacket(int rangeX, int rangeY, int rangeZ) implements IClientboundPacket {
    public static final Type<StructureScannerRangeSyncPacket> TYPE = IPacket.type(
        AnvilCraft.of("structure_scanner_range_sync")
    );
    public static final StreamCodec<ByteBuf, StructureScannerRangeSyncPacket> STREAM_CODEC = StreamCodec.composite(
        ByteBufCodecs.INT,
        StructureScannerRangeSyncPacket::rangeX,
        ByteBufCodecs.INT,
        StructureScannerRangeSyncPacket::rangeY,
        ByteBufCodecs.INT,
        StructureScannerRangeSyncPacket::rangeZ,
        StructureScannerRangeSyncPacket::new
    );

    @Override
    public Type<StructureScannerRangeSyncPacket> type() {
        return TYPE;
    }

    @Override
    public void handleOnClient(Player player) {
        if (Minecraft.getInstance().screen == null) {
            return;
        }
        
        if (player.containerMenu instanceof StructureScannerMenu menu) {
            StructureScannerBlockEntity blockEntity = menu.getBlockEntity();
            if (blockEntity == null) {
                return;
            }
            
            // 更新客户端的范围值
            blockEntity.getRangeX().fromIndex(rangeX);
            blockEntity.getRangeY().fromIndex(rangeY);
            blockEntity.getRangeZ().fromIndex(rangeZ);
        }
    }
}
