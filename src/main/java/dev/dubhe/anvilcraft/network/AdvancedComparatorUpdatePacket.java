package dev.dubhe.anvilcraft.network;

import dev.anvilcraft.lib.v2.network.packet.IPacket;
import dev.anvilcraft.lib.v2.network.packet.IServerboundPacket;
import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.block.entity.AdvancedComparatorBlockEntity;
import dev.dubhe.anvilcraft.inventory.AdvancedComparatorMenu;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.entity.player.Player;

public record AdvancedComparatorUpdatePacket(
    byte compareMode,
    boolean outputInvert,
    boolean redstoneControl,
    int highLimit,
    int lowLimit,
    int inputSignal
) implements IServerboundPacket {
    public static final Type<AdvancedComparatorUpdatePacket> TYPE = IPacket.type(AnvilCraft.of("advanced_comparator_update"));
    public static final StreamCodec<ByteBuf, AdvancedComparatorUpdatePacket> STREAM_CODEC = StreamCodec.composite(
        ByteBufCodecs.BYTE,
        AdvancedComparatorUpdatePacket::compareMode,
        ByteBufCodecs.BOOL,
        AdvancedComparatorUpdatePacket::outputInvert,
        ByteBufCodecs.BOOL,
        AdvancedComparatorUpdatePacket::redstoneControl,
        ByteBufCodecs.VAR_INT,
        AdvancedComparatorUpdatePacket::highLimit,
        ByteBufCodecs.VAR_INT,
        AdvancedComparatorUpdatePacket::lowLimit,
        ByteBufCodecs.VAR_INT,
        AdvancedComparatorUpdatePacket::inputSignal,
        AdvancedComparatorUpdatePacket::new
    );

    @Override
    public Type<AdvancedComparatorUpdatePacket> type() {
        return AdvancedComparatorUpdatePacket.TYPE;
    }

    @Override
    public void handleOnServer(Player player) {
        if (!player.hasContainerOpen()) return;
        if (!(player.containerMenu instanceof AdvancedComparatorMenu menu)) return;
        AdvancedComparatorBlockEntity repeater = menu.getBlockEntity();
        repeater.setCompareMode(AdvancedComparatorBlockEntity.Mode.fromIndex(this.compareMode));
        repeater.setOutputInvert(this.outputInvert);
        repeater.setRedstoneControl(this.redstoneControl);
        repeater.setHighLimit(this.highLimit);
        repeater.setLowLimit(this.lowLimit);
        repeater.setInputtingSignal(this.inputSignal);
    }
}
