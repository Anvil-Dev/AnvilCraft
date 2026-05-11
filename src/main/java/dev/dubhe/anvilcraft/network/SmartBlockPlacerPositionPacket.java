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
        if (!(player.containerMenu instanceof SmartBlockPlacerMenu menu)) return;
        SmartBlockPlacerBlockEntity blockEntity = menu.getBlockEntity();
        if (blockEntity == null) return;
        blockEntity.togglePosition(this.layer, this.position, this.selected);
    }
}
