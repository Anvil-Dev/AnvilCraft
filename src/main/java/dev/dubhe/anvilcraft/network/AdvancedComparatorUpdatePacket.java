package dev.dubhe.anvilcraft.network;

import dev.anvilcraft.lib.v2.network.packet.IPacket;
import dev.anvilcraft.lib.v2.network.packet.IServerboundPacket;
import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.block.AdvancedComparatorBlock;
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
    int lowLimit
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
        AdvancedComparatorUpdatePacket::new
    );

    @Override
    public Type<AdvancedComparatorUpdatePacket> type() {
        return AdvancedComparatorUpdatePacket.TYPE;
    }

    @Override
    public void handleOnServer(Player player) {
        if (player.isSpectator() || this.compareMode < 0 || this.compareMode >= AdvancedComparatorBlockEntity.Mode.values().length) {
            return;
        }
        if (!(player.containerMenu instanceof AdvancedComparatorMenu menu) || !menu.stillValid(player)) {
            return;
        }
        AdvancedComparatorBlockEntity comparator = menu.getBlockEntity();
        comparator.setCompareMode(AdvancedComparatorBlockEntity.Mode.fromIndex(this.compareMode));
        comparator.setOutputInvert(this.outputInvert);
        comparator.setRedstoneControl(this.redstoneControl);
        comparator.setHighLimit(this.highLimit);
        comparator.setLowLimit(this.lowLimit);
        comparator.setChanged();
        player.level().scheduleTick(comparator.getBlockPos(), comparator.getBlockState().getBlock(), AdvancedComparatorBlock.getDelay());
    }
}
