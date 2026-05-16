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

public record SmartBlockPlacerModePacket(boolean pickupMode) implements IServerboundPacket {
    public static final Type<SmartBlockPlacerModePacket> TYPE = IPacket.type(
        AnvilCraft.of("smart_block_placer_mode")
    );
    public static final StreamCodec<ByteBuf, SmartBlockPlacerModePacket> STREAM_CODEC = StreamCodec.composite(
        ByteBufCodecs.BOOL,
        SmartBlockPlacerModePacket::pickupMode,
        SmartBlockPlacerModePacket::new
    );

    @Override
    public Type<SmartBlockPlacerModePacket> type() {
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
        blockEntity.setPickupMode(this.pickupMode);
    }
}
