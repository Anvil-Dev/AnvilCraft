package dev.dubhe.anvilcraft.network;

import dev.anvilcraft.lib.v2.codec.StreamCodecUtil;
import dev.anvilcraft.lib.v2.network.packet.IPacket;
import dev.anvilcraft.lib.v2.network.packet.IServerboundPacket;
import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.inventory.ItemDetectorMenu;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.entity.player.Player;

import static dev.dubhe.anvilcraft.block.entity.ItemDetectorBlockEntity.Mode;

public record MachineCycleFilterModePacket(Mode filterMode) implements IServerboundPacket {
    public static final Type<MachineCycleFilterModePacket> TYPE = IPacket.type(AnvilCraft.of("machine_cycle_filter_mode"));
    public static final StreamCodec<ByteBuf, MachineCycleFilterModePacket> STREAM_CODEC = StreamCodec.composite(
        StreamCodecUtil.enumStreamCodec(Mode.class),
        MachineCycleFilterModePacket::filterMode,
        MachineCycleFilterModePacket::new
    );

    @Override
    public Type<MachineCycleFilterModePacket> type() {
        return TYPE;
    }

    @Override
    public void handleOnServer(Player player) {
        if (!(player.containerMenu instanceof ItemDetectorMenu menu)) return;
        menu.setFilterMode(this.filterMode);
        menu.flush();
    }
}
