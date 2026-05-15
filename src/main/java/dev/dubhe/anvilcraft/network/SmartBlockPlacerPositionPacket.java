package dev.dubhe.anvilcraft.network;

import dev.anvilcraft.lib.v2.network.packet.IPacket;
import dev.anvilcraft.lib.v2.network.packet.IServerboundPacket;
import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.block.entity.SmartBlockPlacerBlockEntity;
import dev.dubhe.anvilcraft.inventory.SmartBlockPlacerMenu;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.entity.player.Player;

public record SmartBlockPlacerPositionPacket(int layer, int position, boolean selected) implements IServerboundPacket {
    public static final Type<SmartBlockPlacerPositionPacket> TYPE = IPacket.type(
        AnvilCraft.of("smart_block_placer_position")
    );
    public static final StreamCodec<ByteBuf, SmartBlockPlacerPositionPacket> STREAM_CODEC = StreamCodec.composite(
        ByteBufCodecs.INT,
        SmartBlockPlacerPositionPacket::layer,
        ByteBufCodecs.INT,
        SmartBlockPlacerPositionPacket::position,
        ByteBufCodecs.BOOL,
        SmartBlockPlacerPositionPacket::selected,
        SmartBlockPlacerPositionPacket::new
    );

    @Override
    public Type<SmartBlockPlacerPositionPacket> type() {
        return TYPE;
    }

    @Override
    public void handleOnServer(Player player) {
        if (!(player.containerMenu instanceof SmartBlockPlacerMenu menu)) {
            return;
        }
        SmartBlockPlacerBlockEntity blockEntity = menu.getBlockEntity();
        if (blockEntity == null) {
            return;
        }
        
        // 验证数据范围，防止恶意客户端发送非法数据
        // layer 必须在 0-4 范围内（5层配置）
        if (this.layer < 0 || this.layer > 4) {
            AnvilCraft.LOGGER.warn(
                "Player {} attempted to set invalid layer {} for SmartBlockPlacer at {}",
                player.getName().getString(),
                this.layer,
                blockEntity.getBlockPos()
            );
            return;
        }
        
        // position 必须在 0-24 范围内（5x5网格）
        if (this.position < 0 || this.position > 24) {
            AnvilCraft.LOGGER.warn(
                "Player {} attempted to set invalid position {} for SmartBlockPlacer at {}",
                player.getName().getString(),
                this.position,
                blockEntity.getBlockPos()
            );
            return;
        }
        
        blockEntity.togglePosition(this.layer, this.position, this.selected);
    }
}
