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

public record SmartBlockPlacerLayerPacket(int layer) implements IServerboundPacket {
    public static final Type<SmartBlockPlacerLayerPacket> TYPE = IPacket.type(
        AnvilCraft.of("smart_block_placer_layer")
    );
    public static final StreamCodec<ByteBuf, SmartBlockPlacerLayerPacket> STREAM_CODEC = StreamCodec.composite(
        ByteBufCodecs.INT,
        SmartBlockPlacerLayerPacket::layer,
        SmartBlockPlacerLayerPacket::new
    );

    @Override
    public Type<SmartBlockPlacerLayerPacket> type() {
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
        blockEntity.setSelectedLayer(this.layer);
    }
}
